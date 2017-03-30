package com.curiosityio.wendy.`interface`

import android.support.v4.app.Fragment
import com.curiosityio.wendy.manager.BaseWendyDataManager
import io.realm.Realm

interface RealmInstanceActivityInitializer {

    interface RealmClosedListener {
        fun beforeRealmClose()
        fun afterRealmClose()
    }

    var uiRealm: Realm?

    fun registerDataManagers(vararg baseDataManagers: BaseWendyDataManager)

    fun addRealmClosedListener(realmClosedListener: RealmClosedListener)

}

fun Fragment.registerDataManagers(vararg baseDataManagers: BaseWendyDataManager) {
    if (activity !is RealmInstanceActivityInitializer) throw RuntimeException("Your activity does not inherit RealmInstanceActivityInitializer")
    else (activity as RealmInstanceActivityInitializer).registerDataManagers(*baseDataManagers)
}

fun android.app.Fragment.registerDataManagers(vararg baseDataManagers: BaseWendyDataManager) {
    if (activity !is RealmInstanceActivityInitializer) throw RuntimeException("Your activity does not inherit RealmInstanceActivityInitializer")
    else (activity as RealmInstanceActivityInitializer).registerDataManagers(*baseDataManagers)
}

fun Fragment.addRealmClosedListener(realmClosedListener: RealmInstanceActivityInitializer.RealmClosedListener) {
    if (activity !is RealmInstanceActivityInitializer) throw RuntimeException("Your activity does not inherit RealmInstanceActivityInitializer")
    else (activity as RealmInstanceActivityInitializer).addRealmClosedListener(realmClosedListener)
}

fun android.app.Fragment.addRealmClosedListener(realmClosedListener: RealmInstanceActivityInitializer.RealmClosedListener) {
    if (activity !is RealmInstanceActivityInitializer) throw RuntimeException("Your activity does not inherit RealmInstanceActivityInitializer")
    else (activity as RealmInstanceActivityInitializer).addRealmClosedListener(realmClosedListener)
}