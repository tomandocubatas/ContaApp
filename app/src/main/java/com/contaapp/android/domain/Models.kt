package com.contaapp.android.domain

data class UserSession(
    val name: String,
    val email: String,
    val accessToken: String,
    val refreshToken: String
)

data class DashboardSummary(
    val totalBalance: Double,
    val totalDebts: Double,
    val income: Double,
    val expense: Double,
    val net: Double,
    val reviewedPct: Double,
    val categorizedPct: Double,
    val expenseCategories: List<CategoryBreakdown> = emptyList(),
    val incomeCategories: List<CategoryBreakdown> = emptyList()
)

data class CategoryBreakdown(
    val name: String,
    val total: Double,
    val count: Int,
    val budgetPct: Double?
)

data class CategoryOption(
    val id: String,
    val name: String,
    val color: String?,
    val icon: String?,
    val type: String
)

data class BankAccountBalance(
    val id: String,
    val bankName: String,
    val accountName: String,
    val balance: Double,
    val currency: String
)

data class Movement(
    val id: String,
    val date: String,
    val description: String,
    val amount: Double,
    val type: String,
    val accountId: String?,
    val categoryId: String?,
    val categoryName: String?,
    val accountName: String?,
    val reviewed: Boolean,
    val notes: String?,
    val parentMovementId: String?,
    val financingId: String?,
    val loanId: String?,
    val propertyId: String?,
    val insuranceId: String?,
    val hasSplits: Boolean,
    val isDuplicate: Boolean
)

data class DebtSummary(
    val title: String,
    val pending: Double,
    val paid: Double,
    val interest: Double
)

data class DebtDetail(
    val id: String,
    val name: String,
    val entity: String,
    val type: String,
    val source: String,
    val pendingAmount: Double,
    val monthlyPayment: Double,
    val nextPaymentDate: String?,
    val status: String,
    val daysUntilPayment: Int?
)

data class InsuranceSummary(
    val activeCount: Int,
    val annualPremium: Double,
    val expiringSoon: Int
)

data class InsurancePolicy(
    val id: String,
    val name: String,
    val company: String?,
    val type: String,
    val renewalDate: String?,
    val endDate: String?,
    val annualPremium: Double,
    val policyNumber: String? = null,
    val coverage: String? = null,
    val status: String? = null,
    val periodicity: String? = null
)

data class InsuranceCoverage(
    val type: String,
    val maxCoverage: Double,
    val totalPremium: Double,
    val ratio: Double
)

data class InsuranceRenewal(
    val date: String,
    val count: Int,
    val names: List<String>
)

data class RentalSummary(
    val income: Double,
    val expense: Double,
    val profit: Double,
    val profitability: Double,
    val activeProperties: Int
)

data class RentalProperty(
    val id: String,
    val name: String,
    val income: Double,
    val expense: Double,
    val profit: Double,
    val profitability: Double
)

data class RentalMonth(
    val month: Int,
    val monthName: String,
    val income: Double,
    val expense: Double,
    val profit: Double,
    val profitability: Double,
    val categories: List<RentalCategoryMonth> = emptyList()
)

data class ChartPoint(
    val label: String,
    val value: Double
)

data class FinancialSeriesPoint(
    val label: String,
    val income: Double,
    val expense: Double,
    val net: Double
)

data class RentalCategoryMonth(
    val period: String,
    val categoryId: String?,
    val categoryName: String,
    val total: Double
)

data class ChartsSnapshot(
    val granularity: String = "month",
    val financialSeries: List<FinancialSeriesPoint> = emptyList(),
    val incomeTrend: List<ChartPoint> = emptyList(),
    val expenseTrend: List<ChartPoint> = emptyList(),
    val debtTrend: List<ChartPoint> = emptyList(),
    val rentalCashflow: List<ChartPoint> = emptyList(),
    val balanceTrend: List<ChartPoint> = emptyList()
)

data class CalendarMonth(
    val year: Int,
    val month: Int,
    val days: List<CalendarDay> = emptyList()
)

data class CalendarDay(
    val date: String,
    val events: List<CalendarEvent>,
    val totalExpense: Double,
    val totalIncome: Double
)

data class CalendarEvent(
    val id: String,
    val type: String,
    val date: String,
    val label: String,
    val amount: Double,
    val currency: String,
    val direction: String,
    val sourceId: String,
    val confirmed: Boolean
)
