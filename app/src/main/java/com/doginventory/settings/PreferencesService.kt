package com.doginventory.settings

import android.content.Context
import com.doginventory.webdav.WebDavConfig
import com.doginventory.webdav.WebDavCredentialStore
import com.doginventory.webdav.WebDavAutoSyncTrigger
import org.json.JSONObject

class PreferencesService(private val context: Context) {
    private val webDavCredentialStore by lazy { WebDavCredentialStore(context) }
    var webDavAutoSyncTrigger: WebDavAutoSyncTrigger? = null

    fun readThemeMode(): String? {
        return preferences().getString(KEY_THEME_MODE, null)
    }

    fun writeThemeMode(themeMode: String, triggerAutoSync: Boolean = true) {
        preferences().edit().putString(KEY_THEME_MODE, themeMode).apply()
        if (triggerAutoSync) {
            webDavAutoSyncTrigger?.requestSync("theme_mode_changed")
        }
    }

    fun readBackupPayload(): BackupPreferencesPayload {
        return BackupPreferencesPayload(
            themeMode = readThemeMode() ?: DEFAULT_THEME_MODE
        )
    }

    fun writeBackupPayload(payload: BackupPreferencesPayload, triggerAutoSync: Boolean = false) {
        writeThemeMode(payload.themeMode, triggerAutoSync = triggerAutoSync)
    }

    fun readLastAutoBackupAt(): Long? {
        return if (preferences().contains(KEY_LAST_AUTO_BACKUP_AT)) {
            preferences().getLong(KEY_LAST_AUTO_BACKUP_AT, 0L)
        } else {
            null
        }
    }

    fun writeLastAutoBackupAt(timestamp: Long) {
        preferences().edit().putLong(KEY_LAST_AUTO_BACKUP_AT, timestamp).apply()
    }

    fun readLastWebDavAutoSyncAt(): Long? {
        return if (preferences().contains(KEY_LAST_WEBDAV_AUTO_SYNC_AT)) {
            preferences().getLong(KEY_LAST_WEBDAV_AUTO_SYNC_AT, 0L)
        } else {
            null
        }
    }

    fun writeLastWebDavAutoSyncAt(timestamp: Long) {
        preferences().edit().putLong(KEY_LAST_WEBDAV_AUTO_SYNC_AT, timestamp).apply()
    }

    fun shouldRunAutoBackup(now: Long = System.currentTimeMillis(), intervalMillis: Long): Boolean {
        val lastBackupAt = readLastAutoBackupAt() ?: return true
        return now - lastBackupAt >= intervalMillis
    }

    fun snapshotRawPreferences(): Map<String, *> = preferences().all.toMap()

    fun restoreRawPreferences(values: Map<String, *>) {
        preferences().edit().clear().apply()
        val editor = preferences().edit()
        values.forEach { (key, value) ->
            when (value) {
                null -> editor.remove(key)
                is String -> editor.putString(key, value)
                is Int -> editor.putInt(key, value)
                is Long -> editor.putLong(key, value)
                is Float -> editor.putFloat(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Set<*> -> editor.putStringSet(key, value.filterIsInstance<String>().toSet())
            }
        }
        editor.apply()
    }

    fun readWebDavConfig(): WebDavConfig? = webDavCredentialStore.read()

    fun writeWebDavConfig(config: WebDavConfig) {
        webDavCredentialStore.write(config)
        webDavAutoSyncTrigger?.requestSync("webdav_config_changed")
    }

    fun clearWebDavConfig() {
        webDavCredentialStore.clear()
    }

    private fun preferences() = context.getSharedPreferences(THEME_PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val THEME_PREFS_NAME = "theme_preferences"
        const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_LAST_AUTO_BACKUP_AT = "last_auto_backup_at"
        const val KEY_LAST_WEBDAV_AUTO_SYNC_AT = "last_webdav_auto_sync_at"
        const val DEFAULT_THEME_MODE = "system"
    }
}

data class BackupPreferencesPayload(
    val themeMode: String = PreferencesService.DEFAULT_THEME_MODE
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("themeMode", themeMode)
    }

    companion object {
        fun fromJson(json: JSONObject): BackupPreferencesPayload {
            return BackupPreferencesPayload(
                themeMode = json.optString("themeMode", PreferencesService.DEFAULT_THEME_MODE)
            )
        }
    }
}
