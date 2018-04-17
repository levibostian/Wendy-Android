package com.levibostian.wendy.extension

import com.levibostian.wendy.db.PersistedPendingTask
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.Wendy

internal fun PersistedPendingTask.getPendingTask(): PendingTask {
    val tasksFactory = Wendy.sharedInstance().tasksFactory
    return tasksFactory.getTaskAssertPopulated(this.tag).fromSqlObject(this)
}