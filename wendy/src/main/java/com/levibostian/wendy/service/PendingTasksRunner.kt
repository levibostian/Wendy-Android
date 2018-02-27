package com.levibostian.wendy.service

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import com.levibostian.wendy.WendyConfig
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import com.levibostian.wendy.util.LogUtil
import kotlin.collections.ArrayList
import android.os.Looper
import android.preference.PreferenceManager

internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    private val rerunCurrentlyRunningTaskKey = "rerunCurrentlyRunningTaskKey"

    internal var lastSuccessfulOrFailedTaskId: Long = 0
    internal var failedTasksGroups: ArrayList<String> = arrayListOf()
    internal var currentlyRunningTask: PendingTask? = null
    internal var rerunCurrentlyRunningTask: Boolean
        get() = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(rerunCurrentlyRunningTaskKey, false)
        @SuppressLint("ApplySharedPref")
        set(value) {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(rerunCurrentlyRunningTaskKey, value).commit()
        }

    @Synchronized fun runAllTasks() {
        if (!WendyConfig.automaticallyRunTasks) {
            LogUtil.d("Wendy configured to not automatically run all tasks. Skipping execution.")
            return
        }

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
            currentlyRunningTask = taskToRun
            Handler(Looper.getMainLooper()).post({
                WendyConfig.getTaskStatusListenerForTask(taskToRun.id).forEach {
                    it.running(taskToRun.id)
                }
                WendyConfig.getTaskRunnerListeners().forEach {
                    it.runningTask(taskToRun)
                }
            })

            taskToRun.runTask({ successful ->
                currentlyRunningTask = null

                if (successful) {
                    LogUtil.d("Task: $taskToRun ran successful.")
                    if (!rerunCurrentlyRunningTask) {
                        LogUtil.d("Deleting task: $taskToRun.")
                        pendingTasksManager.deleteTask(taskToRun.id)
                    } else {
                        LogUtil.d("Not deleting task: $taskToRun. It is set to rerun again.")
                    }
                    rerunCurrentlyRunningTask = false
                } else {
                    LogUtil.d("Task: $taskToRun failed. Skipping it.")
                }

                Handler(Looper.getMainLooper()).post({
                    WendyConfig.getTaskStatusListenerForTask(taskToRun.id).forEach {
                        it.complete(taskToRun.id, successful)
                    }
                    WendyConfig.getTaskRunnerListeners().forEach {
                        it.taskComplete(successful, taskToRun)
                    }
                })

                complete(null, successful)
            })
        }
    }

    private fun resetRunner() {
        lastSuccessfulOrFailedTaskId = 0
        failedTasksGroups = arrayListOf()
        currentlyRunningTask = null
        rerunCurrentlyRunningTask = false
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