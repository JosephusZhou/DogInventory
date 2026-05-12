package com.doginventory.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.doginventory.R
import java.io.File

class BackupStorageBridge(private val context: Context) {
    fun exportBackupFile(sourceFile: File, targetUri: Uri): BackupExportResult {
        context.contentResolver.openOutputStream(targetUri)?.use { output ->
            sourceFile.inputStream().use { input -> input.copyTo(output) }
        } ?: throw BackupException.ExportFailed(context.getString(R.string.backup_write_failed))

        return BackupExportResult(
            fileName = sourceFile.name,
            locationLabel = targetUri.lastPathSegment ?: context.getString(R.string.backup_selected_location)
        )
    }

    fun exportBackupToPublicDownloads(sourceFile: File): BackupExportResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            exportBackupToMediaStore(sourceFile).also {
                pruneOldAutoBackupsMediaStore(keepCount = AUTO_BACKUP_KEEP_COUNT)
            }
        } else {
            exportBackupToLegacyDownloads(sourceFile).also {
                pruneOldAutoBackupsLegacy(keepCount = AUTO_BACKUP_KEEP_COUNT)
            }
        }
    }

    fun copyBackupToCache(sourceUri: Uri): File {
        val target = File(context.cacheDir, "restore_${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: throw BackupException.RestoreFailed(context.getString(R.string.backup_read_failed))
        return target
    }

    private fun exportBackupToMediaStore(sourceFile: File): BackupExportResult {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val targetUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw BackupException.ExportFailed(context.getString(R.string.backup_create_public_download_failed))

        try {
            resolver.openOutputStream(targetUri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            } ?: throw BackupException.ExportFailed(context.getString(R.string.backup_write_public_download_failed))

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(targetUri, contentValues, null, null)

            return BackupExportResult(
                fileName = sourceFile.name,
                locationLabel = Environment.DIRECTORY_DOWNLOADS
            )
        } catch (error: Exception) {
            resolver.delete(targetUri, null, null)
            throw error
        }
    }

    private fun exportBackupToLegacyDownloads(sourceFile: File): BackupExportResult {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if ((!downloadsDirectory.exists() && !downloadsDirectory.mkdirs()) || !downloadsDirectory.canWrite()) {
            throw BackupException.ExportFailed(context.getString(R.string.backup_write_system_download_failed))
        }
        val targetFile = File(downloadsDirectory, sourceFile.name)
        sourceFile.copyTo(targetFile, overwrite = true)
        return BackupExportResult(
            fileName = sourceFile.name,
            locationLabel = downloadsDirectory.absolutePath
        )
    }

    private fun pruneOldAutoBackupsMediaStore(keepCount: Int) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.DATE_ADDED
        )
        val selection = "${MediaStore.Downloads.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("dog_inventory_backup_%")
        val sortOrder = "${MediaStore.Downloads.DATE_ADDED} DESC"

        resolver.query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            var index = 0
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
            while (cursor.moveToNext()) {
                index += 1
                if (index <= keepCount) continue
                val id = cursor.getLong(idColumn)
                val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon().appendPath(id.toString()).build()
                resolver.delete(uri, null, null)
            }
        }
    }

    private fun pruneOldAutoBackupsLegacy(keepCount: Int) {
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val backups = downloadsDirectory.listFiles { file ->
            file.isFile && file.name.startsWith("dog_inventory_backup_") && file.name.endsWith(".zip")
        }?.sortedByDescending { it.lastModified() }.orEmpty()

        backups.drop(keepCount).forEach { it.delete() }
    }

    companion object {
        private const val AUTO_BACKUP_KEEP_COUNT = 3
    }
}
