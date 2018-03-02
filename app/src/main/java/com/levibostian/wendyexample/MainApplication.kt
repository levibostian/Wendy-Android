package com.levibostian.wendyexample

import android.annotation.SuppressLint
import android.app.Application
import com.curiosityio.wendyexample.BuildConfig
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.levibostian.wendy.service.PendingTasks
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.curiosityio.wendyexample.R
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_DESCRIPTION
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_ID
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_NAME

class MainApplication : Application() {

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        PendingTasks.init(this, WendyExamplePendingTasksFactory())
                .debug(BuildConfig.DEBUG)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(ERROR_OCCURRED_CHANNEL_ID, ERROR_OCCURRED_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = ERROR_OCCURRED_CHANNEL_DESCRIPTION

            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}