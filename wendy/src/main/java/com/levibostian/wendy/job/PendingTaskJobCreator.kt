package com.levibostian.wendy.job

import android.content.Context
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import com.levibostian.wendy.service.PendingTasksManager

internal class PendingTaskJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            PendingTasksJob.TAG -> PendingTasksJob()
            else -> null
        }
    }

}