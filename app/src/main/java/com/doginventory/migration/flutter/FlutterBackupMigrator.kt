// TEMPORARY: dog_remind -> DogInventory migration helper.
// Delete the entire com.doginventory.migration.flutter package once all users have migrated.
package com.doginventory.migration.flutter

import android.content.Context
import android.net.Uri
import com.doginventory.data.AppDatabase
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.entity.ShoppingItemEntity
import com.doginventory.data.repository.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.Instant
import java.io.File
import java.util.UUID

class FlutterBackupMigrator(
    private val context: Context,
    private val database: AppDatabase,
    private val repository: InventoryRepository
) {
    suspend fun migrate(sourceUri: Uri): FlutterMigrationResult = withContext(Dispatchers.IO) {
        val extractor = FlutterBackupZipExtractor(context)
        var zipFile: File? = null
        var dbFile: File? = null

        try {
            zipFile = File(context.cacheDir, "flutter_backup_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                zipFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw FlutterMigrationException("无法读取备份文件")

            dbFile = extractor.extractDatabase(zipFile)
            val reader = FlutterSqliteReader.open(dbFile)

            val warnings = mutableListOf<String>()
            var categoriesCount = 0
            var itemsCount = 0
            var rulesCount = 0
            var shoppingCount = 0
            val existingCategoriesByName = database.inventoryDao()
                .watchCategoriesSnapshot()
                .associateBy { it.name.trim() }
                .toMutableMap()

            reader.use {
                repository.withBatchedAutoSync(syncReason = "flutter_backup_migrated") {
                    database.runInTransaction {
                        val now = System.currentTimeMillis()
                        val inventoryDao = database.inventoryDao()

                        val categories = reader.readCategories()
                        val categoryIdMapping = mutableMapOf<String, String>()
                        categories.forEach { row ->
                            val normalizedName = row.name.trim()
                            val existingCategory = existingCategoriesByName[normalizedName]
                            if (existingCategory != null) {
                                categoryIdMapping[row.id] = existingCategory.id
                                return@forEach
                            }

                            val newCategory = InventoryCategoryEntity(
                                id = row.id,
                                name = row.name,
                                color = row.color,
                                icon = row.icon,
                                sortOrder = row.sortOrder,
                                isPreset = false,
                                isDeleted = false,
                                createdAt = row.createdAt.normalizeEpochMillis() ?: now,
                                updatedAt = row.updatedAt.normalizeEpochMillis() ?: now
                            )
                            inventoryDao.insertCategory(newCategory)
                            existingCategoriesByName[normalizedName] = newCategory
                            categoryIdMapping[row.id] = newCategory.id
                            categoriesCount++
                        }

                        val flutterItems = reader.readInventoryItems()
                        val itemIdMapping = mutableMapOf<String, String>()
                        flutterItems.forEach { row ->
                            val newId = UUID.randomUUID().toString()
                            itemIdMapping[row.id] = newId
                            inventoryDao.insertItem(InventoryItemEntity(
                                id = newId,
                                name = row.name,
                                categoryId = row.categoryId?.let(categoryIdMapping::get),
                                quantityCurrent = null,
                                quantityUnit = "",
                                quantityLowThreshold = null,
                                expireAt = row.expireAt.normalizeEpochMillis(),
                                note = row.note,
                                status = "active",
                                createdAt = row.createdAt.normalizeEpochMillis() ?: now,
                                updatedAt = row.updatedAt.normalizeEpochMillis() ?: now
                            ))
                        }
                        itemsCount = flutterItems.size

                        val rules = reader.readReminderRules(itemIdMapping.keys)
                        rules.forEach { row ->
                            val newItemId = itemIdMapping[row.itemId] ?: return@forEach
                            if (!isValidJson(row.payloadJson)) {
                                warnings += "已跳过提醒规则：${row.kind} 的 payload_json 无法解析"
                                return@forEach
                            }
                            inventoryDao.insertRule(InventoryReminderRuleEntity(
                                id = UUID.randomUUID().toString(),
                                itemId = newItemId,
                                kind = row.kind,
                                enabled = row.enabled,
                                payloadJson = normalizeReminderPayloadJson(row.payloadJson),
                                reminderCalendarId = null,
                                reminderCalendarEventId = null,
                                lastTriggeredAt = null,
                                createdAt = row.createdAt.normalizeEpochMillis() ?: now,
                                updatedAt = row.updatedAt.normalizeEpochMillis() ?: now
                            ))
                            rulesCount++
                        }

                        val shopping = reader.readShoppingItems()
                        shopping.forEach { row ->
                            inventoryDao.insertShoppingItem(ShoppingItemEntity(
                                id = UUID.randomUUID().toString(),
                                name = row.name,
                                note = row.note,
                                isDone = row.isDone,
                                doneAt = row.doneAt.normalizeEpochMillis(),
                                createdAt = row.createdAt.normalizeEpochMillis() ?: now,
                                updatedAt = row.updatedAt.normalizeEpochMillis() ?: now
                            ))
                        }
                        shoppingCount = shopping.size
                    }
                }
            }

            repository.resyncAllReminders()

            FlutterMigrationResult(
                categoriesImported = categoriesCount,
                inventoryItemsImported = itemsCount,
                reminderRulesImported = rulesCount,
                shoppingItemsImported = shoppingCount,
                warnings = warnings
            )
        } finally {
            zipFile?.delete()
            dbFile?.let { extractor.cleanup(it) }
        }
    }

    private fun isValidJson(payloadJson: String): Boolean {
        return runCatching {
            JSONObject(payloadJson)
        }.isSuccess
    }

    private fun normalizeReminderPayloadJson(payloadJson: String): String {
        val payload = JSONObject(payloadJson)
        if (payload.has("remindAt") && !payload.isNull("remindAt")) {
            val normalizedRemindAt = when (val value = payload.get("remindAt")) {
                is Number -> value.toLong().normalizeEpochMillis()
                is String -> parseDateTimeStringToMillis(value)
                else -> null
            }
            if (normalizedRemindAt != null) {
                payload.put("remindAt", normalizedRemindAt)
            }
        }
        return payload.toString()
    }

    private fun parseDateTimeStringToMillis(value: String): Long? {
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
    }

    private fun Long?.normalizeEpochMillis(): Long? {
        if (this == null) return null
        return if (this in 0..99_999_999_999L) this * 1000 else this
    }
}
