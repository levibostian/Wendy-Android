package com.levibostian.wendy.listeners

import android.support.annotation.UiThread
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.types.PendingTaskResult
import com.levibostian.wendy.types.ReasonPendingTaskSkipped

/**
 * Listen to status updates on a specific [PendingTask] instance you care to listen to updates about.
 *
 * This is usually used when you want to show UI updates in your app on a task. Show your user when a task needs to run, when it runs successfully, when it's skipped.
 *
 * @see TaskRunnerListener for a more generic listener to listen to updates on the Wendy task runner.
 * @see WendyConfig.addTaskStatusListenerForTask for how to register a listener of this interface.
 */
interface PendingTaskStatusListener {

    /**
     * The task runner is running this exact [PendingTask].
     *
     * @param taskId The task_id of the [PendingTask] being run.
     */
    @UiThread fun running(taskId: Long)

    /**
     * The task runner is done running the [PendingTask]. The task was either successful or not.
     *
     * @param taskId The task_id of the [PendingTask] that just ran.
     * @param successful Indicates if the running of the [PendingTask] was successful or not.
     * @param rescheduled If the task failed but should run again, the task is rescheduled to run again in the future.
     */
    @UiThread fun complete(taskId: Long, successful: Boolean, rescheduled: Boolean)

    /**
     * There was an error recorded to Wendy for this [PendingTask].
     *
     * @param taskId the task_id of the [PendingTask] that an error occurred to.
     * @param errorMessage The human readable error message recorded for the error.
     * @param errorId The error ID recorded to Wendy for the error.
     */
    @UiThread fun errorRecorded(taskId: Long, errorMessage: String?, errorId: String?)

    /**
     * A previously recorded error for this [PendingTask] has been marked as resolved.
     *
     * @param taskId The task_id of the [PendingTask] that the error has been resolved for.
     */
    @UiThread fun errorResolved(taskId: Long)

    /**
     * The task runner skipped running the [PendingTask] for some reason.
     *
     * @param taskId The task_id of the [PendingTask] that was skipped.
     * @param reason The reason that the [PendingTask] was skipped.
     *
     * @see ReasonPendingTaskSkipped for all available reasons why a [PendingTask] was skipped.
     */
    @UiThread fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) // Currently, the only reason a pending task is skipped is if the task `canRunTask()` returns false. There is currently no way to determine if it's because of a group id skip.
}