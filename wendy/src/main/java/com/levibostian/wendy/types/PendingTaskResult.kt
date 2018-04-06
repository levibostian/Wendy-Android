package com.levibostian.wendy.types

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks

/**
 * When one of your [PendingTask] subclasses executes [PendingTask.runTask], Wendy requires that it returns a result. You are required to return one of the following results.
 */
enum class PendingTaskResult {

    /**
     * Indicates the task was run successfully.
     *
     * Wendy will delete the [PendingTask] instance and not run it again.
     */
    SUCCESSFUL,
    /**
     * Indicates the task failed.
     *
     * Wendy will attempt to run the task again the next time that Wendy runs tasks.
     *
     * *Note:* If an error occurs that you need your app user to handle, make sure to call [PendingTasks.recordError]. Wendy will skip the execution of the current [PendingTask] for as long as it has a recorded error for it.
     */
    FAILED;

}