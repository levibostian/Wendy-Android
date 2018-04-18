package com.levibostian.wendy.types

import com.levibostian.wendy.service.PendingTask

/**
 * Filter the Wendy task runner to only run [PendingTask]s with the following options.
 *
 * @param groupId Filter running all of the [PendingTask]s in the task runner by this specific groupId.
 */
class RunAllTasksFilter(val groupId: String?)