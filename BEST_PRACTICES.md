# How do I update a PendingTask that I already added to Wendy?

You don't update the `PendingTask`. This is on purpose.

You know the `data_id` property in `PendingTask`? That `data_id` property is meant to be an identifier used to query data when your `PendingTask` is run.

Because you are supposed to query for the data of a job right before a job is run, there is no need to update a `PendingTask`. If the user edits data represented by a certain `data_id` `PendingData`, the freshest data will be the data that gets synced.

# How do I delete a PendingTask that I added to Wendy?

You don't delete `PendingTask`s. That is on purpose.

`PendingTask`s are meant for user actions. The user does something, that needs to get recorded in your app. If the user creates data and then deletes data in your app, your app should create separate `PendingTask`s for each of those actions instead of deleting the `PendingTask` meant for creating the data.

### Is there an 'undo' function for Wendy PendingTasks?

As explained above, you cannot delete a `PendingTask`. That also includes undo.

What is a best practice here, is when a user performs an action in your app that you want to offer an 'undo' feature for (example: Delete a piece of data), you should:

* Show the user in the UI a way to 'undo' their action. For example: Showing a Snackbar with an undo action.
* If the user presses the undo button in the UI, do not create a `PendingTask`. It's like it never happened.
* If the user does not press the undo button in the UI before the timeout, *then* create the `PendingTask` and add it to Wendy via `PendingTasks.addTask()`. 
