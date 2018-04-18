package com.levibostian.wendy.util

import android.content.Context
import android.preference.PreferenceManager

internal object PendingTasksUtil {

    private const val PREFIX = "WENDY_PREFS_"
    private const val RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY = "${PREFIX}RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY"

    internal fun rerunCurrentlyRunningPendingTask(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY, false)
    }

    internal fun setRerunCurrentlyRunningPendingTask(context: Context, rerun: Boolean) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(RERUN_CURRENTLY_RUNNING_PENDING_TASK_KEY, rerun).commit()
    }

    internal fun resetRerunCurrentlyRunningPendingTask(context: Context) {
        this.setRerunCurrentlyRunningPendingTask(context, false)
    }

}