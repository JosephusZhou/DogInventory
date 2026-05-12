package com.doginventory.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_items",
    foreignKeys = [
        ForeignKey(
            entity = InventoryCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["categoryId"])]
)
data class InventoryItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val categoryId: String?,
    val quantityCurrent: Double?,
    val quantityUnit: String = "",
    val quantityLowThreshold: Double?,
    val expireAt: Long?,
    val note: String = "",
    val status: String = "active",
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
