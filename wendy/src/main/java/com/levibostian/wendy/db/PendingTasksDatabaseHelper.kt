package com.levibostian.wendy.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.levibostian.wendy.extension.ForeignKey
import com.levibostian.wendy.extension.createTable
import com.levibostian.wendy.service.PendingTask
import org.jetbrains.anko.db.*

internal class PendingTasksDatabaseHelper(applicationContext: Context, databaseName: String = "WendyPendingTasksDatabase") : ManagedSQLiteOpenHelper(applicationContext, databaseName, null, 1) {

    companion object {
        private var instance: PendingTasksDatabaseHelper? = null

        @Synchronized fun sharedInstance(applicationContext: Context): PendingTasksDatabaseHelper {
            if (instance == null) { instance = PendingTasksDatabaseHelper(applicationContext) }
            return instance!!
        }
    }

    override fun onCreate(database: SQLiteDatabase) {
        database.createTable(PersistedPendingTask.TABLE_NAME, true,
                listOf(
                        PersistedPendingTask.COLUMN_ID to INTEGER + PRIMARY_KEY, // autoincrementing by default.
                        PersistedPendingTask.COLUMN_TASK_ID to INTEGER + NOT_NULL,
                        PersistedPendingTask.COLUMN_CREATED_AT to INTEGER + NOT_NULL, // storing Date to INTEGER as date.time
                        PersistedPendingTask.COLUMN_MANUALLY_RUN to INTEGER + NOT_NULL, // 0 == false, 1 == true
                        PersistedPendingTask.COLUMN_GROUP_ID to TEXT,
                        PersistedPendingTask.COLUMN_DATA_ID to TEXT,
                        PersistedPendingTask.COLUMN_TAG to TEXT + NOT_NULL),
                unique = PersistedPendingTask.UNIQUE_CONSTRAINT_COLUMNS)

        database.createTable(PendingTaskError.TABLE_NAME, true,
                listOf(
                        PendingTaskError.COLUMN_ID to INTEGER + PRIMARY_KEY, // autoincrementing by default.
                        PendingTaskError.COLUMN_TASK_ID to INTEGER + NOT_NULL,
                        PendingTaskError.COLUMN_CREATED_AT to INTEGER + NOT_NULL, // storing Date to INTEGER as date.time
                        PendingTaskError.COLUMN_ERROR_MESSAGE to TEXT,
                        PendingTaskError.COLUMN_ERROR_ID to TEXT),
                unique = PendingTaskError.UNIQUE_CONSTRAINT_COLUMNS,
                foreignKeys = listOf(
                        PendingTaskError.FOREIGN_KEY_TASK_ID
                ))
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

}