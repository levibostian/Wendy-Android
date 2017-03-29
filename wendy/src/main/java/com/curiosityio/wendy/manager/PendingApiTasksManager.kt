package com.curiosityio.wendy.manager

import com.curiosityio.wendy.model.PendingApiTask
import io.realm.RealmObject
import java.util.*

class PendingApiTasksManager {

    companion object {
        var registeredPendingApiTasks: ArrayList<Class<out PendingApiTask<out Any>>> = ArrayList()

        fun registerPendingApiTasks(vararg pendingApiTaskClasses: Class<out PendingApiTask<out Any>>) {
            registeredPendingApiTasks.addAll(pendingApiTaskClasses)
        }
    }

}

