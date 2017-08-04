# Wendy

Wendy is an Android library designed to help you build offline-first mobile apps. Building offline compatible mobile apps is pretty tough. There is a lot of code to write to get it working. There are services such as Firebase that give you offline capabilities but that is *only* if you use Firebase services. 

Wendy is to help build offline capable mobile apps fast and flexible. Send Wendy data from your API or from a user generated task and it takes care of performing the task for you in the background. 

*Note: Wendy is in alpha stage at the moment. [I am](http://levibostian.com) running it in production apps at the moment with it working great, but the API will change and bugs will be fixed. Enjoy, contribute, open issues.*

Uses the power of [Realm](https://realm.io/), [Retrofit](https://github.com/square/retrofit), [Kotlin](https://kotlinlang.org/), and [Android Job](https://github.com/evernote/android-job) to be fast and efficient. 

# Install

Add this to your root build.gradle at the end of repositories:

```
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

Then, install the Wendy module:

```
compile 'com.github.curiosityio:wendy:03ac99c8d8' # make sure to change 03ac99c8d8 to the commit or tag you wish.
```

# Configure

Add the following code to your Application `onCreate()`:

```
AndroidRealmConfig.overrideRealmInstanceConfig(AndroidRealmInstanceConfig())
AndroidRealmConfig.setRealmMigrationManager(AndroidRealmMigrationManager())
WendyConfig.overrideProcessApiResponse(WendyProcessApiResponse())
WendyConfig.setTasksRunnerManager(WendyTasksRunnerManager())
WendyConfig.setErrorNotifier(ErrorNotifier())

Realm.init(this) // Initialize Realm.
```

*Note: Wendy depends on [AndroidRealm](https://github.com/curiosityio/AndroidRealm) library to work with saving the models to a database. Since you are using this lib, I am assuming you are also using Realm since you are creating the models. So, make sure to configure AndroidRealm as well as this lib.*

# Run tasks

After you create tasks, you probably want to run them. After I save a new `PendingApiTaskModel` to the Realm database, I run the task runner:

```
pendingApiTasksRunner.runPendingTasks()
        .subscribe({
        }, { error -> LogUtil.error(error) })
```

Then, I like to add a BroadcastReceiver for when WiFi state changes:

```
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import com.curiosityio.androidboilerplate.util.InternetConnectionUtil
import com.curiosityio.androidboilerplate.util.LogUtil
import rx.functions.Action1
import rx.functions.Action0
import rx.schedulers.Schedulers
import javax.inject.Inject

class WiFiStateReceiver : BroadcastReceiver() {

    @Inject lateinit var pendingApiTasksRunner: PendingApiTasksRunner

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent == null) return

        MainApplication.component.inject(this)

        if (InternetConnectionUtil.isAnyInternetConnected(context!!)) {
            pendingApiTasksRunner.runPendingTasks()
                    .subscribe({
                    }, { error -> LogUtil.error(error) })
        }
    }

}
```

*Note: Don't forget to add this BroadcastReceiver to your manifest file:*

```
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.curiosityio.foo">

    <application
        ...
        <receiver android:name=".receiver.WiFiStateReceiver">
            <intent-filter android:priority="100">
                <action android:name="android.net.wifi.STATE_CHANGE" />
            </intent-filter>
        </receiver>
    </application>

</manifest>
```

Then, I use the [android-job](https://github.com/evernote/android-job) library to run the runner periodically:

```
compile 'com.evernote:android-job:1.1.3'
```

In your Application's `onCreate()` add:

```
JobManager.create(this).addJobCreator(PendingApiTaskJobCreator())
```

Create `PendingApiTaskJobCreator`:

```
import com.evernote.android.job.Job
import com.evernote.android.job.JobCreator
import android.R.attr.tag

class PendingApiTaskJobCreator : JobCreator {

    override fun create(tag: String?): Job? {
        when (tag) {
            PendingApiTaskJob.TAG -> return PendingApiTaskJob()
            else -> return null
        }
    }

}
```

Create `PendingApiTaskJob`:

```
import com.evernote.android.job.Job
import com.evernote.android.job.JobRequest
import com.curiosityio.androidboilerplate.util.LogUtil
import rx.schedulers.Schedulers
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class PendingApiTaskJob : Job() {

    @Inject lateinit var pendingApiTasksRunner: PendingApiTasksRunner

    companion object {
        val TAG = "pendingApiTaskJob.tag"

        fun scheduleJob() {
            JobRequest.Builder(TAG)
                    .setRequiredNetworkType(JobRequest.NetworkType.CONNECTED)
                    .setPeriodic(TimeUnit.MINUTES.toMillis(15), TimeUnit.MINUTES.toMillis(5))
                    .setRequirementsEnforced(false)
                    .setPersisted(true) // works across reboots.
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .setUpdateCurrent(true) // allows us to only have 1 of TAG jobs running at 1 time. Good for this recurring task.
                    .build()
                    .schedule()
        }
    }

    override fun onRunJob(params: Params?): Result {
        MainApplication.component.inject(this)

        val countdownLatch = CountDownLatch(1)
        pendingApiTasksRunner.runPendingTasks()
                .subscribeOn(Schedulers.io())
                .subscribe({
                    countdownLatch.countDown()
                }, { error ->
                    countdownLatch.countDown()
                })

        try {
            countdownLatch.await()
        } catch (e: InterruptedException) {
            LogUtil.error(e)
        }

        return Result.SUCCESS
    }

}
```

## Author 

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

### Where did the name come from?

User wants fast results. Doesn't want to wait for network calls --> Fast food. User doesn't want to wait for food. --> Wendy.
