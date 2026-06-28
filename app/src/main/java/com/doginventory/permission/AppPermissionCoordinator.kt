package com.doginventory.permission

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.doginventory.reminder.InventoryReminderScheduler

class AppPermissionCoordinator(
    private val activity: ComponentActivity,
    private val reminderScheduler: InventoryReminderScheduler,
    caller: ActivityResultCaller
) {
    private var afterNotificationGranted: (() -> Unit)? = null
    private var pendingAppReminderFlowAfterResume = false

    private val notificationPermissionLauncher: ActivityResultLauncher<String> = caller.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingAppReminderFlowAfterResume = true
            continuePendingAppReminderFlowIfPossible()
        } else {
            afterNotificationGranted = null
            pendingAppReminderFlowAfterResume = false
        }
    }

    fun isNotificationPermissionGranted(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    fun canScheduleExactAlarms(): Boolean = reminderScheduler.canScheduleExactAlarms()

    /**
     * 是否已加入电池优化白名单。加白后可显著降低 Doze / 应用待机下被系统回收的概率，
     * 是 force-stop 之外提升提醒触达率的关键本地手段。
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = activity.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(activity.packageName)
    }

    @SuppressLint("BatteryLife")
    fun requestIgnoreBatteryOptimizations() {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.fromParts("package", activity.packageName, null)
        }
        // 个别 ROM 不响应直接申请弹窗时，回退到系统电池优化列表页让用户手动加白。
        runCatching { activity.startActivity(request) }.onFailure {
            runCatching {
                activity.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
    }

    fun requestNotificationPermission(onGranted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted?.invoke()
            return
        }
        afterNotificationGranted = onGranted
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun openAppPermissionSettings() {
        activity.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        )
    }

    fun openExactAlarmSettings() {
        activity.startActivity(reminderScheduler.exactAlarmSettingsIntent())
    }

    fun onHostResumed() {
        continuePendingAppReminderFlowIfPossible()
    }

    private fun continuePendingAppReminderFlowIfPossible() {
        if (!pendingAppReminderFlowAfterResume) return
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        pendingAppReminderFlowAfterResume = false
        val continuation = afterNotificationGranted
        afterNotificationGranted = null
        activity.window.decorView.post {
            continuation?.invoke()
        }
    }

    fun requestAppReminderPermissions() {
        if (!isNotificationPermissionGranted()) {
            requestNotificationPermission {
                if (!canScheduleExactAlarms()) {
                    openExactAlarmSettings()
                }
            }
            return
        }

        if (!canScheduleExactAlarms()) {
            openExactAlarmSettings()
        }
    }
}
