package com.doginventory.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doginventory.DogInventoryApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InventoryReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, 0)
        val ruleId = intent.getStringExtra(EXTRA_RULE_ID).orEmpty()
        if (body.isBlank() || notificationId == 0) return

        val posted = InventoryReminderNotifier.post(context, notificationId, body)

        // 仅在通知确实投递成功后标记触发，供兜底补发 Worker 去重；
        // 若因权限缺失未投递，则不标记，待权限恢复后由 Worker 补发。
        val app = context.applicationContext as? DogInventoryApp
        if (!posted || ruleId.isBlank() || app == null) return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.updateRuleTriggeredAt(ruleId, System.currentTimeMillis())
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_BODY = "body"
        const val EXTRA_RULE_ID = "rule_id"
    }
}
