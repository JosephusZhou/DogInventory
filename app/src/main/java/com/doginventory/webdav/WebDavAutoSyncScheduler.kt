package com.doginventory.webdav

import com.doginventory.settings.PreferencesService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

interface WebDavAutoSyncTrigger {
    fun requestSync(reason: String)
}

class WebDavAutoSyncScheduler(
    private val preferencesService: PreferencesService,
    private val webDavSyncService: WebDavSyncService
) : WebDavAutoSyncTrigger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var scheduledJob: Job? = null

    override fun requestSync(reason: String) {
        synchronized(this) {
            scheduledJob?.cancel()
            scheduledJob = scope.launch {
                delay(DEBOUNCE_WINDOW_MILLIS)
                val config = preferencesService.readWebDavConfig()
                if (config != null && config.isValid) {
                    runCatching { webDavSyncService.syncToRemote(config) }
                        .onSuccess {
                            preferencesService.writeLastWebDavAutoSyncAt(System.currentTimeMillis())
                        }
                }
                synchronized(this@WebDavAutoSyncScheduler) {
                    if (scheduledJob === this) {
                        scheduledJob = null
                    }
                }
            }
        }
    }

    companion object {
        private const val DEBOUNCE_WINDOW_MILLIS = 5_000L
    }
}
