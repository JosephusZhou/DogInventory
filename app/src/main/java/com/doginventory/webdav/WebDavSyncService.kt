package com.doginventory.webdav

import android.content.Context
import com.doginventory.R
import com.doginventory.backup.BackupArchiveService
import com.doginventory.backup.BackupBundlePaths
import com.doginventory.backup.BackupException
import com.doginventory.backup.BackupManifest
import com.doginventory.backup.RestoreBackupResult
import com.doginventory.data.AppDatabase
import com.doginventory.data.repository.InventoryRepository
import com.doginventory.reminder.InventoryReminderScheduler
import com.doginventory.settings.PreferencesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.time.Instant

class WebDavSyncService(
    private val context: Context,
    private val repository: InventoryRepository,
    private val preferencesService: PreferencesService,
    private val archiveService: BackupArchiveService
) {
    suspend fun testConnection(config: WebDavConfig) = withContext(Dispatchers.IO) {
        requireValidConfig(config)
        try {
            val client = WebDavClient(config, context)
            client.ping()
            client.mkdirRecursive(config.normalizedRemotePath)
        } catch (error: Exception) {
            throw toSyncException(context.getString(R.string.webdav_test_failed), error)
        }
    }

    suspend fun syncToRemote(config: WebDavConfig) = withContext(Dispatchers.IO) {
        requireValidConfig(config)
        val client = WebDavClient(config, context)
        val bundle = archiveService.createSnapshotBundle()
        try {
            client.ping()
            client.mkdirRecursive(config.normalizedRemotePath)
            client.mkdirRecursive("${config.normalizedRemotePath}database/")
            client.mkdirRecursive("${config.normalizedRemotePath}preferences/")

            val localManifest = archiveService.readManifest(bundle.manifestFile)
            val remoteManifest = runCatching {
                BackupManifest.fromJson(JSONObject(client.readText("${config.normalizedRemotePath}manifest.json")))
            }.getOrNull()

            if (remoteManifest == null || shouldUploadDatabase(localManifest, remoteManifest)) {
                client.uploadFile(File(bundle.databaseFile), "${config.normalizedRemotePath}database/database.sqlite")
            }
            if (remoteManifest == null || shouldUploadPreferences(localManifest, remoteManifest)) {
                client.uploadFile(File(bundle.preferencesFile), "${config.normalizedRemotePath}preferences/preferences.json")
            }
            client.writeText("${config.normalizedRemotePath}manifest.json", File(bundle.manifestFile).readText())
        } catch (error: Exception) {
            throw toSyncException(context.getString(R.string.webdav_sync_failed), error)
        } finally {
            archiveService.cleanup(File(bundle.rootDirectory))
        }
    }

    suspend fun restoreFromRemote(config: WebDavConfig): RestoreBackupResult = withContext(Dispatchers.IO) {
        requireValidConfig(config)
        val client = WebDavClient(config, context)
        client.ping()

        val tempRoot = File(context.cacheDir, "webdav_restore_${System.currentTimeMillis()}").apply { mkdirs() }
        val rollbackBundle = archiveService.createSnapshotBundle()
        val rollbackPrefs = preferencesService.snapshotRawPreferences()

        val bundle = BackupBundlePaths(
            rootDirectory = tempRoot.absolutePath,
            databaseFile = File(tempRoot, BackupArchiveService.DATABASE_PATH).absolutePath,
            preferencesFile = File(tempRoot, BackupArchiveService.PREFERENCES_PATH).absolutePath,
            manifestFile = File(tempRoot, BackupArchiveService.MANIFEST_PATH).absolutePath
        )

        try {
            client.downloadFile("${config.normalizedRemotePath}manifest.json", File(bundle.manifestFile))
            client.downloadFile("${config.normalizedRemotePath}database/database.sqlite", File(bundle.databaseFile))
            client.downloadFile("${config.normalizedRemotePath}preferences/preferences.json", File(bundle.preferencesFile))

            ensureBundleIsValid(bundle)
            repository.cancelAllReminders()
            AppDatabase.closeInstance()

            archiveService.restoreDatabaseFile(
                sourcePath = bundle.databaseFile,
                targetPath = AppDatabase.databaseFile(context).absolutePath
            )
            preferencesService.writeBackupPayload(
                archiveService.readPreferencesPayload(bundle.preferencesFile)
            )

            InventoryRepository(
                AppDatabase.getDatabase(context).inventoryDao(),
                InventoryReminderScheduler(context)
            ).resyncAllReminders()

            RestoreBackupResult(requiresRestart = true)
        } catch (error: Exception) {
            AppDatabase.closeInstance()
            archiveService.restoreDatabaseFile(
                sourcePath = rollbackBundle.databaseFile,
                targetPath = AppDatabase.databaseFile(context).absolutePath
            )
            preferencesService.restoreRawPreferences(rollbackPrefs)
            InventoryRepository(
                AppDatabase.getDatabase(context).inventoryDao(),
                InventoryReminderScheduler(context)
            ).resyncAllReminders()
            throw toSyncException(context.getString(R.string.webdav_restore_failed), error)
        } finally {
            archiveService.cleanup(File(tempRoot.absolutePath), File(rollbackBundle.rootDirectory))
        }
    }

    suspend fun checkConsistency(config: WebDavConfig): WebDavConsistencyReport = withContext(Dispatchers.IO) {
        requireValidConfig(config)
        val client = WebDavClient(config, context)
        val localBundle = archiveService.createSnapshotBundle()
        val remoteRoot = File(context.cacheDir, "webdav_consistency_${System.currentTimeMillis()}").apply { mkdirs() }
        val remoteBundle = BackupBundlePaths(
            rootDirectory = remoteRoot.absolutePath,
            databaseFile = File(remoteRoot, BackupArchiveService.DATABASE_PATH).absolutePath,
            preferencesFile = File(remoteRoot, BackupArchiveService.PREFERENCES_PATH).absolutePath,
            manifestFile = File(remoteRoot, BackupArchiveService.MANIFEST_PATH).absolutePath
        )

        try {
            client.ping()
            runCatching { client.downloadFile("${config.normalizedRemotePath}manifest.json", File(remoteBundle.manifestFile)) }
                .getOrElse {
                    return@withContext WebDavConsistencyReport(
                        isConsistent = false,
                        summary = context.getString(R.string.webdav_remote_no_backup_summary),
                        repairSuggestion = context.getString(R.string.webdav_remote_no_backup_repair)
                    )
                }
            client.downloadFile("${config.normalizedRemotePath}database/database.sqlite", File(remoteBundle.databaseFile))
            client.downloadFile("${config.normalizedRemotePath}preferences/preferences.json", File(remoteBundle.preferencesFile))
            ensureBundleIsValid(remoteBundle)

            val remoteManifest = archiveService.readManifest(remoteBundle.manifestFile)
            val expectedEntries = BackupArchiveService.INCLUDED_ENTRIES
            if (remoteManifest.includedEntries != expectedEntries) {
                return@withContext WebDavConsistencyReport(
                    isConsistent = false,
                    summary = context.getString(R.string.webdav_remote_structure_mismatch_summary),
                    repairSuggestion = context.getString(R.string.webdav_remote_structure_mismatch_repair)
                )
            }

            val databaseMatches = sha256(File(localBundle.databaseFile)) == sha256(File(remoteBundle.databaseFile))
            val preferencesMatches = sha256(File(localBundle.preferencesFile)) == sha256(File(remoteBundle.preferencesFile))

            when {
                databaseMatches && preferencesMatches -> WebDavConsistencyReport(
                    isConsistent = true,
                    summary = context.getString(R.string.webdav_consistent_summary),
                    repairSuggestion = null
                )
                databaseMatches -> WebDavConsistencyReport(
                    isConsistent = false,
                    summary = context.getString(R.string.webdav_preferences_mismatch_summary),
                    repairSuggestion = context.getString(R.string.webdav_preferences_mismatch_repair)
                )
                preferencesMatches -> WebDavConsistencyReport(
                    isConsistent = false,
                    summary = context.getString(R.string.webdav_database_mismatch_summary),
                    repairSuggestion = context.getString(R.string.webdav_database_mismatch_repair)
                )
                else -> WebDavConsistencyReport(
                    isConsistent = false,
                    summary = context.getString(R.string.webdav_all_mismatch_summary),
                    repairSuggestion = context.getString(R.string.webdav_all_mismatch_repair)
                )
            }
        } catch (error: Exception) {
            throw toSyncException(context.getString(R.string.webdav_consistency_failed), error)
        } finally {
            archiveService.cleanup(File(localBundle.rootDirectory), File(remoteBundle.rootDirectory))
        }
    }

    private fun ensureBundleIsValid(bundle: BackupBundlePaths) {
        if (!File(bundle.manifestFile).exists()) {
            throw WebDavException(context.getString(R.string.webdav_remote_missing_manifest))
        }
        if (!File(bundle.databaseFile).exists()) {
            throw WebDavException(context.getString(R.string.webdav_remote_missing_database))
        }
        if (!File(bundle.preferencesFile).exists()) {
            throw WebDavException(context.getString(R.string.webdav_remote_missing_preferences))
        }

        val manifest = archiveService.readManifest(bundle.manifestFile)
        if (manifest.backupFormatVersion > BackupArchiveService.CURRENT_BACKUP_FORMAT_VERSION) {
            throw WebDavException(context.getString(R.string.webdav_restore_unsupported_version))
        }
        if (manifest.schemaVersion > AppDatabase.DATABASE_VERSION) {
            throw WebDavException(context.getString(R.string.webdav_restore_schema_too_new))
        }
        archiveService.readPreferencesPayload(bundle.preferencesFile)
    }

    private fun shouldUploadDatabase(local: BackupManifest, remote: BackupManifest): Boolean {
        return parseCreatedAt(local.createdAt).isAfter(parseCreatedAt(remote.createdAt)) || local.schemaVersion != remote.schemaVersion
    }

    private fun shouldUploadPreferences(local: BackupManifest, remote: BackupManifest): Boolean {
        return parseCreatedAt(local.createdAt).isAfter(parseCreatedAt(remote.createdAt))
    }

    private fun parseCreatedAt(value: String): Instant {
        return runCatching { Instant.parse(value) }.getOrElse { Instant.EPOCH }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var read = input.read(buffer)
            while (read >= 0) {
                if (read > 0) {
                    digest.update(buffer, 0, read)
                }
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun requireValidConfig(config: WebDavConfig) {
        if (!config.isValid) {
            throw WebDavException(context.getString(R.string.webdav_config_invalid))
        }
    }

    private fun toSyncException(prefix: String, error: Exception): WebDavException {
        return when (error) {
            is WebDavException -> error
            is BackupException -> WebDavException(error.message ?: prefix)
            else -> WebDavException(
                context.getString(
                    R.string.webdav_error_with_reason,
                    prefix,
                    error.message ?: error.javaClass.simpleName
                )
            )
        }
    }
}

data class WebDavConsistencyReport(
    val isConsistent: Boolean,
    val summary: String,
    val repairSuggestion: String?
) {
    fun formatText(context: Context): String {
        return if (repairSuggestion.isNullOrBlank()) {
            summary
        } else {
            context.getString(
                R.string.settings_webdav_consistency_format,
                summary,
                context.getString(R.string.settings_webdav_consistency_repair_prefix, repairSuggestion)
            )
        }
    }
}
