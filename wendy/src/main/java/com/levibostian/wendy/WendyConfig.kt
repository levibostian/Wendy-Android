package com.levibostian.wendy

import android.os.Handler
import android.os.Looper
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.listeners.TaskRunnerListener
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.Wendy
import com.levibostian.wendy.service.PendingTasksRunner
import com.levibostian.wendy.types.ReasonPendingTaskSkipped
import java.lang.ref.WeakReference

/**
 * Configure Wendy and how it operates.
 */
class WendyConfig {

    companion object {
        /**
         * Turn on and off the ability for Wendy to automatically run all of the [PendingTask]s set to not manually run for you.
         * This takes place when you:
         * 1. Add a task to Wendy via [Wendy.addTask].
         * 2. The periodically scheduled execution of all [PendingTask]s by Wendy.
         *
         * *Note:* You can still have this property set to false and manually call [Wendy.runTasks] to run all tasks.
         */
        var automaticallyRunTasks: Boolean = true
        /**
         * Sets Wendy to strict mode or not. Strict mode helps while developing your app to find and fix potential issues while working with Wendy.
         *
         * Strict mode is **only** used in scenarios that a warning is not critical to the success of your app. How success if defined here is: (1) Your user does not get confused when they perform an action in your app and the request is simply ignored resulting in them being confused and (2) your app crashing only when you have hit a major issue that you really should fix (wendy cannot work without you fixing these crashes).
         *
         * Wendy **does** still throw exceptions. Strict mode does *not* silence all exceptions. Strict simply gives you the option to silence warnings when it's really not a big deal that your code works in a certain way. It's assumed you know what you are doing.
         *
         * When strict is true, warnings throw errors instead of logging the warning to the console.
         * When strict is false, warnings get logged to the console and not crash your app instead.
         *
         * Default: true
         */
        var strict: Boolean = true
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

        /**
         * Removes a listener instance added one or more times with [addTaskRunnerListener]
         * Note: if you added the same instance more than one times, this function will remove all of them
         *
         * @param listener the listener instance to be removed
         *
         * @return true if the listener was removed at least once, false if it was collected by the GC
         * or you didn't added it before
         */
        fun removeTaskRunnerListener(listener: TaskRunnerListener): Boolean {
            return taskRunnerListeners
                .removeAll { it.get()?.equals(listener) ?: false }
        }

        internal fun getTaskRunnerListeners(): List<TaskRunnerListener> {
            if (Looper.getMainLooper().thread != Thread.currentThread()) throw RuntimeException("You must be on UI thread.")
            return taskRunnerListeners
                    .mapNotNull { it.get() }
        }

        private var taskStatusListeners = ArrayList<TaskStatusListener>()
        internal fun getTaskStatusListenerForTask(taskId: Long): List<PendingTaskStatusListener> {
            if (Looper.getMainLooper().thread != Thread.currentThread()) throw RuntimeException("You must be on UI thread.")
            return taskStatusListeners
                    .filter { it.taskId == taskId }
                    .mapNotNull { it.listener.get() }
        }

        /**
         * Listen to updates about a specific task and how it is going. Get updated when this specific task gets run by the task runner, then if it fails/succeeds/gets skipped.
         *
         * If the task with the [taskId] parameter does not exist, you will simply not receive any callbacks. Your listener will be ignored.
         *
         * @param taskId The taskId to the [PendingTask] you want to receive callbacks about.
         * @param listener Instance of listener to receive the callbacks. Note: Wendy keeps a weak reference to your listener. Make sure that *you* keep a strong reference to the listener you give!
         *
         * @see PendingTaskStatusListener to learn more about what callbacks to expect.
         */
        fun addTaskStatusListenerForTask(taskId: Long, listener: PendingTaskStatusListener) {
            val tasksRunner: PendingTasksRunner = Wendy.sharedInstance().runner

            taskStatusListeners.add(TaskStatusListener(taskId, WeakReference(listener)))

            if (tasksRunner.currentlyRunningTask?.id?.equals(taskId) == true) listener.running(taskId)
            Wendy.shared.getLatestError(taskId)?.let { latestError ->
                listener.errorRecorded(taskId, latestError.errorMessage, latestError.errorId)
            }
        }

        /**
         * Removes a listener instance added one or more times with [addTaskStatusListenerForTask]
         * Note: if you added the same instance more than one times, this function will remove all of them
         *
         * @param listener the listener instance to be removed
         *
         * @return true if the listener was removed at least once, false if it was collected by the GC
         * or you didn't added it before
         */
        fun removeTaskStatusListener(listener: PendingTaskStatusListener): Boolean {
            return taskStatusListeners
                .removeAll { it.listener.get()?.equals(listener) ?: false }
        }
    }

    internal class TaskStatusListener(val taskId: Long, val listener: WeakReference<PendingTaskStatusListener>)

}

internal fun WendyConfig.Companion.logTaskSkipped(task: PendingTask, reasonForSkip: ReasonPendingTaskSkipped) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.taskId ?: 0).forEach {
            it.skipped(task.taskId!!, reasonForSkip)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.taskSkipped(reasonForSkip, task)
        }
    })
}

internal fun WendyConfig.Companion.logTaskRunning(task: PendingTask) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.taskId ?: 0).forEach {
            it.running(task.taskId!!)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.runningTask(task)
        }
    })
}

internal fun WendyConfig.Companion.logTaskComplete(task: PendingTask, successful: Boolean) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.taskId ?: 0).forEach {
            it.complete(task.taskId!!, successful)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.taskComplete(successful, task)
        }
    })
}

internal fun WendyConfig.Companion.logErrorRecorded(task: PendingTask, errorMessage: String?, errorId: String?) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.taskId ?: 0).forEach {
            it.errorRecorded(task.taskId!!, errorMessage, errorId)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.errorRecorded(task, errorMessage, errorId)
        }
    })
}

internal fun WendyConfig.Companion.logErrorResolved(task: PendingTask) {
    Handler(Looper.getMainLooper()).post({
        WendyConfig.getTaskStatusListenerForTask(task.taskId ?: 0).forEach {
            it.errorResolved(task.taskId!!)
        }
        WendyConfig.getTaskRunnerListeners().forEach {
            it.errorResolved(task)
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