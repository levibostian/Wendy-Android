package com.curiosityio.wendy.manager

import com.curiosityio.wendy.model.PendingApiTask
import io.realm.RealmObject
import java.util.*

class PendingApiTasksManager {

    companion object {
        var registeredPendingApiTasks: ArrayList<Class<PendingApiTask<Any>>> = ArrayList()

        fun registerPendingApiTasks(vararg pendingApiTaskClasses: Class<PendingApiTask<Any>>) {
            registeredPendingApiTasks.addAll(pendingApiTaskClasses)
        }
    }

}

