package com.contaapp.android.data

import com.contaapp.android.core.AppConfig
import com.contaapp.android.core.network.ApiClient
import com.contaapp.android.core.network.ApiException
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CalendarDay
import com.contaapp.android.domain.CalendarEvent
import com.contaapp.android.domain.CalendarMonth
import com.contaapp.android.domain.CategoryBreakdown
import com.contaapp.android.domain.CategoryOption
import com.contaapp.android.domain.ChartPoint
import com.contaapp.android.domain.ChartsSnapshot
import com.contaapp.android.domain.DashboardSummary
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.FinancialSeriesPoint
import com.contaapp.android.domain.InsuranceCoverage
import com.contaapp.android.domain.InsuranceRenewal
import com.contaapp.android.domain.InsuranceSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.Movement
import com.contaapp.android.domain.RentalProperty
import com.contaapp.android.domain.RentalMonth
import com.contaapp.android.domain.RentalCategoryMonth
import com.contaapp.android.domain.RentalSummary
import com.contaapp.android.domain.UserSession
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class TwoFactorRequiredException(val pendingToken: String) : Exception("Introduce el codigo de doble factor")

class ContaRepository(
    private val api: ApiClient
) {
    fun login(email: String, password: String): UserSession {
        val response = api.post(
            path = "auth/login",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )

        if (response.optBoolean("requiresTwoFactor")) {
            val pendingToken = response.optString("pendingToken")
            if (pendingToken.isBlank()) throw IllegalStateException("El servidor solicito doble factor sin token pendiente")
            throw TwoFactorRequiredException(pendingToken)
        }

        val user = response.optJSONObject("user") ?: JSONObject()
        val accessToken = response.optString("accessToken")
        val refreshToken = response.optString("refreshToken")
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IllegalStateException("El servidor no devolvio una sesion valida")
        }
        return UserSession(
            name = user.optString("name"),
            email = user.optString("email", email),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun verifyTwoFactor(pendingToken: String, code: String): UserSession {
        val response = api.post(
            path = "auth/2fa/verify",
            body = JSONObject()
                .put("pendingToken", pendingToken)
                .put("code", code)
        )
        val user = response.optJSONObject("user") ?: JSONObject()
        val accessToken = response.optString("accessToken")
        val refreshToken = response.optString("refreshToken")
        if (accessToken.isBlank() || refreshToken.isBlank()) {
            throw IllegalStateException("El servidor no devolvio una sesion valida")
        }
        return UserSession(
            name = user.optString("name"),
            email = user.optString("email"),
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    fun healthCheck() {
        api.get("health")
    }

    fun refresh(refreshToken: String): UserSession {
        val response = api.post(
            path = "auth/refresh",
            body = JSONObject().put("refreshToken", refreshToken)
        )
        val user = response.optJSONObject("user") ?: JSONObject()
        val accessToken = response.optString("accessToken")
        if (accessToken.isBlank()) throw ApiException(401, "La sesion ha caducado, vuelve a iniciar sesion")
        return UserSession(
            name = user.optString("name"),
            email = user.optString("email"),
            accessToken = accessToken,
            refreshToken = response.optString("refreshToken", refreshToken)
        )
    }

    fun dashboard(token: String, filters: MovementFilters = MovementFilters()): DashboardSummary {
        return dashboardResponse(token, filters).toDashboardSummary()
    }

    fun dashboardResponse(token: String, filters: MovementFilters = MovementFilters()): JSONObject {
        val query = buildList {
            if (filters.dateFrom != null) add("dateFrom=${filters.dateFrom.urlEncode()}")
            if (filters.dateTo != null) add("dateTo=${filters.dateTo.urlEncode()}")
            if (filters.accountId != null) add("accountId=${filters.accountId.urlEncode()}")
        }.joinToString("&")
        return api.get("dashboard/summary${query.takeIf { it.isNotBlank() }?.let { "?$it" }.orEmpty()}", token)
    }

    fun dashboardFrom(response: JSONObject): DashboardSummary {
        return response.toDashboardSummary()
    }

    private fun JSONObject.toDashboardSummary(): DashboardSummary {
        val kpis = optJSONObject("kpis") ?: JSONObject()
        val categories = optJSONObject("categories") ?: JSONObject()
        return DashboardSummary(
            totalBalance = kpis.optDouble("totalBalance", 0.0),
            totalDebts = kpis.optDouble("totalDebts", 0.0),
            income = kpis.optDouble("income", 0.0),
            expense = kpis.optDouble("expense", 0.0),
            net = kpis.optDouble("net", 0.0),
            reviewedPct = kpis.optDouble("reviewedPct", 0.0),
            categorizedPct = kpis.optDouble("categorizedPct", 0.0),
            expenseCategories = categories.optJSONArray("expense").toCategoryBreakdowns(),
            incomeCategories = categories.optJSONArray("income").toCategoryBreakdowns()
        )
    }

    fun accountBalances(token: String): List<BankAccountBalance> {
        val response = api.get("accounts/balances", token)
        val items = response.firstArray("accounts")
        return items.mapObjects { item ->
            BankAccountBalance(
                id = item.optString("_id", item.optString("id")),
                bankName = item.optString("bankName", item.optString("bank")),
                accountName = item.optString("name", item.optString("accountName")),
                balance = item.optDouble("computedBalance", item.optDouble("balance", 0.0)),
                currency = item.optString("currency", AppConfig.DEFAULT_CURRENCY)
            )
        }
    }

    fun categories(token: String): List<CategoryOption> {
        return api.getArray("categories", token).mapObjects { item ->
            CategoryOption(
                id = item.optString("_id", item.optString("id")),
                name = item.optString("name", "Sin categoria"),
                color = item.optString("color").takeIf { it.isNotBlank() && it != "null" },
                icon = item.optString("icon").takeIf { it.isNotBlank() && it != "null" },
                type = item.optString("type")
            )
        }.sortedWith(compareBy<CategoryOption> { it.type }.thenBy { it.name.lowercase() })
    }

    fun movements(token: String, filters: MovementFilters = MovementFilters()): List<Movement> {
        if (filters.type == MovementTypeGroups.Income) {
            return movementTypeValues(filters.type)
                .flatMap { type -> movements(token, filters.copy(type = type)) }
                .distinctBy { it.id }
                .sortedByDescending { it.date }
                .take(AppConfig.MOVEMENTS_PAGE_SIZE * AppConfig.MOVEMENTS_MAX_PAGES)
        }
        if (filters.type == MovementTypeGroups.Expense) {
            return movementTypeValues(filters.type)
                .flatMap { type -> movements(token, filters.copy(type = type)) }
                .distinctBy { it.id }
                .sortedByDescending { it.date }
                .take(AppConfig.MOVEMENTS_PAGE_SIZE * AppConfig.MOVEMENTS_MAX_PAGES)
        }
        val items = mutableListOf<JSONObject>()
        for (page in 1..AppConfig.MOVEMENTS_MAX_PAGES) {
            val result = movementsPage(token, filters, page)
            items += result.items
            // El backend limita el tamaño de pagina: manda totalPages, no el tamaño pedido.
            val lastPage = result.totalPages?.let { page >= it } ?: (result.items.size < AppConfig.MOVEMENTS_PAGE_SIZE)
            if (lastPage || result.items.isEmpty()) break
        }
        return items.map { it.toMovement() }
    }

    private class MovementsPage(val items: List<JSONObject>, val totalPages: Int?)

    private fun movementsPage(token: String, filters: MovementFilters, page: Int): MovementsPage {
        val query = buildList {
            add("page=$page")
            add("limit=${AppConfig.MOVEMENTS_PAGE_SIZE}")
            add("sortBy=date")
            add("sortDir=desc")
            if (filters.text.isNotBlank()) add("text=${filters.text.urlEncode()}")
            if (filters.type != null) add("type=${filters.type}")
            if (filters.reviewed != null) add("reviewed=${filters.reviewed}")
            if (filters.dateFrom != null) add("dateFrom=${filters.dateFrom.urlEncode()}")
            if (filters.dateTo != null) add("dateTo=${filters.dateTo.exclusiveDateTo().urlEncode()}")
            if (filters.accountId != null) add("accountId=${filters.accountId.urlEncode()}")
            if (filters.categoryId != null) add("categoryId=${filters.categoryId.urlEncode()}")
            if (filters.minAmount != null) add("amountMin=${filters.minAmount}")
            if (filters.maxAmount != null) add("amountMax=${filters.maxAmount}")
        }.joinToString("&")
        val response = api.get("movements-global?$query", token)
        val totalPages = response.optJSONObject("pagination")?.takeIf { it.has("totalPages") }?.optInt("totalPages")
        return MovementsPage(response.firstArray("data", "movements").mapObjects { it }, totalPages)
    }

    private fun JSONObject.toMovement(): Movement {
        val item = this
        return run {
            Movement(
                id = item.optString("_id", item.optString("id")),
                date = item.optString("date"),
                description = item.optString("description"),
                amount = item.optDouble("amount", 0.0),
                type = item.optString("type"),
                accountId = item.optString("accountId").takeIf { it.isNotBlank() && it != "null" },
                categoryId = item.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                categoryName = item.optString("categoryName").takeIf { it.isNotBlank() },
                accountName = item.optString("accountName").takeIf { it.isNotBlank() },
                reviewed = item.optBoolean("reviewed"),
                notes = item.optString("notes").takeIf { it.isNotBlank() },
                parentMovementId = item.optString("parentMovementId").takeIf { it.isNotBlank() && it != "null" },
                financingId = item.optString("financingId").takeIf { it.isNotBlank() && it != "null" },
                loanId = item.optString("loanId").takeIf { it.isNotBlank() && it != "null" },
                propertyId = item.optString("propertyId").takeIf { it.isNotBlank() && it != "null" },
                insuranceId = item.optString("insuranceId").takeIf { it.isNotBlank() && it != "null" },
                hasSplits = item.optBoolean("hasSplits"),
                isDuplicate = item.optBoolean("isDuplicate")
            )
        }
    }

    fun debt(token: String): List<DebtSummary> {
        val loans = api.get("loans/summary/global", token)
        val financings = api.get("financings/summary", token)
        return listOf(
            DebtSummary(
                title = "Prestamos",
                pending = loans.optDouble("totalDebt", 0.0),
                paid = 0.0,
                interest = loans.optDouble("totalInterestToPay", 0.0)
            ),
            DebtSummary(
                title = "Financiaciones",
                pending = financings.optDouble("pendingCapital", financings.optDouble("totalPending", 0.0)),
                paid = financings.optDouble("paidCapital", 0.0),
                interest = financings.optDouble("paidInterest", 0.0) + financings.optDouble("pendingInterest", 0.0)
            )
        )
    }

    fun debtDetails(token: String): List<DebtDetail> {
        val loans = api.getArray("loans/debt-detail", token).toDebtDetails("Prestamo")
        val financings = api.getArray("financings/debt-detail", token).toDebtDetails("Financiacion")
        return (loans + financings).sortedWith(
            compareBy<DebtDetail> { statusRank(it.status) }
                .thenBy { it.daysUntilPayment ?: Int.MAX_VALUE }
                .thenBy { it.name }
        )
    }

    fun insurances(token: String): InsuranceSummary {
        val response = api.get("insurances/summary", token)
        val data = response.optJSONObject("data") ?: response
        return InsuranceSummary(
            activeCount = data.optInt("activeInsurances"),
            annualPremium = data.optDouble("totalAnnualPremium", 0.0),
            expiringSoon = data.optInt("expiringSoon")
        )
    }

    fun expiringInsurances(token: String): List<InsurancePolicy> {
        val response = api.get("insurances/expiring-soon?days=${AppConfig.EXPIRING_INSURANCE_DAYS}", token)
        val items = response.firstArray("data")
        return items.mapObjects { item -> item.toInsurancePolicy() }
    }

    fun insurancePolicies(token: String): List<InsurancePolicy> {
        val response = api.get("insurances/all", token)
        val items = response.firstArray("data", "items", "insurances")
        return items.mapObjects { item -> item.toInsurancePolicy() }
            .map { policy ->
                policy.id.takeIf { it.isNotBlank() }
                    ?.let { id -> runCatching { insurancePolicy(token, id) }.getOrDefault(policy) }
                    ?: policy
            }
    }

    fun insurancePolicy(token: String, id: String): InsurancePolicy {
        val response = api.get("insurances/${id.urlEncode()}", token)
        val data = response.optJSONObject("data") ?: response
        return data.toInsurancePolicy()
    }

    fun insuranceCoverages(token: String): List<InsuranceCoverage> {
        val response = api.get("insurances/coverage-comparison", token)
        return response.firstArray("data", "items").mapObjects { item ->
            InsuranceCoverage(
                type = item.optString("type", "Seguro"),
                maxCoverage = item.optDouble("maxCoverage", 0.0),
                totalPremium = item.optDouble("totalPremium", 0.0),
                ratio = item.optDouble("ratio", 0.0)
            )
        }
    }

    fun insuranceRenewals(token: String): List<InsuranceRenewal> {
        val response = api.get("insurances/renewal-calendar", token)
        return response.firstArray("data", "items").mapObjects { item ->
            val policies = item.optJSONArray("insurances")
            InsuranceRenewal(
                date = item.optString("date"),
                count = policies?.length() ?: item.optInt("count"),
                names = policies?.mapObjects { policy -> policy.optString("name", "Seguro") }.orEmpty()
            )
        }
    }

    fun rentalSummary(token: String, filters: MovementFilters = MovementFilters()): RentalSummary {
        val query = inclusiveDateQuery(filters)
        val response = api.get("rentals/summary${query.querySuffix()}", token)
        return RentalSummary(
            income = response.optDouble("income", 0.0),
            expense = response.optDouble("expense", 0.0),
            profit = response.optDouble("profit", 0.0),
            profitability = response.optDouble("profitability", 0.0),
            activeProperties = response.optInt("activeProperties")
        )
    }

    fun rentalProperties(token: String, filters: MovementFilters = MovementFilters()): List<RentalProperty> {
        val query = inclusiveDateQuery(filters)
        val direct = runCatching { api.getArray("rentals/properties-breakdown${query.querySuffix()}", token) }.getOrNull()
        val items = direct ?: api.get("rentals/properties-breakdown${query.querySuffix()}", token).firstArray("data", "items")
        return items.mapObjects { item ->
            RentalProperty(
                id = item.optString("propertyId", item.optString("_id", item.optString("id"))),
                name = item.optString("propertyName", item.optString("name", "Propiedad")),
                income = item.optDouble("income", 0.0),
                expense = item.optDouble("expense", 0.0),
                profit = item.optDouble("profit", 0.0),
                profitability = item.optDouble("profitability", 0.0)
            )
        }
            .groupBy { it.id.ifBlank { it.name } }
            .map { (_, rows) ->
                val first = rows.first()
                val income = rows.sumOf { it.income }
                val expense = rows.sumOf { it.expense }
                val profit = rows.sumOf { it.profit }
                first.copy(
                    income = income,
                    expense = expense,
                    profit = profit,
                    profitability = if (income > 0.0) (profit / income) * 100.0 else first.profitability
                )
            }
            .sortedByDescending { it.profit }
    }

    fun rentalMonths(token: String, filters: MovementFilters = MovementFilters()): List<RentalMonth> {
        val query = inclusiveDateQuery(filters)
        val categoriesByPeriod = runCatching {
            flexibleArray("rentals/expenses-by-category${query.querySuffix()}", token)
                .mapObjects { item ->
                    RentalCategoryMonth(
                        period = item.optString("period", item.optString("month")),
                        categoryId = item.optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
                        categoryName = item.optString("categoryName", item.optString("name", "Sin categoria")),
                        total = item.optDouble("total", 0.0)
                    )
                }
                .groupBy { it.period }
        }.getOrDefault(emptyMap())
        return flexibleArray("rentals/cashflow${query.querySuffix()}", token).mapObjects { item ->
            val period = item.optString("period", item.optString("month"))
            val month = period.takeLast(2).toIntOrNull() ?: item.optInt("month")
            RentalMonth(
                month = month,
                monthName = monthName(month).ifBlank { period },
                income = item.optDouble("income", 0.0),
                expense = item.optDouble("expense", 0.0),
                profit = item.optDouble("cashflow", item.optDouble("profit", 0.0)),
                profitability = item.optDouble("profitability", 0.0),
                categories = categoriesByPeriod[period].orEmpty().sortedByDescending { it.total }
            )
        }
    }

    fun charts(token: String, dashboard: JSONObject, filters: MovementFilters = MovementFilters()): ChartsSnapshot {
        val query = exclusiveDateQuery(filters)
        val dashboardQuery = inclusiveDateQuery(filters)
        val range = dashboard.optJSONObject("range") ?: JSONObject()
        val series = (dashboard.optJSONObject("series")?.firstArray("current") ?: JSONArray()).mapObjects { item ->
            val expense = kotlin.math.abs(item.optDouble("expense", 0.0))
            FinancialSeriesPoint(
                label = item.optString("date", item.optString("month", item.optString("label"))),
                income = item.optDouble("income", 0.0),
                expense = expense,
                net = item.optDouble("net", item.optDouble("income", 0.0) - expense)
            )
        }
        val income = runCatching {
            flexibleArray("dashboard/category-history?type=income${dashboardQuery.queryContinuation()}", token)
                .mapObjects { it.toMonthlyTotal() }
        }.getOrDefault(emptyList())
        val expense = runCatching {
            flexibleArray("dashboard/category-history?type=expense${dashboardQuery.queryContinuation()}", token)
                .mapObjects { it.toMonthlyTotal() }
        }.getOrDefault(emptyList())
        val debt = runCatching {
            flexibleArray("dashboard/debt-evolution", token)
                .mapObjects { ChartPoint(it.optString("month", it.optString("label")), it.optDouble("totalDebt", 0.0)) }
        }.getOrDefault(emptyList())
        val rentals = runCatching {
            flexibleArray("rentals/cashflow${query.querySuffix()}", token)
                .mapObjects { ChartPoint(it.optString("period", it.optString("month", it.optString("label"))), it.optDouble("cashflow", it.optDouble("profit", 0.0))) }
        }.getOrDefault(emptyList())
        val balances = runCatching {
            val response = api.get("accounts/balance-evolution${query.querySuffix()}", token)
            response.firstArray("evolution", "data").mapObjects {
                ChartPoint(it.optString("month", it.optString("date", it.optString("label"))), it.optDouble("totalBalance", 0.0))
            }
        }.getOrDefault(emptyList())
        return ChartsSnapshot(
            granularity = range.optString("granularity", if (series.size > 45) "month" else "day"),
            financialSeries = series,
            incomeTrend = income,
            expenseTrend = expense,
            debtTrend = debt,
            rentalCashflow = rentals,
            balanceTrend = balances
        )
    }

    fun rentalPropertyMonths(token: String, propertyId: String, year: Int): List<RentalMonth> {
        val response = api.get("properties/$propertyId/monthly-summary?year=$year", token)
        val data = response.optJSONObject("data") ?: response
        return data.firstArray("months").mapObjects { item ->
            RentalMonth(
                month = item.optInt("month"),
                monthName = item.optString("monthName", item.optString("month")),
                income = item.optDouble("income", 0.0),
                expense = item.optDouble("expense", 0.0),
                profit = item.optDouble("profit", 0.0),
                profitability = item.optDouble("profitability", 0.0)
            )
        }
    }

    fun calendarMonth(token: String, year: Int, month: Int): CalendarMonth {
        val response = api.get("calendar/events?year=$year&month=$month", token)
        val data = response.optJSONObject("data") ?: response
        return CalendarMonth(
            year = data.optInt("year", year),
            month = data.optInt("month", month),
            days = data.firstArray("days").mapObjects { day ->
                CalendarDay(
                    date = day.optString("date"),
                    events = day.firstArray("events").mapObjects { event ->
                        CalendarEvent(
                            id = event.optString("id"),
                            type = event.optString("type"),
                            date = event.optString("date"),
                            label = event.optString("label", "Evento"),
                            amount = event.optDouble("amount", 0.0),
                            currency = event.optString("currency", AppConfig.DEFAULT_CURRENCY),
                            direction = event.optString("direction"),
                            sourceId = event.optString("sourceId"),
                            confirmed = event.optBoolean("confirmed")
                        )
                    },
                    totalExpense = day.optDouble("totalExpense", 0.0),
                    totalIncome = day.optDouble("totalIncome", 0.0)
                )
            }
        )
    }

    private fun flexibleArray(path: String, token: String): JSONArray {
        return runCatching { api.getArray(path, token) }
            .getOrElse { api.get(path, token).firstArray("data", "items", "months", "cashflow") }
    }
}

data class MovementFilters(
    val text: String = "",
    val type: String? = null,
    val reviewed: Boolean? = null,
    val dateFrom: String? = null,
    val dateTo: String? = null,
    val accountId: String? = null,
    val categoryId: String? = null,
    val minAmount: Double? = null,
    val maxAmount: Double? = null
)

object MovementTypeGroups {
    const val Income = "income_group"
    const val Expense = "expense_group"
}

fun movementTypeValues(type: String?): List<String> {
    return when (type) {
        MovementTypeGroups.Income -> listOf("ingreso", "transferencia_entrada", "deposito")
        MovementTypeGroups.Expense -> listOf("gastos", "transferencia_salida", "retiro")
        null -> emptyList()
        else -> listOf(type)
    }
}

private fun JSONObject.firstArray(vararg keys: String): JSONArray {
    for (key in keys) {
        val value = opt(key)
        if (value is JSONArray) return value
    }
    return JSONArray()
}

private fun String.urlEncode(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())

private fun String.exclusiveDateTo(): String {
    return runCatching { LocalDate.parse(take(10)).plusDays(1).toString() }.getOrDefault(this)
}

private fun exclusiveDateQuery(filters: MovementFilters): List<String> = buildList {
    if (filters.dateFrom != null) add("dateFrom=${filters.dateFrom.urlEncode()}")
    if (filters.dateTo != null) add("dateTo=${filters.dateTo.exclusiveDateTo().urlEncode()}")
}

private fun inclusiveDateQuery(filters: MovementFilters): List<String> = buildList {
    if (filters.dateFrom != null) add("dateFrom=${filters.dateFrom.urlEncode()}")
    if (filters.dateTo != null) add("dateTo=${filters.dateTo.urlEncode()}")
}

private fun List<String>.querySuffix(): String = takeIf { it.isNotEmpty() }?.joinToString("&", prefix = "?").orEmpty()

private fun List<String>.queryContinuation(): String = takeIf { it.isNotEmpty() }?.joinToString("&", prefix = "&").orEmpty()

private fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(mapper(it)) }
        }
    }
}

