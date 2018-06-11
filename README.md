[![Release](https://jitpack.io/v/levibostian/Wendy-Android.svg)](https://jitpack.io/#levibostian/Wendy-Android)

# Wendy

Remove the difficulty in making offline-first Android apps. Sync your offline device storage with remote cloud storage easily. When building offline-first mobile apps, there are *lots* of use cases to think about. Wendy takes care of handling them all for you!

![project logo](misc/wendy_logo.jpg)

[Read the official announcement of Wendy](https://levibostian.com/blog/no-more-excuses-build-offline-apps/) to learn more about what it does and why to use it.

iOS developer? [I created an iOS version of Wendy too!](https://github.com/levibostian/wendy-ios)

## What is Wendy?

Wendy is an Android library designed to help you make your app offline-first. Use Wendy to define sync tasks, then Wendy will run those tasks periodically to keep your app's device offline data in sync with it's online remote storage.

Wendy is a FIFO task runner. You give it tasks, one by one, it persists those tasks to storage, then when Wendy has determined it's a good time for your task to run, it will call your task's sync function to perform a sync. If your user's device is online and has a good amount of battery, Wendy goes through all of the tasks available one by one running them to succeed or fail and try again.

*Note: Wendy is currently in an alpha stage. The API most definitely could change, but it is used in production apps today. Use the latest release of Wendy as you wish but be prepared for having to update your code base in future releases.*

## Why use Wendy?

When creating offline-first mobile apps there are 2 tasks you need to do in your code. 1. Persisting data to the user's Android device storage and 2. Sync that user's storage with remote online storage.

Wendy helps you with item #2. You define how the local storage is supposed to sync with the remote storage and Wendy takes care of running those tasks for you periodically when the time is right.

Wendy currently has the following functionality:

* Wendy uses the Android Job scheduler API to run tasks every 15 minutes when the device is online to keep data in sync without using the user's battery too much.
* Wendy is not opinionated. You may use whatever method you choose to sync data with it's remote storage and whatever method you choose to store data locally on the device. Wendy works with your workflow you already have. Store user data in Sqlite locally and a Rails API for the cloud storage. Store user data in Realm locally and a Parse server for the cloud storage. Use just shared preferences and GraphQL. Whatever you want, Wendy works with it.
* Dynamically allow and disallow tasks to sync at runtime. Wendy works in a FIFO style with it's tasks. When Wendy is about to run a certain task, it always asks the task if it is able to run.
* Mark tasks to manually run instead of automatically from Wendy. This allows you to use the same Wendy API to define all of your sync tasks, but Wendy will simply not attempt to run these tasks periodically automatically.
* Group tasks together to enforce they all run (and succeed) in an exact order from start to finish.
* Wendy also comes with an error reporter to report errors that your user needs to fix for a task to succeed.
* Wendy takes care of all of the use cases that could happen with building an offline-first mobile app. "What if this task succeeds but this one doesn't? What happens when the network is flaky and a couple of tasks fail but should retry? What happens if this task needs to succeed in order for this task to succeed on my API?" Wendy takes care of handling all of this for you. You define the behavior, Wendy takes care of running it when it is confident it can run the task and succeed.

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
compile 'com.github.levibostian:wendy-android:0.1.0'
```

The latest release version at this time is: [![Release](https://jitpack.io/v/levibostian/Wendy-Android.svg)](https://jitpack.io/#levibostian/Wendy-Android)

# Getting started

For this getting started guide, lets work through an example for you to follow along with. Let's say you are building a grocery list app. We will call it, `Grocery List`.

First, create a `PendingTasksFactory` subclass that stores all of your app's Wendy `PendingTask`s. It's pretty blank to start but we will add more to it later. ([I plan to remove this requirement in the future](https://github.com/levibostian/Wendy-Android/issues/17). PRs welcome 😄)

```
class GroceryListPendingTasksFactory : PendingTasksFactory {

    override fun getTask(tag: String): PendingTask? {
        return null
    }

}
```

Add the following code to your Application `onCreate()`:

```
Wendy.init(this, GroceryListPendingTasksFactory())
```

Wendy is now configured. It's time to use it!

For each separate task that you need to sync local storage with remote cloud storage, you define a `PendingTask` subclass.

In our Grocery List app, we want to allow users to create new grocery items. Every time that a user creates a new grocery list item, we don't want to show them a progress bar saying, "Saving grocery list item..." while we perform an API call! We want to be able to *instantly* save that grocery list item and sync it with the cloud storage later so our user can get on with their life (can't you just see your Play Store reviews going up, up, up right now? ⭐⭐⭐⭐⭐).

Let's create our first `PendingTask` subclass for creating new grocery items.

```
class CreateGroceryListItemPendingTask(groceryStoreItemId: Long) : PendingTask(
    manuallyRun = false,
    dataId = groceryStoreItemId.toString(),
    groupId = null,
    tag = CreateGroceryListItemPendingTask::class.java.simpleName) {

    val GROCERY_STORE_ITEM_TEXT_TOO_LONG = "GROCERY_STORE_ITEM_TEXT_TOO_LONG"

    lateinit var localDatabase: Database

    companion object {
        // Use the `blank()` static constructor to provide dependencies to the PendingTask.
        fun blank(database: Database): CreateGroceryListItemPendingTask = CreateGroceryListItemPendingTask(0).apply {
            localDatabase = database
        }
    }

    override fun runTask(): PendingTaskResult
        // Here, instantiate your dependencies, talk to your DB, your API, etc. Run the task.
        // After the task succeeds or fails, return to Wendy the result.

        val groceryStoreItem = localDatabase.queryGroceryStoreItem(dataId)

        // Your SQL queries, API calls, etc. in `runTask()` need to be synchronous. Don't worry, you are running on a background thread already so it's all good.
        // If you still feel you want to run asynchronous code, [check out the Wendy best practices doc](BEST_PRACTICES.md) to learn how to do so.
        val apiCallResult = performApiCall(groceryStoreItem)

        if (apiCallResult.error != null) {
             // There was an error. Parse the error and decide what to do from here.

             // If it's an error that deserves the attention of your user to fix, make sure and record it with Wendy.
             // If the error is a network error, for example, that does not require the user's attention to fix, do *not* record an error to Wendy.
             // Wendy will not run your task if there is a recorded error for it. Record an error, prompt your user to fix it, then resolve it ASAP so it can run.
             Wendy.shared.recordError(taskId, "Grocery store item too long. Please shorten it up for me.", GROCERY_STORE_ITEM_TEXT_TOO_LONG)
        }

        return if (successful) PendingTaskResult.SUCCESSFUL else PendingTaskResult.FAILED
    }

}
```

Each time that you create a new subclass of `PendingTask`, you need to add that to the `PendingTasksFactory` you created. Your `GroceryListPendingTasksFactory` should look like this now:

```
class GroceryListPendingTasksFactory(private val database: Database): PendingTasksFactory {

    override fun getTask(tag: String): PendingTask? {
        return when (tag) {
            CreateGroceryListItemPendingTask::class.java.simpleName -> CreateGroceryListItem.blank(database)
            else -> null
        }
    }

}
```

*Note:* As you can see, we are providing an instance of `Database` to the `PendingTasksFactory` above which is then passed to `CreateGroceryListItem.blank(database)`. Feel free to use this design pattern of providing dependencies or use a tool such as [Dagger](https://google.github.io/dagger/) to inject the dependencies into your `PendingTasksFactory` instance.

Just about done.

Let's check out the code you wrote in your Grocery List app when your users want to create a new grocery store item in the app.

```
fun createNewGroceryStoreItem(itemName: String) {
    // First thing you need to do to make a mobile app offline-first is to save it to the device's storage.
    // Below, we are saving to a `localDatabase`. Whatever that is. It could be whatever you wish. Sqlite, Realm, shared preferences, whatever you decide to use works. After we save to the database, we probably get an ID back to reference that piece of data in the database. This ID could be the shared preferences key, the database row ID, it doesn't matter. Simply some way to identify that piece of data *to query later*.
    val id: Long = localDatabase.createNewGroceryStoreItem(itemName)

    // We will now create a new `CreateGroceryListItemPendingTask` pending task instance and give it to Wendy.
    val pendingTaskId: Long = Wendy.sharedInstance().addTask(CreateGroceryListItemPendingTask(id))

    // When you add a task to Wendy, you get back an ID for that new `PendingTask`. It's your responsibility to save that ID (or ignore it). It's best practice to save that ID with the data that this `PendingTask` links to. In our example here, the grocery store item in our localDatabase is where we should save the ID.
    localDatabase.queryGroceryStoreItem(id).pendingTaskId = pendingTaskId

    // The reason you may want to save the ID of the `PendingTask` is to assert that it runs successfully. Also, you can show in the UI of your app the syncing status of that data to the user. This is all optional, but recommended for the best user experience.
    WendyConfig.addTaskStatusListenerForTask(pendingTaskId, object : PendingTaskStatusListener {
        override fun skipped(taskId: Long, reason: ReasonPendingTaskSkipped) {
            // The task was skipped to run. You may use the Visitor design pattern, like below, or create a conditional around the enum to find out the reason why it was skipped.
            reason.accept(object : ReasonPendingTaskSkipped.Visitor<String> {
                override fun visitNotReadyToRun(): String {
                    // The task was skipped because the PendingTask instance returned `false` for the function `canRunTask()`.
                }
            })
        }
        override fun running(taskId: Long) {
            // The task is now running.
        }
        override fun complete(taskId: Long, successful: Boolean, rescheduled: Boolean) {
            // The task was complete. It was either successful, or it failed and was rescheduled to run again or not.
        }
    })
}
```

Done! Wendy takes care of all the rest. Wendy will try to run your task right away but if you're offline or in a spotty Internet connection, Wendy will wait and try again later.

Oh, and lastly. If your user decides to logout of your app and you want to delete all of Wendy's data, you can do so via `Wendy.shared.clear()` or `Wendy.shared.clearAsync()`. 

There is a document on [best practices when using Wendy](BEST_PRACTICES.md). Check that out to answer your questions you have about why Wendy works the way that it does.

## Example app

This library comes with an example app. You may open it in Android Studio to test it out and see how the code works with the library.

## Documentation

There is a Javadoc (Kotlin doc, actually) for all of the public classes of Wendy [hosted here](https://levibostian.github.io/Wendy-Android/wendy/) for the `master` branch.

The docs are installed in the `docs/` directory and can be generated from any branch with command: `./gradlew dokka`

## Configure Wendy

Use the class `WendyConfig` to configure the behavior of Wendy.

* Register listeners to Wendy task runner.

```
WendyConfig.addTaskRunnerListener(listener)
```

* Register listeners to a specific Wendy `PendingTask`.

```
WendyConfig.addTaskStatusListenerForTask(pendingTaskId, listener)
```

* Have Wendy log debug statements as it's running during development.

```
WendyConfig.debug = true # default is false.
```

I recommend doing the following:

```
WendyConfig.debug = BuildConfig.DEBUG
```

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

Wendy is open for pull requests. Check out the [list of issues](https://github.com/levibostian/wendy-android/issues) for tasks I am planning on working on. Check them out if you wish to contribute in that way.

**Want to add features to Wendy?** Before you decide to take a bunch of time and add functionality to the library, please, [create an issue](https://github.com/levibostian/Wendy-Android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of Wendy.

### Where did the name come from?

Users wants fast results in their mobile app. They don't to wait for loading screens.

Sometimes people want fast results with their food, too. They want fast food...

...Wendy

Get it?

# Credits

Header photo by [Allef Vinicius](https://unsplash.com/photos/FPDGV38N2mo?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/search/photos/red-head?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText)

Thank you to [evernote/android-job](https://github.com/evernote/android-job) for providing an awesome job scheduling library that Wendy uses to run tasks periodically, but also for inspiration while building this library. When I had questions about best practices and how to design the API, android-job gave me great wisdom.