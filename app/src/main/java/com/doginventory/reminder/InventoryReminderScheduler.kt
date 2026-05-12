package com.doginventory.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.doginventory.R
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.data.entity.InventoryItemEntity
import com.doginventory.data.entity.InventoryReminderRuleEntity
import com.doginventory.ui.inventory.InventoryReminderDraft
import kotlin.math.abs

class InventoryReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    fun exactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = "package:${context.packageName}".toUri()
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = "package:${context.packageName}".toUri()
            }
        }
    }

    fun syncItemReminders(
        item: InventoryItemEntity,
        rules: List<InventoryReminderRuleEntity>,
        category: InventoryCategoryEntity? = null,
        now: Long = System.currentTimeMillis()
    ) {
        rules.forEach { cancel(it.id) }
        if (item.status != "active" || !canScheduleExactAlarms()) return
        rules.forEach { rule ->
            val draft = InventoryReminderDraft.fromEntity(rule)
            val remindAt = draft.scheduledAtForItem(item, now) ?: return@forEach
            schedule(
                ruleId = rule.id,
                itemName = item.name,
                body = inventoryReminderBody(draft.kind, item.name, category?.name),
                triggerAtMillis = remindAt
            )
        }
    }

    fun cancel(ruleId: String) {
        pendingIntent(ruleId, "", "", PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    private fun schedule(ruleId: String, itemName: String, body: String, triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(ruleId, itemName, body, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun inventoryReminderBody(kind: String, itemName: String, categoryName: String?): String {
        val prefix = when (kind) {
            "expire_offset" -> context.getString(R.string.notification_inventory_reminder_expire_offset)
            "expire_at" -> context.getString(R.string.notification_inventory_reminder_expire_at)
            else -> context.getString(R.string.notification_inventory_reminder_default)
        }
        val categoryText = categoryName?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.notification_inventory_reminder_category_format, it) }
            .orEmpty()
        return context.getString(R.string.notification_inventory_reminder_body, prefix, itemName, categoryText)
    }

    private fun pendingIntent(ruleId: String, itemName: String, body: String, flags: Int): PendingIntent? {
        val requestCode = notificationIdForRule(ruleId)
        val intent = Intent(context, InventoryReminderReceiver::class.java).apply {
            putExtra(InventoryReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(InventoryReminderReceiver.EXTRA_ITEM_NAME, itemName)
            putExtra(InventoryReminderReceiver.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        fun notificationIdForRule(ruleId: String): Int = abs("inventory:$ruleId".hashCode()).coerceAtLeast(1)
    }
}
