package com.doginventory.webdav

import org.json.JSONObject

data class WebDavConfig(
    val serverUrl: String,
    val username: String,
    val password: String,
    val remotePath: String = DEFAULT_REMOTE_PATH
) {
    val isValid: Boolean
        get() = serverUrl.trim().isNotEmpty() && username.trim().isNotEmpty() && password.isNotEmpty()

    val normalizedRemotePath: String
        get() {
            var path = remotePath.trim().ifEmpty { DEFAULT_REMOTE_PATH }
            if (!path.startsWith("/")) path = "/$path"
            if (!path.endsWith("/")) path = "$path/"
            return path
        }

    fun toJson(): JSONObject = JSONObject().apply {
        put("serverUrl", serverUrl)
        put("username", username)
        put("password", password)
        put("remotePath", remotePath)
    }

    companion object {
        const val DEFAULT_REMOTE_PATH = "/dog_inventory/"

        fun fromJson(json: JSONObject): WebDavConfig {
            return WebDavConfig(
                serverUrl = json.optString("serverUrl", ""),
                username = json.optString("username", ""),
                password = json.optString("password", ""),
                remotePath = json.optString("remotePath", DEFAULT_REMOTE_PATH)
            )
        }
    }
}
