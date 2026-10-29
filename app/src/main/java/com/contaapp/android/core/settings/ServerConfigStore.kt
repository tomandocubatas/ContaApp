package com.contaapp.android.core.settings

import android.content.Context
import com.contaapp.android.BuildConfig

class ServerConfigStore(context: Context) {
    private val prefs = context.getSharedPreferences("conta_server_config", Context.MODE_PRIVATE)

    fun getBaseUrl(): String {
        return prefs.getString("baseUrl", null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.API_BASE_URL
    }

    fun saveBaseUrl(baseUrl: String) {
        prefs.edit()
            .putString("baseUrl", normalizeBaseUrl(baseUrl))
            .apply()
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        val withProtocol = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
        val withoutSlash = withProtocol.trimEnd('/')
        return if (withoutSlash.endsWith("/api")) withoutSlash else "$withoutSlash/api"
    }
}

