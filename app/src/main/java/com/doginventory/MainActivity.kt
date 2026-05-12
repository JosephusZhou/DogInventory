package com.doginventory

import android.content.res.Configuration
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.doginventory.backup.BackupRestoreCoordinator
import com.doginventory.data.entity.InventoryCategoryEntity
import com.doginventory.permission.AppPermissionCoordinator
import com.doginventory.permission.StoragePermissionCoordinator
import com.doginventory.settings.PreferencesService
import com.doginventory.ui.MainScreen
import com.doginventory.ui.theme.AppThemeMode
import com.doginventory.ui.theme.DogInventoryTheme
import com.doginventory.ui.theme.InventoryCategoryDefaults
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var themeMode by mutableStateOf(AppThemeMode.System)
    private var isNotificationPermissionGranted by mutableStateOf(false)
    private var canScheduleExactAlarms by mutableStateOf(false)
    private lateinit var permissionCoordinator: AppPermissionCoordinator
    private lateinit var storagePermissionCoordinator: StoragePermissionCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val app = application as DogInventoryApp

        val preferences = getSharedPreferences(PreferencesService.THEME_PREFS_NAME, MODE_PRIVATE)
        val systemDarkTheme = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val storedThemeMode = preferences.getString(PreferencesService.KEY_THEME_MODE, null)
        themeMode = if (storedThemeMode != null) {
            AppThemeMode.fromPreferenceValue(storedThemeMode)
        } else {
            if (preferences.getBoolean(PreferencesService.KEY_DARK_THEME_ENABLED, systemDarkTheme)) AppThemeMode.Dark else AppThemeMode.Light
        }
        
        permissionCoordinator = AppPermissionCoordinator(
            activity = this,
            reminderScheduler = app.reminderScheduler,
            caller = this
        )
        storagePermissionCoordinator = StoragePermissionCoordinator(
            activity = this,
            caller = this
        )
        refreshPermissionStates()
        val repository = app.repository
        val preferencesService = app.preferencesService

        setContent {
            val isDarkThemeEnabled = when (themeMode) {
                AppThemeMode.System -> systemDarkTheme
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
            }
            DogInventoryTheme(darkTheme = isDarkThemeEnabled) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        repository = repository,
                        reminderScheduler = app.reminderScheduler,
                        storagePermissionCoordinator = storagePermissionCoordinator,
                        themeMode = themeMode,
                        isNotificationPermissionGranted = isNotificationPermissionGranted,
                        canScheduleExactAlarms = canScheduleExactAlarms,
                        onRequestAppReminderPermissions = permissionCoordinator::requestAppReminderPermissions,
                        onOpenExactAlarmSettings = permissionCoordinator::openExactAlarmSettings,
                        onThemeModeChange = { mode ->
                            themeMode = mode
                            preferencesService.writeThemeMode(mode.preferenceValue)
                        },
                        onRestartApp = {
                            startActivity(
                                Intent(this, SplashActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                            )
                            finish()
                        }
                    )
                }
            }
        }

        lifecycleScope.launch {
            val currentCategories = repository.allCategories.first()
            if (currentCategories.isEmpty()) {
                val presets = listOf(
                    InventoryCategoryEntity("preset-food", getString(R.string.preset_category_food), InventoryCategoryDefaults.FOOD_COLOR_HEX, "🥐", 0, true, false, System.currentTimeMillis()),
                    InventoryCategoryEntity("preset-drink", getString(R.string.preset_category_drink), InventoryCategoryDefaults.DRINK_COLOR_HEX, "🥤", 1, true, false, System.currentTimeMillis()),
                    InventoryCategoryEntity("preset-medicine", getString(R.string.preset_category_medicine), InventoryCategoryDefaults.MEDICINE_COLOR_HEX, "💊", 2, true, false, System.currentTimeMillis()),
                    InventoryCategoryEntity("preset-household", getString(R.string.preset_category_household), InventoryCategoryDefaults.HOUSEHOLD_COLOR_HEX, "🧻", 3, true, false, System.currentTimeMillis()),
                    InventoryCategoryEntity("preset-coupon", getString(R.string.preset_category_coupon), InventoryCategoryDefaults.COUPON_COLOR_HEX, "🎫", 4, true, false, System.currentTimeMillis()),
                    InventoryCategoryEntity("preset-other", getString(R.string.preset_category_other), InventoryCategoryDefaults.OTHER_COLOR_HEX, "📦", 5, true, false, System.currentTimeMillis())
                )
                repository.insertCategories(presets, syncReason = "inventory_default_categories_seeded")
            }
        }

        lifecycleScope.launch {
            if (storagePermissionCoordinator.isRequired() && !storagePermissionCoordinator.isGranted()) {
                return@launch
            }
            if (!preferencesService.shouldRunAutoBackup(intervalMillis = BackupRestoreCoordinator.AUTO_BACKUP_INTERVAL_MILLIS)) {
                return@launch
            }

            runCatching {
                app.backupRestoreCoordinator.createAutomaticBackupToPublicDownloads()
            }.onSuccess {
                preferencesService.writeLastAutoBackupAt(System.currentTimeMillis())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::permissionCoordinator.isInitialized) {
            refreshPermissionStates()
            permissionCoordinator.onHostResumed()
            refreshPermissionStates()
        }
    }

    private fun refreshPermissionStates() {
        isNotificationPermissionGranted = permissionCoordinator.isNotificationPermissionGranted()
        canScheduleExactAlarms = permissionCoordinator.canScheduleExactAlarms()
    }

}
