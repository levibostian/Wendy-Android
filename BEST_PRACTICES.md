# How do I update a PendingTask that I already added to Wendy?

You don't update the `PendingTask`. This is on purpose.

You know the `data_id` property in `PendingTask`? That `data_id` property is meant to be an identifier used to query data when your `PendingTask` is run.

Because you are supposed to query for the data of a job right before a job is run, there is no need to update a `PendingTask`. If the user edits data represented by a certain `data_id` `PendingData`, the freshest data will be the data that gets synced.

# PendingTask subclasses each having 1 specific use case

Each subclass of PendingTask that you create in your app should represent 1 task a user can perform in the app.

Example: You are building a grocery list app. Users of your app can complete the following tasks:

* Add grocery store list items.
* Edit the name of the already added grocery store list items.
* Delete grocery store list items.
* Update the profile picture of their account they created.

Because there are 4 separate, small tasks that users can do in your app, your app code needs to have 4 separate subclasses of PendingTask. One for each task. Sure, you might think you can and/or should combine the top 3 tasks into a PendingTask subclass called `GroceryStoreListItemPendingTask` and have all of the various abilities combined into 1 and have 1 subclass `ProfilePendingTask` for the last task of updating the profile picture. This is not the way Wendy is intended to work.

Wendy requires each subclass of `PendingTask` is designed to perform 1 task. There are checks in Wendy that will throw exceptions on you if you do not follow this rule. If you do, you should not have an issue. Here is a list of those checks Wendy performs:

* Every instance of a `PendingTask` subclass must **all** have a `groupId` or **all** must *not* have a `groupId`.

# How do I delete a PendingTask that I added to Wendy?

You don't delete `PendingTask`s. That is on purpose.

`PendingTask`s are meant for user actions. The user does something, that needs to get recorded in your app. If the user creates data and then deletes data in your app, your app should create separate `PendingTask`s for each of those actions instead of deleting the `PendingTask` meant for creating the data.

### Is there an 'undo' function for Wendy PendingTasks?

As explained above, you cannot delete a `PendingTask`. That also includes undo.

What is a best practice here, is when a user performs an action in your app that you want to offer an 'undo' feature for (example: Delete a piece of data), you should:

* Show the user in the UI a way to 'undo' their action. For example: Showing a Snackbar with an undo action.
* If the user presses the undo button in the UI, do not create a `PendingTask`. It's like it never happened.
* If the user does not press the undo button in the UI before the timeout, *then* create the `PendingTask` and add it to Wendy via `PendingTasks.addTask()`. 

### Can I run async operations in `runTask()` of my `PendingTask`?

If you can help it, no, do not. Run sync operations whenever you can. The code is taking place in a background thread anyway so it's all good.

