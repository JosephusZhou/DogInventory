// TEMPORARY: dog_remind -> DogInventory migration helper.
// Delete the entire com.doginventory.migration.flutter package once all users have migrated.
package com.doginventory.migration.flutter

internal data class FlutterCategoryRow(
    val id: String,
    val name: String,
    val color: String,
    val icon: String,
    val sortOrder: Int,
    val createdAt: Long?,
    val updatedAt: Long?
)

internal data class FlutterInventoryItemRow(
    val id: String,
    val name: String,
    val categoryId: String?,
    val expireAt: Long?,
    val note: String,
    val createdAt: Long?,
    val updatedAt: Long?
)

internal data class FlutterReminderRuleRow(
    val itemId: String,
    val kind: String,
    val enabled: Boolean,
    val payloadJson: String,
    val createdAt: Long?,
    val updatedAt: Long?
)

internal data class FlutterShoppingItemRow(
    val name: String,
    val note: String,
    val isDone: Boolean,
    val doneAt: Long?,
    val createdAt: Long?,
    val updatedAt: Long?
)

data class FlutterMigrationResult(
    val categoriesImported: Int,
    val inventoryItemsImported: Int,
    val reminderRulesImported: Int,
    val shoppingItemsImported: Int,
    val warnings: List<String>
)

class FlutterMigrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
