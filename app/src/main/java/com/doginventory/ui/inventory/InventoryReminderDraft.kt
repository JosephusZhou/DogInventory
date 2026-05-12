package com.doginventory.ui.inventory

import android.content.Context
import com.doginventory.R
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

const val INVENTORY_DEFAULT_REMINDER_HOUR = 9
const val INVENTORY_DEFAULT_REMINDER_MINUTE = 0
val INVENTORY_DEFAULT_OFFSET_DAYS = listOf(7, 1)

data class InventoryReminderDraft(
    val id: String = UUID.randomUUID().toString(),
    val kind: String,
    val enabled: Boolean = true,
    val daysBefore: Int? = null,
    val remindAt: Long? = null,
    val lastTriggeredAt: Long? = null
) {
    fun label(context: Context): String = when (kind) {
        "expire_offset" -> context.getString(
            R.string.inventory_offset_reminder_label,
            daysBefore ?: 0,
            DEFAULT_TIME_TEXT
        )
        "expire_at" -> context.getString(
            R.string.inventory_absolute_reminder_label,
            remindAt?.let(::formatInventoryDateTime).orEmpty()
        )
        else -> kind
    }

    fun toEntity(itemId: String, now: Long = System.currentTimeMillis()): InventoryReminderRuleEntity {
        val payload = JSONObject()
        daysBefore?.let { payload.put("daysBefore", it) }
        remindAt?.let { payload.put("remindAt", it) }
        return InventoryReminderRuleEntity(
            id = id,
            itemId = itemId,
            kind = kind,
            enabled = enabled,
            payloadJson = payload.toString(),
            lastTriggeredAt = lastTriggeredAt,
            createdAt = now,
            updatedAt = now
        )
    }

    fun scheduledAtForItem(item: InventoryItemEntity, now: Long = System.currentTimeMillis()): Long? {
        if (!enabled) return null
        val scheduled = when (kind) {
            "expire_offset" -> {
                val expireAt = item.expireAt ?: return null
                val days = daysBefore ?: return null
                Calendar.getInstance().apply {
                    timeInMillis = expireAt
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.DAY_OF_YEAR, -days)
                    set(Calendar.HOUR_OF_DAY, INVENTORY_DEFAULT_REMINDER_HOUR)
                    set(Calendar.MINUTE, INVENTORY_DEFAULT_REMINDER_MINUTE)
                }.timeInMillis
            }
            "expire_at" -> remindAt
            else -> null
        }
        return scheduled?.takeIf { it > now }
    }

    companion object {
        private const val DEFAULT_TIME_TEXT = "09:00"

        fun defaultOffset(daysBefore: Int, expireAt: Long? = null, now: Long = System.currentTimeMillis()): InventoryReminderDraft {
            val enabled = expireAt?.let { expiry ->
                val item = InventoryItemEntity(
                    id = "draft",
                    name = "",
                    categoryId = null,
                    quantityCurrent = null,
                    quantityLowThreshold = null,
                    expireAt = expiry,
                    createdAt = now
                )
                InventoryReminderDraft(kind = "expire_offset", daysBefore = daysBefore).scheduledAtForItem(item, now) != null
            } ?: true
            return InventoryReminderDraft(kind = "expire_offset", daysBefore = daysBefore, enabled = enabled)
        }

        fun absolute(remindAt: Long): InventoryReminderDraft = InventoryReminderDraft(kind = "expire_at", remindAt = remindAt)

        fun fromEntity(entity: InventoryReminderRuleEntity): InventoryReminderDraft {
            val payload = runCatching { JSONObject(entity.payloadJson) }.getOrElse { JSONObject() }
            return InventoryReminderDraft(
                id = entity.id,
                kind = entity.kind,
                enabled = entity.enabled,
                daysBefore = payload.optIntOrNull("daysBefore"),
                remindAt = payload.optLongOrNull("remindAt"),
                lastTriggeredAt = entity.lastTriggeredAt
            )
        }
    }
}

private fun JSONObject.optIntOrNull(name: String): Int? = if (has(name) && !isNull(name)) optInt(name) else null
private fun JSONObject.optLongOrNull(name: String): Long? = if (has(name) && !isNull(name)) optLong(name) else null
