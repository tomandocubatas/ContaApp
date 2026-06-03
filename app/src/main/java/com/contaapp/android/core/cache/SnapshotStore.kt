package com.contaapp.android.core.cache

import android.content.Context
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CategoryBreakdown
import com.contaapp.android.domain.CategoryOption
import com.contaapp.android.domain.ChartPoint
import com.contaapp.android.domain.ChartsSnapshot
import com.contaapp.android.domain.DashboardSummary
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.FinancialSeriesPoint
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.InsuranceCoverage
import com.contaapp.android.domain.InsuranceRenewal
import com.contaapp.android.domain.InsuranceSummary
import com.contaapp.android.domain.Movement
import com.contaapp.android.domain.RentalCategoryMonth
import com.contaapp.android.domain.RentalMonth
import com.contaapp.android.domain.RentalProperty
import com.contaapp.android.domain.RentalSummary
import org.json.JSONArray
import org.json.JSONObject

data class FinancialSnapshot(
    val loadedAtMillis: Long,
    val dashboard: DashboardSummary?,
    val categories: List<CategoryOption>,
    val accounts: List<BankAccountBalance>,
    val movements: List<Movement>,
    val debts: List<DebtSummary>,
    val debtDetails: List<DebtDetail>,
    val insurance: InsuranceSummary?,
    val expiringInsurances: List<InsurancePolicy>,
    val insuranceCoverages: List<InsuranceCoverage> = emptyList(),
    val insuranceRenewals: List<InsuranceRenewal> = emptyList(),
    val rentalSummary: RentalSummary?,
    val rentalProperties: List<RentalProperty>,
    val rentalMonths: List<RentalMonth>,
    val charts: ChartsSnapshot
)

class SnapshotStore(context: Context) {
    private val prefs = context.getSharedPreferences("conta_snapshot", Context.MODE_PRIVATE)

    fun save(snapshot: FinancialSnapshot) {
        runCatching {
            prefs.edit().putString("home", snapshot.toJson().toString()).apply()
        }
    }

