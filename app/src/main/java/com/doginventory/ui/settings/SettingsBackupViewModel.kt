package com.doginventory.ui.settings

import android.content.res.Resources
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.R
import com.doginventory.backup.BackupException
import com.doginventory.backup.BackupRestoreCoordinator
import com.doginventory.migration.flutter.FlutterBackupMigrator
import com.doginventory.migration.flutter.FlutterMigrationException
import com.doginventory.migration.flutter.FlutterMigrationResult
import com.doginventory.permission.StoragePermissionCoordinator
import com.doginventory.settings.PreferencesService
import com.doginventory.ui.inventory.formatInventoryDateTime
import kotlinx.coroutines.launch

class SettingsBackupViewModel(
    private val resources: Resources,
    private val coordinator: BackupRestoreCoordinator,
    private val flutterBackupMigrator: FlutterBackupMigrator,
    private val storagePermissionCoordinator: StoragePermissionCoordinator,
    private val preferencesService: PreferencesService
) : ViewModel() {
    var isBusy by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var restoreCompleted by mutableStateOf(false)
        private set

    var migrationResult by mutableStateOf<FlutterMigrationResult?>(null)
        private set

    val requiresLegacyStoragePermission: Boolean
        get() = storagePermissionCoordinator.isRequired()

    val hasLegacyStoragePermission: Boolean
        get() = storagePermissionCoordinator.isGranted()

    val lastAutoBackupTimeText: String
        get() = preferencesService.readLastAutoBackupAt()?.let(::formatInventoryDateTime)
            ?: resources.getString(R.string.settings_backup_no_auto_record)

    fun createBackup(targetUri: Uri) {
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            try {
                val result = coordinator.createBackup(targetUri)
                successMessage = resources.getString(R.string.settings_backup_created, result.fileName)
            } catch (error: BackupException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun restoreBackup(sourceUri: Uri) {
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            restoreCompleted = false
            try {
                coordinator.restoreBackup(sourceUri)
                successMessage = resources.getString(R.string.settings_restore_completed)
                restoreCompleted = true
            } catch (error: BackupException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun migrateFromFlutterBackup(sourceUri: Uri) {
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            migrationResult = null
            try {
                val result = flutterBackupMigrator.migrate(sourceUri)
                migrationResult = result
                successMessage = resources.getString(R.string.settings_flutter_migration_success)
            } catch (error: FlutterMigrationException) {
                errorMessage = error.message
            } catch (error: Exception) {
                errorMessage = error.message ?: resources.getString(R.string.settings_flutter_migration_failed)
            } finally {
                isBusy = false
            }
        }
    }

    fun consumeMessages() {
        errorMessage = null
        successMessage = null
    }

    fun consumeRestoreCompleted() {
        restoreCompleted = false
    }

    fun consumeMigrationResult() {
        migrationResult = null
    }

    fun requestLegacyStoragePermission(onResult: (Boolean) -> Unit) {
        storagePermissionCoordinator.request(onResult)
    }
}
