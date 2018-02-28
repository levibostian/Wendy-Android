package com.levibostian.wendy.db

import com.levibostian.wendy.service.PendingTask
import java.text.SimpleDateFormat
import java.util.*

/**
 * Use this class to save a [PendingTask] instance to a SQLite database. [PendingTask] is designed to be a developer facing object.
 *
 * Wendy used to use [PendingTask] to save to a SQLite database directly but there were limitions to what I could and could not do with the class once I decided to use it as a SQLite model (example: I could not even make it abstract). So, I decided to create another class to convert a [PendingTask] to and save that data to a SQLite database.
 */
internal class PersistedPendingTask(internal var id: Long, // internal use only for SQL terms.
                                    override var task_id: Long, // If a user adds a PendingTask to Wendy and later updates it, this task_id is always persisted.
                                    override var created_at: Long,
                                    override var manually_run: Boolean,
                                    override var group_id: String?,
                                    override var data_id: String?,
                                    override var tag: String): PendingTaskFields {

    companion object {
        internal const val TABLE_NAME = "PendingTask"
        internal const val COLUMN_ID = "id" // Primary key, internal use only SQL ID.
        internal const val COLUMN_TASK_ID = "task_id"
        internal const val COLUMN_CREATED_AT = "created_at"
        internal const val COLUMN_MANUALLY_RUN = "manually_run" // 0 == false, 1 == true
        internal const val COLUMN_GROUP_ID = "group_id"
        internal const val COLUMN_DATA_ID = "data_id"
        internal const val COLUMN_TAG = "tag"

        internal const val MANUALLY_RUN: Int = 1
        internal const val NOT_MANUALLY_RUN: Int = 0

        // If you are to update this, make sure to update equals() and hashCode() functions in PendingTask.
        // Update PendingTasksManager's getExistingTask too.
        internal val UNIQUE_CONSTRAINT_COLUMNS = listOf(COLUMN_DATA_ID, COLUMN_TAG)

        fun fromPendingTask(pendingTask: PendingTask): PersistedPendingTask {
            return PersistedPendingTask(
                    0,
                    pendingTask.task_id,
                    pendingTask.created_at,
                    pendingTask.manually_run,
                    pendingTask.group_id,
                    pendingTask.data_id,
                    pendingTask.tag
            )
        }
    }

    internal fun getManuallyRun(): Int {
        return if (manually_run) MANUALLY_RUN else NOT_MANUALLY_RUN
    }

    override fun toString(): String {
        val dateFormatter = SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z", Locale.ENGLISH)

        return "id: $id, task_id: $task_id, created at: ${dateFormatter.format(created_at)}, manually run: $manually_run, group id: ${group_id ?: "none"}, data id: ${data_id ?: "none"}, tag: $tag"
    }

}