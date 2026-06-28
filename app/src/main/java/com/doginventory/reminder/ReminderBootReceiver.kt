package com.doginventory.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.doginventory.DogInventoryApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 设备重启 / 应用更新后，AlarmManager 闹钟会被系统清空。
 * 该接收者在开机或包替换后全量重调度提醒，修复重启丢失。
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            ACTION_QUICKBOOT_POWERON,
            ACTION_HTC_QUICKBOOT_POWERON -> Unit
            else -> return
        }
        val app = context.applicationContext as? DogInventoryApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.resyncAllReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"
        private const val ACTION_HTC_QUICKBOOT_POWERON = "com.htc.intent.action.QUICKBOOT_POWERON"
    }
}
