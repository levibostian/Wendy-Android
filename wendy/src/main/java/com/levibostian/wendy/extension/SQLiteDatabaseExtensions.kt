package com.levibostian.wendy.extension

import android.database.sqlite.SQLiteDatabase
import org.jetbrains.anko.db.SqlType

internal class ForeignKey(val columnName: String,
                 val references: Pair<String, String>, // Table name, column name
                 val onUpdate: String = "CASCADE",
                 val onDelete: String = "CASCADE")

internal fun SQLiteDatabase.createTable(tableName: String, ifNotExists: Boolean = false, columns: List<Pair<String, SqlType>>, unique: List<String>? = null, foreignKeys: List<ForeignKey>? = null) {
    val escapedTableName = tableName.replace("`", "``")
    val ifNotExistsText = if (ifNotExists) "IF NOT EXISTS" else ""
    val uniqueQuery = if (unique != null && unique.isNotEmpty()) ", UNIQUE (${unique.joinToString()}) ON CONFLICT REPLACE" else ""

    val foreignKeyQuery =
            if (foreignKeys != null && foreignKeys.isNotEmpty())
                foreignKeys.joinToString {
                    ", FOREIGN KEY (${it.columnName}) REFERENCES ${it.references.first}(${it.references.second}) ON UPDATE ${it.onUpdate} ON DELETE ${it.onDelete}"
                }
            else ""

    execSQL(
            columns.map { col ->
                "${col.first} ${col.second.render()}"
            }.joinToString(", ", prefix = "CREATE TABLE $ifNotExistsText `$escapedTableName`(", postfix = " $foreignKeyQuery $uniqueQuery);")
    )
}