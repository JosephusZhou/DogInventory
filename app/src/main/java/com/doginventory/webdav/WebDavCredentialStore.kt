package com.doginventory.webdav

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject

class WebDavCredentialStore(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        FILE_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun read(): WebDavConfig? {
        val raw = preferences.getString(KEY_CONFIG_JSON, null) ?: return null
        return runCatching { WebDavConfig.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun write(config: WebDavConfig) {
        preferences.edit().putString(KEY_CONFIG_JSON, config.toJson().toString()).apply()
    }

    fun clear() {
        preferences.edit().remove(KEY_CONFIG_JSON).apply()
    }

    companion object {
        private const val FILE_NAME = "webdav_credentials"
        private const val KEY_CONFIG_JSON = "webdav_config_json"
    }
}
