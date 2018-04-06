package com.levibostian.wendy.db

import com.levibostian.wendy.service.PendingTask

/**
 * Used to make sure that [PersistedPendingTask] and [PendingTask] both have the same fields.
 */
internal interface PendingTaskFields {
    var createdAt: Long
    var manuallyRun: Boolean
    var groupId: String?
    var dataId: String?
    var tag: String
}