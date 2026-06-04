package com.doginventory.share

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class ShareApiClient(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {
    fun createShare(requestJson: String): String {
        val body = requestJson.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(joinUrl("/api/shares"))
            .post(body)
            .build()
        return executeForText(request)
    }

    fun getShare(shareId: String): String {
        val request = Request.Builder()
            .url(joinUrl("/api/shares/$shareId"))
            .get()
            .build()
        return executeForText(request)
    }

    private fun joinUrl(path: String): String {
        val base = baseUrl.trim().trimEnd('/')
        val suffix = path.trim().let { if (it.startsWith("/")) it else "/$it" }
        return "$base$suffix"
    }

    private fun executeForText(request: Request): String {
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw ShareApiException.Http(response.code, "HTTP ${response.code}")
                }
                response.body?.string() ?: throw ShareApiException.Parse("empty body")
            }
        } catch (e: ShareApiException) {
            throw e
        } catch (e: IOException) {
            throw ShareApiException.Network(e.message ?: "network error")
        }
    }

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
