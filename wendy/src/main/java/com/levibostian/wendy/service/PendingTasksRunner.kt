package com.levibostian.wendy.service

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import com.levibostian.wendy.util.LogUtil
import kotlin.collections.ArrayList
import android.os.Looper
import android.util.Log


internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    internal var lastSuccessfulOrFailedTaskId: Long = 0
    internal var failedTasksGroups: ArrayList<String> = arrayListOf()
    internal var currentlyRunningTaskId: Long? = null

    @Synchronized fun runAllTasks() {
        LogUtil.d("Getting next task to run.")
        val nextTaskToRun = pendingTasksManager.getNextTask(lastSuccessfulOrFailedTaskId, failedTasksGroups)

        if (nextTaskToRun == null) {
            LogUtil.d("All done running tasks.")
            Handler(Looper.getMainLooper()).post({
                WendyConfig.getTaskRunnerListeners().forEach {
                    it.allTasksComplete()
                }
            })

            resetRunner()
            return
        }

        lastSuccessfulOrFailedTaskId = nextTaskToRun.id

        runTask(nextTaskToRun.id, { skippedReason, successful ->
            when (skippedReason) {
                ReasonPendingTaskSkipped.NOT_READY_TO_RUN -> {
                    nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }

                    runAllTasks()
                }
                null -> {
                    if (!successful) {
                        nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }
                    }

                    runAllTasks()
                }
            }
        })
    }

    @Synchronized fun runTask(id: Long, complete: (skipped: ReasonPendingTaskSkipped?, successful: Boolean) -> Unit) {
        val taskToRun = pendingTasksManager.getTaskForId(id) ?: return

        if (!taskToRun.canRunTask()) {
            val reasonForSkip = ReasonPendingTaskSkipped.NOT_READY_TO_RUN

            Handler(Looper.getMainLooper()).post({
                WendyConfig.getTaskStatusListenerForTask(taskToRun.id).forEach {
                    it.skipped(taskToRun.id, reasonForSkip)
                }
                WendyConfig.getTaskRunnerListeners().forEach {
                    it.taskSkipped(reasonForSkip, taskToRun)
                }
            })

            LogUtil.d("Task: $taskToRun is not ready to run. Skipping it.")
            complete(reasonForSkip, false)
        } else {
            currentlyRunningTaskId = taskToRun.id
            Handler(Looper.getMainLooper()).post({
                WendyConfig.getTaskStatusListenerForTask(taskToRun.id).forEach {
                    it.running(taskToRun.id)
                }
                WendyConfig.getTaskRunnerListeners().forEach {
                    it.runningTask(taskToRun)
                }
            })

            taskToRun.runTask({ successful ->
                currentlyRunningTaskId = null

                Handler(Looper.getMainLooper()).post({
                    WendyConfig.getTaskStatusListenerForTask(taskToRun.id).forEach {
                        it.complete(taskToRun.id, successful)
                    }
                    WendyConfig.getTaskRunnerListeners().forEach {
                        it.taskComplete(successful, taskToRun)
                    }
                })

                if (successful) {
                    LogUtil.d("Task: $taskToRun ran successful. Deleting it.")
                    pendingTasksManager.deleteTask(taskToRun)
                } else {
                    LogUtil.d("Task: $taskToRun failed. Skipping it.")
                }

                complete(null, successful)
            })
        }
    }

    private fun resetRunner() {
        lastSuccessfulOrFailedTaskId = 0
        failedTasksGroups = arrayListOf()
        currentlyRunningTaskId = null
    }

    internal class PendingTasksRunnerAllTasksAsyncTask(val runner: PendingTasksRunner, val pendingTasksManager: PendingTasksManager) : AsyncTask<Unit, Int, Int>() {

        @Suppress("UNCHECKED_CAST")
        override fun doInBackground(vararg params: Unit): Int {
            val numTasksToRun = pendingTasksManager.getTotalNumberOfTasksForRunnerToRun()
            LogUtil.d("Running all tasks in task runner. Running total of: $numTasksToRun tasks.")

            runner.runAllTasks()

            return numTasksToRun
        }

    }

    internal class PendingTasksRunnerGivenSetTasksAsyncTask(val runner: PendingTasksRunner) : AsyncTask<Long?, Int, Int>() {

        @Suppress("UNCHECKED_CAST")
        override fun doInBackground(vararg params: Long?): Int {
            LogUtil.d("Running a given set of tasks in task runner. Running total of: ${params.size} tasks.")

            fun iterateThroughTasks(tasks: List<Long>) {
                runner.runTask(tasks[0], { _, _ ->
                    val newList = tasks.drop(1)
                    if (newList.isNotEmpty()) iterateThroughTasks(newList)
                })
            }

            iterateThroughTasks(params.toList() as List<Long>)

            return params.size
        }

    }

}