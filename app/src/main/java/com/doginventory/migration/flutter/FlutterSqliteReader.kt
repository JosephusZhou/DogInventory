// TEMPORARY: dog_remind -> DogInventory migration helper.
// Delete the entire com.doginventory.migration.flutter package once all users have migrated.
package com.doginventory.migration.flutter

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

internal class FlutterSqliteReader(private val db: SQLiteDatabase) : AutoCloseable {

    fun readCategories(): List<FlutterCategoryRow> {
        val rows = mutableListOf<FlutterCategoryRow>()
        db.rawQuery("SELECT * FROM inventory_categories WHERE is_deleted = 0", null).use { cursor ->
            val idIndex = cursor.requireColumnIndex("id")
            val nameIndex = cursor.requireColumnIndex("name")
            val colorIndex = cursor.getColumnIndex("color")
            val iconIndex = cursor.getColumnIndex("icon")
            val sortOrderIndex = cursor.getColumnIndex("sort_order")
            val createdAtIndex = cursor.getColumnIndex("created_at")
            val updatedAtIndex = cursor.getColumnIndex("updated_at")
            while (cursor.moveToNext()) {
                rows.add(FlutterCategoryRow(
                    id = cursor.getString(idIndex),
                    name = cursor.getString(nameIndex),
                    color = cursor.getStringOrDefault(colorIndex),
                    icon = cursor.getStringOrDefault(iconIndex, "📦"),
                    sortOrder = cursor.getIntOrDefault(sortOrderIndex),
                    createdAt = cursor.getLongOrNull(createdAtIndex),
                    updatedAt = cursor.getLongOrNull(updatedAtIndex)
                ))
            }
        }
        return rows
    }

    fun readInventoryItems(): List<FlutterInventoryItemRow> {
        val rows = mutableListOf<FlutterInventoryItemRow>()
        db.rawQuery("SELECT * FROM inventory_items WHERE status = 'active'", null).use { cursor ->
            val idIndex = cursor.requireColumnIndex("id")
            val nameIndex = cursor.requireColumnIndex("name")
            val categoryIdIndex = cursor.getColumnIndex("category_id")
            val expireAtIndex = cursor.getColumnIndex("expire_at")
            val noteIndex = cursor.getColumnIndex("note")
            val createdAtIndex = cursor.getColumnIndex("created_at")
            val updatedAtIndex = cursor.getColumnIndex("updated_at")
            while (cursor.moveToNext()) {
                rows.add(FlutterInventoryItemRow(
                    id = cursor.getString(idIndex),
                    name = cursor.getString(nameIndex),
                    categoryId = cursor.getStringOrNull(categoryIdIndex),
                    expireAt = cursor.getLongOrNull(expireAtIndex),
                    note = cursor.getStringOrDefault(noteIndex),
                    createdAt = cursor.getLongOrNull(createdAtIndex),
                    updatedAt = cursor.getLongOrNull(updatedAtIndex)
                ))
            }
        }
        return rows
    }

    fun readReminderRules(itemIds: Set<String>): List<FlutterReminderRuleRow> {
        if (itemIds.isEmpty()) return emptyList()
        val rows = mutableListOf<FlutterReminderRuleRow>()
        val placeholders = itemIds.joinToString(",") { "?" }
        db.rawQuery("SELECT * FROM inventory_reminder_rules WHERE item_id IN ($placeholders)", itemIds.toTypedArray()).use { cursor ->
            val itemIdIndex = cursor.requireColumnIndex("item_id")
            val kindIndex = cursor.requireColumnIndex("kind")
            val enabledIndex = cursor.getColumnIndex("enabled")
            val payloadJsonIndex = cursor.requireColumnIndex("payload_json")
            val createdAtIndex = cursor.getColumnIndex("created_at")
            val updatedAtIndex = cursor.getColumnIndex("updated_at")
            while (cursor.moveToNext()) {
                rows.add(FlutterReminderRuleRow(
                    itemId = cursor.getString(itemIdIndex),
                    kind = cursor.getString(kindIndex),
                    enabled = cursor.getIntOrDefault(enabledIndex, 1) == 1,
                    payloadJson = cursor.getString(payloadJsonIndex),
                    createdAt = cursor.getLongOrNull(createdAtIndex),
                    updatedAt = cursor.getLongOrNull(updatedAtIndex)
                ))
            }
        }
        return rows
    }

    fun readShoppingItems(): List<FlutterShoppingItemRow> {
        val rows = mutableListOf<FlutterShoppingItemRow>()
        db.rawQuery("SELECT * FROM shopping_items", null).use { cursor ->
            val nameIndex = cursor.requireColumnIndex("name")
            val noteIndex = cursor.getColumnIndex("note")
            val isDoneIndex = cursor.getColumnIndex("is_done")
            val doneAtIndex = cursor.getColumnIndex("done_at")
            val createdAtIndex = cursor.getColumnIndex("created_at")
            val updatedAtIndex = cursor.getColumnIndex("updated_at")
            while (cursor.moveToNext()) {
                rows.add(FlutterShoppingItemRow(
                    name = cursor.getString(nameIndex),
                    note = cursor.getStringOrDefault(noteIndex),
                    isDone = cursor.getIntOrDefault(isDoneIndex) == 1,
                    doneAt = cursor.getLongOrNull(doneAtIndex),
                    createdAt = cursor.getLongOrNull(createdAtIndex),
                    updatedAt = cursor.getLongOrNull(updatedAtIndex)
                ))
            }
        }
        return rows
    }

    override fun close() {
        db.close()
    }

    companion object {
        fun open(dbFile: File): FlutterSqliteReader {
            val db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
            val tables = mutableSetOf<String>()
            db.rawQuery("SELECT name FROM sqlite_master WHERE type='table'", null).use { cursor ->
                while (cursor.moveToNext()) {
                    tables.add(cursor.getString(0))
                }
            }
            val required = setOf("inventory_categories", "inventory_items", "inventory_reminder_rules", "shopping_items")
            if (!tables.containsAll(required)) {
                db.close()
                throw FlutterMigrationException("备份数据库缺少必要的表")
            }
            return FlutterSqliteReader(db)
        }
    }
}

private fun Cursor.requireColumnIndex(name: String): Int {
    val index = getColumnIndex(name)
    if (index >= 0) return index
    throw FlutterMigrationException("备份数据库缺少必要字段: $name")
}

private fun Cursor.getStringOrNull(index: Int): String? =
    if (index < 0 || isNull(index)) null else getString(index)

private fun Cursor.getStringOrDefault(index: Int, defaultValue: String = ""): String =
    getStringOrNull(index) ?: defaultValue

private fun Cursor.getLongOrNull(index: Int): Long? =
    if (index < 0 || isNull(index)) null else getLong(index)

private fun Cursor.getIntOrDefault(index: Int, defaultValue: Int = 0): Int =
    if (index < 0 || isNull(index)) defaultValue else getInt(index)
