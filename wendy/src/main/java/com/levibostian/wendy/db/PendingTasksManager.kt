package com.levibostian.wendy.db

import android.content.ContentValues
import android.content.Context
import com.levibostian.wendy.extension.getPendingTask
import com.levibostian.wendy.extension.getTaskAssertPopulated
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.Wendy
import com.levibostian.wendy.service.PendingTasksRunner
import com.levibostian.wendy.types.RunAllTasksFilter
import com.levibostian.wendy.util.LogUtil
import org.jetbrains.anko.db.*
import java.util.*

internal class PendingTasksManager(private val context: Context) {

    private val db = PendingTasksDatabaseHelper.sharedInstance(context)

    @Synchronized
    internal fun insertPendingTask(pendingTaskToAdd: PendingTask): PendingTask {
        if (pendingTaskToAdd.tag.isBlank()) throw RuntimeException("You need to set a unique tag for ${PendingTask::class.java.simpleName} instances.")

        val persistedPendingTask = PersistedPendingTask.fromPendingTask(pendingTaskToAdd)

        return db.use {
            persistedPendingTask.createdAt = Date().time // Very important. This determines the sort order of when tasks run by the task runner. createdAt needs to be set by Wendy internally and only modified by Wendy under certain circumstances.

            val id = insert(PersistedPendingTask.TABLE_NAME,
                    PersistedPendingTask.COLUMN_CREATED_AT to persistedPendingTask.createdAt,
                    PersistedPendingTask.COLUMN_MANUALLY_RUN to persistedPendingTask.getManuallyRun(),
                    PersistedPendingTask.COLUMN_GROUP_ID to persistedPendingTask.groupId,
                    PersistedPendingTask.COLUMN_DATA_ID to persistedPendingTask.dataId,
                    PersistedPendingTask.COLUMN_TAG to persistedPendingTask.tag)
            persistedPendingTask.id = id

            LogUtil.d("Successfully added task to Wendy. Task: $pendingTaskToAdd")

            pendingTaskToAdd.fromSqlObject(persistedPendingTask)
            pendingTaskToAdd
        }
    }

