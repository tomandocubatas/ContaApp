package com.contaapp.android.core.settings

import android.content.Context

class AppPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences("conta_app_preferences", Context.MODE_PRIVATE)

    fun isDarkMode(): Boolean = prefs.getBoolean("darkMode", false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean("darkMode", enabled).apply()
    }

    fun isDynamicColor(): Boolean = prefs.getBoolean("dynamicColor", false)

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean("dynamicColor", enabled).apply()
    }

    fun isPrivacyDefault(): Boolean = prefs.getBoolean("privacyDefault", false)

    fun setPrivacyDefault(enabled: Boolean) {
        prefs.edit().putBoolean("privacyDefault", enabled).apply()
    }

    fun cacheTtlMinutes(): Int {
        val minutes = prefs.getInt("cacheTtlMinutes", 360)
        return if (minutes < 60) 360 else minutes
    }

    fun setCacheTtlMinutes(minutes: Int) {
        prefs.edit().putInt("cacheTtlMinutes", minutes.coerceAtLeast(1)).apply()
    }

    fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
}
