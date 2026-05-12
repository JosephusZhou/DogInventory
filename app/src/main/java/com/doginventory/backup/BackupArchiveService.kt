package com.doginventory.backup

import android.content.Context
import com.doginventory.R
import com.doginventory.data.AppDatabase
import com.doginventory.settings.BackupPreferencesPayload
import com.doginventory.settings.PreferencesService
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupArchiveService(
    private val context: Context,
    private val preferencesService: PreferencesService
) {
    fun createBackupArchive(snapshot: BackupSnapshot): File {
        val bundle = createBundleDirectory(snapshot)
        val archiveFile = File(bundle.rootDirectory, buildBackupFileName())
        ZipOutputStream(FileOutputStream(archiveFile)).use { output ->
            addFileToZip(output, File(bundle.manifestFile), MANIFEST_PATH)
            addFileToZip(output, File(bundle.databaseFile), DATABASE_PATH)
            addFileToZip(output, File(bundle.preferencesFile), PREFERENCES_PATH)
        }
        return archiveFile
    }

    fun createSnapshot(): BackupSnapshot {
        val tempDatabase = File(createTempDirectory("backup_db_"), DATABASE_FILE_NAME)
        AppDatabase.exportConsistentSnapshot(context, tempDatabase)
        return BackupSnapshot(
            databaseFile = tempDatabase.absolutePath,
            preferences = preferencesService.readBackupPayload()
        )
    }

    fun createSnapshotBundle(): BackupBundlePaths {
        return createBundleDirectory(createSnapshot())
    }

    fun extractBackupArchive(zipFile: File): BackupBundlePaths {
        val extractionRoot = createTempDirectory("restore_bundle_")
        ZipInputStream(FileInputStream(zipFile)).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val target = File(extractionRoot, entry.name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    FileOutputStream(target).use { output -> input.copyTo(output) }
                }
                input.closeEntry()
                entry = input.nextEntry
            }
        }
        return BackupBundlePaths(
            rootDirectory = extractionRoot.absolutePath,
            databaseFile = File(extractionRoot, DATABASE_PATH).absolutePath,
            preferencesFile = File(extractionRoot, PREFERENCES_PATH).absolutePath,
            manifestFile = File(extractionRoot, MANIFEST_PATH).absolutePath
        )
    }

    fun readManifest(path: String): BackupManifest {
        return BackupManifest.fromJson(JSONObject(File(path).readText()))
    }

    fun readPreferencesPayload(path: String): BackupPreferencesPayload {
        return BackupPreferencesPayload.fromJson(JSONObject(File(path).readText()))
    }

    fun restoreDatabaseFile(sourcePath: String, targetPath: String) {
        val source = File(sourcePath)
        val target = File(targetPath)
        if (!source.exists()) {
            throw BackupException.ValidationFailed(context.getString(R.string.backup_missing_database))
        }
        target.parentFile?.mkdirs()
        if (target.exists()) {
            target.delete()
        }
        source.copyTo(target, overwrite = true)
    }

    fun cleanup(vararg files: File?) {
        files.filterNotNull().forEach { file ->
            runCatching {
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            }
        }
    }

    private fun createBundleDirectory(snapshot: BackupSnapshot): BackupBundlePaths {
        val root = createTempDirectory("backup_bundle_")
        val databaseFile = File(root, DATABASE_PATH)
        val preferencesFile = File(root, PREFERENCES_PATH)
        val manifestFile = File(root, MANIFEST_PATH)

        databaseFile.parentFile?.mkdirs()
        preferencesFile.parentFile?.mkdirs()
        snapshot.preferences.toJson().toString(2).also(preferencesFile::writeText)
        File(snapshot.databaseFile).copyTo(databaseFile, overwrite = true)

        BackupManifest(
            backupFormatVersion = CURRENT_BACKUP_FORMAT_VERSION,
            createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            appVersion = context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty(),
            schemaVersion = AppDatabase.DATABASE_VERSION,
            platform = "android",
            includedEntries = INCLUDED_ENTRIES
        ).toJson().toString(2).also(manifestFile::writeText)

        return BackupBundlePaths(
            rootDirectory = root.absolutePath,
            databaseFile = databaseFile.absolutePath,
            preferencesFile = preferencesFile.absolutePath,
            manifestFile = manifestFile.absolutePath
        )
    }

    private fun addFileToZip(output: ZipOutputStream, file: File, entryName: String) {
        output.putNextEntry(ZipEntry(entryName))
        FileInputStream(file).use { input -> input.copyTo(output) }
        output.closeEntry()
    }

    private fun createTempDirectory(prefix: String): File {
        return File(context.cacheDir, "$prefix${System.currentTimeMillis()}").apply { mkdirs() }
    }

    private fun buildBackupFileName(): String {
        val instant = Instant.now().toString().replace(":", "-")
        return "dog_inventory_backup_$instant.zip"
    }

    companion object {
        const val CURRENT_BACKUP_FORMAT_VERSION = 1
        const val MANIFEST_PATH = "manifest.json"
        const val DATABASE_PATH = "database/database.sqlite"
        const val PREFERENCES_PATH = "preferences/preferences.json"
        const val DATABASE_FILE_NAME = "database.sqlite"
        val INCLUDED_ENTRIES = listOf(MANIFEST_PATH, DATABASE_PATH, PREFERENCES_PATH)
    }
}
