package com.levibostian.wendy.db

import com.levibostian.wendy.extension.ForeignKey
import com.levibostian.wendy.service.PendingTask

/**
 * Use this class to save a [PendingTask] instance to a SQLite database. [PendingTask] is designed to be a developer facing object.
 *
 * Wendy used to use [PendingTask] to save to a SQLite database directly but there were limitions to what I could and could not do with the class once I decided to use it as a SQLite model (example: I could not even make it abstract). So, I decided to create another class to convert a [PendingTask] to and save that data to a SQLite database.
 *
 * @property taskId The [PendingTask.taskId] you want to record an error for.
 * @property createdAt The Date your error was recorded in the Wendy database.
 * @property errorMessage The human readable error message you may use to show to the end user describing the error.
 * @property errorId The identifier you, the developer, use to determine what type of error was caused and how to fix it.
 * @property pendingTask The [PendingTask] this error is associated with.
 */
class PendingTaskError(internal var id: Long, // internal use only for SQL terms.
                            var taskId: Long,
                            var createdAt: Long,
                            var errorMessage: String?,
                            var errorId: String?) {

    lateinit var pendingTask: PendingTask

    companion object {
        internal const val TABLE_NAME = "PendingTaskError"
        internal const val COLUMN_ID = "id" // Primary key, internal use only SQL ID.
        internal const val COLUMN_TASK_ID = "task_id" // Maps to PendingTask.taskId
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
                "taskId: $taskId, " +
                "createdAt: $createdAt, " +
                "errorMessage: $errorMessage " +
                "errorId: $errorId"
    }

}