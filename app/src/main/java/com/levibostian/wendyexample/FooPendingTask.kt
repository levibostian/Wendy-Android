package com.levibostian.wendyexample

import android.os.Handler
import android.os.Looper
import com.levibostian.wendy.service.PendingTask

class FooPendingTask(manuallyRun: Boolean,
                     groupId: String?,
                     data: String) : PendingTask(tag = FooPendingTask::class.java.simpleName) {

    init {
        manually_run = manuallyRun
        group_id = groupId
        data_id = data
    }

    companion object {
        fun blank(): FooPendingTask { return FooPendingTask(false, null, "") }
    }

    override fun runTask(complete: (successful: Boolean) -> Unit) {
        // Here, instantiate your dependencies, talk to your DB, your API, etc. Run the task.

        Handler(Looper.getMainLooper()).postDelayed({
            complete(true)
        }, 1000)
    }

}