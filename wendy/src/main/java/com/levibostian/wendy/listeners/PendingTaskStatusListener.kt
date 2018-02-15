package com.levibostian.wendy.listeners

import com.levibostian.wendy.types.ReasonPendingTaskSkipped

interface PendingTaskStatusListener {
    fun running(taskId: Long)
    fun complete(taskId: Long, successful: Boolean)
    fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) // Currently, the only reason a pending task is skipped is if the task `canRunTask()` returns false. There is currently no way to determine if it's because of a group id skip.
}