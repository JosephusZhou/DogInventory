package com.doginventory.backup

import android.content.Context
import android.net.Uri
import com.doginventory.R
import com.doginventory.data.AppDatabase
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.settings.PreferencesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class BackupRestoreCoordinator(
    private val context: Context,
    private val repository: InventoryRepository,
    private val preferencesService: PreferencesService,
    private val archiveService: BackupArchiveService,
    private val storageBridge: BackupStorageBridge
) {
    suspend fun createBackup(targetUri: Uri): BackupExportResult = withContext(Dispatchers.IO) {
        val snapshot = archiveService.createSnapshot()
        val archiveFile = archiveService.createBackupArchive(snapshot)
        try {
            storageBridge.exportBackupFile(archiveFile, targetUri)
        } finally {
            archiveService.cleanup(File(snapshot.databaseFile).parentFile, archiveFile.parentFile)
        }
    }

    suspend fun restoreBackup(sourceUri: Uri): RestoreBackupResult = withContext(Dispatchers.IO) {
        val importedZip = storageBridge.copyBackupToCache(sourceUri)
        val extractedBundle = archiveService.extractBackupArchive(importedZip)
        val rollbackSnapshot = archiveService.createSnapshot()
        val rollbackPrefs = preferencesService.snapshotRawPreferences()

        try {
            ensureBundleIsValid(extractedBundle)
            repository.cancelAllReminders()
            AppDatabase.closeInstance()

            archiveService.restoreDatabaseFile(
                sourcePath = extractedBundle.databaseFile,
                targetPath = AppDatabase.databaseFile(context).absolutePath
            )
            preferencesService.writeBackupPayload(
                archiveService.readPreferencesPayload(extractedBundle.preferencesFile)
            )

            val restoredDatabase = AppDatabase.getDatabase(context)
            InventoryRepository(
                restoredDatabase.inventoryDao(),
                InventoryReminderScheduler(context)
            ).resyncAllReminders()

            RestoreBackupResult(requiresRestart = true)
        } catch (error: Exception) {
            AppDatabase.closeInstance()
            archiveService.restoreDatabaseFile(
                sourcePath = rollbackSnapshot.databaseFile,
                targetPath = AppDatabase.databaseFile(context).absolutePath
            )
            preferencesService.restoreRawPreferences(rollbackPrefs)
            val rollbackDatabase = AppDatabase.getDatabase(context)
            InventoryRepository(
                rollbackDatabase.inventoryDao(),
                InventoryReminderScheduler(context)
            ).resyncAllReminders()

            when (error) {
                is BackupException -> throw error
                else -> throw BackupException.RestoreFailed(context.getString(R.string.backup_restore_failed_retry))
            }
        } finally {
            archiveService.cleanup(importedZip, File(extractedBundle.rootDirectory), File(rollbackSnapshot.databaseFile).parentFile)
        }
    }

    suspend fun createAutomaticBackupToPublicDownloads(): BackupExportResult = withContext(Dispatchers.IO) {
        val snapshot = archiveService.createSnapshot()
        val archiveFile = archiveService.createBackupArchive(snapshot)
        try {
            storageBridge.exportBackupToPublicDownloads(archiveFile)
        } finally {
            archiveService.cleanup(File(snapshot.databaseFile).parentFile, archiveFile.parentFile)
        }
    }

    private fun ensureBundleIsValid(bundle: BackupBundlePaths) {
        if (!File(bundle.manifestFile).exists()) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_missing_manifest))
        }
        if (!File(bundle.databaseFile).exists()) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_missing_database))
        }
        if (!File(bundle.preferencesFile).exists()) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_missing_preferences))
        }

        val manifest = archiveService.readManifest(bundle.manifestFile)
        if (manifest.backupFormatVersion > BackupArchiveService.CURRENT_BACKUP_FORMAT_VERSION) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_unsupported_version))
        }
        if (manifest.schemaVersion > AppDatabase.DATABASE_VERSION) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_schema_too_new))
        }

        archiveService.readPreferencesPayload(bundle.preferencesFile)
    }

    companion object {
        const val AUTO_BACKUP_INTERVAL_MILLIS: Long = 3L * 24L * 60L * 60L * 1000L
    }
}
