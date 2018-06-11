package com.levibostian.wendy.service

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.preference.PreferenceManager
import com.evernote.android.job.JobManager
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.db.PendingTaskError
import com.levibostian.wendy.db.PendingTasksManager
import com.levibostian.wendy.db.PersistedPendingTask
import com.levibostian.wendy.extension.getPendingTask
import com.levibostian.wendy.extension.getTaskAssertPopulated
import com.levibostian.wendy.job.PendingTaskJobCreator
import com.levibostian.wendy.job.PendingTasksJob
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.listeners.TaskRunnerListener
import com.levibostian.wendy.logErrorRecorded
import com.levibostian.wendy.logErrorResolved
import com.levibostian.wendy.logNewTaskAdded
import com.levibostian.wendy.types.PendingTaskResult
import com.levibostian.wendy.types.RunAllTasksFilter
import com.levibostian.wendy.util.LogUtil
import com.levibostian.wendy.util.PendingTasksUtil

/**
 * How you interact with Wendy with [PendingTask] instances you create. Add tasks to Wendy to run, get a list of all the [PendingTask]s registered to Wendy, etc.
 */
class Wendy private constructor(private val context: Context, internal val tasksFactory: PendingTasksFactory) {

    companion object {
        private var instance: Wendy? = null

        /**
         * Initialize Wendy. It's recommended to do this in your [Application] class of your app.
         *
         * This function essentially just creates a singleton instance of [Wendy] for your application to share. Future calls to this function will be ignored. The instance is only initialized once.
         *
         * @param context Android context (usually given in your Application class's onCreate() call)
         * @param tasksFactory [PendingTasksFactory] instance for your app to construct [PendingTask] instances used by Wendy.
         */
        @JvmStatic fun init(context: Context, tasksFactory: PendingTasksFactory): Wendy {
            if (instance == null) {
                instance = Wendy(context, tasksFactory)
                instance!!.init(context)
            }

            return instance!!
        }

        /**
         * Get singleton instance of [Wendy].
         *
         * @throws RuntimeException If you have not called [Wendy.Companion.init] yet to initialize singleton instance.
         */
        @JvmStatic fun sharedInstance(): Wendy {
            if (instance == null) throw RuntimeException("Sorry, you must initialize the instance first.")
            return instance!!
        }

        /**
         * Short hand version of calling [sharedInstance].
         */
        @JvmStatic val shared: Wendy by lazy { sharedInstance() }

    }

