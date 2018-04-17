package com.levibostian.wendy.service

import android.content.Context
import android.os.AsyncTask
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import com.levibostian.wendy.util.LogUtil
import kotlin.collections.ArrayList
import android.support.annotation.WorkerThread
import com.levibostian.wendy.*
import com.levibostian.wendy.db.PendingTasksManager
import com.levibostian.wendy.types.PendingTaskResult

internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    private var lastSuccessfulOrFailedTaskId: Long = 0
    private var failedTasksGroups: ArrayList<String> = arrayListOf()

    internal var currentlyRunningTask: PendingTask? = null

    @Synchronized
    @WorkerThread
    fun runAllTasks(filter: RunAllTasksFilter?) {
        LogUtil.d("Getting next task to run.")
        val nextTaskToRun = pendingTasksManager.getNextTaskToRun(lastSuccessfulOrFailedTaskId, filter)

        if (nextTaskToRun == null) {
            LogUtil.d("All done running tasks.")
            WendyConfig.logAllTasksComplete()

            resetRunner()
            return
        }

        lastSuccessfulOrFailedTaskId = nextTaskToRun.taskId!!
        if (nextTaskToRun.groupId != null && failedTasksGroups.contains(nextTaskToRun.groupId!!)) {
            WendyConfig.logTaskSkipped(nextTaskToRun, ReasonPendingTaskSkipped.PART_OF_FAILED_GROUP)
            LogUtil.d("Task: $nextTaskToRun belongs to a failing group of tasks. Skipping it.")
            runAllTasks(filter)
            return
        }

        val jobRunResult = runTask(nextTaskToRun.taskId!!)
        jobRunResult.accept(object : PendingTasksRunnerJobRunResult.Visitor<Unit?> {
            override fun visitSkippedUnresolvedRecordedError(): Unit? {
                nextTaskToRun.groupId?.let { failedTasksGroups.add(it) }
                return runAllTasks(filter)
            }
            override fun visitSuccessful(): Unit? {
                return runAllTasks(filter)
            }
            override fun visitNotSuccessful(): Unit? {
                nextTaskToRun.groupId?.let { failedTasksGroups.add(it) }
                return runAllTasks(filter)
            }
            override fun visitTaskDoesntExist(): Unit? {
                // Ignore this. If it doesn't exist, it doesn't exist.
                return runAllTasks(filter)
            }
            override fun visitSkippedNotReady(): Unit? {
                nextTaskToRun.groupId?.let { failedTasksGroups.add(it) }
                return runAllTasks(filter)
            }
        })
    }

    /**
     * @throws [RuntimeException] when in [WendyConfig.strict] mode and you say that your [PendingTask] was [PendingTaskResult.SUCCESSFUL] when you have an unresolved error recorded for that [PendingTask].
     */
    @Synchronized
    @WorkerThread
    fun runTask(taskId: Long): PendingTasksRunnerJobRunResult {
        val persistedPendingTaskId: Long = pendingTasksManager.getTaskByTaskId(taskId)?.id ?: return PendingTasksRunnerJobRunResult.SKIPPED_TASK_DOESNT_EXIST
        val taskToRun: PendingTask = pendingTasksManager.getPendingTaskTaskById(taskId)!!

        if (!taskToRun.isReadyToRun()) {
            WendyConfig.logTaskSkipped(taskToRun, ReasonPendingTaskSkipped.NOT_READY_TO_RUN)
            LogUtil.d("Task: $taskToRun is not ready to run. Skipping it.")
            return PendingTasksRunnerJobRunResult.SKIPPED_NOT_READY
        }
        if (pendingTasksManager.getLatestError(taskToRun.taskId!!) != null) {
            WendyConfig.logTaskSkipped(taskToRun, ReasonPendingTaskSkipped.UNRESOLVED_RECORDED_ERROR)
            LogUtil.d("Task: $taskToRun has a unresolved error recorded. Skipping it.")
            return PendingTasksRunnerJobRunResult.SKIPPED_UNRESOLVED_RECORDED_ERROR
        }

        currentlyRunningTask = taskToRun

        WendyConfig.logTaskRunning(taskToRun)
        LogUtil.d("Running task: $taskToRun.")
        val result = taskToRun.runTask()
        currentlyRunningTask = null
        var runJobResult = PendingTasksRunnerJobRunResult.SUCCESSFUL

        when (result) {
            PendingTaskResult.SUCCESSFUL -> {
                if (Wendy.shared.doesErrorExist(taskToRun.taskId!!)) {
                    val errorMessage = "You returned ${PendingTaskResult.SUCCESSFUL} for running your ${PendingTask::class.java.simpleName}, but you have unresolved issues for task: $taskToRun. You should resolve the previously recorded error to Wendy, or return ${PendingTaskResult.FAILED}."
                    if (WendyConfig.strict) throw RuntimeException(errorMessage) else LogUtil.w(errorMessage)
                }

                LogUtil.d("Task: $taskToRun ran successful. Deleting it.")
                pendingTasksManager.deleteTask(persistedPendingTaskId)
                runJobResult = PendingTasksRunnerJobRunResult.SUCCESSFUL
                WendyConfig.logTaskComplete(taskToRun, true, false)
            }
            PendingTaskResult.FAILED -> {
                LogUtil.d("Task: $taskToRun failed but is rescheduled. Skipping it.")
                runJobResult = PendingTasksRunnerJobRunResult.NOT_SUCCESSFUL
                WendyConfig.logTaskComplete(taskToRun, false, true)
            }
        }

        return runJobResult
    }

    private fun resetRunner() {
        lastSuccessfulOrFailedTaskId = 0
        failedTasksGroups = arrayListOf()
        currentlyRunningTask = null
    }

    internal class PendingTasksRunnerAllTasksAsyncTask(val runner: PendingTasksRunner, val pendingTasksManager: PendingTasksManager) : AsyncTask<RunAllTasksFilter?, Int, Int>() {

        @Suppress("UNCHECKED_CAST")
        @WorkerThread
        override fun doInBackground(vararg params: RunAllTasksFilter?): Int {
            val filterTasksToRun: RunAllTasksFilter? = params.filterNotNull().firstOrNull()

            val numTasksToRun = pendingTasksManager.getTotalNumberOfTasksForRunnerToRun(filterTasksToRun)
            LogUtil.d("Running all tasks in task runner ${if (filterTasksToRun != null) "(with filter)" else ""}. Running total of: $numTasksToRun tasks.")

            runner.runAllTasks(filterTasksToRun)

            return numTasksToRun
        }
    }

    internal class PendingTasksRunnerGivenSetTasksAsyncTask(val runner: PendingTasksRunner) : AsyncTask<Long?, Int, Int>() {

        /**
         * You may send a list of [PendingTask] IDs to run or simply send 1.
         */
        @Suppress("UNCHECKED_CAST")
        @WorkerThread
        override fun doInBackground(vararg params: Long?): Int {
            LogUtil.d("Running a given set of tasks in task runner. Running total of: ${params.size} tasks.")

            params.toList().forEach { taskId ->
                if (taskId != null) runner.runTask(taskId)
            }

            return params.size
        }

    }

    internal class RunAllTasksFilter(val groupId: String?)

    /**
     * Internal purposes job runner result.
     *
     * I created this because it can be more detailed then a simple public facing job result object. Example: If a task is skipped because it doesn't exist in the database, Wendy will simply ignore your request (maybe in future thrown an error?). I don't need to make that result public facing, just internal so I handle it.
     */
    internal enum class PendingTasksRunnerJobRunResult {

        SUCCESSFUL {
            override fun <E> accept(visitor: Visitor<E>): E = visitor.visitSuccessful()
        },
        NOT_SUCCESSFUL {
            override fun <E> accept(visitor: Visitor<E>): E = visitor.visitNotSuccessful()
        },
        SKIPPED_TASK_DOESNT_EXIST {
            override fun <E> accept(visitor: Visitor<E>): E = visitor.visitTaskDoesntExist()
        },
        SKIPPED_NOT_READY {
            override fun <E> accept(visitor: Visitor<E>): E = visitor.visitSkippedNotReady()
        },
        SKIPPED_UNRESOLVED_RECORDED_ERROR {
            override fun <E> accept(visitor: Visitor<E>): E = visitor.visitSkippedUnresolvedRecordedError()
        };

        abstract fun <E> accept(visitor: Visitor<E>): E

        interface Visitor<out E> {
            fun visitSuccessful(): E
            fun visitNotSuccessful(): E
            fun visitTaskDoesntExist(): E
            fun visitSkippedNotReady(): E
            fun visitSkippedUnresolvedRecordedError(): E
        }

    }

}