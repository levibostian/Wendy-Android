package com.levibostian.wendy.job

import android.content.Context
import com.evernote.android.job.JobRequest
import com.evernote.android.job.Job.Params
import com.evernote.android.job.Job
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.service.PendingTasksManager
import com.levibostian.wendy.service.PendingTasksRunner
import java.util.concurrent.TimeUnit

internal class PendingTasksJob : Job() {

    override fun onRunJob(params: Params): Job.Result {
        runTheJob()

        return Job.Result.SUCCESS
    }

    private fun runTheJob() {
        PendingTasks.sharedInstance().runTasks()
    }

    companion object {
        val TAG = "WendyPendingTasksJob"

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