If you need to run async operations, here is some inspiration thanks to [evernote/android-job](https://github.com/evernote/android-job/wiki/FAQ#how-can-i-run-async-operations-in-a-job)'s wiki page.

```kotlin
public class FooPendingTask extends PendingTask {

    @Override
    public PendingTaskResult runTask() {
        final CountDownLatch countDownLatch = new CountDownLatch(1);

        new Thread() {
            @Override
            public void run() {
                // do async operation here

                SystemClock.sleep(3_000L);
                countDownLatch.countDown();
            }
        }.start();

        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }

        return PendingTaskResult.SUCCESSFUL;
    }
}
```

### How do I handle errors in my `runTask()`?

Errors may seem like a scary thing when working with Wendy or any other method of creating offline first apps. Because errors may happen at anytime in the background when Wendy decides to run the `PendingTask`s. When errors do happen, a big question you may have is, "What do I do?!"

Here is the best practice for how to handle errors with Wendy.

Let's say you have a `PendingTask` named `CreateGroceryStoreItemPendingTask`. It calls your backend API to create a new grocery store item in the database that the user of your app entered in.

Let's say that your API only allows grocery store items to be of String length 10. If the String sent to the API is greater then 10, your API will return back an error to you.

**Best practice: Use client side code when you can to prevent even having errors occur in the first place.** In this example above, if this was an app that I was developing, I would have code written that when a user *attempts* to save a grocery store item in the app, I first check if the string length is <= 10. Then, in the UI, instruct the user to fix it right then and there before I even create a `PendingTask`. Then, there would be no worry for an error occurring. Keep in mind, the example above was chosen to be a simple example.

Here is our `CreateGroceryStoreItemPendingTask`:

```kotlin
class CreateGroceryStoreItemPendingTask(groceryStoreItem: String): PendingTask(
        manually_run = false,
        data_id = groceryStoreItem,
        group_id = null,
        tag = "Creates a grocery store item") {

    override fun runTask(): PendingTaskResult {
        val apiCallResult = performApiCallToCreateGroceryStoreItem(data_id)

        if (apiCallResult.error.httpCode == 400) {
            // Uh, oh. The grocery store item is too long. Our API is configured to return a 400 when the string is too long.
            // The only way to fix this is to have the user of our app fix this for us.

            return PendingTaskResult.FAILED_DO_NOT_RESCHEDULE
        } else {
            return PendingTaskResult.SUCCESSFUL
        }
    }

}
```

This `PendingTask` highlights a couple good practices:

**Best practice: When an error occurs in `runTask()` of a `PendingTask` of yours, determine if it's an error that the user of your app needs to fix, or it was an error that can simply be tried again by Wendy in the future (such as a network timeout or DNS issue).** If you encounter an error that the user does not need to fix, it's very simple. Simply return `PendingTaskResult.FAILED_RESCHEDULE` from `runTask()` and Wendy will reschedule it for you.

If `runTask()` encountered an error that requires the user's attention, then here is what you should do:

* Return `PendingTaskResult.FAILED_DO_NOT_RESCHEDULE` from `runTask()` which will *delete* the `PendingTask` from the Wendy database (essentially the same behavior as when it's successful, but Wendy will call a different callback on your listeners).

* Record the error to Wendy. Wendy comes with a few utility methods to deal with errors.

In your `runTask()` when an error occurs, record an error to Wendy so you can refer to it later in your app. Below is an edited `runTask()` function including the recording of an error:

```kotlin
class CreateGroceryStoreItemPendingTask(groceryStoreItem: String): PendingTask(
        manually_run = false,
        data_id = groceryStoreItem,
        group_id = null,
        tag = "Creates a grocery store item") {

    companion object {
        const val GROCERY_STORE_ITEM_TOO_LONG_ERROR = "groceryStoreItemTooLongErrorCode"
    }

    override fun runTask(): PendingTaskResult {
        val apiCallResult = performApiCallToCreateGroceryStoreItem(data_id)

        if (apiCallResult.error.httpCode == 400) {
            PendingTasks.sharedInstance().recordError(task_id, "Sorry, the grocery store item you entered is too long. It's ${data_id!!.length} long and needs to be 10 characters long.", GROCERY_STORE_ITEM_TOO_LONG_ERROR)
            // Uh, oh. The grocery store item is too long. Our API is configured to return a 400 when the string is too long.
            // The only way to fix this is to have the user of our app fix this for us.

            return PendingTaskResult.FAILED_DO_NOT_RESCHEDULE
        } else {
            return PendingTaskResult.SUCCESSFUL
        }
    }

}
```

* If you registered a `PendingTaskStatusListener` in the UI of your app, you will get notified that an error occurred on the `PendingTask`. Then, you can display in the UI of your app that an error occurred. If you need in the future to check if an error exists for a `PendingTask`, you can call: `PendingTasks.sharedInstance().getLatestError(taskId)`.

* Now it's time for the user to *fix* the issue within the app. For our example of a grocery store app, in the list of grocery store items, we could show a button by the grocery store items that have an error with them. When the user presses that button, the app could go to an `EditText`, detail view, `DialogFragment`, whatever UI you wish to allow the user to edit the text.

Use the `error_id` in the `PendingTaskError` instance to help you determine how to fix the issue. Maybe you have an ID of "CreateGroceryStoreItem" which tells your app to take the user to a UI where the user creates grocery store items. You have an ID of "StringLength" that tells your app to show a UI to edit the string length of a String. The ID you use is up to you. It's designed to help you determine how a user fixes a certain error.

When the user has edited the text and have pressed a "save" button or something, then you need to (1) create a new `CreateGroceryStoreItemPendingTask` and add it to Wendy to have Wendy attempt to save this grocery store item again to the API and (2) tell Wendy that the issue has been resolved: `PendingTasks.sharedInstance().resolveError(task_id)`. If the user decides to "cancel" editing the grocery store item, do not mark the error has resolved.

* If your app has a `TaskRunnerListener` registered, when an error has occurred in your app, you can display a notification to the user to notify them that an error has occurred in the app.
