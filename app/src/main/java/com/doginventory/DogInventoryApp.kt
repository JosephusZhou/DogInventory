package com.doginventory

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.doginventory.backup.BackupArchiveService
import com.doginventory.backup.BackupRestoreCoordinator
import com.doginventory.backup.BackupStorageBridge
import com.doginventory.data.AppDatabase
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.migration.flutter.FlutterBackupMigrator
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.reminder.ReminderBackstopWorker
import com.doginventory.settings.PreferencesService
import com.doginventory.share.ShareApiClient
import com.doginventory.share.ShareService
import com.doginventory.webdav.WebDavAutoSyncScheduler
import com.doginventory.webdav.WebDavSyncService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    val flutterBackupMigrator: FlutterBackupMigrator by lazy {
        FlutterBackupMigrator(
            context = applicationContext,
            database = database,
            repository = repository
        )
    }

    val shareApiClient: ShareApiClient by lazy {
        ShareApiClient(baseUrl = BuildConfig.SHARE_BASE_URL)
    }

    val shareService: ShareService by lazy {
        ShareService(applicationContext, shareApiClient)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        preferencesService.webDavAutoSyncTrigger = webDavAutoSyncScheduler
        // 进程冷启动即全量重调度：覆盖进程被杀重启、被 OEM 强杀后用户重开 App 的恢复场景。
        appScope.launch { repository.resyncAllReminders() }
        scheduleReminderBackstop()
    }

    private fun scheduleReminderBackstop() {
        val request = PeriodicWorkRequestBuilder<ReminderBackstopWorker>(6, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ReminderBackstopWorker.UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
