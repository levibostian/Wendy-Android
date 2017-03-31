package com.curiosityio.wendy.runner

import io.realm.Realm

interface WendyTasksRunnerManager {
    fun shouldRunApiTasks(): Boolean
    fun doneRunningTasks(tempInstance: Boolean)
    fun errorRunningTasks(tempInstance: Boolean, error: Throwable)
}