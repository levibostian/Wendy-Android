package com.levibostian.wendy.service

import java.text.SimpleDateFormat
import java.util.*

// Not making abstract because we need to save it in the sqlite db and it cannot be abstract to do that.
open class PendingTask(open var id: Long = 0,  // auto increments
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

}