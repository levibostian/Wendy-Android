package com.curiosityio.wendy.model

interface PendingApiModelInterface {

    var api_sync_in_progress: Boolean // use in UI to show progress bar as model is syncing
    var number_pending_api_syncs: Int // used for isPendingApiSync() to indicate if *all* service tasks for syncing this model are complete.

    var deleted: Boolean

    fun isPendingApiSync(): Boolean // use in UI to show that the model is not fully synced yet with API.

}
