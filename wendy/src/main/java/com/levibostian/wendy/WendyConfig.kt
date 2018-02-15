package com.levibostian.wendy

import android.os.Looper
import com.levibostian.wendy.listeners.PendingTaskStatusListener
import com.levibostian.wendy.listeners.TaskRunnerListener
import java.lang.ref.WeakReference

open class WendyConfig {

    companion object {
        var debug: Boolean = false
        var logTag: String = "WENDY"

        private var taskRunnerListeners = ArrayList<WeakReference<TaskRunnerListener>>()
        /**
         * Note: Wendy keeps a weak reference to your listener. Make sure that *you* keep a strong reference to the listener you give!
         */
        fun addTaskRunnerListener(listener: TaskRunnerListener) = taskRunnerListeners.add(WeakReference(listener))
        fun getTaskRunnerListeners(): List<TaskRunnerListener> {
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
        internal fun addTaskStatusListenerForTask(id: Long, listener: PendingTaskStatusListener) {
            taskStatusListeners.add(TaskStatusListener(id, WeakReference(listener)))
        }
    }

    class TaskStatusListener(val taskId: Long, val listener: WeakReference<PendingTaskStatusListener>)

}