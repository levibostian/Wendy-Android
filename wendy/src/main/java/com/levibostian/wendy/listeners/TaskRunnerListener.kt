package com.levibostian.wendy.listeners

import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
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
     * @param id The ID of the [PendingTask] added to Wendy.
     */
    fun newTaskAdded(id: Long)

    /**
     * The task runner is now running this given task.
     *
     * @param task The [PendingTask] being run by the task runner.
     */
    fun runningTask(task: PendingTask)

    /**
     * If the task runner decides to skip the given [PendingTask] for some reason.
     *
     * @param reason The reason why the task was skipped.
     * @param task The [PendingTask] the was skipped.
     */
    fun taskSkipped(reason: ReasonPendingTaskSkipped, task: PendingTask)

    /**
     * Task has either successfully run or failed it's run by the task runner.
     *
     * @param success Indicates if the task that was run by the task runner was run successfully or not.
     * @param task The [PendingTask] that was run.
     */
    fun taskComplete(success: Boolean, task: PendingTask)

    /**
     * The task runner has completed running all of it's tasks.
     */
    fun allTasksComplete()

}