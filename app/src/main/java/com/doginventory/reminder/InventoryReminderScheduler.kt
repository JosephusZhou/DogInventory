package com.doginventory.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import com.doginventory.SplashActivity
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
                body = InventoryReminderNotifier.body(context, draft.kind, item.name, category?.name),
                triggerAtMillis = remindAt
            )
        }
    }

    fun cancel(ruleId: String) {
        pendingIntent(ruleId, "", PendingIntent.FLAG_NO_CREATE)?.let(alarmManager::cancel)
    }

    private fun schedule(ruleId: String, body: String, triggerAtMillis: Long) {
        val pendingIntent = pendingIntent(ruleId, body, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        // setAlarmClock 完全豁免 Doze，是面向用户的精确提醒里最强的投递保证；
        // 代价是状态栏出现闹钟图标并计入系统「下一个闹钟」。
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent())
        alarmManager.setAlarmClock(info, pendingIntent)
    }

    private fun showIntent(): PendingIntent {
        val launch = Intent(context, SplashActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            SHOW_INTENT_REQUEST_CODE,
            launch,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun pendingIntent(ruleId: String, body: String, flags: Int): PendingIntent? {
        val requestCode = notificationIdForRule(ruleId)
        val intent = Intent(context, InventoryReminderReceiver::class.java).apply {
            putExtra(InventoryReminderReceiver.EXTRA_NOTIFICATION_ID, requestCode)
            putExtra(InventoryReminderReceiver.EXTRA_BODY, body)
            putExtra(InventoryReminderReceiver.EXTRA_RULE_ID, ruleId)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val SHOW_INTENT_REQUEST_CODE = 1
        fun notificationIdForRule(ruleId: String): Int = abs("inventory:$ruleId".hashCode()).coerceAtLeast(1)
    }
}
