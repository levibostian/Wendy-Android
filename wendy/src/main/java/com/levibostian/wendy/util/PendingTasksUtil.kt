package com.levibostian.wendy.util

import android.content.Context
import android.preference.PreferenceManager
import java.util.*

internal object PendingTasksUtil {

    private const val PREFIX = "WENDY_PREFS_"
    private const val RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY = "${PREFIX}RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY"

    internal fun rerunCurrentlyRunningPendingTask(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY, 0L) > 0
    }

    internal fun getRerunCurrentlyRunningPendingTaskValue(context: Context): Date? {
        val value = PreferenceManager.getDefaultSharedPreferences(context).getLong(RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY, 0L)
        return if (value > 0L) Date(value) else null
    }

    internal fun setRerunCurrentlyRunningPendingTask(context: Context, date: Date? = Date()) {
        val valueToSet: Long = date?.time ?: 0L
        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong(RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY, valueToSet).commit()
    }

    internal fun resetRerunCurrentlyRunningPendingTask(context: Context) {
        this.setRerunCurrentlyRunningPendingTask(context, null)
    }

}