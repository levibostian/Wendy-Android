package com.levibostian.wendy.db

import com.levibostian.wendy.extension.ForeignKey
import com.levibostian.wendy.service.PendingTask
import java.text.SimpleDateFormat
import java.util.*

/**
 * Use this class to save a [PendingTask] instance to a SQLite database. [PendingTask] is designed to be a developer facing object.
 *
 * Wendy used to use [PendingTask] to save to a SQLite database directly but there were limitions to what I could and could not do with the class once I decided to use it as a SQLite model (example: I could not even make it abstract). So, I decided to create another class to convert a [PendingTask] to and save that data to a SQLite database.
 *
 * @property task_id The [PendingTask.task_id] you want to record an error for.
 * @property created_at The Date your error was recorded in the Wendy database.
 * @property error_message The human readable error message you may use to show to the end user describing the error.
 * @property error_id The identifier you, the developer, use to determine what type of error was caused and how to fix it.
 * @property pending_task The [PendingTask] this error is associated with.
 */
open class PendingTaskError(internal var id: Long, // internal use only for SQL terms.
                            var task_id: Long,
                            var created_at: Long,
                            var error_message: String?,
                            var error_id: String?) {

    lateinit var pending_task: PendingTask

    companion object {
        internal const val TABLE_NAME = "PendingTaskError"
        internal const val COLUMN_ID = "id" // Primary key, internal use only SQL ID.
        internal const val COLUMN_TASK_ID = "task_id" // Maps to PendingTask.task_id
        internal const val COLUMN_CREATED_AT = "created_at"
        internal const val COLUMN_ERROR_MESSAGE = "error_message"
        internal const val COLUMN_ERROR_ID = "error_id"

        internal val UNIQUE_CONSTRAINT_COLUMNS = listOf(COLUMN_TASK_ID)

        internal val FOREIGN_KEY_TASK_ID = ForeignKey(COLUMN_TASK_ID, PersistedPendingTask.TABLE_NAME to PersistedPendingTask.COLUMN_ID)

        internal fun init(taskId: Long, errorMessage: String?, errorId: String?): PendingTaskError {
            return PendingTaskError(0,
                    taskId,
                    0,
                    errorMessage,
                    errorId)
        }
    }

    /**
     * Gives you a String representation of [PendingTaskError]
     */
    override fun toString(): String {
        return "id: $id, " +
                "task_id: $task_id, " +
                "created_at: $created_at, " +
                "error_message: $error_message " +
                "error_id: $error_id"
    }

}