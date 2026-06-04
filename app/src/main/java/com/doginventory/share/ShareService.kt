package com.doginventory.share

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

class ShareService(
    private val context: Context,
    private val apiClient: ShareApiClient
) {
    suspend fun createShare(request: ShareCreateRequest): ShareCreateResult = withContext(Dispatchers.IO) {
        val raw = apiClient.createShare(request.toJson().toString())
        parseCreateResult(raw)
    }

    suspend fun fetchShare(shareId: String): SharedList = withContext(Dispatchers.IO) {
        val raw = apiClient.getShare(shareId)
        parseSharedList(raw)
    }

    private fun parseCreateResult(raw: String): ShareCreateResult {
        val obj = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw ShareApiException.Parse(e.message ?: "parse error")
        }
        return ShareCreateResult(
            shareId = obj.optString("shareId", ""),
            url = obj.optString("url", ""),
            expiresAt = obj.optLong("expiresAt", 0L)
        )
    }

    private fun parseSharedList(raw: String): SharedList {
        val obj = try {
            JSONObject(raw)
        } catch (e: JSONException) {
            throw ShareApiException.Parse(e.message ?: "parse error")
        }
        return SharedList.fromJson(obj)
    }
}
