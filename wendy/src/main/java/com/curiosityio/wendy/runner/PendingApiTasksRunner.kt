package com.curiosityio.wendy.runner

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.os.NetworkOnMainThreadException
import android.security.keystore.UserNotAuthenticatedException
import android.support.v4.app.NotificationCompat
import com.curiosityio.androidboilerplate.manager.SharedPreferencesManager
import com.curiosityio.androidboilerplate.util.InternetConnectionUtil
import com.curiosityio.androidboilerplate.util.ThreadUtil
import com.google.gson.Gson

import com.curiosityio.androidboilerplate.util.LogUtil
import com.curiosityio.androidboilerplate.util.PermissionUtil
import com.curiosityio.androidrealm.extensions.findFirstOrNull
import com.curiosityio.androidrealm.manager.RealmInstanceManager
import com.curiosityio.wendy.R
import com.curiosityio.wendy.config.WendyConfig
import com.curiosityio.wendy.error.*
import com.curiosityio.wendy.manager.PendingApiTasksManager
import com.curiosityio.wendy.model.OfflineCapableModel
import com.curiosityio.wendy.model.PendingApiTask
import com.curiosityio.wendy.vo.ErrorResponseVo
import io.realm.Realm
import okhttp3.Headers
import retrofit2.Response
import rx.schedulers.Schedulers
import java.io.IOException
import java.util.*

import io.realm.RealmObject
import io.realm.Sort
import retrofit2.Retrofit
import rx.*
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

open class PendingApiTasksRunner(val context: Context) {

    var lastFailedApiTaskCreatedAtTime: Date? = null // when an API task fails because of an API error (user error from API), we keep track of that created time to allow us to skip this parent and move onto the next group of tasks.

    @Volatile private var currentlyRunningTasks = false
    @Volatile var numberPendingApiTasksRemaining: BehaviorSubject<Long>? = null

    fun getNumberPendingApiTasksOnce(): Long {
        return numberPendingApiTasksRemaining?.value ?: getNumberPendingApiTasksRemaining()
    }

    private fun getNumberPendingApiTasksRemaining(): Long {
        val realm: Realm = RealmInstanceManager.getInstance()

        var numberPendingTasks: Long = 0
        PendingApiTasksManager.registeredPendingApiTasks.forEach { pendingApiTaskClass ->
            numberPendingTasks += realm.where(pendingApiTaskClass as Class<RealmObject>).count()
        }
        realm.close()
        return numberPendingTasks
    }

    @Synchronized fun runPendingTasks(): Completable {
        if (currentlyRunningTasks || !(WendyConfig.wendyTasksRunnerManager?.shouldRunApiTasks() ?: true)) {
            return Completable.complete().subscribeOn(Schedulers.io())
        } else {
            currentlyRunningTasks = true

            return Completable.create { subscriber ->
                fun getNextTaskToRun(realm: Realm): PendingApiTask<Any>? {
                    if (numberPendingApiTasksRemaining == null) numberPendingApiTasksRemaining = BehaviorSubject.create(getNumberPendingApiTasksRemaining())
                    numberPendingApiTasksRemaining!!.onNext(getNumberPendingApiTasksRemaining())

                    PendingApiTasksManager.registeredPendingApiTasks.forEach { pendingApiTaskClass ->
                        var getAllPendingApiTasksQuery = realm.where(pendingApiTaskClass as Class<RealmObject>)

                        if (lastFailedApiTaskCreatedAtTime != null) {
                            getAllPendingApiTasksQuery = getAllPendingApiTasksQuery.greaterThan("created_at", lastFailedApiTaskCreatedAtTime)
                        }

                        getAllPendingApiTasksQuery.findAllSorted("created_at", Sort.ASCENDING).let { allPendingApiTasks ->
                            allPendingApiTasks.forEach { apiTask ->
                                if ((apiTask as PendingApiTask<Any>).canRunTask(realm)) {
                                    return apiTask
                                }
                            }
                        }
                    }
                    return null
                }

                fun runNextPendingApiTask() {
                    fun stopRunningApiTasks(error: Throwable? = null) {
                        lastFailedApiTaskCreatedAtTime = null
                        currentlyRunningTasks = false
                        if (error != null) subscriber.onError(error) else subscriber.onCompleted()
                    }

                    val realm: Realm = RealmInstanceManager.getInstance()
                    val nextTaskToRun = getNextTaskToRun(realm)

                    if (nextTaskToRun == null) {
                        realm.close()
                        stopRunningApiTasks()
                    } else {
                        val apiSyncController = realm.copyFromRealm(nextTaskToRun as RealmObject) as PendingApiTask<Any>

                        realm.close()
                        runTask(apiSyncController).subscribe({
                            runNextPendingApiTask()
                        }, { error ->
                            if (error is NoInternetConnectionException || error is APIDownException) {
                                stopRunningApiTasks(error)
                            } else {
                                // If API comes back with error, we don't want to block *all* API tasks. Skip this parent and all it's children and move onto the next.
                                lastFailedApiTaskCreatedAtTime = apiSyncController.created_at
                                runNextPendingApiTask()
                            }
                        })
                    }
                }

                runNextPendingApiTask()
            }.subscribeOn(Schedulers.io())
        }
    }

