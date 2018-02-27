package com.levibostian.wendy.service

import android.content.Context
import com.levibostian.wendy.db.PendingTasksDatabaseHelper
import com.levibostian.wendy.db.PersistedPendingTask
import com.levibostian.wendy.util.LogUtil
import org.jetbrains.anko.db.*

internal class PendingTasksManager(context: Context) {

    private val db = PendingTasksDatabaseHelper.sharedInstance(context)

    fun addTask(pendingTask: PendingTask): Long {
        if (pendingTask.tag.isBlank()) throw RuntimeException("You need to set a unique tag for ${PendingTask::class.java.simpleName} instances.")

        val persistedPendingTask = PersistedPendingTask.fromPendingTask(pendingTask)

        return db.use {
            val id = insert(PersistedPendingTask.TABLE_NAME,
                    PersistedPendingTask.COLUMN_CREATED_AT to persistedPendingTask.created_at,
                    PersistedPendingTask.COLUMN_MANUALLY_RUN to persistedPendingTask.getManuallyRun(),
                    PersistedPendingTask.COLUMN_GROUP_ID to persistedPendingTask.group_id,
                    PersistedPendingTask.COLUMN_DATA_ID to persistedPendingTask.data_id,
                    PersistedPendingTask.COLUMN_TAG to persistedPendingTask.tag)

            pendingTask.id = id
            LogUtil.d("Successfully added task to Wendy. Task: $pendingTask")

            id
        }
    }

    fun getAllTasks(): List<PendingTask> {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .exec {
                        parseList(classParser<PersistedPendingTask>()).map {
                            tasksFactory.getTask(it.tag).fromSqlObject(it)
                        }
                    }
        }
    }

    fun getTaskForId(id: Long): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_ID} = $id")
                    .exec {
                        val task = parseOpt(classParser<PersistedPendingTask>())
                        if (task == null) null else tasksFactory.getTask(task.tag).fromSqlObject(task)
                    }
        }
    }

    fun getNextTask(afterTaskId: Long = 0, notInvolvingGroups: List<String>? = null): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory

        var nextTask: PersistedPendingTask?
        return db.use {
            nextTask = if (notInvolvingGroups != null && notInvolvingGroups.isNotEmpty()) {
                select(PersistedPendingTask.TABLE_NAME)
                        .whereArgs("(${PersistedPendingTask.COLUMN_ID} > $afterTaskId) and " +
                                "(${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN}) and " +
                                "(${PersistedPendingTask.COLUMN_GROUP_ID} is null or ${PersistedPendingTask.COLUMN_GROUP_ID} not in (${notInvolvingGroups.joinToString(separator = ",", transform = { "'$it'" })}))")
                        .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull() }
            } else {
                select(PersistedPendingTask.TABLE_NAME)
                        .whereArgs("(${PersistedPendingTask.COLUMN_ID} > $afterTaskId) AND (${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN})")
                        .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull() }
            }

            if (nextTask == null) null else tasksFactory.getTask(nextTask!!.tag).fromSqlObject(nextTask!!)
        }
    }

    fun getTotalNumberOfTasksForRunnerToRun(): Int {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN}")
                    .exec { parseList(classParser<PersistedPendingTask>()).size }
        }
    }

    fun deleteTask(taskId: Long) {
        db.use {
            delete(PersistedPendingTask.TABLE_NAME, "${PersistedPendingTask.COLUMN_ID} = $taskId")
        }
    }

}