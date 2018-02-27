package com.levibostian.wendy

import android.os.Handler
import android.os.Looper
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.listeners.TaskRunnerListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.service.PendingTasksRunner
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import java.lang.ref.WeakReference

open class WendyConfig {

    companion object {
        /**
         * Turn on and off the ability for Wendy to automatically run all of the [PendingTask]s set to not manually run for you.
         */
        var automaticallyRunTasks: Boolean = true
        /**
         * Turn on and off debug mode. This turns on and off Wendy sending messages to the logcat.
         */
        var debug: Boolean = false
        /**
         * The tag used when Wendy sends debug messages to the logcat.
         */
        var logTag: String = "WENDY"

        private var taskRunnerListeners = ArrayList<WeakReference<TaskRunnerListener>>()
        /**
         * Listen to updates about the Wendy task runner. Get updates about when tasks get executed, when the task runner is done running all the tasks, etc.
         *
         * @param listener Instance of listener to receive the callbacks. Note: Wendy keeps a weak reference to your listener. Make sure that *you* keep a strong reference to the listener you give!
         *
         * @see TaskRunnerListener to learn more about what callbacks to expect.
         */
        fun addTaskRunnerListener(listener: TaskRunnerListener) = taskRunnerListeners.add(WeakReference(listener))
        internal fun getTaskRunnerListeners(): List<TaskRunnerListener> {
            if (Looper.getMainLooper().thread != Thread.currentThread()) throw RuntimeException("You must be on UI thread.")
            return taskRunnerListeners
                    .mapNotNull { it.get() }
        }

        private var taskStatusListeners = ArrayList<TaskStatusListener>()
        internal fun getTaskStatusListenerForTask(id: Long): List<PendingTaskStatusListener> {
            if (Looper.getMainLooper().thread != Thread.currentThread()) throw RuntimeException("You must be on UI thread.")
            return taskStatusListeners
                    .filter { it.taskId == id }
                    .mapNotNull { it.listener.get() }
        }

        /**
         * Listen to updates about a specific task and how it is going. Get updated when this specific task gets run by the task runner, then if it fails/succeeds/gets skipped.
         *
         * If the task with the [id] parameter does not exist, you will simply not receive any callbacks. Your listener will be ignored.
         *
         * @param id ID to the [PendingTask] you want to receive callbacks about.
         * @param listener Instance of listener to receive the callbacks. Note: Wendy keeps a weak reference to your listener. Make sure that *you* keep a strong reference to the listener you give!
         *
         * @see PendingTaskStatusListener to learn more about what callbacks to expect.
         */
        fun addTaskStatusListenerForTask(id: Long, listener: PendingTaskStatusListener) {
            val tasksRunner: PendingTasksRunner = PendingTasks.sharedInstance().runner

            taskStatusListeners.add(TaskStatusListener(id, WeakReference(listener)))

            if (tasksRunner.currentlyRunningTask?.id?.equals(id) == true) listener.running(id)
        }
    }

    internal class TaskStatusListener(val taskId: Long, val listener: WeakReference<PendingTaskStatusListener>)

}

internal fun WendyConfig.Companion.logTaskSkipped(task: PendingTask, reasonForSkip: ReasonPendingTaskSkipped) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.id).forEach {
            it.skipped(task.id, reasonForSkip)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.taskSkipped(reasonForSkip, task)
        }
    })
}

internal fun WendyConfig.Companion.logTaskRunning(task: PendingTask) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.id).forEach {
            it.running(task.id)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.runningTask(task)
        }
    })
}

internal fun WendyConfig.Companion.logTaskComplete(task: PendingTask, successful: Boolean, rescheduled: Boolean) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.id).forEach {
            it.complete(task.id, successful, rescheduled)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.taskComplete(successful, task, rescheduled)
        }
    })
}

internal fun WendyConfig.Companion.logAllTasksComplete() {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskRunnerListeners().forEach {
            it.allTasksComplete()
        }
    })
}

internal fun WendyConfig.Companion.logNewTaskAdded(task: PendingTask) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskRunnerListeners().forEach {
            it.newTaskAdded(task)
        }
    })
}