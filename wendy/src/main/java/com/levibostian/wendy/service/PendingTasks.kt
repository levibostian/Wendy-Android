package com.levibostian.wendy.service

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.support.annotation.WorkerThread
import com.evernote.android.job.JobManager
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.job.PendingTaskJobCreator
import com.levibostian.wendy.job.PendingTasksJob
import com.levibostian.wendy.logNewTaskAdded

open class PendingTasks private constructor(context: Context, val tasksFactory: PendingTasksFactory) {

    companion object {
        private var instance: PendingTasks? = null

        /**
         * Initialize Wendy. It's recommended to do this in your [Application] class of your app.
         *
         * This function essentially just creates a singleton instance of [PendingTasks] for your application to share. Future calls to this function will be ignored. The instance is only initialized once.
         */
        @JvmStatic fun init(context: Context, tasksFactory: PendingTasksFactory): PendingTasks {
            if (instance == null) {
                instance = PendingTasks(context, tasksFactory)
                instance!!.init(context)
            }

            return instance!!
        }

        /**
         * Get singleton instance of [PendingTasks].
         *
         * @throws RuntimeException If you have not called [PendingTasks.init] yet to initialize singleton instance.
         */
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
        jobManager.addJobCreator(PendingTaskJobCreator())

        PendingTasksJob.scheduleJob()
    }

    internal var tasksManager: PendingTasksManager = PendingTasksManager(context)
    internal var runner: PendingTasksRunner = PendingTasksRunner(context, tasksManager)

    /**
     * Call when you have a new [PendingTask] that you would like to register to Wendy to run.
     *
     * Wendy works in a FIFO order. Your task gets added to the end of the queue of tasks to run.
     *
     * @param pendingTask Task you want to add to Wendy.
     *
     * @throws IllegalArgumentException Wendy will check to make sure that you have remembered to add your argument's [PendingTask] subclass to your instance of [PendingTasksFactory] when you call this method. If your [PendingTasksFactory] throws an exception (which probably means that you forgot to include a [PendingTask]) then an [IllegalArgumentException] will be thrown.
     */
    fun addTask(pendingTask: PendingTask): Long {
        try {
            tasksFactory.getTask(pendingTask.tag)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Exception thrown while calling ${tasksFactory::class.java.simpleName}'s getTask(). Did you forgot to add ${pendingTask::class.java.simpleName} to your instance of ${tasksFactory::class.java.simpleName}?")
        }

        val id = tasksManager.addTask(pendingTask)
        WendyConfig.logNewTaskAdded(tasksManager.getTaskForId(id)!!)

        runTask(id) // Run task right now in case this newly added task can run right away.

        return id
    }

    /**
     * Manually run all pending tasks. Wendy takes care of running this periodically for you, but you can manually run tasks here.
     */
    fun runTasks() {
        PendingTasksRunner.PendingTasksRunnerAllTasksAsyncTask(runner, tasksManager).execute(null)
    }

    /**
     * Use this function along with manually run tasks to have Wendy run it for you. If it is successful, Wendy will take care of deleting it for you.
     *
     * @param id ID of [PendingTask] you wish to run. If there is not a [PendingTask] with this ID, the task runner simply ignores your request and moves on.
     *
     * @see [WendyConfig.addTaskStatusListenerForTask] to learn how to listen to the status of a task that you have set to run.
     */
    fun runTask(id: Long) {
        PendingTasksRunner.PendingTasksRunnerGivenSetTasksAsyncTask(runner).execute(id)
    }

    /**
     * If you, for some reason, wish to receive a copy of all the [PendingTask] instances that still need to run successfully by Wendy, here is how you get them.
     */
    open fun getAllTasks(): List<PendingTask> {
        return tasksManager.getAllTasks()
    }

}