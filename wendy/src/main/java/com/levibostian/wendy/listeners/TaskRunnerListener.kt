package com.levibostian.wendy.listeners

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.types.ReasonPendingTaskSkipped

interface TaskRunnerListener {
    fun newTaskAdded(id: Long)
    fun runningTask(task: PendingTask)
    fun taskSkipped(reason: ReasonPendingTaskSkipped, task: PendingTask)
    fun taskComplete(success: Boolean, task: PendingTask)
    fun allTasksComplete()
    fun allTasksReset()
}