package com.levibostian.wendy.service

import android.support.annotation.WorkerThread
import com.levibostian.wendy.db.PendingTaskFields
import com.levibostian.wendy.db.PersistedPendingTask
import com.levibostian.wendy.types.PendingTaskResult
import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a single task to perform. Usually used to sync offline data stored on the device with online remote storage.
 *
 * To use this class, create a subclass of it. It is not marked as abstract because these files are saved into a sqlite database and in order to do that, this file cannot be abstract.
 *
 * @property task_id ID of the [PendingTask]. After you have used [PendingTasks.addTask] to add this task to Wendy, this property will become populated and available to you. It is then up to *you* to hang onto this ID if you want to reference it later on.
 * @property created_at The date/time that the task was created.
 * @property manually_run Sometimes you may want your user to be in charge of when a task is run. Setting [manually_run] to true will assert that this task does not get run automatically by the Wendy [PendingTasksRunner].
 * @property group_id If this task needs to be run after a set of previous tasks before it were all successfully run then mark this property with an identifier for the group. Wendy will run through all the tasks of this group until one of them fails. When one fails, Wendy will then skip all the other tasks belonging to this group and move on.
 * @property data_id This field is used to help you identify what offline device data needs to be synced with online remote storage. Example: You have a sqlite database table named Employees. Your user creates a new Employee in your app. First, you will create a new Employee table row for this new employee and then create a new CreateEmployeePendingTask instance to sync this new Employees sqlite row with your online remote storage. Set [data_id] to the newly created Employee table row id. So then when your CreateEmployeePendingTask instance is run by Wendy, you can query your database and sync that data with your remote storage.
 * @property tag This is annoying, I know. I hope to remove it soon. This identifies your subclass with Wendy so when Wendy queries your [PendingTask] in the sqlite DB, it knows to run your subclass. It's recommended to set the tag to: `NameOfYourSubclass::class.java.simpleName`.
 *
 * *Note:* The [tag] and [data_id] are the 2 properties that determine a unique instance of [PendingTask]. Wendy is configured to mark [tag] and [data_id] as unique constraints in the Wendy task database. So, even if the [group_id], [manually_run] is defined differently between 2 [PendingTask] instances, Wendy will write and run the most recently added [PendingTask].
 */
abstract class PendingTask(override var manually_run: Boolean,
                           override var data_id: String?,
                           override var group_id: String?,
                           override var tag: String): PendingTaskFields {

    var task_id: Long = 0
    override var created_at: Long = Date().time

    /**
     * The method Wendy calls when it's time for your task to run. This is where you will perform database operations on the device, perform API calls, etc.
     *
     * When you are done performing the task, return if the task was run successfully or not. You have 3 options as outlined in [PendingTaskResult].
     *
     * This method is run on a background thread for you already. Make sure to run all of your code in a synchronous style. If you would like to run async code, check out the `BEST_PRACTICES.md` file in the root of this project to learn more on how you could do this.
     *
     * @see PendingTaskResult to learn about the different results you may return after the task runs.
     */
    @WorkerThread abstract fun runTask(): PendingTaskResult

    /**
     * Override this to dynamically set if this task is ready to run or not.
     */
    open fun canRunTask(): Boolean = true

    /**
     * Use to go from [PersistedPendingTask] to [PendingTask] after running a SQLite query.
     */
    internal fun fromSqlObject(pendingTask: PersistedPendingTask): PendingTask {
        this.task_id = pendingTask.id
        this.created_at = pendingTask.created_at
        this.manually_run = pendingTask.manually_run
        this.group_id = pendingTask.group_id
        this.data_id = pendingTask.data_id
        this.tag = pendingTask.tag

        return this
    }

    /**
     * Print contents of [PendingTask].
     */
    override fun toString(): String {
        val dateFormatter = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)

        return "task_id: $task_id, created at: ${dateFormatter.format(created_at)}, manually run: $manually_run, group id: ${group_id ?: "none"}, data id: ${data_id ?: "none"}, tag: $tag"
    }

    /**
     * Run comparisons between two instances of [PendingTask].
     */
    final override fun equals(other: Any?): Boolean {
        if (other !is PendingTask) return false

        // If the tasks have the same task id, we can assume they are the same already.
        if (other.task_id == this.task_id) return true

        // If they have the same data_id and tag, then they are the same in SQL unique terms.
        return other.data_id == this.data_id &&
                other.tag == this.tag
    }

    /**
     * Your typical Java hashCode() function to match [equals].
     */
    final override fun hashCode(): Int {
        var result = data_id?.hashCode() ?: 0
        result = 31 * result + tag.hashCode()
        return result
    }

}