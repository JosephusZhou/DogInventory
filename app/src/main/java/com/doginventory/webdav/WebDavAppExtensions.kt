package com.doginventory.webdav

interface WebDavAutoSyncTrigger {
    fun requestSync(reason: String)
    fun pauseSync()
    fun resumeSync(flushReason: String? = null)
}
