package com.contaapp.android.core.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.contaapp.android.domain.UserSession
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("conta_session", Context.MODE_PRIVATE)
    private val crypto = SessionCrypto()

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

private class SessionCrypto {
    private val alias = "contaapp_session_key"
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun encrypt(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = cipher.iv + encrypted
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? {
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val iv = payload.copyOfRange(0, 12)
            val encrypted = payload.copyOfRange(12, payload.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }
}
