package com.levibostian.wendy.listeners

import androidx.annotation.UiThread
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.service.PendingTask
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
     * @param taskId The taskId of the [PendingTask] being run.
     */
    @UiThread fun running(taskId: Long)

    /**
     * The task runner is done running the [PendingTask]. The task was either successful or not.
     *
     * @param taskId The taskId of the [PendingTask] that just ran.
     * @param successful Indicates if the running of the [PendingTask] was successful or not.
     */
    @UiThread fun complete(taskId: Long, successful: Boolean)

    /**
     * There was an error recorded to Wendy for this [PendingTask].
     *
     * *Tip:* It's recommended that when [errorRecorded] gets called and if you decide to show something in the UI about a [PendingTask] having an error, keep that UI up until the user dismisses it. They touch it, swipe it, whatever. Why? Because other methods may get called after [errorRecorded] gets called and errors are a pretty important thing to fix.
     *
     * @param taskId the taskId of the [PendingTask] that an error occurred to.
     * @param errorMessage The human readable error message recorded for the error.
     * @param errorId The error ID recorded to Wendy for the error.
     */
    @UiThread fun errorRecorded(taskId: Long, errorMessage: String?, errorId: String?)

    /**
     * A previously recorded error for this [PendingTask] has been marked as resolved.
     *
     * @param taskId The taskId of the [PendingTask] that the error has been resolved for.
     */
    @UiThread fun errorResolved(taskId: Long)

    /**
     * The task runner skipped running the [PendingTask] for some reason.
     *
     * @param taskId The taskId of the [PendingTask] that was skipped.
     * @param reason The reason that the [PendingTask] was skipped.
     *
     * @see ReasonPendingTaskSkipped for all available reasons why a [PendingTask] was skipped.
     */
    @UiThread fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) // Currently, the only reason a pending task is skipped is if the task `isReadyToRun()` returns false. There is currently no way to determine if it's because of a group id skip.
}