package com.levibostian.wendy.service

import android.content.Context
import kotlin.collections.ArrayList

internal class PendingTasksRunner(val context: Context,
                                  private val pendingTasksManager: PendingTasksManager) {

    private var lastSuccessfulOrFailedTaskId: Long = 0
    private var failedTasksGroups: ArrayList<String> = arrayListOf()

    @Synchronized fun runPendingTasks() {
        val nextTaskToRun = pendingTasksManager.getNextTask(lastSuccessfulOrFailedTaskId, failedTasksGroups)

        if (nextTaskToRun == null) {
            resetRunner()
            return
        }

        lastSuccessfulOrFailedTaskId = nextTaskToRun.id

        if (!nextTaskToRun.canRunTask()) {
            nextTaskToRun.group_id?.let { failedTasksGroups.add(it) }

            runPendingTasks()
        } else {
            nextTaskToRun.runTask({ successful ->
                if (successful) {
                    pendingTasksManager.deleteTask(nextTaskToRun)
                } else {
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

}