private fun JSONObject.toInsurancePolicy(): InsurancePolicy {
    val premium = optJSONObject("premium") ?: JSONObject()
    val companyObject = optJSONObject("company")
    return InsurancePolicy(
        id = optString("_id", optString("id")),
        name = optString("name", optString("policyName", "Seguro")),
        company = companyObject?.optString("name")?.takeIf { it.isNotBlank() }
            ?: optString("companyName").takeIf { it.isNotBlank() }
            ?: optString("company").takeIf { it.isNotBlank() && it != "null" },
        type = optString("type", optString("category", "Seguro")),
        renewalDate = optString("renewalDate").takeIf { it.isNotBlank() && it != "null" }
            ?: optString("nextRenewalDate").takeIf { it.isNotBlank() && it != "null" },
        endDate = optString("endDate").takeIf { it.isNotBlank() && it != "null" }
            ?: optString("expirationDate").takeIf { it.isNotBlank() && it != "null" },
        annualPremium = premium.optDouble(
            "annualizedAmount",
            optDouble("annualPremium", optDouble("premiumAmount", premium.optDouble("amount", 0.0)))
        ),
        policyNumber = optString("policyNumber").takeIf { it.isNotBlank() && it != "null" },
        coverage = optString("coverage").takeIf { it.isNotBlank() && it != "null" },
        status = optString("status").takeIf { it.isNotBlank() && it != "null" },
        periodicity = optString("periodicity", optString("paymentFrequency")).takeIf { it.isNotBlank() && it != "null" }
    )
}

