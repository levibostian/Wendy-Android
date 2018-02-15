package com.levibostian.wendy.service

import android.content.Context
import android.os.AsyncTask
import com.levibostian.wendy.util.LogUtil
import kotlin.collections.ArrayList

internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    private var lastSuccessfulOrFailedTaskId: Long = 0
    private var failedTasksGroups: ArrayList<String> = arrayListOf()

    @Synchronized fun runPendingTasks() {
        LogUtil.d("Getting next task to run.")
        val nextTaskToRun = pendingTasksManager.getNextTask(lastSuccessfulOrFailedTaskId, failedTasksGroups)

        if (nextTaskToRun == null) {
            LogUtil.d("All done running tasks.")
            resetRunner()
            return
        }

        lastSuccessfulOrFailedTaskId = nextTaskToRun.id

        if (!nextTaskToRun.canRunTask()) {
            nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }

            LogUtil.d("Task: $nextTaskToRun is not ready to run. Skipping and moving onto the next task.")
            runPendingTasks()
        } else {
            nextTaskToRun.runTask({ successful ->
                if (successful) {
                    LogUtil.d("Task: $nextTaskToRun ran successful. Deleting and then moving onto the next task.")
                    pendingTasksManager.deleteTask(nextTaskToRun)
                } else {
                    LogUtil.d("Task: $nextTaskToRun failed. Skipping and moving onto the next task.")
                    nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }
                }

                runPendingTasks()
            })
        }
    }

    fun resetRunner() {
        lastSuccessfulOrFailedTaskId = 0
        failedTasksGroups = arrayListOf()
    }

    class PendingTasksRunnerAsyncTask(val runner: PendingTasksRunner, val pendingTasksManager: PendingTasksManager) : AsyncTask<Long?, Int, Int>() {

        override fun doInBackground(vararg params: Long?): Int {
            val numTasksToRun = pendingTasksManager.getTotalNumberOfTasksForRunnerToRun()
            LogUtil.d("Running task runner. Running total of: $numTasksToRun tasks.")

            runner.runPendingTasks()

            return numTasksToRun
        }

    }

}