package com.doginventory.backup

import com.doginventory.settings.BackupPreferencesPayload
import org.json.JSONArray
import org.json.JSONObject

data class BackupManifest(
    val backupFormatVersion: Int,
    val createdAt: String,
    val appVersion: String,
    val schemaVersion: Int,
    val platform: String,
    val includedEntries: List<String>
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("backupFormatVersion", backupFormatVersion)
        put("createdAt", createdAt)
        put("appVersion", appVersion)
        put("schemaVersion", schemaVersion)
        put("platform", platform)
        put("includedEntries", JSONArray(includedEntries))
    }

    companion object {
        fun fromJson(json: JSONObject): BackupManifest {
            val entriesJson = json.optJSONArray("includedEntries") ?: JSONArray()
            val entries = buildList {
                for (index in 0 until entriesJson.length()) {
                    add(entriesJson.optString(index))
                }
            }
            return BackupManifest(
                backupFormatVersion = json.optInt("backupFormatVersion", 0),
                createdAt = json.optString("createdAt", ""),
                appVersion = json.optString("appVersion", ""),
                schemaVersion = json.optInt("schemaVersion", 0),
                platform = json.optString("platform", "android"),
                includedEntries = entries
            )
        }
    }
}

data class BackupBundlePaths(
    val rootDirectory: String,
    val databaseFile: String,
    val preferencesFile: String,
    val manifestFile: String
)

data class BackupSnapshot(
    val databaseFile: String,
    val preferences: BackupPreferencesPayload
)

data class BackupExportResult(
    val fileName: String,
    val locationLabel: String
)

data class RestoreBackupResult(
    val requiresRestart: Boolean
)

sealed class BackupException(message: String) : Exception(message) {
    class ExportFailed(message: String) : BackupException(message)
    class RestoreFailed(message: String) : BackupException(message)
    class ValidationFailed(message: String) : BackupException(message)
}
