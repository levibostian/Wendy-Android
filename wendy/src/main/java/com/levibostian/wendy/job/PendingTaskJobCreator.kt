package com.levibostian.wendy.job

import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator

internal class PendingTaskJobCreator : JobCreator {

    override fun create(tag: String): Job? {
        return when (tag) {
            PendingTasksJob.TAG -> PendingTasksJob()
            else -> null
        }
    }

}