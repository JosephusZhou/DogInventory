package com.doginventory.ui.inventory

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.doginventory.R
import com.doginventory.ui.theme.InventoryCategoryDefaults
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val INVENTORY_SOON_WINDOW_MILLIS = 30L * 24L * 60L * 60L * 1000L
private const val DATE_PATTERN = "yyyy-MM-dd"
private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm"

fun isInventoryExpired(expireAt: Long?, now: Long = System.currentTimeMillis()): Boolean {
    if (expireAt == null) return false
    return expireAt <= now
}

fun isInventorySoon(expireAt: Long?, now: Long = System.currentTimeMillis()): Boolean {
    if (expireAt == null) return false
    val soonThreshold = now + INVENTORY_SOON_WINDOW_MILLIS
    return expireAt in (now + 1)..soonThreshold
}

fun formatInventoryDate(millis: Long): String {
    return SimpleDateFormat(DATE_PATTERN, Locale.getDefault()).format(Date(millis))
}

fun formatInventoryDateTime(millis: Long): String {
    return SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault()).format(Date(millis))
}

fun formatInventoryExpireText(context: Context, expireAt: Long?): String {
    if (expireAt == null) return context.getString(R.string.inventory_never_expires)
    return context.getString(R.string.inventory_expire_time, formatInventoryDate(expireAt))
}

fun parseInventoryCategoryColor(hex: String?): Color {
    val colorHex = hex ?: InventoryCategoryDefaults.FALLBACK_COLOR_HEX
    return try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (_: Exception) {
        Color(android.graphics.Color.parseColor(InventoryCategoryDefaults.FALLBACK_COLOR_HEX))
    }
}
