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
    val taskId = assertHasBeenAddedToWendy()

    PendingTasks.shared.recordError(taskId, humanReadableErrorMessage, errorId)

    return PendingTaskResult.FAILED
}

/**
 * Extension to [PendingTasks.resolveError] easily from a [PendingTask] instance.
 */
fun PendingTask.resolveError() {
    val taskId = assertHasBeenAddedToWendy()
    PendingTasks.shared.resolveError(taskId)
}

/**
 * Extension to [PendingTasks.getLatestError] easily from a [PendingTask] instance.
 */
fun PendingTask.getLatestError(): PendingTaskError? {
    val taskId = assertHasBeenAddedToWendy()
    return PendingTasks.shared.getLatestError(taskId)
}

/**
 * Extension to [WendyConfig.addTaskStatusListenerForTask] easily from a [PendingTask] instance.
 */
fun PendingTask.addTaskStatusListenerForTask(listener: PendingTaskStatusListener) {
    val taskId = assertHasBeenAddedToWendy()
    WendyConfig.addTaskStatusListenerForTask(taskId, listener)
}

/**
 * Checks to see if the [PendingTask] has been added to Wendy yet.
 *
 * This function simply checks if the [PendingTask.task_id] is null or not.
 */
fun PendingTask.hasBeenAddedToWendy(): Boolean = this.task_id != null

internal fun PendingTask.assertHasBeenAddedToWendy(): Long {
    if (!hasBeenAddedToWendy()) throw RuntimeException("Cannot record error for your task because it has not been added to Wendy (aka: the task id has not been set yet)")

    return this.task_id!!
}