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

    // Note: Make sure to keep the query at "delete this table item by ID _____".
    // Because of this scenario: The runner is running a task with ID 1. While the task is running a user decides to update that data. This results in having to run that PendingTask a 2nd time (if the running task is successful) to sync the newest changes. To assert this 2nd change, we take advantage of SQLite's unique constraint. On unique constraint collision we replace (update) the data in the database which results in all the PendingTask data being the same except for the ID being incremented. So, after the runner runs the task successfully and wants to delete the task here, it will not delete the task because the ID no longer exists. It has been incremented so the newest changes can be run.
    fun deleteTask(taskId: Long) {
        db.use {
            delete(PersistedPendingTask.TABLE_NAME, "${PersistedPendingTask.COLUMN_ID} = $taskId")
        }
    }

}