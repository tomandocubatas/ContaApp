package com.contaapp.android.core.session

import android.content.Context
import com.contaapp.android.domain.UserSession

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("conta_session", Context.MODE_PRIVATE)
    private val crypto = SecureCipher()

    fun save(session: UserSession) {
        prefs.edit()
            .putString("name", crypto.encrypt(session.name))
            .putString("email", crypto.encrypt(session.email))
            .putString("accessToken", crypto.encrypt(session.accessToken))
            .putString("refreshToken", crypto.encrypt(session.refreshToken))
            .apply()
    }

    fun get(): UserSession? {
        return runCatching {
            val accessToken = prefs.getString("accessToken", null)?.let(crypto::decrypt) ?: return null
            val refreshToken = prefs.getString("refreshToken", null)?.let(crypto::decrypt) ?: return null
            UserSession(
                name = prefs.getString("name", "")?.let(crypto::decrypt).orEmpty(),
                email = prefs.getString("email", "")?.let(crypto::decrypt).orEmpty(),
                accessToken = accessToken,
                refreshToken = refreshToken
            )
        }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    fun hasSession(): Boolean = runCatching { get() != null }.getOrDefault(false)
}
