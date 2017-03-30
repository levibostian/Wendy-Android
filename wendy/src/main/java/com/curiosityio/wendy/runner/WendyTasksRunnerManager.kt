package com.curiosityio.wendy.runner

interface WendyTasksRunnerManager {
    fun shouldRunApiTasks(): Boolean
    fun doneRunningTasks(tempInstance: Boolean)
    fun errorRunningTasks(tempInstance: Boolean, error: Throwable)
}