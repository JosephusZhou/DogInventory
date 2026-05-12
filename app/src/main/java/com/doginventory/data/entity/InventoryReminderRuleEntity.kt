package com.doginventory.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "inventory_reminder_rules",
    foreignKeys = [
        ForeignKey(
            entity = InventoryItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class InventoryReminderRuleEntity(
    @PrimaryKey val id: String,
    val itemId: String,
    val kind: String,
    val enabled: Boolean = true,
    val payloadJson: String,
    val reminderCalendarId: String? = null,
    val reminderCalendarEventId: String? = null,
    val lastTriggeredAt: Long? = null,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis()
)
