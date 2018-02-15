package com.levibostian.wendy.service

import android.content.Context
import com.levibostian.wendy.db.PendingTasksDatabaseHelper
import com.levibostian.wendy.util.LogUtil
import org.jetbrains.anko.db.*

internal class PendingTasksManager(context: Context) {

    val db = PendingTasksDatabaseHelper.sharedInstance(context)

    fun addTask(pendingTask: PendingTask): Long {
        if (pendingTask.tag.isBlank()) throw RuntimeException("You need to set a unique tag for ${PendingTask::class.java.simpleName} instances.")

        return db.use {
            val id = insert(PendingTask.TABLE_NAME,
                    PendingTask.COLUMN_CREATED_AT to pendingTask.created_at,
                    PendingTask.COLUMN_MANUALLY_RUN to pendingTask.getManuallyRun(),
                    PendingTask.COLUMN_GROUP_ID to pendingTask.group_id,
                    PendingTask.COLUMN_DATA_ID to pendingTask.data_id,
                    PendingTask.COLUMN_TAG to pendingTask.tag)

            pendingTask.id = id
            LogUtil.d("Successfully added task to Wendy. Task: $pendingTask")

            id
        }
    }

    fun deleteAllTasks() {
        return db.use {
            delete(PendingTask.TABLE_NAME)
            LogUtil.d("Deleted all pending tasks.")
        }
    }

    fun getAllTasks(): List<PendingTask> {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory
        return db.use {
            select(PendingTask.TABLE_NAME)
                    .exec { parseList(classParser<PendingTask>()).map { tasksFactory.getTask(it.tag, it) } }
        }
    }

    fun getTaskForId(id: Long): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory
        return db.use {
            select(PendingTask.TABLE_NAME)
                    .whereArgs("${PendingTask.COLUMN_ID} = $id")
                    .exec {
                        val task = parseOpt(classParser<PendingTask>())
                        if (task == null) null else tasksFactory.getTask(task.tag, task)
                    }
        }
    }

    fun getNextTask(afterTaskId: Long = 0, notInvolvingGroups: List<String>? = null): PendingTask? {
        val tasksFactory = PendingTasks.sharedInstance().tasksFactory

        var nextTask: PendingTask?
        return db.use {
            nextTask = if (notInvolvingGroups != null && notInvolvingGroups.isNotEmpty()) {
                select(PendingTask.TABLE_NAME)
                        .whereArgs("(${PendingTask.COLUMN_ID} > $afterTaskId) and " +
                                "(${PendingTask.COLUMN_MANUALLY_RUN} = ${PendingTask.NOT_MANUALLY_RUN}) and " +
                                "(${PendingTask.COLUMN_GROUP_ID} is null or ${PendingTask.COLUMN_GROUP_ID} not in (${notInvolvingGroups.joinToString(separator = ",", transform = { "'$it'" })}))")
                        .exec { parseList(classParser<PendingTask>()).firstOrNull() }
            } else {
                select(PendingTask.TABLE_NAME)
                        .whereArgs("(${PendingTask.COLUMN_ID} > $afterTaskId) AND (${PendingTask.COLUMN_MANUALLY_RUN} = ${PendingTask.NOT_MANUALLY_RUN})")
                        .exec { parseList(classParser<PendingTask>()).firstOrNull() }
            }

            if (nextTask == null) null else tasksFactory.getTask(nextTask!!.tag, nextTask!!)
        }
    }

    fun getTotalNumberOfTasksForRunnerToRun(): Int {
        return db.use {
            select(PendingTask.TABLE_NAME)
                    .whereArgs("${PendingTask.COLUMN_MANUALLY_RUN} = ${PendingTask.NOT_MANUALLY_RUN}")
                    .exec { parseList(classParser<PendingTask>()).size }
        }
    }

    fun deleteTask(pendingTask: PendingTask) {
        db.use {
            delete(PendingTask.TABLE_NAME, "${PendingTask.COLUMN_ID} = ${pendingTask.id}")
        }
    }

}