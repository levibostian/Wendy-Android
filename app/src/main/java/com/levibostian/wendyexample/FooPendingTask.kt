package com.levibostian.wendyexample

import android.os.Handler
import android.os.Looper
import com.levibostian.wendy.service.PendingTask
import java.util.*

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
            val rand = Random()
            val n = rand.nextInt(100) + 1 // random number between 1 and 100
            val successful: Boolean = n <= 25 // fail 25% of the time.

            complete(successful)
        }, 1000)
    }

}