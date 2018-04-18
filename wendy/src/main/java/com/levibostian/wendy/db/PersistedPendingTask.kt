package com.levibostian.wendy.db

import com.levibostian.wendy.service.PendingTask
import java.text.SimpleDateFormat
import java.util.*

/**
 * Use this class to save a [PendingTask] instance to a SQLite database. [PendingTask] is designed to be a developer facing object.
 *
 * Wendy used to use [PendingTask] to save to a SQLite database directly but there were limitions to what I could and could not do with the class once I decided to use it as a SQLite model (example: I could not even make it abstract). So, I decided to create another class to convert a [PendingTask] to and save that data to a SQLite database.
 */
internal class PersistedPendingTask(var id: Long, // SQL primary key auto increment. this maps to PendingTask's taskId property.
                                    override var createdAt: Long,
                                    override var manuallyRun: Boolean,
                                    override var groupId: String?,
                                    override var dataId: String?,
                                    override var tag: String): PendingTaskFields {

    companion object {
        internal const val TABLE_NAME = "PendingTask"
        internal const val COLUMN_ID = "id" // Primary key of SQL. Maps to PendingTask's taskId property. Simply used as an identifier. Is not used for sort order of when PendingTasks are run by the task runner.
        internal const val COLUMN_CREATED_AT = "created_at" // Used to determine the sort order of when PendingTasks are run by the task runner.
        internal const val COLUMN_MANUALLY_RUN = "manually_run" // 0 == false, 1 == true
        internal const val COLUMN_GROUP_ID = "group_id"
        internal const val COLUMN_DATA_ID = "data_id"
        internal const val COLUMN_TAG = "tag"

        internal const val MANUALLY_RUN: Int = 1
        internal const val NOT_MANUALLY_RUN: Int = 0

        // If you are to update this, make sure to update equals() and hashCode() functions in PendingTask.
        // Update PendingTasksManager's getExistingTask too.
        internal val UNIQUE_CONSTRAINT_COLUMNS = listOf(COLUMN_DATA_ID, COLUMN_TAG)

        internal fun fromPendingTask(pendingTask: PendingTask): PersistedPendingTask {
            return PersistedPendingTask(
                    0,
                    0,
                    pendingTask.manuallyRun,
                    pendingTask.groupId,
                    pendingTask.dataId,
                    pendingTask.tag
            )
        }
    }

    internal fun getManuallyRun(): Int {
        return if (manuallyRun) MANUALLY_RUN else NOT_MANUALLY_RUN
    }

    /**
     * Run comparisons between two instances of [PersistedPendingTask].
     */
    override fun equals(other: Any?): Boolean {
        if (other !is PersistedPendingTask) return false

        // If the tasks have the same task id, we can assume they are the same already.
        if (other.id == this.id) return true

        // If they have the same dataId and tag, then they are the same in SQL unique terms.
        return other.dataId == this.dataId &&
                other.tag == this.tag
    }

    /**
     * Your typical Java hashCode() function to match [equals].
     */
    override fun hashCode(): Int {
        var result = dataId?.hashCode() ?: 0
        result = 31 * result + tag.hashCode()
        return result
    }

    override fun toString(): String {
        val dateFormatter = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)

        return "id: $id, created at: ${dateFormatter.format(createdAt)}, manually run: $manuallyRun, group id: ${groupId ?: "none"}, data id: ${dataId ?: "none"}, tag: $tag"
    }

}