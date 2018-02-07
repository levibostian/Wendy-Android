package com.levibostian.wendy.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import com.levibostian.wendy.service.PendingTasksManager

internal class PendingTaskJobCreator(private val context: Context, private val tasksManager: PendingTasksManager) : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            PendingTasksJob.TAG -> PendingTasksJob(context, tasksManager)
            else -> null
        }
    }

}