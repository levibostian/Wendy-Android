package com.levibostian.wendyexample

import android.os.Handler
import android.os.Looper
import com.levibostian.wendy.extension.recordError
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.types.PendingTaskResult
import java.util.*

class FooPendingTask(manuallyRun: Boolean,
                     groupId: String?,
                     data: String) : PendingTask(manuallyRun, data, groupId, FooPendingTask::class.java.simpleName) {

    companion object {
        fun blank(): FooPendingTask { return FooPendingTask(false, null, "") }

        const val RANDOMLY_GENERATED_ERROR_ERROR_ID = "randomlyGeneratedErrorErrorId"
    }

    override fun runTask(): PendingTaskResult {
        // Here, instantiate your dependencies, talk to your DB, your API, etc. Run the task.

        Thread.sleep(2000)

        val rand = Random()
        val n = rand.nextInt(100) + 1 // random number between 1 and 100
        val successful: Boolean = n <= 50 // fail 50% of the time.
        if (!successful) {
            if (rand.nextInt(100) <= 50) {
                this.recordError("Random error....", RANDOMLY_GENERATED_ERROR_ERROR_ID)
            }
        }

        return if (successful) PendingTaskResult.SUCCESSFUL else PendingTaskResult.FAILED
    }

}