package com.doginventory.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "inventory_categories")
data class InventoryCategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: String = "",
    val icon: String = "📦",
    val sortOrder: Int = 0,
    val isPreset: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