    fun load(): FinancialSnapshot? {
        val raw = prefs.getString("home", null) ?: return null
        return runCatching { JSONObject(raw).toSnapshot() }.getOrNull()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

private fun FinancialSnapshot.toJson(): JSONObject {
    return JSONObject()
        .put("loadedAtMillis", loadedAtMillis)
        .put("dashboard", dashboard?.toJson())
        .put("categories", categories.toJsonArray { it.toJson() })
        .put("accounts", accounts.toJsonArray { it.toJson() })
        .put("movements", movements.toJsonArray { it.toJson() })
        .put("debts", debts.toJsonArray { it.toJson() })
        .put("debtDetails", debtDetails.toJsonArray { it.toJson() })
        .put("insurance", insurance?.toJson())
        .put("expiringInsurances", expiringInsurances.toJsonArray { it.toJson() })
        .put("insuranceCoverages", insuranceCoverages.toJsonArray { it.toJson() })
        .put("insuranceRenewals", insuranceRenewals.toJsonArray { it.toJson() })
        .put("rentalSummary", rentalSummary?.toJson())
        .put("rentalProperties", rentalProperties.toJsonArray { it.toJson() })
        .put("rentalMonths", rentalMonths.toJsonArray { it.toJson() })
        .put("charts", charts.toJson())
}

private fun JSONObject.toSnapshot(): FinancialSnapshot {
    return FinancialSnapshot(
        loadedAtMillis = optLong("loadedAtMillis", 0L),
        dashboard = optJSONObject("dashboard")?.toDashboard(),
        categories = optJSONArray("categories").mapObjects { it.toCategoryOption() },
        accounts = optJSONArray("accounts").mapObjects { it.toAccount() },
        movements = optJSONArray("movements").mapObjects { it.toMovement() },
        debts = optJSONArray("debts").mapObjects { it.toDebt() },
        debtDetails = optJSONArray("debtDetails").mapObjects { it.toDebtDetail() },
        insurance = optJSONObject("insurance")?.toInsurance(),
        expiringInsurances = optJSONArray("expiringInsurances").mapObjects { it.toInsurancePolicy() },
        insuranceCoverages = optJSONArray("insuranceCoverages").mapObjects { it.toInsuranceCoverage() },
        insuranceRenewals = optJSONArray("insuranceRenewals").mapObjects { it.toInsuranceRenewal() },
        rentalSummary = optJSONObject("rentalSummary")?.toRentalSummary(),
        rentalProperties = optJSONArray("rentalProperties").mapObjects { it.toRentalProperty() },
        rentalMonths = optJSONArray("rentalMonths").mapObjects { it.toRentalMonth() },
        charts = optJSONObject("charts")?.toChartsSnapshot() ?: ChartsSnapshot()
    )
}

private fun CategoryOption.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("color", color)
    .put("icon", icon)
    .put("type", type)

private fun JSONObject.toCategoryOption() = CategoryOption(
    id = optString("id"),
    name = optString("name", "Sin categoria"),
    color = optString("color").takeIf { it.isNotBlank() && it != "null" },
    icon = optString("icon").takeIf { it.isNotBlank() && it != "null" },
    type = optString("type")
)

private fun DashboardSummary.toJson() = JSONObject()
    .put("totalBalance", totalBalance.jsonNumber())
    .put("totalDebts", totalDebts.jsonNumber())
    .put("income", income.jsonNumber())
    .put("expense", expense.jsonNumber())
    .put("net", net.jsonNumber())
    .put("reviewedPct", reviewedPct.jsonNumber())
    .put("categorizedPct", categorizedPct.jsonNumber())
    .put("expenseCategories", expenseCategories.toJsonArray { it.toJson() })
    .put("incomeCategories", incomeCategories.toJsonArray { it.toJson() })

private fun JSONObject.toDashboard() = DashboardSummary(
    totalBalance = optDouble("totalBalance"),
    totalDebts = optDouble("totalDebts"),
    income = optDouble("income"),
    expense = optDouble("expense"),
    net = optDouble("net"),
    reviewedPct = optDouble("reviewedPct"),
    categorizedPct = optDouble("categorizedPct"),
    expenseCategories = optJSONArray("expenseCategories").mapObjects { it.toCategory() },
    incomeCategories = optJSONArray("incomeCategories").mapObjects { it.toCategory() }
)

private fun CategoryBreakdown.toJson() = JSONObject()
    .put("name", name)
    .put("total", total.jsonNumber())
    .put("count", count)
    .put("budgetPct", budgetPct?.jsonNumber())

private fun JSONObject.toCategory() = CategoryBreakdown(
    name = optString("name"),
    total = optDouble("total"),
    count = optInt("count"),
    budgetPct = takeIf { has("budgetPct") && !isNull("budgetPct") }?.optDouble("budgetPct")
)

private fun BankAccountBalance.toJson() = JSONObject()
    .put("id", id)
    .put("bankName", bankName)
    .put("accountName", accountName)
    .put("balance", balance.jsonNumber())
    .put("currency", currency)

private fun JSONObject.toAccount() = BankAccountBalance(
    id = optString("id"),
    bankName = optString("bankName"),
    accountName = optString("accountName"),
    balance = optDouble("balance"),
    currency = optString("currency", "EUR")
)

private fun Movement.toJson() = JSONObject()
    .put("id", id)
    .put("date", date)
    .put("description", description)
    .put("amount", amount.jsonNumber())
    .put("type", type)
    .put("accountId", accountId)
    .put("categoryId", categoryId)
    .put("categoryName", categoryName)
    .put("accountName", accountName)
    .put("reviewed", reviewed)
    .put("notes", notes)
    .put("parentMovementId", parentMovementId)
    .put("financingId", financingId)
    .put("loanId", loanId)
    .put("propertyId", propertyId)
    .put("insuranceId", insuranceId)
    .put("hasSplits", hasSplits)
    .put("isDuplicate", isDuplicate)

private fun JSONObject.toMovement() = Movement(
    id = optString("id"),
    date = optString("date"),
    description = optString("description"),
    amount = optDouble("amount"),
    type = optString("type"),
    accountId = optString("accountId").takeIf { it.isNotBlank() && it != "null" },
    categoryId = optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
    categoryName = optString("categoryName").takeIf { it.isNotBlank() && it != "null" },
    accountName = optString("accountName").takeIf { it.isNotBlank() && it != "null" },
    reviewed = optBoolean("reviewed"),
    notes = optString("notes").takeIf { it.isNotBlank() && it != "null" },
    parentMovementId = optString("parentMovementId").takeIf { it.isNotBlank() && it != "null" },
    financingId = optString("financingId").takeIf { it.isNotBlank() && it != "null" },
    loanId = optString("loanId").takeIf { it.isNotBlank() && it != "null" },
    propertyId = optString("propertyId").takeIf { it.isNotBlank() && it != "null" },
    insuranceId = optString("insuranceId").takeIf { it.isNotBlank() && it != "null" },
    hasSplits = optBoolean("hasSplits"),
    isDuplicate = optBoolean("isDuplicate")
)

private fun DebtSummary.toJson() = JSONObject()
    .put("title", title)
    .put("pending", pending.jsonNumber())
    .put("paid", paid.jsonNumber())
    .put("interest", interest.jsonNumber())

private fun JSONObject.toDebt() = DebtSummary(
    title = optString("title"),
    pending = optDouble("pending"),
    paid = optDouble("paid"),
    interest = optDouble("interest")
)

private fun DebtDetail.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("entity", entity)
    .put("type", type)
    .put("source", source)
    .put("pendingAmount", pendingAmount.jsonNumber())
    .put("monthlyPayment", monthlyPayment.jsonNumber())
    .put("nextPaymentDate", nextPaymentDate)
    .put("status", status)
    .put("daysUntilPayment", daysUntilPayment)

private fun JSONObject.toDebtDetail() = DebtDetail(
    id = optString("id"),
    name = optString("name"),
    entity = optString("entity"),
    type = optString("type"),
    source = optString("source"),
    pendingAmount = optDouble("pendingAmount"),
    monthlyPayment = optDouble("monthlyPayment"),
    nextPaymentDate = optString("nextPaymentDate").takeIf { it.isNotBlank() && it != "null" },
    status = optString("status"),
    daysUntilPayment = takeIf { has("daysUntilPayment") && !isNull("daysUntilPayment") }?.optInt("daysUntilPayment")
)

private fun InsuranceSummary.toJson() = JSONObject()
    .put("activeCount", activeCount)
    .put("annualPremium", annualPremium.jsonNumber())
    .put("expiringSoon", expiringSoon)

private fun JSONObject.toInsurance() = InsuranceSummary(
    activeCount = optInt("activeCount"),
    annualPremium = optDouble("annualPremium"),
    expiringSoon = optInt("expiringSoon")
)

private fun InsurancePolicy.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("company", company)
    .put("type", type)
    .put("renewalDate", renewalDate)
    .put("endDate", endDate)
    .put("annualPremium", annualPremium.jsonNumber())
    .put("policyNumber", policyNumber)
    .put("coverage", coverage)
    .put("status", status)
    .put("periodicity", periodicity)

private fun JSONObject.toInsurancePolicy() = InsurancePolicy(
    id = optString("id"),
    name = optString("name"),
    company = optString("company").takeIf { it.isNotBlank() && it != "null" },
    type = optString("type"),
    renewalDate = optString("renewalDate").takeIf { it.isNotBlank() && it != "null" },
    endDate = optString("endDate").takeIf { it.isNotBlank() && it != "null" },
    annualPremium = optDouble("annualPremium"),
    policyNumber = optString("policyNumber").takeIf { it.isNotBlank() && it != "null" },
    coverage = optString("coverage").takeIf { it.isNotBlank() && it != "null" },
    status = optString("status").takeIf { it.isNotBlank() && it != "null" },
    periodicity = optString("periodicity").takeIf { it.isNotBlank() && it != "null" }
)

private fun InsuranceCoverage.toJson() = JSONObject()
    .put("type", type)
    .put("maxCoverage", maxCoverage.jsonNumber())
    .put("totalPremium", totalPremium.jsonNumber())
    .put("ratio", ratio.jsonNumber())

private fun JSONObject.toInsuranceCoverage() = InsuranceCoverage(
    type = optString("type"),
    maxCoverage = optDouble("maxCoverage"),
    totalPremium = optDouble("totalPremium"),
    ratio = optDouble("ratio")
)

private fun InsuranceRenewal.toJson() = JSONObject()
    .put("date", date)
    .put("count", count)
    .put("names", JSONArray(names))

private fun JSONObject.toInsuranceRenewal() = InsuranceRenewal(
    date = optString("date"),
    count = optInt("count"),
    names = optJSONArray("names").mapStrings()
)

private fun RentalSummary.toJson() = JSONObject()
    .put("income", income.jsonNumber())
    .put("expense", expense.jsonNumber())
    .put("profit", profit.jsonNumber())
    .put("profitability", profitability.jsonNumber())
    .put("activeProperties", activeProperties)

private fun JSONObject.toRentalSummary() = RentalSummary(
    income = optDouble("income"),
    expense = optDouble("expense"),
    profit = optDouble("profit"),
    profitability = optDouble("profitability"),
    activeProperties = optInt("activeProperties")
)

private fun RentalProperty.toJson() = JSONObject()
    .put("id", id)
    .put("name", name)
    .put("income", income.jsonNumber())
    .put("expense", expense.jsonNumber())
    .put("profit", profit.jsonNumber())
    .put("profitability", profitability.jsonNumber())

private fun JSONObject.toRentalProperty() = RentalProperty(
    id = optString("id"),
    name = optString("name"),
    income = optDouble("income"),
    expense = optDouble("expense"),
    profit = optDouble("profit"),
    profitability = optDouble("profitability")
)

private fun RentalMonth.toJson() = JSONObject()
    .put("month", month)
    .put("monthName", monthName)
    .put("income", income.jsonNumber())
    .put("expense", expense.jsonNumber())
    .put("profit", profit.jsonNumber())
    .put("profitability", profitability.jsonNumber())
    .put("categories", categories.toJsonArray { it.toJson() })

private fun JSONObject.toRentalMonth() = RentalMonth(
    month = optInt("month"),
    monthName = optString("monthName"),
    income = optDouble("income"),
    expense = optDouble("expense"),
    profit = optDouble("profit"),
    profitability = optDouble("profitability"),
    categories = optJSONArray("categories").mapObjects { it.toRentalCategoryMonth() }
)

private fun RentalCategoryMonth.toJson() = JSONObject()
    .put("period", period)
    .put("categoryId", categoryId)
    .put("categoryName", categoryName)
    .put("total", total.jsonNumber())

private fun JSONObject.toRentalCategoryMonth() = RentalCategoryMonth(
    period = optString("period"),
    categoryId = optString("categoryId").takeIf { it.isNotBlank() && it != "null" },
    categoryName = optString("categoryName"),
    total = optDouble("total")
)

private fun ChartsSnapshot.toJson() = JSONObject()
    .put("granularity", granularity)
    .put("financialSeries", financialSeries.toJsonArray { it.toJson() })
    .put("incomeTrend", incomeTrend.toJsonArray { it.toJson() })
    .put("expenseTrend", expenseTrend.toJsonArray { it.toJson() })
    .put("debtTrend", debtTrend.toJsonArray { it.toJson() })
    .put("rentalCashflow", rentalCashflow.toJsonArray { it.toJson() })
    .put("balanceTrend", balanceTrend.toJsonArray { it.toJson() })

private fun JSONObject.toChartsSnapshot() = ChartsSnapshot(
    granularity = optString("granularity", "month"),
    financialSeries = optJSONArray("financialSeries").mapObjects { it.toFinancialSeriesPoint() },
    incomeTrend = optJSONArray("incomeTrend").mapObjects { it.toChartPoint() },
    expenseTrend = optJSONArray("expenseTrend").mapObjects { it.toChartPoint() },
    debtTrend = optJSONArray("debtTrend").mapObjects { it.toChartPoint() },
    rentalCashflow = optJSONArray("rentalCashflow").mapObjects { it.toChartPoint() },
    balanceTrend = optJSONArray("balanceTrend").mapObjects { it.toChartPoint() }
)

private fun FinancialSeriesPoint.toJson() = JSONObject()
    .put("label", label)
    .put("income", income.jsonNumber())
    .put("expense", expense.jsonNumber())
    .put("net", net.jsonNumber())

private fun JSONObject.toFinancialSeriesPoint() = FinancialSeriesPoint(
    label = optString("label"),
    income = optDouble("income"),
    expense = optDouble("expense"),
    net = optDouble("net")
)

private fun ChartPoint.toJson() = JSONObject()
    .put("label", label)
    .put("value", value.jsonNumber())

private fun Double.jsonNumber(): Double = if (isFinite()) this else 0.0

private fun JSONObject.toChartPoint() = ChartPoint(
    label = optString("label"),
    value = optDouble("value")
)

private fun <T> List<T>.toJsonArray(mapper: (T) -> JSONObject): JSONArray {
    val array = JSONArray()
    forEach { array.put(mapper(it)) }
    return array
}

private fun <T> JSONArray?.mapObjects(mapper: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optJSONObject(index)?.let { add(mapper(it)) }
        }
    }
}

private fun JSONArray?.mapStrings(): List<String> {
    if (this == null) return emptyList()
    return buildList {
        for (index in 0 until length()) {
            optString(index).takeIf { it.isNotBlank() }?.let { add(it) }
        }
    }
}
