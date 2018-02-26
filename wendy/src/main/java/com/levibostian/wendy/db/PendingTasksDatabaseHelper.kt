package com.levibostian.wendy.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
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
        database.createTable(PendingTask.TABLE_NAME, true,
                PendingTask.UNIQUE_CONSTRAINT_COLUMNS,
                PendingTask.COLUMN_ID to INTEGER + PRIMARY_KEY, // autoincrementing by default.
                PendingTask.COLUMN_CREATED_AT to INTEGER + NOT_NULL, // storing Date to INTEGER as date.time
                PendingTask.COLUMN_MANUALLY_RUN to INTEGER + NOT_NULL, // 0 == false, 1 == true
                PendingTask.COLUMN_GROUP_ID to TEXT,
                PendingTask.COLUMN_DATA_ID to TEXT,
                PendingTask.COLUMN_TAG to TEXT + NOT_NULL)
    }

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
    }

}

fun SQLiteDatabase.createTable(tableName: String, ifNotExists: Boolean = false, unique: List<String>, vararg columns: Pair<String, SqlType>) {
    val escapedTableName = tableName.replace("`", "``")
    val ifNotExistsText = if (ifNotExists) "IF NOT EXISTS" else ""
    val uniqueQuery = if (unique.isNotEmpty()) "UNIQUE (${unique.joinToString()}) ON CONFLICT REPLACE" else ""
    execSQL(
            columns.map { col ->
                "${col.first} ${col.second.render()}"
            }.joinToString(", ", prefix = "CREATE TABLE $ifNotExistsText `$escapedTableName`(", postfix = " $uniqueQuery);")
    )
}