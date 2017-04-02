package com.curiosityio.wendy.runner

import com.curiosityio.wendy.model.PendingApiTask
import io.realm.Realm

interface WendyTasksRunnerManager {
    fun shouldRunApiTasks(): Boolean
    fun doneRunningTasks(tempInstance: Boolean)
    fun errorRunningTasks(tempInstance: Boolean, error: Throwable)

    fun <RESPONSE: Any> doneRunningSingleTask(pendingApiTask: PendingApiTask<RESPONSE>)
    fun <RESPONSE: Any> errorRunningSingleTask(pendingApiTask: PendingApiTask<RESPONSE>, error: Throwable)
}