package com.levibostian.wendy.service

import android.content.Context
import com.levibostian.wendy.db.PendingTasksDatabaseHelper
import org.jetbrains.anko.db.*

internal class PendingTasksManager(context: Context) {

    val db = PendingTasksDatabaseHelper.sharedInstance(context)

    fun addTask(pendingTask: PendingTask) {
        if (pendingTask.tag.isBlank()) throw RuntimeException("You need to set a unique tag for ${PendingTask::class.java.simpleName} instances.")

        db.use {
            insert(PendingTask.TABLE_NAME,
                    PendingTask.COLUMN_CREATED_AT to pendingTask.created_at,
                    PendingTask.COLUMN_MANUALLY_RUN to pendingTask.getManuallyRun(),
                    PendingTask.COLUMN_GROUP_ID to pendingTask.group_id,
                    PendingTask.COLUMN_DATA_ID to pendingTask.data_id,
                    PendingTask.COLUMN_TAG to pendingTask.tag)
        }
    }

    fun getNextTask(afterTaskId: Long = 0, notInvolvingGroups: List<String>? = null): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory

        var nextTask: PendingTask?
        return db.use {
            nextTask = if (notInvolvingGroups != null) {
                select(PendingTask.TABLE_NAME)
                        .whereArgs("${PendingTask.COLUMN_ID} > $afterTaskId and ${PendingTask.COLUMN_MANUALLY_RUN} == ${PendingTask.NOT_MANUALLY_RUN} and ${PendingTask.COLUMN_GROUP_ID} is null or ${PendingTask.COLUMN_GROUP_ID} not in (${notInvolvingGroups.joinToString(separator = ",", transform = { "'$it'" })})")
                        .exec { parseList(classParser<PendingTask>()).firstOrNull() }
            } else {
                select(PendingTask.TABLE_NAME)
                        .whereArgs("${PendingTask.COLUMN_ID} > $afterTaskId and ${PendingTask.COLUMN_MANUALLY_RUN} == ${PendingTask.NOT_MANUALLY_RUN}")
                        .exec { parseList(classParser<PendingTask>()).firstOrNull() }
            }

            if (nextTask == null) null else tasksFactory.getTask(nextTask!!.tag, nextTask!!)
        }
    }

    fun deleteTask(pendingTask: PendingTask) {
        db.use {
            delete(PendingTask.TABLE_NAME, "${PendingTask.COLUMN_ID} = ${pendingTask.id}")
        }
    }

}