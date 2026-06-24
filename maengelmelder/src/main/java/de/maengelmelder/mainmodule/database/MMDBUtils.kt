package de.maengelmelder.mainmodule.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

object MMDBUtils {

    fun createTable(db: SQLiteDatabase,
                    tableName: String,
                    ifNotExists: Boolean = false,
                    vararg columns: Pair<String, SqlType>) {
        val escapedTableName = tableName.replace("`", "``")
        val ifNotExistsText = if (ifNotExists) "IF NOT EXISTS" else ""
        db.execSQL(
            columns.map { col ->
                "${col.first} ${col.second.render()}"
            }.joinToString(", ", prefix = "CREATE TABLE $ifNotExistsText `$escapedTableName`(", postfix = ");")
        )
    }

    fun dropTable(db: SQLiteDatabase, tableName: String, ifExists: Boolean = false) {
        val escapedTableName = tableName.replace("`", "``")
        val ifExistsText = if (ifExists) "IF EXISTS" else ""
        db.execSQL("DROP TABLE $ifExistsText `$escapedTableName`;")
    }

    fun insert(db: SQLiteDatabase, tableName: String, vararg values: Pair<String, Any?>): Long {
        return db.insert(tableName, null, values.toContentValues())
    }

    fun replace(db: SQLiteDatabase, tableName: String, vararg values: Pair<String, Any?>): Long {
        return db.replace(tableName, null, values.toContentValues())
    }

    fun delete(db: SQLiteDatabase, tableName: String, whereClause: String = ""): Int {
        return db.delete(tableName, whereClause, null)
    }

    fun transaction(db: SQLiteDatabase?, code: SQLiteDatabase.() -> Unit) {
        try {
            db?.beginTransaction()
            db?.let { dbobj -> code(dbobj) }
            db?.setTransactionSuccessful()
        } catch (e: Exception) {
            // Do nothing, just stop the transaction
        } finally {
            db?.endTransaction()
        }
    }
}

fun Array<out Pair<String, Any?>>.toContentValues(): ContentValues {
    val values = ContentValues()
    for ((key, value) in this) {
        when (value) {
            null -> values.putNull(key)
            is Boolean -> values.put(key, value)
            is Byte -> values.put(key, value)
            is ByteArray -> values.put(key, value)
            is Double -> values.put(key, value)
            is Float -> values.put(key, value)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Short -> values.put(key, value)
            is String -> values.put(key, value)
            else -> throw IllegalArgumentException("Non-supported value type: ${value.javaClass.name}")
        }
    }
    return values
}

interface SqlType {
    val name: String

    fun render(): String
    operator fun plus(m: SqlTypeModifier): SqlType

    companion object {
        fun create(name: String): SqlType = SqlTypeImpl(name)
    }
}

interface SqlTypeModifier {
    val modifier: String
    companion object {
        fun create(modifier: String): SqlTypeModifier = SqlTypeModifierImpl(modifier)
    }
}

val PRIMARY_KEY: SqlTypeModifier = SqlTypeModifierImpl("PRIMARY KEY")
val NOT_NULL: SqlTypeModifier = SqlTypeModifierImpl("NOT NULL")
val AUTOINCREMENT: SqlTypeModifier = SqlTypeModifierImpl("AUTOINCREMENT")
fun DEFAULT(value: String): SqlTypeModifier = SqlTypeModifierImpl("DEFAULT $value")
val UNIQUE: SqlTypeModifier = SqlTypeModifierImpl("UNIQUE")
val NULL: SqlType = SqlTypeImpl("NULL")
val INTEGER: SqlType = SqlTypeImpl("INTEGER")
val REAL: SqlType = SqlTypeImpl("REAL")
val TEXT: SqlType = SqlTypeImpl("TEXT")
val BLOB: SqlType = SqlTypeImpl("BLOB")

open class SqlTypeModifierImpl(override val modifier: String) : SqlTypeModifier
open class SqlTypeImpl(override val name: String, val modifiers: String? = null) : SqlType {
    override fun render() = if (modifiers == null) name else "$name $modifiers"
    override fun plus(m: SqlTypeModifier): SqlType {
        return SqlTypeImpl(name, if (modifiers == null) m.modifier else "$modifiers ${m.modifier}")
    }
}