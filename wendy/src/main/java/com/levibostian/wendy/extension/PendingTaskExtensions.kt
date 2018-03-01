package com.levibostian.wendy.extension

import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks

/**
 * Extension to [PendingTasks.recordError] easily from a [PendingTask] instance.
 */
fun PendingTask.recordError(humanReadableErrorMessage: String?, errorId: String?) = PendingTasks.shared.recordError(this.task_id, humanReadableErrorMessage, errorId)

/**
 * Extension to [PendingTasks.resolveError] easily from a [PendingTask] instance.
 */
fun PendingTask.resolveError() = PendingTasks.shared.resolveError(this.task_id)

/**
 * Extension to [PendingTasks.getLatestError] easily from a [PendingTask] instance.
 */
fun PendingTask.getLatestError(): PendingTaskError? = PendingTasks.shared.getLatestError(this.task_id)

/**
 * Extension to [WendyConfig.addTaskStatusListenerForTask] easily from a [PendingTask] instance.
 */
fun PendingTask.addTaskStatusListenerForTask(listener: PendingTaskStatusListener) = WendyConfig.addTaskStatusListenerForTask(this.task_id, listener)