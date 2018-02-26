package com.levibostian.wendy.service

import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a single task to perform. Usually used to sync offline data stored on the device with online remote storage.
 *
 * To use this class, create a subclass of it. It is not marked as abstract because these files are saved into a sqlite database and in order to do that, this file cannot be abstract.
 *
 * @property id Auto incrementing id. You know, your typical id field in a SQL database. After you have used [PendingTasks.addTask] to add this task to Wendy, this property will become populated. It is then up to *you* to hang onto this ID if you want to reference it later on.
 * @property created_at The date/time that the task was created.
 * @property manually_run Sometimes you may want your user to be in charge of when a task is run. Setting [manually_run] to true will assert that this task does not get run automatically by the Wendy [PendingTasksRunner].
 * @property group_id If this task needs to be run after a set of previous tasks before it were all successfully run then mark this property with an identifier for the group. Wendy will run through all the tasks of this group until one of them fails. When one fails, Wendy will then skip all the other tasks belonging to this group and move on.
 * @property data_id This field is used to help you identify what offline device data needs to be synced with online remote storage. Example: You have a sqlite database table named Employees. Your user creates a new Employee in your app. First, you will create a new Employee table row for this new employee and then create a new CreateEmployeePendingTask instance to sync this new Employees sqlite row with your online remote storage. Set [data_id] to the newly created Employee table row id. So then when your CreateEmployeePendingTask instance is run by Wendy, you can query your database and sync that data with your remote storage.
 * @property tag This is annoying, I know. I hope to remove it soon. This identifies your subclass with Wendy so when Wendy queries your [PendingTask] in the sqlite DB, it knows to run your subclass. It's recommended to set the tag to: `NameOfYourSubclass::class.java.simpleName`.
 *
 * *Note:* The [tag] and [data_id] are the 2 properties that determine a unique instance of [PendingTask]. Wendy is configured to mark [tag] and [data_id] as unique constraints in the Wendy task database. So, even if the [group_id], [manually_run] is defined differently between 2 [PendingTask] instances, Wendy will write and run the most recently added [PendingTask].
 */
open class PendingTask(open var id: Long = 0, // auto increments
                       open var created_at: Long = Date().time,
                       open var manually_run: Boolean = false,
                       open var group_id: String? = null,
                       open var data_id: String? = null,
                       open var tag: String = "") {

    internal fun getManuallyRun(): Int {
        return if (manually_run) MANUALLY_RUN else NOT_MANUALLY_RUN
    }

    companion object {
        internal const val TABLE_NAME = "PendingTask"
        internal const val COLUMN_ID = "id"
        internal const val COLUMN_CREATED_AT = "created_at"
        internal const val COLUMN_MANUALLY_RUN = "manually_run" // 0 == false, 1 == true
        internal const val COLUMN_GROUP_ID = "group_id"
        internal const val COLUMN_DATA_ID = "data_id"
        internal const val COLUMN_TAG = "tag"

        internal const val MANUALLY_RUN: Int = 1
        internal const val NOT_MANUALLY_RUN: Int = 0

        // If you are to update this, make sure to update equals() and hashCode() functions.
        internal val UNIQUE_CONSTRAINT_COLUMNS = listOf(COLUMN_DATA_ID, COLUMN_TAG)
    }

    /**
     * Must override this to run the actual task.
     *
     * Note: This method is run on a background thread for you already.
     */
    open fun runTask(complete: (successful: Boolean) -> Unit) {
        throw RuntimeException("Override me.")
    }

    /**
     * Override this to dynamically set if this task is ready to run or not.
     */
    open fun canRunTask(): Boolean = true

    /**
     * I hope to remove this in the future. Use inside of your [PendingTasksFactory] instance to instantiate objects.
     */
    open fun fromSqlObject(pendingTask: PendingTask): PendingTask {
        this.id = pendingTask.id
        this.created_at = pendingTask.created_at
        this.manually_run = pendingTask.manually_run
        this.group_id = pendingTask.group_id
        this.data_id = pendingTask.data_id
        this.tag = pendingTask.tag

        return this
    }

    override fun toString(): String {
        val dateFormatter = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)

        return "id: $id, created at: ${dateFormatter.format(created_at)}, manually run: $manually_run, group id: ${group_id ?: "none"}, data id: ${data_id ?: "none"}, tag: $tag"
    }

    override fun equals(other: Any?): Boolean {
        return other is PendingTask &&
                other.data_id == this.data_id &&
                other.tag == this.tag
    }

    override fun hashCode(): Int {
        var result = data_id?.hashCode() ?: 0
        result = 31 * result + tag.hashCode()
        return result
    }

}