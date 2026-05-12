package com.doginventory.ui.settings

import android.content.res.Resources
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.doginventory.R
import com.doginventory.settings.PreferencesService
import com.doginventory.ui.inventory.formatInventoryDateTime
import com.doginventory.webdav.WebDavConfig
import com.doginventory.webdav.WebDavConsistencyReport
import com.doginventory.webdav.WebDavException
import com.doginventory.webdav.WebDavSyncService
import kotlinx.coroutines.launch

class SettingsWebdavSyncViewModel(
    private val resources: Resources,
    private val preferencesService: PreferencesService,
    private val webDavSyncService: WebDavSyncService
) : ViewModel() {
    var serverUrl by mutableStateOf("")
        private set

    var username by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set

    var remotePath by mutableStateOf(WebDavConfig.DEFAULT_REMOTE_PATH)
        private set

    var isBusy by mutableStateOf(false)
        private set

    var hasSavedConfig by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var successMessage by mutableStateOf<String?>(null)
        private set

    var restoreCompleted by mutableStateOf(false)
        private set

    var consistencyReport by mutableStateOf<WebDavConsistencyReport?>(null)
        private set

    var lastAutoSyncTimeText by mutableStateOf(resources.getString(R.string.settings_webdav_no_auto_record))
        private set

    init {
        loadSavedConfig()
    }

    fun updateServerUrl(value: String) {
        serverUrl = value
    }

    fun updateUsername(value: String) {
        username = value
    }

    fun updatePassword(value: String) {
        password = value
    }

    fun updateRemotePath(value: String) {
        remotePath = value
    }

    fun loadSavedConfig() {
        preferencesService.readWebDavConfig()?.let { config ->
            serverUrl = config.serverUrl
            username = config.username
            password = config.password
            remotePath = config.remotePath
            hasSavedConfig = true
        }
        refreshLastAutoSyncTime()
    }

    fun saveConfig() {
        val config = currentConfigOrNull() ?: run {
            errorMessage = resources.getString(R.string.settings_webdav_fill_complete_config)
            return
        }
        preferencesService.writeWebDavConfig(config)
        hasSavedConfig = true
        successMessage = resources.getString(R.string.settings_webdav_config_saved)
        refreshLastAutoSyncTime()
    }

    fun syncToRemote() {
        val config = ensureSavedConfigOrReport() ?: return
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            try {
                webDavSyncService.syncToRemote(config)
                successMessage = resources.getString(R.string.settings_webdav_sync_success)
                refreshLastAutoSyncTime()
            } catch (error: WebDavException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun testConnection() {
        val config = currentConfigOrNull() ?: run {
            errorMessage = resources.getString(R.string.settings_webdav_fill_complete_config)
            return
        }
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            try {
                webDavSyncService.testConnection(config)
                successMessage = resources.getString(R.string.settings_webdav_test_success)
            } catch (error: WebDavException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun checkConsistency() {
        val config = ensureSavedConfigOrReport() ?: return
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            try {
                consistencyReport = webDavSyncService.checkConsistency(config)
                successMessage = if (consistencyReport?.isConsistent == true) {
                    resources.getString(R.string.settings_webdav_consistency_ok)
                } else {
                    resources.getString(R.string.settings_webdav_consistency_diff)
                }
            } catch (error: WebDavException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun restoreFromRemote() {
        val config = ensureSavedConfigOrReport() ?: return
        viewModelScope.launch {
            isBusy = true
            errorMessage = null
            successMessage = null
            restoreCompleted = false
            try {
                webDavSyncService.restoreFromRemote(config)
                successMessage = resources.getString(R.string.settings_restore_completed)
                restoreCompleted = true
            } catch (error: WebDavException) {
                errorMessage = error.message
            } finally {
                isBusy = false
            }
        }
    }

    fun consumeMessages() {
        errorMessage = null
        successMessage = null
    }

    fun consumeRestoreCompleted() {
        restoreCompleted = false
    }

    private fun ensureSavedConfigOrReport(): WebDavConfig? {
        val config = currentConfigOrNull()
        if (config == null) {
            errorMessage = resources.getString(R.string.settings_webdav_fill_complete_config)
            return null
        }
        if (!hasSavedConfig) {
            errorMessage = resources.getString(R.string.settings_webdav_save_first)
            return null
        }
        return config
    }

    private fun currentConfigOrNull(): WebDavConfig? {
        val config = WebDavConfig(
            serverUrl = serverUrl.trim(),
            username = username.trim(),
            password = password,
            remotePath = remotePath.trim().ifEmpty { WebDavConfig.DEFAULT_REMOTE_PATH }
        )
        return config.takeIf { it.isValid }
    }

    private fun refreshLastAutoSyncTime() {
        lastAutoSyncTimeText = preferencesService.readLastWebDavAutoSyncAt()?.let(::formatInventoryDateTime)
            ?: resources.getString(R.string.settings_webdav_no_auto_record)
    }
}
