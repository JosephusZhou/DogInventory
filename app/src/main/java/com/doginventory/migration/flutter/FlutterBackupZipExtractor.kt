// TEMPORARY: dog_remind -> DogInventory migration helper.
// Delete the entire com.doginventory.migration.flutter package once all users have migrated.
package com.doginventory.migration.flutter

import android.content.Context
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

internal class FlutterBackupZipExtractor(private val context: Context) {
    fun extractDatabase(zipFile: File): File {
        val extractDir = File(context.cacheDir, "flutter_migration_${System.currentTimeMillis()}")
        extractDir.mkdirs()

        ZipFile(zipFile).use { archive ->
            val entry = archive.getEntry("database/database.sqlite")
                ?: throw FlutterMigrationException("备份文件中未找到 database/database.sqlite")

            val target = File(extractDir, "database.sqlite")
            val normalizedTarget = target.canonicalPath
            val normalizedRoot = extractDir.canonicalPath + File.separator
            if (!normalizedTarget.startsWith(normalizedRoot)) {
                throw FlutterMigrationException("Zip entry path traversal detected")
            }

            BufferedInputStream(archive.getInputStream(entry)).use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            }
            return target
        }
    }

    fun cleanup(extractDir: File) {
        extractDir.parentFile?.deleteRecursively()
    }
}
