package com.levibostian.wendyexample

import android.annotation.SuppressLint
import android.app.Application
import com.levibostian.wendyexample.BuildConfig
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.levibostian.wendy.service.Wendy
import android.os.Build
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_DESCRIPTION
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_ID
import com.levibostian.wendyexample.NotificationChannelUtil.Companion.ERROR_OCCURRED_CHANNEL_NAME
import com.levibostian.wendyexample.worker.PendingTasksWorker
import java.util.concurrent.TimeUnit

class MainApplication : Application() {

    @SuppressLint("NewApi")
    override fun onCreate() {
        super.onCreate()

        Wendy.init(this, WendyExamplePendingTasksFactory())
                .debug(BuildConfig.DEBUG)

        val pendingTaskWorkerBuilder = PeriodicWorkRequest.Builder(PendingTasksWorker::class.java, 30, TimeUnit.MINUTES)
        val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        WorkManager.getInstance().enqueue(pendingTaskWorkerBuilder.setConstraints(constraints).build())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(ERROR_OCCURRED_CHANNEL_ID, ERROR_OCCURRED_CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = ERROR_OCCURRED_CHANNEL_DESCRIPTION

            val notificationManager = this.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

}