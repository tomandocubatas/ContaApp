package com.contaapp.android.core.session

import com.contaapp.android.core.network.ApiException
import com.contaapp.android.domain.UserSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class SessionRefresherTest {
    private val session = UserSession(name = "Ana", email = "ana@x.es", accessToken = "old", refreshToken = "r1")
    private var saved: UserSession? = null
    private var refreshCalls = 0

    private fun refresher(refresh: (String) -> UserSession) =
        SessionRefresher(
            refresh = { token -> refreshCalls++; refresh(token) },
            save = { saved = it }
        )

    @Test
    fun okCall_doesNotRefresh() {
        val result = refresher { error("no debe renovar") }.run(session) { "ok-$it" }

        assertEquals("ok-old", result)
        assertEquals(0, refreshCalls)
        assertNull(saved)
    }

    @Test
    fun unauthorized_refreshesSavesAndRetriesWithNewToken() {
        val newSession = UserSession(name = "", email = "", accessToken = "new", refreshToken = "r2")
        val tokens = mutableListOf<String>()

        val result = refresher { newSession }.run(session) { token ->
            tokens += token
            if (token == "old") throw ApiException(401, "caducado") else "datos"
        }

        assertEquals("datos", result)
        assertEquals(listOf("old", "new"), tokens)
        assertEquals("new", saved?.accessToken)
        assertEquals("Ana", saved?.name)
        assertEquals("ana@x.es", saved?.email)
    }

    @Test
    fun nonAuthError_isRethrownWithoutRefreshing() {
        try {
            refresher { error("no debe renovar") }.run(session) { throw ApiException(500, "fallo") }
            fail("Debia propagar el error")
        } catch (error: ApiException) {
            assertEquals(500, error.statusCode)
        }
        assertEquals(0, refreshCalls)
    }

    @Test
    fun refreshRejected_throwsSessionExpired() {
        for (code in listOf(400, 401, 403)) {
            try {
                refresher { throw ApiException(code, "refresh invalido") }
                    .run(session) { throw ApiException(401, "caducado") }
                fail("Debia caducar la sesion con $code")
            } catch (error: SessionExpiredException) {
                assertEquals("refresh invalido", error.message)
            }
        }
        assertNull(saved)
    }

    @Test
    fun stillUnauthorizedAfterRefresh_throwsSessionExpired() {
        val newSession = session.copy(accessToken = "new")

        try {
            refresher { newSession }.run(session) { throw ApiException(401, "sigue sin permiso") }
            fail("Debia caducar la sesion")
        } catch (error: SessionExpiredException) {
            assertTrue(error.message!!.contains("sigue sin permiso"))
        }
    }

    @Test
    fun networkFailureDuringRefresh_isNotTreatedAsExpiredSession() {
        try {
            refresher { throw IOException("sin red") }.run(session) { throw ApiException(401, "caducado") }
            fail("Debia propagar el error de red")
        } catch (error: IOException) {
            assertTrue(error !is SessionExpiredException)
        }
    }

    @Test
    fun serverErrorDuringRefresh_isNotTreatedAsExpiredSession() {
        try {
            refresher { throw ApiException(503, "servidor caido") }.run(session) { throw ApiException(401, "x") }
            fail("Debia propagar el error")
        } catch (error: ApiException) {
            assertEquals(503, error.statusCode)
        }
    }
}