private fun JSONArray.toDebtDetails(source: String): List<DebtDetail> {
    return mapObjects { item ->
        DebtDetail(
            id = item.optString("id", item.optString("_id")),
            name = item.optString("name", source),
            entity = item.optString("entity", "Entidad no informada"),
            type = item.optString("type", source),
            source = source,
            pendingAmount = item.optDouble("pendingAmount", 0.0),
            monthlyPayment = item.optDouble("monthlyPayment", 0.0),
            nextPaymentDate = item.optString("nextPaymentDate").takeIf { it.isNotBlank() && it != "null" },
            status = item.optString("status", "ok"),
            daysUntilPayment = item.takeIf { it.has("daysUntilPayment") && !it.isNull("daysUntilPayment") }?.optInt("daysUntilPayment")
        )
    }
}

private fun statusRank(status: String): Int {
    return when (status.lowercase()) {
        "overdue" -> 0
        "warning" -> 1
        else -> 2
    }
}

private fun JSONArray?.toCategoryBreakdowns(): List<CategoryBreakdown> {
    if (this == null) return emptyList()
    return mapObjects { item ->
        CategoryBreakdown(
            name = item.optString("name", "Sin categoria"),
            total = item.optDouble("total", 0.0),
            count = item.optInt("count"),
            budgetPct = item.takeIf { it.has("budgetPct") && !it.isNull("budgetPct") }?.optDouble("budgetPct", 0.0)
        )
    }
}

private fun JSONObject.toMonthlyTotal(): ChartPoint {
    val categories = optJSONArray("categories")
    val total = if (categories != null) {
        (0 until categories.length()).sumOf { index -> categories.optJSONObject(index)?.optDouble("total", 0.0) ?: 0.0 }
    } else {
        optDouble("total", optDouble("value", 0.0))
    }
    return ChartPoint(optString("month", optString("period", optString("label"))), total)
}

private fun monthName(month: Int): String {
    return listOf(
        "", "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    ).getOrElse(month) { "" }
}
