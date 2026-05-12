package com.doginventory.data.repository

import android.content.Context
import com.doginventory.data.dao.InventoryDao
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.entity.ShoppingItemEntity
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.webdav.WebDavAutoSyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val reminderScheduler: InventoryReminderScheduler? = null
) {
    var webDavAutoSyncTrigger: WebDavAutoSyncTrigger? = null

    val allCategories: Flow<List<InventoryCategoryEntity>> = inventoryDao.watchCategories()
    val activeItems: Flow<List<InventoryItemEntity>> = inventoryDao.watchActiveItems()
    val shoppingItems: Flow<List<ShoppingItemEntity>> = inventoryDao.watchShoppingItems()

    fun watchRulesByItemId(itemId: String): Flow<List<InventoryReminderRuleEntity>> =
        inventoryDao.watchRulesByItemId(itemId)

    fun watchAllRules(): Flow<List<InventoryReminderRuleEntity>> = inventoryDao.watchAllRules()

    fun watchItemById(id: String): Flow<InventoryItemEntity?> = inventoryDao.watchItemById(id)

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

    suspend fun resyncAllReminders() {
        val categories = allCategories.first().associateBy { it.id }
        val items = activeItems.first().associateBy { it.id }
        val groupedRules = inventoryDao.getAllRules().groupBy { it.itemId }
        items.values.forEach { item ->
            syncReminders(item, groupedRules[item.id].orEmpty(), categories[item.categoryId])
        }
    }

    suspend fun cancelAllReminders() {
        inventoryDao.getAllRules().forEach { reminderScheduler?.cancel(it.id) }
    }

    suspend fun updateCategory(category: InventoryCategoryEntity) {
        inventoryDao.updateCategory(category)
        webDavAutoSyncTrigger?.requestSync("inventory_category_updated")
    }

    suspend fun insertCategory(category: InventoryCategoryEntity) {
        inventoryDao.insertCategory(category)
        webDavAutoSyncTrigger?.requestSync("inventory_category_inserted")
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

    private suspend fun syncReminders(
        item: InventoryItemEntity,
        rules: List<InventoryReminderRuleEntity>,
        category: InventoryCategoryEntity? = null
    ) {
        val resolvedCategory = category ?: item.categoryId?.let { inventoryDao.getCategoryById(it) }
        reminderScheduler?.syncItemReminders(item, rules, resolvedCategory)
    }
}
