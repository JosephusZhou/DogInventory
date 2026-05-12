package com.doginventory.webdav

import com.doginventory.settings.PreferencesService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WebDavAutoSyncScheduler(
    private val preferencesService: PreferencesService,
    // Deferred lookup avoids constructing WebDavSyncService while repository is
    // wiring its auto-sync trigger during app startup.
    private val webDavSyncServiceProvider: () -> WebDavSyncService
) : WebDavAutoSyncTrigger {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile
    private var scheduledJob: Job? = null
    private var pauseDepth = 0
    private var pendingReason: String? = null

    override fun requestSync(reason: String) {
        synchronized(this) {
            if (pauseDepth > 0) {
                pendingReason = reason
                return
            }
            scheduledJob?.cancel()
            scheduledJob = scope.launch {
                delay(DEBOUNCE_WINDOW_MILLIS)
                val config = preferencesService.readWebDavConfig()
                if (config != null && config.isValid) {
                    runCatching { webDavSyncServiceProvider().syncToRemote(config) }
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

    override fun pauseSync() {
        synchronized(this) {
            pauseDepth += 1
        }
    }

    override fun resumeSync(flushReason: String?) {
        val reasonToFlush = synchronized(this) {
            if (pauseDepth == 0) {
                pendingReason = flushReason ?: pendingReason
                return
            }
            pauseDepth -= 1
            if (flushReason != null) {
                pendingReason = flushReason
            }
            if (pauseDepth == 0) {
                pendingReason.also { pendingReason = null }
            } else {
                null
            }
        }
        if (!reasonToFlush.isNullOrBlank()) {
            requestSync(reasonToFlush)
        }
    }

    companion object {
        private const val DEBOUNCE_WINDOW_MILLIS = 5_000L
    }
}
