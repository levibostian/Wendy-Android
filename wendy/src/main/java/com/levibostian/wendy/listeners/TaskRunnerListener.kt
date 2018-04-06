package com.levibostian.wendy.listeners

import android.support.annotation.UiThread
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.types.ReasonPendingTaskSkipped

/**
 * Listen to status updates from the Wendy task runner. You will get notified about the status of the task runner as well as general task updates on all of the tasks that it runs.
 *
 * @see PendingTaskStatusListener for an interface to receive callbacks on a specific [PendingTask].
 * @see WendyConfig.addTaskRunnerListener for how to register a listener.
 */
interface TaskRunnerListener {

    /**
     * New task added to Wendy to be run.
     *
     * @param task The [PendingTask] newly added to Wendy.
     */
    @UiThread fun newTaskAdded(task: PendingTask)

    /**
     * The task runner is now running this given task.
     *
     * @param task The [PendingTask] being run by the task runner.
     */
    @UiThread fun runningTask(task: PendingTask)

    /**
     * If the task runner decides to skip the given [PendingTask] for some reason.
     *
     * @param reason The reason why the task was skipped.
     * @param task The [PendingTask] the was skipped.
     */
    @UiThread fun taskSkipped(reason: ReasonPendingTaskSkipped, task: PendingTask)

    /**
     * There was an error recorded to Wendy.
     *
     * @param task The [PendingTask] that had an error occur to.
     * @param errorMessage The human readable error message recorded to Wendy.
     * @param errorId The error ID recorded to Wendy.
     */
    @UiThread fun errorRecorded(task: PendingTask, errorMessage: String?, errorId: String?)

    /**
     * A previously recorded error for a [PendingTask] has been marked as resolved.
     *
     * @param task the [PendingTask] that the recorded error has been marked as resolved.
     */
    @UiThread fun errorResolved(task: PendingTask)

    /**
     * Task has either successfully run or failed it's run by the task runner.
     *
     * @param success Indicates if the task that was run by the task runner was run successfully or not.
     * @param task The [PendingTask] that was run.
     * @param rescheduled If the task failed but should run again, the task is rescheduled to run again in the future.
     */
    @UiThread fun taskComplete(success: Boolean, task: PendingTask, rescheduled: Boolean)

    /**
     * The task runner has completed running all of it's tasks.
     */
    @UiThread fun allTasksComplete()

}