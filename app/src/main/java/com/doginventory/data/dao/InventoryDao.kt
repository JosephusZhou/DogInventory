package com.doginventory.data.dao

import androidx.room.*
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    // Categories
    @Query("SELECT * FROM inventory_categories WHERE isDeleted = 0 ORDER BY sortOrder ASC, createdAt ASC")
    fun watchCategories(): Flow<List<InventoryCategoryEntity>>

    @Query("SELECT * FROM inventory_categories WHERE id = :id")
    suspend fun getCategoryById(id: String): InventoryCategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: InventoryCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<InventoryCategoryEntity>)

    @Update
    suspend fun updateCategory(category: InventoryCategoryEntity)

    @Update
    suspend fun updateCategories(categories: List<InventoryCategoryEntity>)

    @Delete
    suspend fun deleteCategory(category: InventoryCategoryEntity)

    // Items
    @Query("SELECT * FROM inventory_items WHERE status = 'active' ORDER BY updatedAt DESC")
    fun watchActiveItems(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    fun watchItemById(id: String): Flow<InventoryItemEntity?>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getItemById(id: String): InventoryItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryItemEntity)

    @Update
    suspend fun updateItem(item: InventoryItemEntity)

    @Delete
    suspend fun deleteItem(item: InventoryItemEntity)

    // Shopping Items
    @Query("SELECT * FROM shopping_items ORDER BY isDone ASC, updatedAt DESC")
    fun watchShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_items WHERE id = :id")
    suspend fun getShoppingItemById(id: String): ShoppingItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_items WHERE isDone = 1")
    suspend fun deleteDoneShoppingItems()

    // Reminder Rules
    @Query("SELECT * FROM inventory_reminder_rules WHERE itemId = :itemId")
    fun watchRulesByItemId(itemId: String): Flow<List<InventoryReminderRuleEntity>>

    @Query("SELECT * FROM inventory_reminder_rules WHERE itemId = :itemId")
    suspend fun getRulesByItemId(itemId: String): List<InventoryReminderRuleEntity>

    @Query("SELECT * FROM inventory_reminder_rules")
    suspend fun getAllRules(): List<InventoryReminderRuleEntity>

    @Query("SELECT * FROM inventory_reminder_rules")
    fun watchAllRules(): Flow<List<InventoryReminderRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: InventoryReminderRuleEntity)

    @Query("DELETE FROM inventory_reminder_rules WHERE itemId = :itemId")
    suspend fun deleteRulesByItemId(itemId: String)
    
    @Transaction
    suspend fun deleteItemWithRules(itemId: String) {
        deleteRulesByItemId(itemId)
        val item = getItemById(itemId)
        if (item != null) {
            deleteItem(item)
        }
    }
}
