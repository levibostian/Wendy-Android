package com.levibostian.wendy.db.migrations

import android.database.sqlite.SQLiteDatabase

internal interface WendyDbMigration {
    val notes: String
    /**
     * @param oldVersion Version the database *begins* at before they opened the app.
     * @param newVersion Version the database is *going to* after the migration.
     * @param currentVersion The version the database is currently migrating from.
     */
    fun onUpgrade(database: SQLiteDatabase, oldVersion: Int, newVersion: Int, currentVersion: Int)
}