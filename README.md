[![Release](https://jitpack.io/v/levibostian/Wendy-Android.svg)](https://jitpack.io/#levibostian/Wendy-Android)

# Wendy

Remove the difficulty in making offline-first Android apps.

![](misc/wendy_logo.jpg)

## What is Wendy?

Wendy is an Android library designed to help you make your app offline-first. Use Wendy to define sync tasks, then Wendy will run those tasks at the best time periodically to keep your app's offline data in sync with it's online storage.

Essentially, Wendy is a step up from the Android job runner API. It is used in production today in apps created by [Levi Bostian](https://levibostian.com).

## Why use Wendy?

When creating offline-first mobile apps there are 2 parts. 1. Persisting data to the user's Android device storage and 2. Sync that Android device's local storage with an online storage service.

Wendy helps you with item `#2`. You define how the local storage is supposed to sync with the remote storage and Wendy takes care of running those tasks for you periodically when the time is right.

Wendy is a FIFO task runner. You give it tasks, one by one, it persists those tasks to storage, then when Wendy has determined it's a good time for your task to run, it will call your task's sync function to perform a sync. If your user's device is online and has a good amount of battery, Wendy goes through all of the tasks available one by one until they all succeed or the network connection goes away where it will then try again later.

Wendy currently has the following functionality:

* Wendy uses the Android Job scheduler API to run tasks every 15 minutes when the device is online to keep data in sync without using the user's battery too much.
* Wendy is not opinionated. You may use whatever method you choose to sync data with it's remote storage and whatever method you choose to store data locally on the device. Wendy works with your workflow you already have.
* Dynamically allow and disallow tasks to sync at runtime. Wendy works in a FIFO style with it's tasks. When Wendy is about to run a certain task, it always asks the task if it is able to run.
* Mark tasks to manually run instead of automatically from Wendy. This allows you to use the same Wendy API to define all of your sync tasks, but Wendy will simply not attempt to run these tasks periodically automatically.
* Group tasks together to say, "if one of these tasks fails, skip all the future tasks in this group".

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

# Configure

Create a `PendingTasksFactory` instance that stores all of your app's Wendy `PendingTask`s. ([I plan to remove this requirement in the future](https://github.com/levibostian/Wendy-Android/issues/17). PRs welcome 😄)

```
class WendyExamplePendingTasksFactory : PendingTasksFactory {

    override fun getTask(tag: String, task: PendingTask): PendingTask {
        return when (tag) {
            FooPendingTask::class.java.simpleName -> FooPendingTask.blank().fromSqlObject(task)
            else -> throw RuntimeException("No idea what task that is... tag: $tag")
        }
    }

}
```

Add the following code to your Application `onCreate()`:

```
PendingTasks.init(this, WendyExamplePendingTasksFactory())
```

Wendy is not configured. It's time to use it!

To use Wendy to sync your local storage with remote storage, first define a `PendingTask`:

```
class FooPendingTask : PendingTask() {

    companion object {
        fun blank(): FooPendingTask { return FooPendingTask() }
    }

    override fun runTask(complete: (successful: Boolean) -> Unit) {
        // Here, instantiate your dependencies, talk to your DB, your API, etc. Run the task.
        // After you are done (or failed), call `complete()` defining if the task was successful and does not need to be run again or it failed and it must be run in the future.
        complete(true)
    }

}
```

Then, when your app user's perform a task in the app that needs to sync with remote storage, create an instance of your `FooPendingTask` and hand that to Wendy:

```
PendingTasks.sharedInstance().addTask(new FooPendingTask())
```

Wendy takes care of all the rest.

## Author

* Levi Bostian - [GitHub](https://github.com/levibostian), [Twitter](https://twitter.com/levibostian), [Website/blog](http://levibostian.com)

![Levi Bostian image](https://gravatar.com/avatar/22355580305146b21508c74ff6b44bc5?s=250)

## Contribute

Wendy is open for pull requests. Before you decide to take a bunch of time and add functionality to the library, please, [create an issue](https://github.com/levibostian/Wendy-Android/issues/new) stating what you wish to add. This might save you some time in case your purpose does not fit well in the use cases of Wendy.

### Where did the name come from?

User wants fast results. Apps without loading screens...

Sometimes people want fast results with their food. Fast food....

...Wendy

# Credits

Header photo by [Allef Vinicius](https://unsplash.com/photos/FPDGV38N2mo?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText) on [Unsplash](https://unsplash.com/search/photos/red-head?utm_source=unsplash&utm_medium=referral&utm_content=creditCopyText)