    /**
     * @throws IllegalArgumentException if task by taskId does not exist.
     * @throws IllegalArgumentException if task by taskId does not belong to any groups.
     */
    @Synchronized
    internal fun isTaskFirstTaskOfGroup(taskId: Long): Boolean {
        val pendingTask = getTaskByTaskId(taskId) ?: throw IllegalArgumentException("Task with id: $taskId does not exist.")
        if (pendingTask.groupId == null) throw IllegalArgumentException("Task: $pendingTask does not belong to a group.")

        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("(${PersistedPendingTask.COLUMN_GROUP_ID} = '${pendingTask.groupId}')")
                    .orderBy(PersistedPendingTask.COLUMN_CREATED_AT, SqlOrderDirection.ASC)
                    .exec {
                        val tasksInGroup: List<PersistedPendingTask> = parseList(classParser())
                        tasksInGroup[0].id == taskId
                    }
        }
    }

    /**
     * Note: It's assumed that you have checked if taskId exists.
     */
    @Synchronized
    internal fun updatePlaceInLine(taskId: Long, createdAt: Date) {
        db.use {
            update(PersistedPendingTask.TABLE_NAME, PersistedPendingTask.COLUMN_CREATED_AT to createdAt.time)
                    .whereArgs("(${PersistedPendingTask.COLUMN_ID} = $taskId)")
                    .exec()
        }
    }

    @Synchronized
    internal fun getRandomTaskForTag(tag: String): PendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("(${PersistedPendingTask.COLUMN_TAG} = '$tag')")
                    .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull()?.getPendingTask() }
        }
    }

    /**
     * Note: It's assumed that you have checked if the PendingTaskError.taskId exists.
     */
    @Synchronized
    internal fun insertPendingTaskError(pendingTaskError: PendingTaskError): PendingTaskError {
        return db.use {
            pendingTaskError.createdAt = Date().time

            val id = insert(PendingTaskError.TABLE_NAME,
                    PendingTaskError.COLUMN_TASK_ID to pendingTaskError.taskId,
                    PendingTaskError.COLUMN_CREATED_AT to Date().time,
                    PendingTaskError.COLUMN_ERROR_MESSAGE to pendingTaskError.errorMessage,
                    PendingTaskError.COLUMN_ERROR_ID to pendingTaskError.errorId)

            pendingTaskError.id = id
            LogUtil.d("Successfully recorded error to Wendy. Error: $pendingTaskError")

            pendingTaskError
        }
    }

    @Synchronized
    internal fun getLatestError(pendingTaskId: Long): PendingTaskError? {
        return db.use {
            select(PendingTaskError.TABLE_NAME)
                    .whereArgs("${PendingTaskError.COLUMN_TASK_ID} = $pendingTaskId")
                    .exec { parseOpt(classParser()) }
        }
    }

    @Synchronized
    internal fun getExistingTasks(pendingTask: PendingTask): List<PersistedPendingTask>? {
        return db.use {
            var whereArgs = "${PersistedPendingTask.COLUMN_TAG} = '${pendingTask.tag}'"

            pendingTask.groupId?.let { groupId ->
                whereArgs += " AND (${PersistedPendingTask.COLUMN_GROUP_ID} = '$groupId')"
            }
            pendingTask.dataId?.let { dataId ->
                whereArgs += " AND (${PersistedPendingTask.COLUMN_DATA_ID} = '$dataId')"
            }

            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs(whereArgs)
                    .orderBy(PersistedPendingTask.COLUMN_CREATED_AT, SqlOrderDirection.ASC)
                    .exec {
                        val result: List<PersistedPendingTask> = parseList(classParser())
                        if (result.isEmpty()) null else result
                    }
        }
    }

    @Synchronized
    internal fun getLastPendingTaskInGroup(groupId: String): PersistedPendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_GROUP_ID} = '$groupId'")
                    .orderBy(PersistedPendingTask.COLUMN_CREATED_AT, SqlOrderDirection.DESC)
                    .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull() }
        }
    }

    @Synchronized
    internal fun getAllTasks(): List<PendingTask> {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .orderBy(PersistedPendingTask.COLUMN_CREATED_AT, SqlOrderDirection.ASC)
                    .exec { parseList(classParser<PersistedPendingTask>()).map { it.getPendingTask() } }
        }
    }

    @Synchronized
    internal fun getTaskByTaskId(taskId: Long): PersistedPendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_ID} = $taskId")
                    .exec { parseOpt(classParser()) }
        }
    }

    @Synchronized
    internal fun getAllErrors(): List<PendingTaskError> {
        return db.use {
            select(PendingTaskError.TABLE_NAME)
                    .orderBy(PendingTaskError.COLUMN_CREATED_AT, SqlOrderDirection.ASC)
                    .exec { parseList(classParser<PendingTaskError>()).map {
                        it.pendingTask = getPendingTaskTaskById(it.taskId)!!
                        it
                    }}
        }
    }

    @Synchronized
    internal fun getPendingTaskTaskById(taskId: Long): PendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_ID} = $taskId")
                    .exec { parseOpt(classParser<PersistedPendingTask>())?.getPendingTask() }
        }
    }

    @Synchronized
    internal fun getNextTaskToRun(afterTaskId: Long = 0, filter: RunAllTasksFilter? = null): PendingTask? {
        return db.use {
            var whereArgs = "(${PersistedPendingTask.COLUMN_ID} > $afterTaskId) AND (${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN})"

            filter?.groupId?.let { filterByGroupId ->
                whereArgs += " AND (${PersistedPendingTask.COLUMN_GROUP_ID} = '$filterByGroupId')"
            }

            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs(whereArgs)
                    .orderBy(PersistedPendingTask.COLUMN_CREATED_AT, SqlOrderDirection.ASC)
                    .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull()?.getPendingTask() }
        }
    }

    @Synchronized
    internal fun getTotalNumberOfTasksForRunnerToRun(filter: RunAllTasksFilter? = null): Int {
        return db.use {
            var whereArgs = "(${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN})"

            filter?.groupId?.let { filterByGroupId ->
                whereArgs += " AND (${PersistedPendingTask.COLUMN_GROUP_ID} = '$filterByGroupId')"
            }

            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs(whereArgs)
                    .exec { parseList(classParser<PersistedPendingTask>()).size }
        }
    }

    // Note: Make sure to keep the query at "delete this table item by ID _____".
    // Because of this scenario: The runner is running a task with ID 1. While the task is running a user decides to update that data. This results in having to run that PendingTask a 2nd time (if the running task is successful) to sync the newest changes. To assert this 2nd change, we take advantage of SQLite's unique constraint. On unique constraint collision we replace (update) the data in the database which results in all the PendingTask data being the same except for the ID being incremented. So, after the runner runs the task successfully and wants to delete the task here, it will not delete the task because the ID no longer exists. It has been incremented so the newest changes can be run.
    @Synchronized
    internal fun deleteTask(id: Long) {
        db.use {
            delete(PersistedPendingTask.TABLE_NAME, "${PersistedPendingTask.COLUMN_ID} = $id")
        }
    }

    @Synchronized
    internal fun deletePendingTaskError(taskId: Long): Boolean {
        return db.use {
            val numberOfRowsEffected = delete(PendingTaskError.TABLE_NAME, "${PendingTaskError.COLUMN_TASK_ID} = $taskId")
            numberOfRowsEffected > 0
        }
    }

    @Synchronized
    internal fun clear() {
        if (!context.deleteDatabase(PendingTasksDatabaseHelper.DATABASE_NAME)) throw RuntimeException("Wendy database did not delete successfully.")
    }

}