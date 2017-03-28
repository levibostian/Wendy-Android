package com.curiosityio.wendy.manager

import android.content.Context
import com.curiosityio.androidboilerplate.manager.SharedPreferencesManager
import com.curiosityio.androidboilerplate.util.ThreadUtil
import com.curiosityio.androidrealm.extensions.findFirstOrNull
import com.curiosityio.wendy.R
import com.curiosityio.wendy.model.OfflineCapableModel
import com.curiosityio.wendy.model.PendingApiModelInterface
import com.curiosityio.wendy.model.PendingApiTask
import io.realm.Realm
import io.realm.RealmObject
import rx.Completable
import rx.schedulers.Schedulers

abstract class BaseWendyDataManager(val context: Context) {

    @Throws(RuntimeException::class)
    protected fun performRealmTransaction(changeData: Realm.Transaction, realm: Realm = Realm.getDefaultInstance()) {
        if (ThreadUtil.isOnMainThread()) {
            throw RuntimeException("Cannot perform transaction from UI thread.")
        }
        realm.executeTransaction(changeData)
        if (!realm.isClosed) realm.close()
    }

    @Throws(RuntimeException::class)
    protected fun performRealmTransactionCompletable(changeData: Realm.Transaction, realm: Realm = Realm.getDefaultInstance()): Completable {
        return Completable.create { subscriber ->
            if (ThreadUtil.isOnMainThread()) {
                throw RuntimeException("Cannot perform transaction from UI thread.")
            }
            realm.executeTransaction(changeData)
            if (!realm.isClosed) realm.close()

            subscriber.onCompleted()
        }.subscribeOn(Schedulers.io())
    }

    fun getNextAvailableTempModelId(): Long {
        val lastUsedId = SharedPreferencesManager.getLong(context, context.getString(R.string.preferences_last_used_temp_model_id), Long.MAX_VALUE)

        val nextAvailableTempModelId = lastUsedId - 1

        if (SharedPreferencesManager.edit(context).setLong(context.getString(R.string.preferences_last_used_temp_model_id), nextAvailableTempModelId).commit()) {
            return nextAvailableTempModelId
        } else {
            throw RuntimeException("Error saving to device.")
        }
    }

    // surround these create, update, delete undodelete calls in `performRealmTransactionCompletable()`.
    protected fun <MODEL, PENDING_API_TASK_MODEL> createData(realm: Realm, data: MODEL, makeAdditionalRealmChanges: (Realm, MODEL) -> Unit, pendingApiTask: PENDING_API_TASK_MODEL) where MODEL: RealmObject, MODEL: PendingApiModelInterface, MODEL: OfflineCapableModel, PENDING_API_TASK_MODEL: RealmObject, PENDING_API_TASK_MODEL: PendingApiTask<Any> {
        val managedData = realm.copyToRealmOrUpdate(data)

        makeAdditionalRealmChanges(realm, managedData)

        realm.copyToRealm(pendingApiTask)
        managedData.number_pending_api_syncs += 1
    }

    // in future, make the pendingApiTask have have an interface for updating. We want to make sure that
    // only create, update, delete pending API models are calling the appropriate methods.
    protected fun <MODEL> updateData(realm: Realm, modelClass: Class<MODEL>, realmId: Int, updateValues: (MODEL) -> Unit, pendingApiTask: RealmObject) where MODEL: RealmObject, MODEL: PendingApiModelInterface {
        val modelToUpdateValues = realm.where(modelClass).equalTo("realm_id", realmId).findFirstOrNull() ?: throw RuntimeException(modelClass.simpleName + " model to update is null. Cannot find it in Realm.")

        updateValues(modelToUpdateValues)

        pendingApiTask as PendingApiTask<Any>
        val existingPendingApiTask = pendingApiTask.buildQueryForExistingTask(realm.where(pendingApiTask.javaClass as Class<RealmObject>)).findFirstOrNull()
        if (existingPendingApiTask == null) {
            modelToUpdateValues.number_pending_api_syncs += 1
        }
        realm.copyToRealmOrUpdate(pendingApiTask) // updates created_at value which tells pending API task runner to run *another* update on the model.
    }

    protected fun <MODEL> deleteData(realm: Realm, modelClass: Class<MODEL>, realmId: Int, updateValues: (MODEL) -> Unit, pendingApiTask: RealmObject? = null) where MODEL: RealmObject, MODEL: PendingApiModelInterface {
        val modelToUpdateValues = realm.where(modelClass).equalTo("realm_id", realmId).findFirstOrNull() ?: throw RuntimeException(modelClass.simpleName + " model to update is null. Cannot find it in Realm.")

        modelToUpdateValues.deleted = true
        updateValues(modelToUpdateValues)

        pendingApiTask?.let { pendingApiTask ->
            pendingApiTask as PendingApiTask<Any>

            val existingPendingApiTask = pendingApiTask.buildQueryForExistingTask(realm.where(pendingApiTask.javaClass as Class<RealmObject>)).findFirstOrNull()
            if (existingPendingApiTask == null) {
                modelToUpdateValues.number_pending_api_syncs += 1
            }
            realm.copyToRealmOrUpdate(pendingApiTask) // updates created_at value which tells pending API task runner to run *another* update on the model.
        }
    }

    protected fun <MODEL> undoDeleteData(realm: Realm, modelClass: Class<MODEL>, realmId: Int, updateValues: (MODEL) -> Unit) where MODEL: RealmObject, MODEL: PendingApiModelInterface {
        val modelToUpdateValues = realm.where(modelClass).equalTo("realm_id", realmId).findFirstOrNull() ?: throw RuntimeException(modelClass.simpleName + " model to update is null. Cannot find it in Realm.")

        modelToUpdateValues.deleted = false
        updateValues(modelToUpdateValues)
    }

}
