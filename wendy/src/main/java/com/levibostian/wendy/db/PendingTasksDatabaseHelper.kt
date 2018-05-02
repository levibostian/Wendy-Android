package com.levibostian.wendy.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.levibostian.wendy.db.migrations.Migration1
import com.levibostian.wendy.extension.ForeignKey
import com.levibostian.wendy.extension.createTable
import com.levibostian.wendy.service.PendingTask
import com.levibostian.wendy.util.LogUtil
import org.jetbrains.anko.db.*

internal class PendingTasksDatabaseHelper(applicationContext: Context) : ManagedSQLiteOpenHelper(applicationContext, PendingTasksDatabaseHelper.TABLE_NAME, null, 2) {

    companion object {
        const val TABLE_NAME = "WendyPendingTasksDatabase"
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
                        PersistedPendingTask.COLUMN_CREATED_AT to INTEGER + NOT_NULL, // storing Date to INTEGER as date.time
                        PersistedPendingTask.COLUMN_MANUALLY_RUN to INTEGER + NOT_NULL, // 0 == false, 1 == true
                        PersistedPendingTask.COLUMN_GROUP_ID to TEXT,
                        PersistedPendingTask.COLUMN_DATA_ID to TEXT,
                        PersistedPendingTask.COLUMN_TAG to TEXT + NOT_NULL))

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

    override fun onConfigure(db: SQLiteDatabase?) {
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        for (currentVersion in oldVersion..newVersion) {
            when (currentVersion) {
                1 -> {
                    val migration = Migration1()
                    LogUtil.d("Running Wendy database migration. Migration notes: ${migration.notes}")
                    migration.onUpgrade(database, oldVersion, newVersion, currentVersion)
                }
            }
        }
    }

}