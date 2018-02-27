package com.levibostian.wendy.types

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks

enum class PendingTaskResult {

    /**
     * Indicates the task was run successfully. Wendy will delete the [PendingTask] instance and not run it again.
     */
    SUCCESSFUL,
    /**
     * Indicates the task failed, but it should be run again the next time that Wendy runs tasks.
     */
    FAILED_RESCHEDULE,
    /**
     * Indicates the task failed, and Wendy should not run it again the next time that Wendy runs tasks.
     *
     * This will delete the [PendingTask] instance from the Wendy database to not run again. If you wish to run this task again at some point in the future, you must add it to Wendy again via [PendingTasks.addTask].
     *
     * This result is usually used if the task failed to run and you need the user to perform an action in order to fix it.
     */
    FAILED_DO_NOT_RESCHEDULE;

}