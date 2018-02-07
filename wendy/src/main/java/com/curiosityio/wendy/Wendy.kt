package com.curiosityio.wendy

import com.curiosityio.wendy.config.WendyConfig
import com.curiosityio.wendy.model.PendingApiTask
import com.curiosityio.wendy.runner.PendingApiTasksRunner
import rx.subjects.BehaviorSubject

class Wendy {

    companion object {

        fun runTasks() {
            PendingApiTasksRunner.runPendingTasks().subscribe({
                WendyConfig.wendyTasksRunnerManager?.doneRunningTasks(false)
            }, { error ->
                WendyConfig.wendyTasksRunnerManager?.errorRunningTasks(false, error)
            })
        }

        fun runTempInstanceTasks() {
            PendingApiTasksRunner.runPendingTasks(true).subscribe({
                WendyConfig.wendyTasksRunnerManager?.doneRunningTasks(true)
            }, { error ->
                WendyConfig.wendyTasksRunnerManager?.errorRunningTasks(true, error)
            })
        }

        fun runTask(pendingApiTask: PendingApiTask<Any>, useTempRealmInstance: Boolean = false) {
            PendingApiTasksRunner.runSingleTask(pendingApiTask, useTempRealmInstance).subscribe({
                WendyConfig.wendyTasksRunnerManager?.doneRunningSingleTask(pendingApiTask)
            }, { error ->
                WendyConfig.wendyTasksRunnerManager?.errorRunningSingleTask(pendingApiTask, error)
            })
        }

        fun subNumberPendingTasks(): BehaviorSubject<Long> {
            val subject: BehaviorSubject<Long> = PendingApiTasksRunner.numberPendingApiTasksRemaining ?: throw RuntimeException("You have not initialized Wendy yet. Call WendyConfig.Builder()....init() in your MainApplication to do so.")

            return subject
        }

        fun subNumberTempPendingTasks(): BehaviorSubject<Long> {
            val subject: BehaviorSubject<Long> = PendingApiTasksRunner.numberTempInstancePendingApiTasksRemaining ?: throw RuntimeException("You have not initialized Wendy yet. Call WendyConfig.Builder()....init() in your MainApplication to do so.")

            return subject
        }

    }

}
