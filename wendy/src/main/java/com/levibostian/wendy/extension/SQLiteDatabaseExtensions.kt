package com.levibostian.wendy.extension

import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.SqlType

internal fun SQLiteDatabase.createTable(tableName: String, ifNotExists: Boolean = false, unique: List<String>, vararg columns: Pair<String, SqlType>) {
    val escapedTableName = tableName.replace("`", "``")
    val ifNotExistsText = if (ifNotExists) "IF NOT EXISTS" else ""
    val uniqueQuery = if (unique.isNotEmpty()) ", UNIQUE (${unique.joinToString()}) ON CONFLICT REPLACE" else ""
    execSQL(
            columns.map { col ->
                "${col.first} ${col.second.render()}"
            }.joinToString(", ", prefix = "CREATE TABLE $ifNotExistsText `$escapedTableName`(", postfix = " $uniqueQuery);")
    )
}