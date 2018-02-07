package com.levibostian.wendyexample

import android.app.Application
import com.levibostian.wendy.service.PendingTasks

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        PendingTasks.init(this, WendyExamplePendingTasksFactory())
    }

}