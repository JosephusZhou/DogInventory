package com.doginventory

import android.app.Application
import com.doginventory.backup.BackupArchiveService
import com.doginventory.backup.BackupRestoreCoordinator
import com.doginventory.backup.BackupStorageBridge
import com.doginventory.data.AppDatabase
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.settings.PreferencesService
import com.doginventory.webdav.WebDavAutoSyncScheduler
import com.doginventory.webdav.WebDavSyncService

class DogInventoryApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.getDatabase(this) }

    val reminderScheduler: InventoryReminderScheduler by lazy {
        InventoryReminderScheduler(applicationContext)
    }

    val preferencesService: PreferencesService by lazy {
        PreferencesService(applicationContext)
    }

    val backupArchiveService: BackupArchiveService by lazy {
        BackupArchiveService(applicationContext, preferencesService)
    }

    val repository: InventoryRepository by lazy {
        InventoryRepository(database.inventoryDao(), reminderScheduler).also { repository ->
            // Keep the trigger assignment lazy-safe: the scheduler must not eagerly
            // resolve WebDavSyncService here, otherwise repository -> scheduler ->
            // syncService -> repository becomes a startup cycle.
            repository.webDavAutoSyncTrigger = webDavAutoSyncScheduler
        }
    }

    val webDavSyncService: WebDavSyncService by lazy {
        WebDavSyncService(
            context = applicationContext,
            repository = repository,
            preferencesService = preferencesService,
            archiveService = backupArchiveService
        )
    }

    val webDavAutoSyncScheduler: WebDavAutoSyncScheduler by lazy {
        // Pass a provider instead of the service instance to avoid a lazy init cycle
        // during application startup. WebDavSyncService is only needed when a sync
        // is actually executed.
        WebDavAutoSyncScheduler(preferencesService) { webDavSyncService }
    }

    val backupRestoreCoordinator: BackupRestoreCoordinator by lazy {
        BackupRestoreCoordinator(
            context = applicationContext,
            repository = repository,
            preferencesService = preferencesService,
            archiveService = backupArchiveService,
            storageBridge = BackupStorageBridge(applicationContext)
        )
    }

    override fun onCreate() {
        super.onCreate()
        preferencesService.webDavAutoSyncTrigger = webDavAutoSyncScheduler
    }
}
