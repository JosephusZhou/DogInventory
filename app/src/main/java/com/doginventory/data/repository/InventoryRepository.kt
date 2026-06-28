package com.doginventory.data.repository

import com.doginventory.data.dao.InventoryDao
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.entity.ShoppingItemEntity
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.share.ShareImportResult
import com.doginventory.share.SharedCategoryDto
import com.doginventory.share.SharedItemDto
import com.doginventory.webdav.WebDavAutoSyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.util.UUID

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val reminderScheduler: InventoryReminderScheduler? = null
) {
    var webDavAutoSyncTrigger: WebDavAutoSyncTrigger? = null

    // 串行化全量重调度：boot 接收者、App 启动、兜底 Worker 可能并发调用 resyncAllReminders()。
    private val reminderSyncMutex = Mutex()

    val allCategories: Flow<List<InventoryCategoryEntity>> = inventoryDao.watchCategories()
    val activeItems: Flow<List<InventoryItemEntity>> = inventoryDao.watchActiveItems()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = inventoryDao.watchShoppingItems()

    fun watchRulesByItemId(itemId: String): Flow<List<InventoryReminderRuleEntity>> =
        inventoryDao.watchRulesByItemId(itemId)

    fun watchAllRules(): Flow<List<InventoryReminderRuleEntity>> = inventoryDao.watchAllRules()

    fun watchItemById(id: String): Flow<InventoryItemEntity?> = inventoryDao.watchItemById(id)

    suspend fun getCategoriesOnce(): List<InventoryCategoryEntity> = allCategories.first()

    suspend fun insertItem(item: InventoryItemEntity, rules: List<InventoryReminderRuleEntity>) {
        inventoryDao.insertItem(item)
        rules.forEach { inventoryDao.insertRule(it) }
        syncReminders(item, rules)
        webDavAutoSyncTrigger?.requestSync("inventory_item_inserted")
    }

    suspend fun updateItem(item: InventoryItemEntity, rules: List<InventoryReminderRuleEntity>) {
        val existingRules = inventoryDao.getRulesByItemId(item.id)
        existingRules.forEach { reminderScheduler?.cancel(it.id) }
        inventoryDao.updateItem(item)
        inventoryDao.deleteRulesByItemId(item.id)
        rules.forEach { inventoryDao.insertRule(it) }
        syncReminders(item, rules)
        webDavAutoSyncTrigger?.requestSync("inventory_item_updated")
    }

    suspend fun deleteItem(itemId: String) {
        inventoryDao.getRulesByItemId(itemId).forEach { reminderScheduler?.cancel(it.id) }
        inventoryDao.deleteItemWithRules(itemId)
        webDavAutoSyncTrigger?.requestSync("inventory_item_deleted")
    }

    suspend fun resyncAllReminders() = reminderSyncMutex.withLock {
        val categories = allCategories.first().associateBy { it.id }
        val items = activeItems.first().associateBy { it.id }
        val groupedRules = inventoryDao.getAllRules().groupBy { it.itemId }
        items.values.forEach { item ->
            syncReminders(item, groupedRules[item.id].orEmpty(), categories[item.categoryId])
        }
    }

    suspend fun getActiveItemsSnapshot(): List<InventoryItemEntity> = activeItems.first()

    suspend fun getAllRulesSnapshot(): List<InventoryReminderRuleEntity> = inventoryDao.getAllRules()

    suspend fun updateRuleTriggeredAt(ruleId: String, ts: Long) {
        inventoryDao.updateRuleTriggeredAt(ruleId, ts)
    }

    suspend fun cancelAllReminders() {
        inventoryDao.getAllRules().forEach { reminderScheduler?.cancel(it.id) }
    }

    suspend fun updateCategory(category: InventoryCategoryEntity) {
        inventoryDao.updateCategory(category)
        webDavAutoSyncTrigger?.requestSync("inventory_category_updated")
    }

    suspend fun updateCategories(categories: List<InventoryCategoryEntity>, syncReason: String) {
        withBatchedAutoSync(syncReason) {
            inventoryDao.updateCategories(categories)
        }
    }

    suspend fun insertCategory(category: InventoryCategoryEntity) {
        inventoryDao.insertCategory(category)
        webDavAutoSyncTrigger?.requestSync("inventory_category_inserted")
    }

    suspend fun insertCategories(categories: List<InventoryCategoryEntity>, syncReason: String) {
        withBatchedAutoSync(syncReason) {
            inventoryDao.insertCategories(categories)
        }
    }

    suspend fun deleteCategory(category: InventoryCategoryEntity) {
        inventoryDao.deleteCategory(category)
        webDavAutoSyncTrigger?.requestSync("inventory_category_deleted")
    }

    suspend fun getItemById(id: String): InventoryItemEntity? =
        inventoryDao.getItemById(id)

    suspend fun getCategoryById(id: String): InventoryCategoryEntity? =
        inventoryDao.getCategoryById(id)

    suspend fun insertShoppingItem(item: ShoppingItemEntity) {
        inventoryDao.insertShoppingItem(item)
        webDavAutoSyncTrigger?.requestSync("shopping_item_inserted")
    }

    suspend fun updateShoppingItem(item: ShoppingItemEntity) {
        inventoryDao.updateShoppingItem(item)
        webDavAutoSyncTrigger?.requestSync("shopping_item_updated")
    }

    suspend fun getShoppingItemById(id: String): ShoppingItemEntity? =
        inventoryDao.getShoppingItemById(id)

    suspend fun deleteShoppingItem(id: String) {
        inventoryDao.getShoppingItemById(id)?.let {
            inventoryDao.deleteShoppingItem(it)
            webDavAutoSyncTrigger?.requestSync("shopping_item_deleted")
        }
    }

    suspend fun deleteDoneShoppingItems() {
        inventoryDao.deleteDoneShoppingItems()
        webDavAutoSyncTrigger?.requestSync("shopping_done_items_deleted")
    }

    suspend fun <T> withBatchedAutoSync(syncReason: String, block: suspend () -> T): T {
        val trigger = webDavAutoSyncTrigger
        trigger?.pauseSync()
        return try {
            block()
        } finally {
            trigger?.resumeSync(flushReason = syncReason)
        }
    }

    suspend fun insertItemsFromShare(
        sharedItems: List<SharedItemDto>,
        sharedCategories: List<SharedCategoryDto>,
        importRules: Boolean
    ): ShareImportResult = withBatchedAutoSync("share_import") {
        val localCategories = inventoryDao.watchCategoriesSnapshot()
        val byName = localCategories.associateBy { it.name.lowercase() }
        val nameToLocalId = mutableMapOf<String, String>()
        var newCategoryCount = 0
        sharedCategories.forEach { sc ->
            val local = byName[sc.name.lowercase()]
            if (local != null) {
                nameToLocalId[sc.name] = local.id
            } else {
                val id = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                inventoryDao.insertCategory(
                    InventoryCategoryEntity(
                        id = id,
                        name = sc.name,
                        color = sc.color ?: "",
                        icon = sc.icon ?: "📦",
                        sortOrder = sc.sortOrder,
                        isPreset = false,
                        isDeleted = false,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                nameToLocalId[sc.name] = id
                newCategoryCount++
            }
        }
        val now = System.currentTimeMillis()
        val rulesByItemId = mutableMapOf<String, MutableList<InventoryReminderRuleEntity>>()
        val newItems = sharedItems.map { s ->
            val itemId = UUID.randomUUID().toString()
            if (importRules) {
                val bucket = rulesByItemId.getOrPut(itemId) { mutableListOf() }
                s.rules.forEach { r ->
                    val payload = JSONObject()
                    if (r.kind == "expire_offset") {
                        payload.put("daysBefore", r.daysBefore ?: 7)
                    } else {
                        payload.put("remindAt", r.remindAt ?: 0L)
                    }
                    bucket += InventoryReminderRuleEntity(
                        id = UUID.randomUUID().toString(),
                        itemId = itemId,
                        kind = r.kind,
                        enabled = r.enabled,
                        payloadJson = payload.toString(),
                        reminderCalendarId = null,
                        reminderCalendarEventId = null,
                        lastTriggeredAt = null,
                        createdAt = now,
                        updatedAt = now
                    )
                }
            }
            InventoryItemEntity(
                id = itemId,
                name = s.name,
                categoryId = s.categoryName?.let { nameToLocalId[it] },
                quantityCurrent = s.quantityCurrent,
                quantityUnit = s.quantityUnit,
                quantityLowThreshold = s.quantityLowThreshold,
                expireAt = s.expireAt,
                note = s.note,
                status = "active",
                createdAt = now,
                updatedAt = now
            )
        }
        newItems.forEach { item ->
            inventoryDao.insertItem(item)
            val itemRules = rulesByItemId[item.id].orEmpty()
            if (itemRules.isNotEmpty()) {
                itemRules.forEach { inventoryDao.insertRule(it) }
            }
            syncReminders(item, itemRules)
        }
        ShareImportResult(importedItemCount = newItems.size, newCategoryCount = newCategoryCount)
    }

    private suspend fun syncReminders(
        item: InventoryItemEntity,
        rules: List<InventoryReminderRuleEntity>,
        category: InventoryCategoryEntity? = null
    ) {
        val resolvedCategory = category ?: item.categoryId?.let { inventoryDao.getCategoryById(it) }
        reminderScheduler?.syncItemReminders(item, rules, resolvedCategory)
    }
}
