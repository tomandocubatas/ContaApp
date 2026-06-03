package com.contaapp.android.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.contaapp.android.MainActivity
import com.contaapp.android.R
import com.contaapp.android.core.cache.FinancialSnapshot
import com.contaapp.android.core.cache.SnapshotStore
import com.contaapp.android.core.settings.AppPreferencesStore
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.InsurancePolicy
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

class ContaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = SnapshotStore(context).load()
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, snapshot))
        }
    }

    companion object {
        private val ContaTeal = Color.rgb(45, 212, 191)
        private val ContaCoral = Color.rgb(255, 107, 102)
        private val ContaAmber = Color.rgb(245, 158, 11)

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ContaWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                ContaWidgetProvider().onUpdate(context, manager, ids)
            }
        }
    }

    private fun buildViews(context: Context, snapshot: FinancialSnapshot?): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_conta_summary)
        val preferences = AppPreferencesStore(context)
        val hidden = preferences.isPrivacyDefault()
        val dashboard = snapshot?.dashboard
        val balance = dashboard?.totalBalance ?: 0.0
        val income = dashboard?.income ?: 0.0
        val expense = dashboard?.expense ?: 0.0
        val net = dashboard?.net ?: income - expense
        val visibleDebts = snapshot?.debtDetails?.visibleDebts().orEmpty()
        val visibleInsurances = snapshot?.expiringInsurances?.visibleInsurances().orEmpty()
        val pendingMovements = snapshot?.movements?.count { !it.reviewed } ?: 0
        val debtAlerts = visibleDebts.count { it.status.equals("overdue", true) || it.status.equals("warning", true) }
        val insuranceAlerts = visibleInsurances.count { it.daysUntil() <= 30 }
        val health = healthScore(snapshot, net, pendingMovements, debtAlerts, insuranceAlerts)
        val openApp = openAppIntent(context)

        views.setTextViewText(R.id.widget_updated, snapshot.freshnessLabel(preferences.cacheTtlMinutes()))
        views.setTextViewText(R.id.widget_balance, money(balance, hidden))
        views.setTextViewText(R.id.widget_income, "Ing. ${money(income, hidden)}")
        views.setTextViewText(R.id.widget_expense, "Gas. ${money(expense, hidden)}")
        views.setTextViewText(R.id.widget_net, "Neto ${money(net, hidden)}")
        views.setTextColor(R.id.widget_net, if (net >= 0.0) ContaTeal else ContaCoral)
        views.setTextViewText(R.id.widget_health, "Salud $health%")
        views.setTextColor(R.id.widget_health, healthColor(health))
        views.setProgressBar(R.id.widget_health_bar, 100, health, false)
        views.setTextViewText(R.id.widget_mix, compositionLabel(income, expense))
        views.setTextViewText(R.id.widget_top_expense, topExpenseLabel(snapshot, hidden))
        views.setTextViewText(R.id.widget_next, nextObligationLabel(visibleDebts, visibleInsurances, hidden))
        views.setTextViewText(
            R.id.widget_attention,
            attentionText(snapshot != null, pendingMovements, debtAlerts, insuranceAlerts)
        )

        listOf(
            R.id.widget_root,
            R.id.widget_balance,
            R.id.widget_result_row,
            R.id.widget_income_card,
            R.id.widget_income,
            R.id.widget_expense_card,
            R.id.widget_expense,
            R.id.widget_net_card,
            R.id.widget_net,
            R.id.widget_top_expense_card,
            R.id.widget_top_expense,
            R.id.widget_next_card,
            R.id.widget_next,
            R.id.widget_attention_card,
            R.id.widget_attention
        ).forEach { views.setOnClickPendingIntent(it, openApp) }
        return views
    }

    private fun attentionText(hasCache: Boolean, pending: Int, debtAlerts: Int, insuranceAlerts: Int): String {
        if (!hasCache) return "Abre la app para cargar datos"
        val parts = buildList {
            if (debtAlerts > 0) add("$debtAlerts criticos")
            if (pending > 0) add("$pending pendientes")
            if (insuranceAlerts > 0) add("$insuranceAlerts seguros")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Todo al dia"
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun money(value: Double, hidden: Boolean): String {
        if (hidden) return "•••• €"
        return compactMoney(value)
    }

    private fun compactMoney(value: Double): String {
        val abs = kotlin.math.abs(value)
        val suffixValue = when {
            abs >= 1_000_000 -> value / 1_000_000
            abs >= 10_000 -> value / 1_000
            else -> value
        }
        val suffix = when {
            abs >= 1_000_000 -> " M€"
            abs >= 10_000 -> " k€"
            else -> " €"
        }
        val format = NumberFormat.getNumberInstance(Locale("es", "ES")).apply {
            maximumFractionDigits = if (abs >= 10_000) 1 else 2
            minimumFractionDigits = if (abs >= 10_000) 0 else 2
        }
        return format.format(suffixValue) + suffix
    }

    private fun FinancialSnapshot?.freshnessLabel(ttlMinutes: Int): String {
        val loadedAt = this?.loadedAtMillis?.takeIf { it > 0L } ?: return "Sin cache"
        val elapsedMinutes = ((System.currentTimeMillis() - loadedAt) / 60_000L).coerceAtLeast(0)
        val remainingMinutes = (ttlMinutes - elapsedMinutes).coerceAtLeast(0)
        val time = Instant.ofEpochMilli(loadedAt)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
        val status = if (remainingMinutes > 0) {
            val hours = remainingMinutes / 60
            if (hours > 0) "${hours}h vig." else "${remainingMinutes}m vig."
        } else {
            "vencida"
        }
        return "$time · $status"
    }

    private fun healthScore(
        snapshot: FinancialSnapshot?,
        net: Double,
        pendingMovements: Int,
        debtAlerts: Int,
        insuranceAlerts: Int
    ): Int {
        val dashboard = snapshot?.dashboard
        val reviewed = dashboard?.reviewedPct?.coerceIn(0.0, 100.0) ?: 70.0
        val categorized = dashboard?.categorizedPct?.coerceIn(0.0, 100.0) ?: 70.0
        val netFactor = if (net >= 0.0) 100.0 else 62.0
        val penalty = (pendingMovements.coerceAtMost(25) * 0.6) + (debtAlerts * 8.0) + (insuranceAlerts * 4.0)
        return ((reviewed * 0.38) + (categorized * 0.36) + (netFactor * 0.26) - penalty)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun healthColor(score: Int): Int = when {
        score >= 75 -> ContaTeal
        score >= 50 -> ContaAmber
        else -> ContaCoral
    }

    private fun compositionLabel(income: Double, expense: Double): String {
        val total = kotlin.math.abs(income) + kotlin.math.abs(expense)
        if (total <= 0.0) return "Sin actividad"
        val incomePct = ((kotlin.math.abs(income) / total) * 100).roundToInt()
        val expensePct = 100 - incomePct
        return "Mix $incomePct% ing. · $expensePct% gas."
    }

    private fun topExpenseLabel(snapshot: FinancialSnapshot?, hidden: Boolean): String {
        val top = snapshot?.dashboard?.expenseCategories
            ?.filter { it.total > 0.0 }
            ?.maxByOrNull { it.total }
            ?: return "Top: sin datos"
        return "Top: ${top.name} · ${money(top.total, hidden)}"
    }

    private fun nextObligationLabel(
        debts: List<DebtDetail>,
        insurances: List<InsurancePolicy>,
        hidden: Boolean
    ): String {
        val nextDebt = debts
            .filter { it.daysUntilPayment != null }
            .minByOrNull { it.daysUntilPayment ?: Int.MAX_VALUE }
            ?.let { obligation("Deuda", it.name, it.monthlyPayment, it.daysUntilPayment, hidden) }
        val nextInsurance = insurances
            .mapNotNull { policy ->
                val days = policy.daysUntil().takeIf { it != Int.MAX_VALUE } ?: return@mapNotNull null
                obligation("Seguro", policy.name, policy.annualPremium / 12.0, days, hidden)
            }
            .minByOrNull { it.daysSort }
        val best = listOfNotNull(nextDebt, nextInsurance).minByOrNull { it.daysSort }
        return best?.label ?: "Vence: sin vencimientos"
    }

    private fun obligation(type: String, name: String, amount: Double, days: Int?, hidden: Boolean): WidgetObligation {
        val dayText = when {
            days == null -> "sin fecha"
            days < 0 -> "vencido ${kotlin.math.abs(days)}d"
            days == 0 -> "hoy"
            else -> "en ${days}d"
        }
        return WidgetObligation(
            label = "${type.take(6)}: ${name.take(18)} · $dayText · ${money(amount, hidden)}",
            daysSort = days ?: Int.MAX_VALUE
        )
    }

    private fun List<DebtDetail>.visibleDebts(): List<DebtDetail> {
        return filter { debt ->
            debt.pendingAmount > 0.0 &&
                debt.status.lowercase() !in setOf("paid", "cancelled", "canceled", "closed", "expired", "inactive")
        }
    }

    private fun List<InsurancePolicy>.visibleInsurances(): List<InsurancePolicy> {
        val today = LocalDate.now()
        return filter { policy ->
            val status = policy.status.orEmpty().lowercase()
            val date = policy.policyDate()
            status !in setOf("expired", "cancelled", "canceled", "inactive") &&
                (date == null || !date.isBefore(today))
        }
    }

    private fun InsurancePolicy.policyDate(): LocalDate? {
        val raw = (renewalDate ?: endDate)?.take(10) ?: return null
        return runCatching { LocalDate.parse(raw) }.getOrNull()
    }

    private fun InsurancePolicy.daysUntil(): Int {
        val date = policyDate() ?: return Int.MAX_VALUE
        return ChronoUnit.DAYS.between(LocalDate.now(), date).toInt()
    }

    private data class WidgetObligation(
        val label: String,
        val daysSort: Int
    )
}
