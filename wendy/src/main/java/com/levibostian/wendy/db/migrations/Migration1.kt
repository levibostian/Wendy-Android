package com.levibostian.wendy.db.migrations

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.levibostian.wendy.db.PersistedPendingTask
import com.levibostian.wendy.extension.ForeignKey
import com.levibostian.wendy.extension.createTable
import com.levibostian.wendy.service.PendingTask
import org.jetbrains.anko.db.*

internal class Migration1: WendyDbMigration {

    override val notes: String = "Migration to remove the unique constraint in ${PersistedPendingTask.TABLE_NAME}. A unique constraint used to exist for ${PersistedPendingTask.COLUMN_DATA_ID} and ${PersistedPendingTask.COLUMN_TAG}. This migration removes that."

    override fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int, currentVersion: Int) {
        // http://sqlite.org/lang_altertable.html on how to remove a unique constraint.
        if (database.inTransaction()) database.endTransaction() // onUpgrade() automatically runs within a transaction but I must control when I begin it so I end the first one, then create a new one below.
        try {
            database.setForeignKeyConstraintsEnabled(false)
            database.transaction {
                database.createTable("new_${PersistedPendingTask.TABLE_NAME}", true,
                        listOf(
                                PersistedPendingTask.COLUMN_ID to INTEGER + PRIMARY_KEY,
                                PersistedPendingTask.COLUMN_CREATED_AT to INTEGER + NOT_NULL,
                                PersistedPendingTask.COLUMN_MANUALLY_RUN to INTEGER + NOT_NULL,
                                PersistedPendingTask.COLUMN_GROUP_ID to TEXT,
                                PersistedPendingTask.COLUMN_DATA_ID to TEXT,
                                PersistedPendingTask.COLUMN_TAG to TEXT + NOT_NULL))
                database.execSQL("INSERT INTO new_${PersistedPendingTask.TABLE_NAME} SELECT * FROM ${PersistedPendingTask.TABLE_NAME}")
                database.dropTable(PersistedPendingTask.TABLE_NAME)
                database.execSQL("ALTER TABLE new_${PersistedPendingTask.TABLE_NAME} RENAME TO ${PersistedPendingTask.TABLE_NAME}")
                if (!database.isDatabaseIntegrityOk) throw RuntimeException("Database migration failed. Current version in migration: $currentVersion, oldVersion: $oldVersion, newVersion: $newVersion")
            }
            database.setForeignKeyConstraintsEnabled(true)
            database.beginTransaction() // onUpgrade() must end while in a transaction or it will throw `IllegalStateException: Cannot perform this operation because there is no current transaction.` so I begin one before we end the upgrade process.
        } catch (e: Throwable) {
            throw RuntimeException("Wendy database migration failed. Please make an issue: https://github.com/levibostian/wendy-android/issues/new \n\n Error: Current version in migration: $currentVersion, oldVersion: $oldVersion, newVersion: $newVersion. Error message: ${e.localizedMessage}\n\nStacktrace: ${e.printStackTrace()}")
        }
    }

}