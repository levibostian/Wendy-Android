package com.levibostian.wendy.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import com.levibostian.wendy.util.LogUtil
import kotlin.collections.ArrayList
import android.os.Looper
import android.preference.PreferenceManager
import android.support.annotation.WorkerThread
import com.levibostian.wendy.*
import com.levibostian.wendy.types.PendingTaskResult

internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    internal var lastSuccessfulOrFailedTaskId: Long = 0
    internal var failedTasksGroups: ArrayList<String> = arrayListOf()
    internal var currentlyRunningTask: PendingTask? = null

    @Synchronized
    @WorkerThread
    fun runAllTasks() {
        if (!WendyConfig.automaticallyRunTasks) {
            LogUtil.d("Wendy configured to not automatically run all tasks. Skipping execution.")
            return
        }

        LogUtil.d("Getting next task to run.")
        val nextTaskToRun = pendingTasksManager.getNextTask(lastSuccessfulOrFailedTaskId, failedTasksGroups)

        if (nextTaskToRun == null) {
            LogUtil.d("All done running tasks.")
            WendyConfig.logAllTasksComplete()

            resetRunner()
            return
        }

        lastSuccessfulOrFailedTaskId = nextTaskToRun.id

        val jobRunResult = runTask(nextTaskToRun.id)
        jobRunResult.accept(object : PendingTasksRunnerJobRunResult.Visitor<Unit?> {
            override fun visitSuccessful(): Unit? {
                runAllTasks()
                return null
            }
            override fun visitNotSuccessful(): Unit? {
                nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }
                runAllTasks()
                return null
            }
            override fun visitTaskDoesntExist(): Unit? {
                // Ignore this. If it doesn't exist, it doesn't exist.
                runAllTasks()
                return null
            }
            override fun visitSkippedNotReady(): Unit? {
                nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }
                runAllTasks()
                return null
            }
        })
    }

    @Synchronized
    @WorkerThread
    fun runTask(id: Long): PendingTasksRunnerJobRunResult {
        val taskToRun = pendingTasksManager.getTaskForId(id) ?: return PendingTasksRunnerJobRunResult.SKIPPED_TASK_DOESNT_EXIST

        if (!taskToRun.canRunTask()) {
            WendyConfig.logTaskSkipped(taskToRun, ReasonPendingTaskSkipped.NOT_READY_TO_RUN)
            LogUtil.d("Task: $taskToRun is not ready to run. Skipping it.")
            return PendingTasksRunnerJobRunResult.SKIPPED_NOT_READY
        } else {
            currentlyRunningTask = taskToRun

            WendyConfig.logTaskRunning(taskToRun)
            LogUtil.d("Running task: $taskToRun.")
            val result = taskToRun.runTask()
            currentlyRunningTask = null
            var runJobResult = PendingTasksRunnerJobRunResult.SUCCESSFUL

            when (result) {
                PendingTaskResult.SUCCESSFUL -> {
                    LogUtil.d("Task: $taskToRun ran successful. Deleting it.")
                    pendingTasksManager.deleteTask(taskToRun.id)
                    runJobResult = PendingTasksRunnerJobRunResult.SUCCESSFUL
                    WendyConfig.logTaskComplete(taskToRun, true, false)
                }
                PendingTaskResult.FAILED_RESCHEDULE -> {
                    LogUtil.d("Task: $taskToRun failed but will reschedule it. Skipping it.")
                    runJobResult = PendingTasksRunnerJobRunResult.NOT_SUCCESSFUL
                    WendyConfig.logTaskComplete(taskToRun, false, true)
                }
                PendingTaskResult.FAILED_DO_NOT_RESCHEDULE -> {
                    LogUtil.d("Task: $taskToRun failed and will *not* reschedule. Deleting it.")
                    pendingTasksManager.deleteTask(taskToRun.id)
                    runJobResult = PendingTasksRunnerJobRunResult.NOT_SUCCESSFUL
                    WendyConfig.logTaskComplete(taskToRun, false, false)
                }
            }

            return runJobResult
        }
    }

    private fun resetRunner() {
        lastSuccessfulOrFailedTaskId = 0
        failedTasksGroups = arrayListOf()
        currentlyRunningTask = null
    }

    internal class PendingTasksRunnerAllTasksAsyncTask(val runner: PendingTasksRunner, val pendingTasksManager: PendingTasksManager) : AsyncTask<Unit, Int, Int>() {

        @Suppress("UNCHECKED_CAST")
        @WorkerThread
        override fun doInBackground(vararg params: Unit): Int {
            val numTasksToRun = pendingTasksManager.getTotalNumberOfTasksForRunnerToRun()
            LogUtil.d("Running all tasks in task runner. Running total of: $numTasksToRun tasks.")

            runner.runAllTasks()

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
        };

        abstract fun <E> accept(visitor: Visitor<E>): E

        interface Visitor<out E> {
            fun visitSuccessful(): E
            fun visitNotSuccessful(): E
            fun visitTaskDoesntExist(): E
            fun visitSkippedNotReady(): E
        }

    }

}