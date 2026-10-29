package com.contaapp.android.core.network

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(
    val statusCode: Int,
    override val message: String
) : IOException(message)

class ApiClient(
    private val baseUrlProvider: () -> String
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun get(path: String, token: String? = null): JSONObject {
        val request = baseRequest(path, token).get().build()
        return execute(request)
    }

    fun getArray(path: String, token: String? = null): JSONArray {
        val request = baseRequest(path, token).get().build()
        return executeArray(request)
    }

    fun post(path: String, body: JSONObject, token: String? = null): JSONObject {
        val request = baseRequest(path, token)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        return execute(request)
    }

    private fun baseRequest(path: String, token: String?): Request.Builder {
        val cleanBase = baseUrlProvider().trimEnd('/')
        val cleanPath = path.trimStart('/')
        val builder = Request.Builder()
            .url("$cleanBase/$cleanPath")
            .addHeader("Accept", "application/json")

        if (token != null) {
            builder.addHeader("Authorization", "Bearer $token")
        }

        return builder
    }

    private fun execute(request: Request): JSONObject {
        val raw = executeRaw(request)
        return if (raw.isBlank()) JSONObject() else JSONObject(raw)
    }

    private fun executeArray(request: Request): JSONArray {
        val raw = executeRaw(request)
        return if (raw.isBlank()) JSONArray() else JSONArray(raw)
    }

    private fun executeRaw(request: Request): String {
        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val parsed = runCatching { JSONObject(raw) }.getOrNull()
                val message = parsed?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: parsed?.optString("error")?.takeIf { it.isNotBlank() }
                    ?: "HTTP ${response.code}"
                throw ApiException(response.code, message)
            }
            return raw
        }
    }
}
