package com.doginventory.permission

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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

    fun requestNotificationPermission(onGranted: (() -> Unit)? = null) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            onGranted?.invoke()
            return
        }
        afterNotificationGranted = onGranted
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
