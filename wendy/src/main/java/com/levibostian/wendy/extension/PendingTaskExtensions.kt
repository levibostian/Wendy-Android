package com.levibostian.wendy.extension

import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.types.PendingTaskResult

/**
 * Extension to [PendingTasks.recordError] easily from a [PendingTask] instance.
 *
 * @return [PendingTaskResult.FAILED] The extension automatically returns [PendingTaskResult.FAILED] for you so that you can simply call: `return recordError()` in your [PendingTask] to avoid return [PendingTaskResult.SUCCESSFUL] by accident from your [PendingTask].
 */
fun PendingTask.recordError(humanReadableErrorMessage: String?, errorId: String?): PendingTaskResult {
    PendingTasks.shared.recordError(this.task_id, humanReadableErrorMessage, errorId)

    return PendingTaskResult.FAILED
}

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