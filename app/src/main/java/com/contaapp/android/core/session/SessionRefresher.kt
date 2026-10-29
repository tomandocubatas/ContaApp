package com.contaapp.android.core.session

import com.contaapp.android.core.network.ApiException
import com.contaapp.android.domain.UserSession
import java.io.IOException

/** La sesion no se puede renovar: hay que volver a iniciar sesion. */
class SessionExpiredException(message: String) : IOException(message)

/**
 * Ejecuta una llamada autenticada y, si el servidor responde 401, renueva la sesion una vez y repite.
 * Si la renovacion o el reintento siguen rechazados, lanza [SessionExpiredException].
 */
class SessionRefresher(
    private val refresh: (refreshToken: String) -> UserSession,
    private val save: (UserSession) -> Unit
) {
    fun <T> run(session: UserSession, call: (accessToken: String) -> T): T {
        try {
            return call(session.accessToken)
        } catch (error: ApiException) {
            if (error.statusCode != UNAUTHORIZED) throw error
        }

        val refreshed = try {
            refresh(session.refreshToken)
        } catch (error: ApiException) {
            if (error.statusCode in REJECTED) throw SessionExpiredException(error.message)
            throw error
        }
        val merged = refreshed.copy(
            name = refreshed.name.ifBlank { session.name },
            email = refreshed.email.ifBlank { session.email }
        )
        save(merged)

        return try {
            call(merged.accessToken)
        } catch (error: ApiException) {
            if (error.statusCode == UNAUTHORIZED) throw SessionExpiredException(error.message)
            throw error
        }
    }

    private companion object {
        const val UNAUTHORIZED = 401
        val REJECTED = setOf(400, 401, 403)
    }
}
