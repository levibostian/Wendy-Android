package com.curiosityio.wendy.activity

import android.support.v4.app.Fragment
import com.curiosityio.wendy.manager.BaseWendyDataManager
import io.realm.Realm

interface RealmInstanceActivityInitializer {

    // Note: I may get rid of this eventually. I use it for bad practice usually.
    interface RealmClosedListener {
        fun beforeRealmClose()
        fun afterRealmClose()
    }

    // Members to override and have:
    /*
    override var uiRealm: Realm? = null
    private var registeredDataManagers = ArrayList<BaseWendyDataManager>()
    private var realmClosedListeners = ArrayList<RealmInstanceActivityInitializer.RealmClosedListener>()
     */

    // Usually do this in onCreate?
    /*
    uiRealm = RealmInstanceManager.getTempInstance()
    registeredDataManagers.forEach {
        it.uiRealm = uiRealm
    }
    registeredDataManagers = ArrayList()
     */
    var uiRealm: Realm?

    /*
    override fun registerDataManagers(vararg baseDataManagers: BaseWendyDataManager) {
        if (uiRealm != null) {
            baseDataManagers.forEach {
                it.uiRealm = uiRealm
            }
        } else {
            registeredDataManagers.addAll(baseDataManagers)
        }
    }
     */
    fun registerDataManagers(vararg baseDataManagers: BaseWendyDataManager)

    /*
    override fun addRealmClosedListener(realmClosedListener: RealmInstanceActivityInitializer.RealmClosedListener) {
        realmClosedListeners.add(realmClosedListener)
    }
     */
    fun addRealmClosedListener(realmClosedListener: RealmClosedListener)

    /*
    override fun closeRealmInstance() {
        uiRealm?.let {
            realmClosedListeners.forEach {
                it.beforeRealmClose()
            }
            uiRealm!!.close()
            realmClosedListeners.forEach {
                it.afterRealmClose()
            }
            realmClosedListeners = ArrayList()
        }
    }
     */
    // Call in onDestroy()
    fun closeRealmInstance()

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