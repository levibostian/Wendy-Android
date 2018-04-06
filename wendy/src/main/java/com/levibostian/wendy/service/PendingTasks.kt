package com.levibostian.wendy.service

import android.app.Application
import android.content.Context
import com.evernote.android.job.JobManager
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.db.PendingTasksManager
import com.levibostian.wendy.job.PendingTaskJobCreator
import com.levibostian.wendy.job.PendingTasksJob
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.listeners.TaskRunnerListener
import com.levibostian.wendy.logErrorRecorded
import com.levibostian.wendy.logErrorResolved
import com.levibostian.wendy.logNewTaskAdded
import com.levibostian.wendy.util.LogUtil

/**
 * How you interact with Wendy with [PendingTask] instances you create. Add tasks to Wendy to run, get a list of all the [PendingTask]s registered to Wendy, etc.
 */
open class PendingTasks private constructor(context: Context, internal val tasksFactory: PendingTasksFactory) {

    companion object {
        private var instance: PendingTasks? = null

        /**
         * Initialize Wendy. It's recommended to do this in your [Application] class of your app.
         *
         * This function essentially just creates a singleton instance of [PendingTasks] for your application to share. Future calls to this function will be ignored. The instance is only initialized once.
         *
         * @param context Android context (usually given in your Application class's onCreate() call)
         * @param tasksFactory [PendingTasksFactory] instance for your app to construct [PendingTask] instances used by Wendy.
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
         * @throws RuntimeException If you have not called [PendingTasks.Companion.init] yet to initialize singleton instance.
         */
        @JvmStatic fun sharedInstance(): PendingTasks {
            if (instance == null) throw RuntimeException("Sorry, you must initialize the instance first.")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        @JvmStatic val shared: PendingTasks by lazy { sharedInstance() }

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
     * *Note: If you attempt to add a [PendingTask] instance that has the same [PendingTask.tag] and [PendingTask.data_id] as another [PendingTask] already added to Wendy, your request (including calling the task runner listener) will be ignored (except for the exception below in that there was an error recorded previously for a [PendingTask]).*
     *
     * *Note:* If you attempt to add a [PendingTask] instance that has the same [PendingTask.tag] and [PendingTask.data_id] as another [PendingTask] already added to Wendy that previously had an error recorded for it, that error will be marked as resolved (by internally calling [PendingTasks.resolveError].
     *
     * @param pendingTask Task you want to add to Wendy.
     *
     * @throws IllegalArgumentException Wendy will check to make sure that you have remembered to add your argument's [PendingTask] subclass to your instance of [PendingTasksFactory] when you call this method. If your [PendingTasksFactory] throws an exception (which probably means that you forgot to include a [PendingTask]) then an [IllegalArgumentException] will be thrown.
     */
    fun addTask(pendingTask: PendingTask, resolveErrorIfTaskExists: Boolean = true): Long {
        try {
            tasksFactory.getTask(pendingTask.tag)
        } catch (t: Throwable) {
            throw IllegalArgumentException("Exception thrown while calling ${tasksFactory::class.java.simpleName}'s getTask(). Did you forgot to add ${pendingTask::class.java.simpleName} to your instance of ${tasksFactory::class.java.simpleName}?")
        }

        tasksManager.getExistingTask(pendingTask)?.let { existingPersistedPendingTask ->
            if (doesErrorExist(existingPersistedPendingTask.id) && resolveErrorIfTaskExists) {
                resolveError(existingPersistedPendingTask.id)
            }

            return existingPersistedPendingTask.id
        }

        val addedTask = tasksManager.insertPendingTask(pendingTask)
        WendyConfig.logNewTaskAdded(addedTask)

        if (WendyConfig.automaticallyRunTasks && !addedTask.manually_run) {
            LogUtil.d("Wendy is configured to automatically run tasks. Wendy will now attempt to run newly added task: $addedTask")
            runTask(addedTask.task_id!!) // Run task right now in case this newly added task can run right away.
        } else LogUtil.d("Wendy configured to not automatically run tasks. Skipping execution of newly added task: $addedTask")

        return addedTask.task_id!!
    }

    /**
     * Manually run all pending tasks. Wendy takes care of running this periodically for you, but you can manually run tasks here.
     *
     * *Note:* This will run all tasks even if you use [WendyConfig.automaticallyRunTasks] to enable/disable running of all the [PendingTask]s.
     *
     * @param groupId Limit running of the tasks to only tasks of this specific group id.
     */
    fun runTasks(groupId: String? = null) {
        PendingTasksRunner.PendingTasksRunnerAllTasksAsyncTask(runner, tasksManager).execute(PendingTasksRunner.RunAllTasksFilter(groupId))
    }

    /**
     * Use this function along with manually run tasks to have Wendy run it for you. If it is successful, Wendy will take care of deleting it for you.
     *
     * @param taskId The [PendingTask.task_id] of [PendingTask] you wish to run. If there is not a [PendingTask] with this ID, the task runner simply ignores your request and moves on.
     *
     * @see [WendyConfig.addTaskStatusListenerForTask] to learn how to listen to the status of a task that you have set to run.
     */
    fun runTask(taskId: Long) {
        PendingTasksRunner.PendingTasksRunnerGivenSetTasksAsyncTask(runner).execute(taskId)
    }

    /**
     * If you, for some reason, wish to receive a copy of all the [PendingTask] instances that still need to run successfully by Wendy, here is how you get them.
     */
    fun getAllTasks(): List<PendingTask> {
        return tasksManager.getAllTasks()
    }

    /**
     * If you encounter an error while executing [PendingTask.runTask] in one of your [PendingTask]s, you can record it here to handle later in your app. This is usually used when your [PendingTask] encounters an error that requires the app user to fix (example: A string sent up to your API is too long. The user must shorten it up).
     *
     * *Note:* If you attempt to record an error using a [taskId] that does not exist, your request to record an error will be ignored.
     *
     * @param taskId The [PendingTask.task_id] for the [PendingTask] that encountered an error.
     * @param humanReadableErrorMessage A human readable error message that you may choose to show in the UI of your app. This message describes the error to the end user. Make sure they can understand it so they can resolve their issue.
     * @param errorId An ID identifying this error to Wendy. This exists for you, the developer. If and when the user of your app decides to fix the error, you can use this ID to determine what it was that was broken so you can show a UI to the user to fix the issue. Example: An `errorId` of "CreateGroceryItem" in your app could map to a UI in your app that shows the text of the entered grocery store item and the option for your user to edit it.
     */
    fun recordError(taskId: Long, humanReadableErrorMessage: String?, errorId: String?) {
        val pendingTask = tasksManager.getPendingTaskTaskById(taskId) ?: return

        tasksManager.insertPendingTaskError(PendingTaskError.init(taskId, humanReadableErrorMessage, errorId))

        WendyConfig.logErrorRecorded(pendingTask, humanReadableErrorMessage, errorId)
    }

    /**
     * How to check if an error has been recorded for a [PendingTask].
     *
     * @param taskId The task_id of a [PendingTask] you may or may not have recorded an error for.
     */
    fun getLatestError(taskId: Long): PendingTaskError? {
        val pendingTask = tasksManager.getPendingTaskTaskById(taskId) ?: return null
        val pendingTaskError = tasksManager.getLatestError(taskId)

        pendingTaskError?.pending_task = pendingTask

        return pendingTaskError
    }

    /**
     * Convenient method to see if an error has been recorded for a [PendingTask] and has not been resolved yet.
     */
    fun doesErrorExist(taskId: Long): Boolean = getLatestError(taskId) != null

    /**
     * Mark a previously recorded error for a [PendingTask] as resolved.
     *
     * *Note:* If you attempt to resolve an error using a [taskId] that does not exist, your request to record an error will be ignored.
     *
     * *Note:* If you attempt to resolve an error when an error does not exist in Wendy (because it has already been resolved or was never recorded) then the [TaskRunnerListener.errorResolved] and [PendingTaskStatusListener.errorResolved] will not be called.
     *
     * *Note:* The task runner will attempt to run your task after it has been resolved immediately. If the task belongs to a group, the task runner will attempt to run all the tasks in the group.
     *
     * @param taskId The task_id of a [PendingTask] previously recorded an error for.
     * @return If [PendingTask] had a previously recorded error and it has been marked as resolved now.
     *
     * @see recordError This is how to record an error.
     */
    fun resolveError(taskId: Long): Boolean {
        val pendingTask = tasksManager.getPendingTaskTaskById(taskId) ?: return false

        if (tasksManager.deletePendingTaskError(taskId)) { // Only log error as resolved if an error was even recorded in the first place.
            WendyConfig.logErrorResolved(pendingTask)
            LogUtil.d("Task: $pendingTask successfully resolved previously recorded error.")

            val groupId: String? = pendingTask.group_id
            if (groupId != null) runTasks(groupId)
            else runTask(taskId)

            return true
        }
        return false
    }

    /**
     * Get all errors that currently exist for [PendingTask]s.
     */
    fun getAllErrors(): List<PendingTaskError> {
        return tasksManager.getAllErrors()
    }

}