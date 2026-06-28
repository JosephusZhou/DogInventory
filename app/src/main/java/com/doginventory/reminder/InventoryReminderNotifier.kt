package com.doginventory.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.doginventory.MainActivity
import com.doginventory.R

/**
 * 存货提醒通知的统一构建/投递入口。
 * 由实时闹钟（[InventoryReminderReceiver]）与兜底补发（[ReminderBackstopWorker]）共用，
 * 避免渠道、文案、权限检查逻辑重复。
 */
object InventoryReminderNotifier {
    const val CHANNEL_ID = "inventory_reminder"

    /** 构建提醒正文，供调度与补发两条链路保持一致文案。 */
    fun body(context: Context, kind: String, itemName: String, categoryName: String?): String {
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

    /** 投递一条提醒通知；权限缺失或参数无效时静默返回 false，不抛异常。 */
    fun post(context: Context, notificationId: Int, body: String): Boolean {
        if (notificationId == 0 || body.isBlank()) return false
        ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.getString(R.string.notification_inventory_reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_inventory_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_inventory_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
