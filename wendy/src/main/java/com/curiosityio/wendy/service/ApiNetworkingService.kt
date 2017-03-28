package com.curiosityio.wendy.service

import android.content.Context
import android.os.NetworkOnMainThreadException
import com.curiosityio.androidboilerplate.util.InternetConnectionUtil
import com.curiosityio.wendy.config.WendyConfig
import com.curiosityio.wendy.error.*
import com.curiosityio.wendy.vo.ErrorResponseVo
import com.google.gson.Gson
import okhttp3.Headers
import retrofit2.Response
import rx.Observable
import rx.Single

class ApiNetworkingService {

    companion object {
        fun <RESPONSE> executeApiCall(context: Context, call: Observable<Response<RESPONSE>>, errorVo: Class<out ErrorResponseVo>): Single<RESPONSE> {
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
        fun <RESPONSE> processApiResponse(response: Response<RESPONSE>, errorVo: Class<out ErrorResponseVo>): Single<ApiResponse<RESPONSE>> {
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

}