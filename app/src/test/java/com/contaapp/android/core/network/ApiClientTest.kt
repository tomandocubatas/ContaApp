package com.contaapp.android.core.network

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        client = ApiClient { server.url("/api/").toString() }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun get_sendsBearerTokenAndBuildsUrlWithoutDoubleSlash() {
        server.enqueue(MockResponse().setBody("""{"ok":true}"""))

        val response = client.get("/health", token = "abc")

        val request = server.takeRequest()
        assertEquals("/api/health", request.path)
        assertEquals("Bearer abc", request.getHeader("Authorization"))
        assertTrue(response.getBoolean("ok"))
    }

    @Test
    fun get_withoutToken_omitsAuthorizationHeader() {
        server.enqueue(MockResponse().setBody("{}"))

        client.get("health")

        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun get_blankBody_returnsEmptyObject() {
        server.enqueue(MockResponse().setBody(""))

        assertEquals(0, client.get("health").length())
    }

    @Test
    fun getArray_blankBody_returnsEmptyArray() {
        server.enqueue(MockResponse().setBody(""))

        assertEquals(0, client.getArray("categories").length())
    }

    @Test
    fun error_usesServerMessageAndStatusCode() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Token caducado"}"""))

        try {
            client.get("dashboard/summary", token = "old")
            fail("Debia lanzar ApiException")
        } catch (error: ApiException) {
            assertEquals(401, error.statusCode)
            assertEquals("Token caducado", error.message)
        }
    }

    @Test
    fun error_fallsBackToErrorFieldThenHttpCode() {
        server.enqueue(MockResponse().setResponseCode(400).setBody("""{"error":"Peticion invalida"}"""))
        server.enqueue(MockResponse().setResponseCode(500).setBody("no es json"))

        val first = runCatching { client.get("a") }.exceptionOrNull() as ApiException
        val second = runCatching { client.getArray("b") }.exceptionOrNull() as ApiException

        assertEquals("Peticion invalida", first.message)
        assertEquals("HTTP 500", second.message)
        assertEquals(500, second.statusCode)
    }

    @Test
    fun post_sendsJsonBody() {
        server.enqueue(MockResponse().setBody("{}"))

        client.post("auth/login", JSONObject().put("email", "a@b.c"))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("""{"email":"a@b.c"}""", request.body.readUtf8())
    }
}
