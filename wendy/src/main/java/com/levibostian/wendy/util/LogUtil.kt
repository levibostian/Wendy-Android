package com.levibostian.wendy.util

import android.util.Log
import com.levibostian.wendy.WendyConfig

internal class LogUtil {
    companion object {
        fun d(message: String) {
            if (WendyConfig.debug) {
                Log.d(WendyConfig.logTag, message)
            }
        }

        fun w(message: String) {
            if (WendyConfig.debug) {
                Log.w(WendyConfig.logTag, message)
            }
        }
    }
}