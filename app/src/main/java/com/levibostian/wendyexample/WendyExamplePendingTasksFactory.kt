package com.levibostian.wendyexample

import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasksFactory


class WendyExamplePendingTasksFactory : PendingTasksFactory {

    override fun getTask(tag: String, task: PendingTask): PendingTask {
        return when (tag) {
            FooPendingTask::class.java.simpleName -> FooPendingTask.blank().fromSqlObject(task)
            else -> throw RuntimeException("No idea what task that is... tag: $tag")
        }
    }

}