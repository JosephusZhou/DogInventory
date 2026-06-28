package com.doginventory.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.doginventory.DogInventoryApp
import com.doginventory.ui.inventory.InventoryReminderDraft

/**
 * 兜底补发 Worker：周期性扫描所有提醒规则，补发在 App 被杀 / 闹钟被丢弃期间漏掉的提醒，
 * 并重新注册仍在未来的闹钟。WorkManager 自身在重启后会重排其 Job，构成与
 * [ReminderBootReceiver] 互补的第二道防线。
 */
class ReminderBackstopWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? DogInventoryApp ?: return Result.success()
        val repository = app.repository
        val now = System.currentTimeMillis()

        val items = repository.getActiveItemsSnapshot().associateBy { it.id }
        val categories = repository.getCategoriesOnce().associateBy { it.id }
        val rules = repository.getAllRulesSnapshot()

        rules.forEach { rule ->
            if (!rule.enabled) return@forEach
            val item = items[rule.itemId] ?: return@forEach
            val draft = InventoryReminderDraft.fromEntity(rule)
            val scheduledAt = draft.rawScheduledAtForItem(item) ?: return@forEach
            val missed = scheduledAt <= now &&
                scheduledAt >= now - BACKFILL_WINDOW_MS &&
                (rule.lastTriggeredAt == null || rule.lastTriggeredAt < scheduledAt)
            if (missed) {
                val body = InventoryReminderNotifier.body(
                    applicationContext,
                    draft.kind,
                    item.name,
                    categories[item.categoryId]?.name
                )
                val posted = InventoryReminderNotifier.post(
                    applicationContext,
                    InventoryReminderScheduler.notificationIdForRule(rule.id),
                    body
                )
                // 仅在投递成功后标记，未投递（如权限缺失）时留待下次运行补发。
                if (posted) repository.updateRuleTriggeredAt(rule.id, now)
            }
        }

        // 重新注册仍在未来的闹钟，即便部分曾被系统丢弃。
        repository.resyncAllReminders()
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "reminder_backstop"
        // 只补发最近 14 天内漏掉的提醒，避免重装/长期未启动后刷出大量陈旧通知。
        private const val BACKFILL_WINDOW_MS = 14L * 24 * 60 * 60 * 1000
    }
}
