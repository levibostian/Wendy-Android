package com.curiosityio.wendy.`interface`

import android.support.v4.app.Fragment
import com.curiosityio.wendy.manager.BaseWendyDataManager
import io.realm.Realm

interface RealmInstanceActivityInitializer {

    // Note: I may get rid of this eventually. I use it for bad practice usually.
    interface RealmClosedListener {
        fun beforeRealmClose()
        fun afterRealmClose()
    }

    // Usually do this in onCreate?
    // uiRealm = RealmInstanceManager.getInstance()
    //for (BaseDataManager baseDataManager : mRegisteredDataManagers) {
    //  baseDataManager.setUiRealm(realm)
    //}
    //mRegisteredDataManagers = new ArrayList<>()
    var uiRealm: Realm?

    /*
    public void registerDataManagers(BaseDataManager... baseDataManagers) {
        if (uiRealm != null) {
            for (BaseDataManager dataManager : baseDataManagers) {
                dataManager.setUiRealm(realm)
            }
        } else {
            Collections.addAll(mRegisteredDataManagers, baseDataManagers);
        }
    }
     */
    fun registerDataManagers(vararg baseDataManagers: BaseWendyDataManager)

    /*
    private ArrayList<RealmClosedListener> mRealmClosedListeners = new ArrayList<>()

    public void addRealmClosedListener(RealmClosedListener realmClosedListener) {
        mRealmClosedListeners.add(realmClosedListener)
    }
     */
    fun addRealmClosedListener(realmClosedListener: RealmClosedListener)

    /*
    private void closeRealmInstance() {
        if (realm != null) {
            for (RealmClosedListener listener : mRealmClosedListeners) {
                listener.beforeRealmClose();
            }

            realm.close();

            for (RealmClosedListener listener : mRealmClosedListeners) {
                listener.afterRealmClose();
            }

            mRealmClosedListeners = new ArrayList<>();
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