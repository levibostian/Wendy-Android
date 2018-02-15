package com.levibostian.wendy.service

import android.content.Context
import android.os.Handler
import com.evernote.android.job.JobManager
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.job.PendingTaskJobCreator
import com.levibostian.wendy.job.PendingTasksJob

open class PendingTasks private constructor(context: Context, val tasksFactory: PendingTasksFactory) {

    companion object {
        private var instance: PendingTasks? = null

        @JvmStatic fun init(context: Context, tasksFactory: PendingTasksFactory): PendingTasks {
            if (instance == null) {
                instance = PendingTasks(context, tasksFactory)
                instance!!.init(context)
            }

            return instance!!
        }

        @JvmStatic fun sharedInstance(): PendingTasks {
            if (instance == null) throw RuntimeException("Sorry, you must initialize the instance first.")
            return instance!!
        }
    }

    /**
     * Turn on and off debug log messages from Wendy.
     *
     * Designed like a builder pattern so you can: PendingTasks(this, Factory()).enableDebug()
     */
    fun debug(enableDebug: Boolean = false): PendingTasks {
        WendyConfig.debug = enableDebug
        return this
    }

    private fun init(context: Context) {
        val jobManager = JobManager.create(context)
        jobManager.addJobCreator(PendingTaskJobCreator(context, tasksManager))

        PendingTasksJob.scheduleJob()
    }

    private var tasksManager: PendingTasksManager = PendingTasksManager(context)
    private var runner: PendingTasksRunner = PendingTasksRunner(context, tasksManager)

    /**
     * Call when you have a new PendingTask that you would like to register to Wendy to run.
     */
    open fun addTask(pendingTask: PendingTask): Long {
        val id = tasksManager.addTask(pendingTask)

        runTasks() // Run tasks right now in case this newly added task can run right away.

        return id
    }

    /**
     * Manually run all pending tasks. Wendy takes care of running this periodically for you, but you can manually run tasks here.
     */
    open fun runTasks() {
        PendingTasksRunner.PendingTasksRunnerAsyncTask(runner, tasksManager).execute(null)
    }

    open fun resetTasks() {
        tasksManager.deleteAllTasks()
    }

}