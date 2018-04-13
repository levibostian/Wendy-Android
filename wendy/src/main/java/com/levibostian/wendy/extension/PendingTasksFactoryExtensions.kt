package com.levibostian.wendy.extension

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasksFactory

internal fun PendingTasksFactory.getTaskAssertPopulated(tag: String): PendingTask {
    return this.getTask(tag) ?: throw RuntimeException("You forgot to add $tag to your ${PendingTasksFactory::class.java.simpleName}.")
}