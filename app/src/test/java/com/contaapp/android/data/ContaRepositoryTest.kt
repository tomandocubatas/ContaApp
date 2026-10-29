package com.contaapp.android.data

import com.contaapp.android.core.network.ApiClient
import com.contaapp.android.core.network.ApiException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class ContaRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: ContaRepository

    @Before
    fun setUp() {
        server = MockWebServer().also { it.start() }
        repository = ContaRepository(ApiClient { server.url("/api/").toString() })
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun login_returnsSession() {
        server.enqueue(
            MockResponse().setBody(
                """{"accessToken":"a","refreshToken":"r","user":{"name":"Ana","email":"ana@x.es"}}"""
            )
        )

        val session = repository.login("ana@x.es", "pw")

        assertEquals("a", session.accessToken)
        assertEquals("r", session.refreshToken)
        assertEquals("Ana", session.name)
    }

    @Test
    fun login_twoFactorRequired_throwsWithPendingToken() {
        server.enqueue(MockResponse().setBody("""{"requiresTwoFactor":true,"pendingToken":"p1"}"""))

        try {
            repository.login("ana@x.es", "pw")
            fail("Debia pedir doble factor")
        } catch (error: TwoFactorRequiredException) {
            assertEquals("p1", error.pendingToken)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun login_withoutTokens_fails() {
        server.enqueue(MockResponse().setBody("""{"user":{}}"""))

        repository.login("ana@x.es", "pw")
    }

    @Test
    fun refresh_withoutAccessToken_isTreatedAsExpiredSession() {
        server.enqueue(MockResponse().setBody("""{"refreshToken":"r2"}"""))

        try {
            repository.refresh("r1")
            fail("Debia lanzar ApiException")
        } catch (error: ApiException) {
            assertEquals(401, error.statusCode)
        }
    }

    @Test
    fun refresh_keepsOldRefreshTokenWhenServerOmitsIt() {
        server.enqueue(MockResponse().setBody("""{"accessToken":"a2"}"""))

        val session = repository.refresh("r1")

        assertEquals("a2", session.accessToken)
        assertEquals("r1", session.refreshToken)
    }

    @Test
    fun dashboard_missingKpis_yieldZerosNotNaN() {
        server.enqueue(MockResponse().setBody("""{"kpis":{"income":12.5}}"""))

        val dashboard = repository.dashboard("t")

        assertEquals(12.5, dashboard.income, 0.0)
        assertEquals(0.0, dashboard.expense, 0.0)
        assertFalse(dashboard.totalBalance.isNaN())
        assertFalse(dashboard.reviewedPct.isNaN())
    }

    @Test
    fun accountBalances_usesComputedBalanceAndDefaults() {
        server.enqueue(
            MockResponse().setBody(
                """{"accounts":[
                    {"_id":"a1","bankName":"BBVA","name":"Nomina","computedBalance":100.5,"currency":"USD"},
                    {"id":"a2","bank":"ING","accountName":"Ahorro"}
                ]}"""
            )
        )

        val accounts = repository.accountBalances("t")

        assertEquals(2, accounts.size)
        assertEquals(100.5, accounts[0].balance, 0.0)
        assertEquals("USD", accounts[0].currency)
        assertEquals(0.0, accounts[1].balance, 0.0)
        assertEquals("EUR", accounts[1].currency)
        assertEquals("ING", accounts[1].bankName)
    }

    @Test
    fun movements_pagesUntilShortPageAndKeepsAll() {
        server.enqueue(MockResponse().setBody(movementsPage(100, 0)))
        server.enqueue(MockResponse().setBody(movementsPage(3, 100)))

        val movements = repository.movements("t")

        assertEquals(103, movements.size)
        assertEquals(2, server.requestCount)
        assertTrue(server.takeRequest().path!!.contains("page=1"))
        assertTrue(server.takeRequest().path!!.contains("page=2"))
    }

    @Test
    fun movements_stopsAtMaxPages() {
        repeat(20) { server.enqueue(MockResponse().setBody(movementsPage(100, it * 100))) }

        val movements = repository.movements("t")

        assertEquals(1000, movements.size)
        assertEquals(10, server.requestCount)
    }

    @Test
    fun movements_singleShortPage_makesOneRequest() {
        server.enqueue(MockResponse().setBody(movementsPage(2, 0)))

        assertEquals(2, repository.movements("t").size)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun expiringInsurances_requestsConfiguredWindow() {
        server.enqueue(MockResponse().setBody("""{"policies":[]}"""))

        repository.expiringInsurances("t")

        assertTrue(server.takeRequest().path!!.endsWith("days=60"))
    }

    @Test
    fun movements_followsTotalPagesEvenWhenServerCapsPageSize() {
        server.enqueue(MockResponse().setBody(movementsPage(100, 0, totalPages = 2)))
        server.enqueue(MockResponse().setBody(movementsPage(100, 100, totalPages = 2)))
        server.enqueue(MockResponse().setBody(movementsPage(100, 200, totalPages = 2)))

        assertEquals(200, repository.movements("t").size)
        assertEquals(2, server.requestCount)
    }

    private fun movementsPage(count: Int, offset: Int, totalPages: Int? = null): String {
        val items = (0 until count).joinToString(",") { i ->
            """{"_id":"m${offset + i}","date":"2026-10-01","description":"d","amount":1.0,"type":"expense"}"""
        }
        val pagination = totalPages?.let { ""","pagination":{"totalPages":$it}""" }.orEmpty()
        return """{"data":[$items]$pagination}"""
    }
}