    /**
     * Turn on and off debug log messages from Wendy.
     *
     * Designed like a builder pattern so you can: Wendy(this, Factory()).enableDebug()
     */
    fun debug(enableDebug: Boolean = false): Wendy {
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

    private var runAllTasksAsyncTask: PendingTasksRunner.PendingTasksRunnerAllTasksAsyncTask? = null
    private var runGivenSetTasksAsyncTask: PendingTasksRunner.PendingTasksRunnerGivenSetTasksAsyncTask? = null

    /**
     * Call when you have a new [PendingTask] that you would like to register to Wendy to run.
     *
     * Wendy works in a FIFO order. Your task gets added to the end of the queue of tasks to run.
     *
     * *Note: If you attempt to add a [PendingTask] instance that has the same [PendingTask.tag] and [PendingTask.dataId] as another [PendingTask] already added to Wendy, your request (including calling the task runner listener) will be ignored (except for the exception below in that there was an error recorded previously for a [PendingTask]).*
     *
     * *Note:* If you attempt to add a [PendingTask] instance that has the same [PendingTask.tag] and [PendingTask.dataId] as another [PendingTask] already added to Wendy that previously had an error recorded for it, that error will be marked as resolved (by internally calling [Wendy.resolveError].
     *
     * @param pendingTask Task you want to add to Wendy.
     *
     * @throws RuntimeException Wendy will check to make sure that you have remembered to add your argument's [PendingTask] subclass to your instance of [PendingTasksFactory] when you call this method. If your [PendingTasksFactory] returns null (which probably means that you forgot to include a [PendingTask]) then an [RuntimeException] will be thrown.
     * @throws IllegalArgumentException If your [PendingTask] subclass does not follow the enforced best practice of: All subclasses of a [PendingTask] must **all** have a groupId or **none** of them have a groupId.
     */
    fun addTask(pendingTask: PendingTask, resolveErrorIfTaskExists: Boolean = true): Long {
        tasksFactory.getTaskAssertPopulated(pendingTask.tag)

        // We enforce a best practice here.
        tasksManager.getRandomTaskForTag(pendingTask.tag)?.let { similarPendingTask ->
            if (similarPendingTask.groupId == null && pendingTask.groupId != null) {
                throw IllegalArgumentException("Cannot add task: $pendingTask. All subclasses of a PendingTask must either **all** have a groupId or **none** of them have a groupId. Other ${pendingTask.tag}'s you have previously added does not have a groupId. The task you are trying to add does have a groupId.")
            }
            if (similarPendingTask.groupId != null && pendingTask.groupId == null) {
                throw IllegalArgumentException("Cannot add task: $pendingTask. All subclasses of a PendingTask must either **all** have a groupId or **none** of them have a groupId. Other ${pendingTask.tag}'s you have previously added does have a groupId. The task you are trying to add does not have a groupId.")
            }
        }

        tasksManager.getExistingTasks(pendingTask)?.let { existingPendingTasks ->
            if (existingPendingTasks.isNotEmpty()) {
                val sampleExistingPendingTask: PersistedPendingTask = existingPendingTasks.last()

                runner.currentlyRunningTask?.let { currentlyRunningTask ->
                    if (currentlyRunningTask == sampleExistingPendingTask) {
                        PendingTasksUtil.setRerunCurrentlyRunningPendingTask(context)
                        return sampleExistingPendingTask.id
                    }
                }
                if (doesErrorExist(sampleExistingPendingTask.id) && resolveErrorIfTaskExists) {
                    existingPendingTasks.forEach { existingPendingTask ->
                        resolveError(existingPendingTask.id)
                    }
                    return sampleExistingPendingTask.id
                }
                // If a PendingTask belongs to a group, but is *not* the last item in the group, we want to insert it into the DB. Let is pass through below to add itself.
                if (pendingTask.groupId != null) {
                    val groupId = pendingTask.groupId!!

                    tasksManager.getLastPendingTaskInGroup(groupId)?.let { lastPendingTaskInGroup ->
                        if (lastPendingTaskInGroup.getPendingTask() == pendingTask) {
                            return lastPendingTaskInGroup.id
                        }
                    }
                } else {
                    return sampleExistingPendingTask.id
                }
            }
        }

        val addedTask = tasksManager.insertPendingTask(pendingTask)
        WendyConfig.logNewTaskAdded(addedTask)

        runTaskIfAbleTo(addedTask)

        return addedTask.taskId!!
    }

    /**
     * Note: This function is for internal use only. There are no checks to make sure that it exists and stuff. It's assumed you know what you're doing.
     */
    private fun runTaskIfAbleTo(pendingTask: PendingTask): Boolean {
        if (!WendyConfig.automaticallyRunTasks) {
            LogUtil.d("Wendy configured to not automatically run tasks. Skipping execution of newly added task: $pendingTask")
            return false
        }
        if (pendingTask.manuallyRun) {
            LogUtil.d("Task is set to manually run. Skipping execution of newly added task: $pendingTask")
            return false
        }
        if (!isTaskAbleToManuallyRun(pendingTask.taskId!!)) {
            LogUtil.d("Task is not able to manually run. Skipping execution of newly added task: $pendingTask")
            return false
        }

        LogUtil.d("Wendy is configured to automatically run tasks. Wendy will now attempt to run newly added task: $pendingTask")
        runTask(pendingTask.taskId!!)

        return true
    }

    /**
     * Manually run all pending tasks. Wendy takes care of running this periodically for you, but you can manually run tasks here.
     *
     * *Note:* This will run all tasks even if you use [WendyConfig.automaticallyRunTasks] to enable/disable running of all the [PendingTask]s.
     *
     * @param filter Limit running of the tasks to match the criteria outlined in [RunAllTasksFilter] object.
     * @throws [RuntimeException] when in [WendyConfig.strict] mode and you say that your [PendingTask] was [PendingTaskResult.SUCCESSFUL] when you have an unresolved error recorded for that [PendingTask].
     */
    fun runTasks(filter: RunAllTasksFilter?) {
        runAllTasksAsyncTask = PendingTasksRunner.PendingTasksRunnerAllTasksAsyncTask(runner, tasksManager).apply {
            execute(filter)
        }
    }

    /**
     * Use this function along with manually run tasks to have Wendy run it for you. If it is successful, Wendy will take care of deleting it for you.
     *
     * @param taskId The [PendingTask.taskId] of [PendingTask] you wish to run.
     *
     * @throws [IllegalArgumentException] If the [PendingTask] for the taskId provided does not exist.
     * @throws [RuntimeException] If the [PendingTask] returns false for [isTaskAbleToManuallyRun].
     *
     * @see [WendyConfig.addTaskStatusListenerForTask] to learn how to listen to the status of a task that you have set to run.
     */
    fun runTask(taskId: Long) {
        val pendingTask: PendingTask = assertPendingTaskExists(taskId)

        if (!isTaskAbleToManuallyRun(taskId)) {
            throw RuntimeException("Task is not able to manually run. Task: $pendingTask")
        }

        runGivenSetTasksAsyncTask = PendingTasksRunner.PendingTasksRunnerGivenSetTasksAsyncTask(runner).apply {
            execute(taskId)
        }
    }

    /**
     * Tells you if a [PendingTask] is able to run yet or not.
     *
     * To be able to run, all of these must be true:
     * 1. A [PendingTask] is not part of a group or it is the first task of a group.
     *
     * Call this before calling [runTask] to avoid [runTask] throwing an exception on you for it not being ready to run.
     *
     * @throws [IllegalArgumentException] If the [PendingTask] for the taskId provided does not exist.
     */
    fun isTaskAbleToManuallyRun(taskId: Long): Boolean {
        val pendingTask: PendingTask = assertPendingTaskExists(taskId)

        if (pendingTask.groupId == null) return true
        return tasksManager.isTaskFirstTaskOfGroup(taskId)
    }

    /**
     * Checks to make sure that a [PendingTask] does exist in the database, else throw an exception.
     *
     * Why throw an exception? I used to simply ignore your request if you called a function such as [recordError] if you gave a taskId parameter for a task that did not exist in the database. But I decided to remove that because [PendingTask] should always be found in the database unless one of the following happens:
     *
     * 1. You did not add the [PendingTask] to the database in the first place which you should get an exception thrown on you then to make sure you fix that.
     * 2. The [PendingTask] previously existed, but the task ran successfully and the task runner deleted. In that case, you *should* not be doing actions such as trying to record errors then, right? You should have returns [PendingTaskResult.FAILED] instead which will not delete your task.
     *
     * You do not need to use this function. But you should use it if there is a scenario when a [PendingTask] could be deleted and your code tries to perform an action on it. Race conditions are real and we do keep them in mind. But if your code *should* be following best practices, then we should throw exceptions instead to get you to fix your code.
     */
    internal fun assertPendingTaskExists(taskId: Long): PendingTask {
        return tasksManager.getPendingTaskTaskById(taskId) ?: throw IllegalArgumentException("Task with id: $taskId does not exist.")
    }

    /**
     * @see [assertPendingTaskExists]
     */
    internal fun assertPersistedPendingTaskExists(taskId: Long): PersistedPendingTask {
        return tasksManager.getTaskByTaskId(taskId) ?: throw IllegalArgumentException("Task with id: $taskId does not exist.")
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
     * @param taskId The [PendingTask.taskId] for the [PendingTask] that encountered an error.
     * @param humanReadableErrorMessage A human readable error message that you may choose to show in the UI of your app. This message describes the error to the end user. Make sure they can understand it so they can resolve their issue.
     * @param errorId An ID identifying this error to Wendy. This exists for you, the developer. If and when the user of your app decides to fix the error, you can use this ID to determine what it was that was broken so you can show a UI to the user to fix the issue. Example: An `errorId` of "CreateGroceryItem" in your app could map to a UI in your app that shows the text of the entered grocery store item and the option for your user to edit it.
     *
     * @throws [IllegalArgumentException] If the [PendingTask] for the taskId provided does not exist. This should only happen if you (1) never added the [PendingTask] to Wendy in the first place or (2) the [PendingTask] ran, and was successful (then you should never have recorded an error in the first place).
     */
    fun recordError(taskId: Long, humanReadableErrorMessage: String?, errorId: String?) {
        val pendingTask: PendingTask = assertPendingTaskExists(taskId)

        tasksManager.insertPendingTaskError(PendingTaskError.init(taskId, humanReadableErrorMessage, errorId))

        WendyConfig.logErrorRecorded(pendingTask, humanReadableErrorMessage, errorId)
    }

    /**
     * How to check if an error has been recorded for a [PendingTask].
     *
     * @param taskId The taskId of a [PendingTask] you may or may not have recorded an error for.
     *
     * @throws [IllegalArgumentException] If the [PendingTask] for the taskId provided does not exist.
     */
    fun getLatestError(taskId: Long): PendingTaskError? {
        val pendingTask: PendingTask = assertPendingTaskExists(taskId)
        val pendingTaskError = tasksManager.getLatestError(taskId)

        pendingTaskError?.pendingTask = pendingTask

        return pendingTaskError
    }

    /**
     * Convenient method to see if an error has been recorded for a [PendingTask] and has not been resolved yet.
     */
    fun doesErrorExist(taskId: Long): Boolean = getLatestError(taskId) != null

    /**
     * Mark a previously recorded error for a [PendingTask] as resolved.
     *
     * *Note:* If your [PendingTask] belongs to a group, this function will iterate all of the [PendingTask]s of that group and resolve the first (and only the first one) error that it comes upon. This is because (1) groups allow having "duplicate" [PendingTask]s in them (2) it is assumed that if an error is resolved for one [PendingTask] in a group, the error is resolved for *all* [PendingTask]s in the group and, well, a group cannot execute if the *very first* [PendingTask] is blocking the group from executing. So with this feature, you technically only have to keep track of *1* [PendingTask.taskId] in your app when you call [Wendy.addTask] for a [PendingTask] that belongs to a group instead of having to keep track of multiple [PendingTask.taskId]s for every time you call [Wendy.addTask].
     *
     * *Note:* If you attempt to resolve an error when an error does not exist in Wendy (because it has already been resolved or was never recorded) then the [TaskRunnerListener.errorResolved] and [PendingTaskStatusListener.errorResolved] will not be called.
     *
     * *Note:* If [WendyConfig.automaticallyRunTasks] is true, the task runner will attempt to run your task after it has been resolved immediately. If the task belongs to a group, the task runner will attempt to run all the tasks in the group.
     *
     * @param taskId The taskId of a [PendingTask] previously recorded an error for.
     * @return If [PendingTask] had a previously recorded error and it has been marked as resolved now.
     *
     * @throws [IllegalArgumentException] If the [PendingTask] for the taskId provided does not exist.
     *
     * @see recordError This is how to record an error.
     */
    fun resolveError(taskId: Long): Boolean {
        val pendingTask: PendingTask = assertPendingTaskExists(taskId)

        tasksManager.getExistingTasks(pendingTask)?.let { existingPendingTasks ->
            if (existingPendingTasks.isEmpty()) return false

            existingPendingTasks.forEach { existingPendingTask ->
                val existingTaskPendingTask: PendingTask = existingPendingTask.getPendingTask()

                if (tasksManager.deletePendingTaskError(existingPendingTask.id)) { // Only log error as resolved if an error was even recorded in the first place.
                    WendyConfig.logErrorResolved(existingTaskPendingTask)
                    LogUtil.d("Task: $existingTaskPendingTask successfully resolved previously recorded error.")

                    val groupId: String? = existingTaskPendingTask.groupId
                    if (groupId != null) runTasks(RunAllTasksFilter(groupId))
                    else runTaskIfAbleTo(existingTaskPendingTask)

                    return true
                }
            }
        }

        return false
    }

    /**
     * Stop the task runner and clear all of the [PendingTask]s added to Wendy.
     *
     * This function was written with the intention of using when a user of your app is logged out and you want to clear all of the data associated with that user.
     *
     * This function will stop the task runner any tasks, deletes all of the data from the task database, and deletes all the data stored in 
     */
    fun clear(complete: () -> Unit?) {
        LogUtil.d("Cancel running all PendingTasks.")
        runAllTasksAsyncTask?.cancel(true)
        LogUtil.d("Cancel running given set of PendingTasks.")
        runGivenSetTasksAsyncTask?.cancel(true)

        ClearAsyncTask(tasksManager, PreferenceManager.getDefaultSharedPreferences(context), { error ->
            error?.let {
                LogUtil.d("Error deleting data: $it")
                throw it
            }
            LogUtil.d("Data cleared successfully.")
            complete()
        }).execute()
    }

    /**
     * Get all errors that currently exist for [PendingTask]s.
     */
    fun getAllErrors(): List<PendingTaskError> {
        return tasksManager.getAllErrors()
    }

    private class ClearAsyncTask(private val tasksManager: PendingTasksManager,
                                 private val sharedPreferences: SharedPreferences,
                                 private val complete: (error: Throwable?) -> Unit?): AsyncTask<Unit?, Unit?, Unit?>() {

        private var doInBackgroundError: Throwable? = null

        override fun doInBackground(vararg params: Unit?): Unit? {
            try {
                LogUtil.d("Deleting database.")
                tasksManager.clear()

                LogUtil.d("Deleting SharedPreferences entries.")
                val sharedPrefsToDelete: ArrayList<String> = ArrayList()
                sharedPreferences.all.forEach {
                    if (it.key.startsWith(PendingTasksUtil.PREFIX)) sharedPrefsToDelete.add(it.key)
                }
                sharedPreferences.edit().apply {
                    sharedPrefsToDelete.forEach { remove(it) }
                    commit()
                }
            } catch (e: Throwable) {
                doInBackgroundError = e
            }

            return null
        }

        override fun onPostExecute(result: Unit?) {
            super.onPostExecute(result)

            complete(doInBackgroundError)
        }

    }

}