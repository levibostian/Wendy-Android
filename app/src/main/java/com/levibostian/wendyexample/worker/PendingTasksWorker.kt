package com.levibostian.wendyexample.worker

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.levibostian.wendy.service.Wendy

class PendingTasksWorker(context: Context, params: WorkerParameters): Worker(context, params) {

    override fun doWork(): Result {
        // Here, we always return SUCCESS. We do not want to return FAILED because then this worker will be rescheduled to run again. We do not want that as we are running this task periodically anyway.
        Wendy.sharedInstance().runTasks(null)

        return Result.SUCCESS
    }

}