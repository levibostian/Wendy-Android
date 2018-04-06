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
 * @property taskId ID of the [PendingTask]. After you have used [Wendy.addTask] to add this task to Wendy, this property will become populated and available to you. It is then up to *you* to hang onto this ID if you want to reference it later on.
 * @property createdAt The date/time that the task was created.
 * @property manuallyRun Sometimes you may want your user to be in charge of when a task is run. Setting [manuallyRun] to true will assert that this task does not get run automatically by the Wendy task runner. You will need to manually run the task yourself via [Wendy.runTask].
 * @property groupId If this task needs to be run after a set of previous tasks before it were all successfully run then mark this property with an identifier for the group. Wendy will run through all the tasks of this group until one of them fails. When one fails, Wendy will then skip all the other tasks belonging to this group and move on.
 * @property dataId This field is used to help you identify what offline device data needs to be synced with online remote storage. Example: You have a sqlite database table named Employees. Your user creates a new Employee in your app. First, you will create a new Employee table row for this new employee and then create a new CreateEmployeePendingTask instance to sync this new Employees sqlite row with your online remote storage. Set [dataId] to the newly created Employee table row id. So then when your CreateEmployeePendingTask instance is run by Wendy, you can query your database and sync that data with your remote storage.
 * @property tag This is annoying, I know. I hope to remove it soon. This identifies your subclass with Wendy so when Wendy queries your [PendingTask] in the sqlite DB, it knows to run your subclass. It's recommended to set the tag to: `NameOfYourSubclass::class.java.simpleName`.
 *
 * *Note:* The [tag] and [dataId] are the 2 properties that determine a unique instance of [PendingTask]. Wendy is configured to mark [tag] and [dataId] as unique constraints in the Wendy task database. So, even if the [groupId], [manuallyRun] is defined differently between 2 [PendingTask] instances, Wendy will write and run the most recently added [PendingTask].
 */
abstract class PendingTask(override var manuallyRun: Boolean,
                           override var dataId: String?,
                           override var groupId: String?,
                           override var tag: String): PendingTaskFields {

    var taskId: Long? = null
    override var createdAt: Long = Date().time

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
        this.taskId = pendingTask.id
        this.createdAt = pendingTask.createdAt
        this.manuallyRun = pendingTask.manuallyRun
        this.groupId = pendingTask.groupId
        this.dataId = pendingTask.dataId
        this.tag = pendingTask.tag

        return this
    }

    /**
     * Print contents of [PendingTask].
     */
    override fun toString(): String {
        val dateFormatter = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)

        return "taskId: $taskId, created at: ${dateFormatter.format(createdAt)}, manually run: $manuallyRun, group id: ${groupId ?: "none"}, data id: ${dataId ?: "none"}, tag: $tag"
    }

    /**
     * Run comparisons between two instances of [PendingTask].
     */
    final override fun equals(other: Any?): Boolean {
        if (other !is PendingTask) return false

        // If the tasks have the same task id, we can assume they are the same already.
        if (other.taskId == this.taskId) return true

        // If they have the same dataId and tag, then they are the same in SQL unique terms.
        return other.dataId == this.dataId &&
                other.tag == this.tag
    }

    /**
     * Your typical Java hashCode() function to match [equals].
     */
    final override fun hashCode(): Int {
        var result = dataId?.hashCode() ?: 0
        result = 31 * result + tag.hashCode()
        return result
    }

}