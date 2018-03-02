package com.levibostian.wendy.db

import com.levibostian.wendy.service.PendingTask

/**
 * Used to make sure that [PersistedPendingTask] and [PendingTask] both have the same fields.
 */
internal interface PendingTaskFields {
    var task_id: Long
    var created_at: Long
    var manually_run: Boolean
    var group_id: String?
    var data_id: String?
    var tag: String
}