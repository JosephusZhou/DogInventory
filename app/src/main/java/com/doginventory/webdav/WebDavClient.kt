package com.doginventory.webdav

import android.content.Context
import com.doginventory.R
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

class WebDavClient(
    private val config: WebDavConfig,
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
) {
    private val authHeader = Credentials.basic(config.username, config.password)

    fun ping() {
        execute(Request.Builder().url(resolveUrl("")).header("Authorization", authHeader).get().build())
    }

    fun mkdirRecursive(remotePath: String) {
        val segments = remotePath.split('/').filter { it.isNotBlank() }
        var current = ""
        segments.forEach { segment ->
            current += "/$segment"
            val request = Request.Builder()
                .url(resolveUrl("$current/"))
                .header("Authorization", authHeader)
                .method("MKCOL", ByteArray(0).toRequestBody(null))
                .build()
            executeAllowingAlreadyExists(request)
        }
    }

    fun uploadFile(localFile: File, remotePath: String) {
        val request = Request.Builder()
            .url(resolveUrl(remotePath))
            .header("Authorization", authHeader)
            .put(localFile.asRequestBody("application/octet-stream".toMediaType()))
            .build()
        execute(request)
    }

    fun downloadFile(remotePath: String, localFile: File) {
        localFile.parentFile?.mkdirs()
        val request = Request.Builder()
            .url(resolveUrl(remotePath))
            .header("Authorization", authHeader)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw response.toWebDavException(
                    defaultMessage = context.getString(R.string.webdav_download_failed),
                    context = context
                )
            }
            localFile.outputStream().use { output ->
                response.body?.byteStream()?.use { input -> input.copyTo(output) }
                    ?: throw WebDavException(context.getString(R.string.webdav_download_empty_body))
            }
        }
    }

    fun readText(remotePath: String): String {
        val request = Request.Builder()
            .url(resolveUrl(remotePath))
            .header("Authorization", authHeader)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw response.toWebDavException(
                    defaultMessage = context.getString(R.string.webdav_read_remote_failed),
                    context = context
                )
            }
            return response.body?.string() ?: throw WebDavException(context.getString(R.string.webdav_read_remote_empty_body))
        }
    }

    fun writeText(remotePath: String, value: String) {
        val request = Request.Builder()
            .url(resolveUrl(remotePath))
            .header("Authorization", authHeader)
            .put(value.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()
        execute(request)
    }

    private fun execute(request: Request) {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw response.toWebDavException(context = context)
            }
        }
    }

    private fun executeAllowingAlreadyExists(request: Request) {
        client.newCall(request).execute().use { response ->
            if (response.isSuccessful || response.code == HttpURLConnection.HTTP_CONFLICT || response.code == HttpURLConnection.HTTP_BAD_METHOD) {
                return
            }
            throw response.toWebDavException(
                defaultMessage = context.getString(R.string.webdav_create_remote_dir_failed),
                context = context
            )
        }
    }

    private fun resolveUrl(remotePath: String): String {
        val base = config.serverUrl.trim().trimEnd('/')
        val normalized = remotePath.trim().removePrefix("/")
        return if (normalized.isEmpty()) base else "$base/$normalized"
    }
}

class WebDavException(message: String) : Exception(message)

private fun okhttp3.Response.toWebDavException(
    defaultMessage: String = "",
    context: Context
): WebDavException {
    val reason = when (code) {
        HttpURLConnection.HTTP_UNAUTHORIZED, HttpURLConnection.HTTP_FORBIDDEN -> context.getString(R.string.webdav_auth_failed)
        HttpURLConnection.HTTP_NOT_FOUND -> context.getString(R.string.webdav_remote_not_found)
        else -> context.getString(
            R.string.webdav_http_error,
            defaultMessage.ifBlank { context.getString(R.string.webdav_request_failed) },
            code
        )
    }
    return WebDavException(reason)
}
