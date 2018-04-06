package com.levibostian.wendy.extension

import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.service.Wendy

/**
 * Extension to [Wendy.resolveError] easily from a [PendingTaskError] instance.
 */
fun PendingTaskError.resolveError() = Wendy.shared.resolveError(this.taskId)