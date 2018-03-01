package com.levibostian.wendy.db

import android.content.Context
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.service.PendingTasks
import com.levibostian.wendy.util.LogUtil
import org.jetbrains.anko.db.*
import java.util.*

internal class PendingTasksManager(context: Context) {

    private val db = PendingTasksDatabaseHelper.sharedInstance(context)

    @Synchronized
    internal fun insertPendingTask(pendingTaskToAdd: PendingTask): PendingTask {
        if (pendingTaskToAdd.tag.isBlank()) throw RuntimeException("You need to set a unique tag for ${PendingTask::class.java.simpleName} instances.")

        val similarPendingTask = getExistingTask(pendingTaskToAdd)
        pendingTaskToAdd.task_id = similarPendingTask?.task_id ?: getNextTaskId()
        val persistedPendingTask = PersistedPendingTask.fromPendingTask(pendingTaskToAdd)

        return db.use {
            persistedPendingTask.created_at = Date().time
            pendingTaskToAdd.created_at = persistedPendingTask.created_at

            val id = insert(PersistedPendingTask.TABLE_NAME,
                    PersistedPendingTask.COLUMN_TASK_ID to persistedPendingTask.task_id,
                    PersistedPendingTask.COLUMN_CREATED_AT to persistedPendingTask.created_at,
                    PersistedPendingTask.COLUMN_MANUALLY_RUN to persistedPendingTask.getManuallyRun(),
                    PersistedPendingTask.COLUMN_GROUP_ID to persistedPendingTask.group_id,
                    PersistedPendingTask.COLUMN_DATA_ID to persistedPendingTask.data_id,
                    PersistedPendingTask.COLUMN_TAG to persistedPendingTask.tag)

            persistedPendingTask.id = id
            LogUtil.d("Successfully added task to Wendy. Task: $pendingTaskToAdd")

            pendingTaskToAdd
        }
    }

    /**
     * Note: It's assumed that you have checked if the PendingTaskError.task_id exists.
     */
    @Synchronized
    internal fun insertPendingTaskError(pendingTaskError: PendingTaskError): PendingTaskError {
        return db.use {
            pendingTaskError.created_at = Date().time

            val id = insert(PendingTaskError.TABLE_NAME,
                    PendingTaskError.COLUMN_TASK_ID to pendingTaskError.task_id,
                    PendingTaskError.COLUMN_CREATED_AT to Date().time,
                    PendingTaskError.COLUMN_ERROR_MESSAGE to pendingTaskError.error_message,
                    PendingTaskError.COLUMN_ERROR_ID to pendingTaskError.error_id)

            pendingTaskError.id = id
            LogUtil.d("Successfully recorded error to Wendy. Error: $pendingTaskError")

            pendingTaskError
        }
    }

    @Synchronized
    internal fun getLatestError(pendingTaskId: Long): PendingTaskError? {
        return db.use {
            select(PendingTaskError.TABLE_NAME)
                    .whereArgs("${PendingTaskError.COLUMN_TASK_ID} = '$pendingTaskId'")
                    .exec { parseOpt(classParser()) }
        }
    }

    @Synchronized
    private fun getExistingTask(pendingTask: PendingTask): PersistedPendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_DATA_ID} = '${pendingTask.data_id}' AND " +
                            "${PersistedPendingTask.COLUMN_TAG} = '${pendingTask.tag}'")
                    .exec { parseOpt(classParser()) }
        }
    }

    /**
     * task_id starts at 1 and increments from there. Get the next one available.
     */
    @Synchronized
    private fun getNextTaskId(): Long {
        return db.use {
            val task: PersistedPendingTask? = select(PersistedPendingTask.TABLE_NAME)
                    .orderBy(PersistedPendingTask.COLUMN_ID, SqlOrderDirection.DESC)
                    .limit(1)
                    .exec { parseOpt(classParser()) }

            task?.id?.plus(1) ?: 1
        }
    }

    internal fun getAllTasks(): List<PendingTask> {
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

    @Synchronized
    internal fun getTaskByTaskId(taskId: Long): PersistedPendingTask? {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_TASK_ID} = $taskId")
                    .exec { parseOpt(classParser()) }
        }
    }

    @Synchronized
    internal fun getAllErrors(): List<PendingTaskError> {
        return db.use {
            select(PendingTaskError.TABLE_NAME)
                    .exec { parseList(classParser<PendingTaskError>()).map {
                        it.pending_task = getPendingTaskTaskById(it.task_id)!!
                        it
                    }}
        }
    }

    @Synchronized
    internal fun getPendingTaskTaskById(taskId: Long): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_TASK_ID} = $taskId")
                    .exec {
                        val task = parseOpt(classParser<PersistedPendingTask>())
                        if (task == null) null else tasksFactory.getTask(task.tag).fromSqlObject(task)
                    }
        }
    }

    @Synchronized
    internal fun getNextTaskToRun(afterTaskId: Long = 0): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory

        var nextTask: PersistedPendingTask?
        return db.use {
            nextTask = select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("(${PersistedPendingTask.COLUMN_TASK_ID} > $afterTaskId) AND (${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN})")
                    .exec { parseList(classParser<PersistedPendingTask>()).firstOrNull() }

            if (nextTask == null) null else tasksFactory.getTask(nextTask!!.tag).fromSqlObject(nextTask!!)
        }
    }

    @Synchronized
    internal fun getTotalNumberOfTasksForRunnerToRun(): Int {
        return db.use {
            select(PersistedPendingTask.TABLE_NAME)
                    .whereArgs("${PersistedPendingTask.COLUMN_MANUALLY_RUN} = ${PersistedPendingTask.NOT_MANUALLY_RUN}")
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

}