    private fun runTask(pendingApiTaskController: PendingApiTask<Any>): Completable {
        return Completable.create { subscriber ->

            var apiCall: Observable<Response<Any>>? = null
            RealmInstanceManager.getInstance().executeTransaction { realm ->
                pendingApiTaskController.getModelPendingApiTaskRepresents(realm).api_sync_in_progress = true

                apiCall = pendingApiTaskController.getApiCall(realm)
            }

            if (apiCall != null) {
                // We save this to compare later on. If created_at times dont line up, then the model has been edited since API call triggered.
                SharedPreferencesManager.edit(context).setLong(context.getString(R.string.preferences_current_api_sync_task_created_at), pendingApiTaskController.created_at.time).commit()

                executeApiCall(apiCall!!, pendingApiTaskController.getApiErrorVo()).subscribe({ response ->
                    RealmInstanceManager.getInstance().executeTransaction { realm ->
                        (response as? OfflineCapableModel)?.setRealmIdToApiId()
                        pendingApiTaskController.processApiResponse(realm, response)

                        val managedModelPendingApiTaskRepresents = pendingApiTaskController.getModelPendingApiTaskRepresents(realm)
                        managedModelPendingApiTaskRepresents.api_sync_in_progress = false

                        // If created_at times before and after API call are the same, the model has not been updated by user action and we can safely delete this task and not run again. (update tasks can update during API call)
                        val managedOriginalPendingApiTask: PendingApiTask<Any> = pendingApiTaskController.buildQueryForExistingTask(realm.where(pendingApiTaskController.javaClass as Class<RealmObject>)).findFirstOrNull()!! as PendingApiTask<Any>
                        if (SharedPreferencesManager.getLong(context, context.getString(R.string.preferences_current_api_sync_task_created_at)) == managedOriginalPendingApiTask.created_at.time) {
                            (managedOriginalPendingApiTask as RealmObject).deleteFromRealm()
                            managedModelPendingApiTaskRepresents.number_pending_api_syncs -= 1
                        }
                    }

                    subscriber.onCompleted()
                }, { error ->
                    RealmInstanceManager.getInstance().executeTransaction { realm ->
                        pendingApiTaskController.getModelPendingApiTaskRepresents(realm).api_sync_in_progress = false
                    }

                    subscriber.onError(error)
                })
            } else {
                subscriber.onCompleted()
            }
        }
    }

    protected fun <RESPONSE> executeApiCall(call: Observable<Response<RESPONSE>>, errorVo: Class<out ErrorResponseVo>): Single<RESPONSE> {
        return Single.create { subscriber ->
            if (!InternetConnectionUtil.isAnyInternetConnected(context)) {
                subscriber.onError(NoInternetConnectionException("No Internet connection."))
            } else {
                call.toSingle().subscribe({ response ->
                    processApiResponse(response, errorVo).subscribe({ apiSuccessResponse ->
                        subscriber.onSuccess(apiSuccessResponse.response)
                    }, { error -> subscriber.onError(error) })
                }, { error ->
                    if (error is NetworkOnMainThreadException) {
                        throw RuntimeException("Running network on main thread exception ")
                    }

                    subscriber.onError(error)
                })
            }
        }
    }

    class ApiResponse<out RESPONSE>(val response: RESPONSE, val headers: Headers)
    // Retrofit considers a success differently then I do. Therefore, I have to check the response code to decide for myself.
    protected fun <RESPONSE> processApiResponse(response: Response<RESPONSE>, errorVo: Class<out ErrorResponseVo>): Single<ApiResponse<RESPONSE>> {
        return Single.create { subscriber ->
            if (response.isSuccessful) {
                WendyConfig.wendyProcessApiResponse?.success(response.body() as Any, response.headers())
                subscriber.onSuccess(ApiResponse(response.body(), response.headers()))
            } else {
                val userProcessedError = WendyConfig.wendyProcessApiResponse?.error(response.code(), response.errorBody(), response.headers())

                if (userProcessedError != null) {
                    subscriber.onError(userProcessedError)
                } else {
                    if (response.code() >= 500) {
                        val error = APIDownException("The system is currently down. Come back later and try again.")
                        WendyConfig.wendyErrorNotifier?.errorEncountered(error)
                        subscriber.onError(error)
                    } else if (response.code() == 403) {
                        val error = UserNotEnoughPrivilegesException("You do not have enough privileges to continue.")
                        WendyConfig.wendyErrorNotifier?.errorEncountered(error)
                        subscriber.onError(error)
                    } else if (response.code() == 401) {
                        subscriber.onError(UserUnauthorizedException("Unauthorized"))
                    } else {
                        try {
                            val parsedErrorMessageFromAPI = Gson().fromJson(response.errorBody().charStream(), errorVo).errorMessageToDisplayToUser
                            subscriber.onError(UserApiError(parsedErrorMessageFromAPI))
                        } catch (e: Exception) {
                            WendyConfig.wendyErrorNotifier?.errorEncountered(e)
                            subscriber.onError(ParseErrorFromAPIException("Unknown error. Sorry, try again."))
                        }
                    }
                }
            }
        }
    }

}
