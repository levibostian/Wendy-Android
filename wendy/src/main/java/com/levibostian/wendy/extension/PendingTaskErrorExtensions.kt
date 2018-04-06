package com.levibostian.wendy.extension

import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.service.PendingTasks

/**
 * Extension to [PendingTasks.resolveError] easily from a [PendingTaskError] instance.
 */
fun PendingTaskError.resolveError() = PendingTasks.shared.resolveError(this.taskId)