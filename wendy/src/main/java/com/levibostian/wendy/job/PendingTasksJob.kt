package com.levibostian.wendy.job

import com.evernote.android.job.JobRequest
import com.evernote.android.job.Job
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.util.LogUtil
import java.util.concurrent.TimeUnit

internal class PendingTasksJob : Job() {

    override fun onRunJob(params: Params): Job.Result {
        runTheJob()

        return Job.Result.SUCCESS
    }

    private fun runTheJob() {
        if (WendyConfig.automaticallyRunTasks) {
            LogUtil.d("Wendy configured to automatically run tasks. Running the periodically scheduled job.")
            PendingTasks.sharedInstance().runTasks()
        } else LogUtil.d("Wendy configured to *not* automatically run tasks. Skipping execution of periodically scheduled job.")
    }

    companion object {
        const val TAG = "WendyPendingTasksJob"

        fun scheduleJob() {
            JobRequest.Builder(PendingTasksJob.TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                    .setRequirementsEnforced(true)
                    .setUpdateCurrent(true)
                    .build()
                    .schedule()
        }
    }
}