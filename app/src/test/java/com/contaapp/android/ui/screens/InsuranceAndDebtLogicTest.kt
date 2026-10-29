package com.contaapp.android.ui.screens

import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsurancePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class InsuranceAndDebtLogicTest {
    private val today = LocalDate.now()

    private fun policy(id: String, status: String?, date: LocalDate?) = InsurancePolicy(
        id = id,
        name = "Poliza $id",
        company = null,
        type = "auto",
        renewalDate = date?.toString(),
        endDate = null,
        annualPremium = 100.0,
        status = status
    )

    @Test
    fun visiblePolicies_keepsOverdueOnesButDropsClosedStatuses() {
        val policies = listOf(
            policy("activa", "active", today.plusDays(200)),
            policy("pendiente-pasada", "pending_renewal", today.minusDays(5)),
            policy("activa-pasada", "active", today.minusDays(40)),
            policy("expirada", "expired", today.minusDays(10)),
            policy("cancelada", "cancelled", today.plusDays(10)),
            policy("inactiva", "inactive", null)
        )

        val ids = policies.visibleInsurancePolicies().map { it.id }

        assertEquals(listOf("activa", "pendiente-pasada", "activa-pasada"), ids)
    }

    @Test
    fun urgency_flagsPastDatesAsExpired() {
        assertEquals(InsuranceUrgency.Expired, insuranceUrgency(today.minusDays(1).toString()))
        assertEquals(InsuranceUrgency.Soon, insuranceUrgency(today.plusDays(10).toString()))
        assertEquals(InsuranceUrgency.Future, insuranceUrgency(today.plusDays(100).toString()))
        assertEquals(InsuranceUrgency.Unknown, insuranceUrgency(null))
    }

    @Test
    fun range_separatesOverdueFromUpcomingWindows() {
        val overdue = today.minusDays(3).toString()
        assertTrue(InsuranceRange.Overdue.matches(overdue))
        assertFalse(InsuranceRange.Overdue.matches(today.toString()))
        assertFalse(InsuranceRange.Days30.matches(overdue))
        assertTrue(InsuranceRange.Days30.matches(today.toString()))
        assertTrue(InsuranceRange.Days30.matches(today.plusDays(30).toString()))
        assertFalse(InsuranceRange.Days30.matches(today.plusDays(31).toString()))
        assertTrue(InsuranceRange.Days60.matches(today.plusDays(45).toString()))
        assertFalse(InsuranceRange.Days30.matches(null))
        assertFalse(InsuranceRange.Overdue.matches(null))
        assertTrue(InsuranceRange.All.matches(null))
    }

    @Test
    fun knownSum_isNullWhenAnyValueIsUnknown() {
        val loans = DebtSummary("Prestamos", pending = 100.0, paid = null, interest = 5.0)
        val financing = DebtSummary("Financiaciones", pending = 50.0, paid = 20.0, interest = 2.0)

        assertNull(listOf(loans, financing).knownSum { it.paid })
        assertEquals(7.0, listOf(loans, financing).knownSum { it.interest }!!, 0.0)
    }

    @Test
    fun knownSum_sumsWhenAllKnownAndIsNullWhenEmpty() {
        val a = DebtSummary("A", 1.0, paid = 3.0, interest = 0.0)
        val b = DebtSummary("B", 1.0, paid = 4.5, interest = 0.0)

        assertEquals(7.5, listOf(a, b).knownSum { it.paid }!!, 0.0)
        assertNull(emptyList<DebtSummary>().knownSum { it.paid })
    }
}
