package com.doginventory.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_items",
    indices = [Index(value = ["isDone", "updatedAt"])]
)
data class ShoppingItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val note: String = "",
    val isDone: Boolean = false,
    val doneAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
