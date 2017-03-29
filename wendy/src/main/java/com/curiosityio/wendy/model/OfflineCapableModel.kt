package com.curiosityio.wendy.model

interface OfflineCapableModel {

    var realm_id: Long
    var api_id: Long

    // GSON does not allow you to have 2+ fields of same name so you must manually set the realm_id from api_id here so they both match. This function is meant to set realm_id = api_id for parent and sift down children.
    fun setRealmIdToApiId() // realm_id = api_id

    // use in UI to show progress bar as model is syncing
    var api_sync_in_progress: Boolean // = false

    // used for isPendingApiSync() to indicate if *all* service tasks for syncing this model are complete.
    var number_pending_api_syncs: Int // = 0
    var deleted: Boolean // = false

    // use in UI to show that the model is not fully synced yet with API.
    fun isPendingApiSync(): Boolean // if (number_pending_api_syncs > 0) return true else iterateChildren().isPendingApiSync

    fun <TASK> statusUpdate(task: TASK, status: PendingApiModelInterfaceStatus, error: Throwable? = null) where TASK: PendingApiTask<out Any>

}

enum class PendingApiModelInterfaceStatus {
    RUNNING {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitRunning()
    },
    SUCCESS {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitSuccess()
    },
    ERROR {
        override fun <E> accept(visitor: Visitor<E>): E = visitor.visitError()
    };

    interface Visitor<out E> {
        fun visitRunning(): E
        fun visitSuccess(): E
        fun visitError(): E
    }

    abstract fun <E> accept(visitor: Visitor<E>): E
}
