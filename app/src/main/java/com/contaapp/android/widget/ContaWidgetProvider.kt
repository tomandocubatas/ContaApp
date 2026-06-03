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
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ContaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = SnapshotStore(context).load()
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(appWidgetId, buildViews(context, snapshot))
        }
    }

    companion object {
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
        val dashboard = snapshot?.dashboard
        val balance = dashboard?.totalBalance ?: 0.0
        val income = dashboard?.income ?: 0.0
        val expense = dashboard?.expense ?: 0.0
        val net = dashboard?.net ?: income - expense
        val pending = snapshot?.movements?.count { !it.reviewed } ?: 0
        val debtAlerts = snapshot?.debtDetails?.count {
            it.pendingAmount > 0.0 &&
                it.status.lowercase() !in setOf("paid", "cancelled", "canceled", "closed", "expired", "inactive") &&
                (it.status.equals("overdue", true) || it.status.equals("warning", true))
        } ?: 0
        val insuranceAlerts = snapshot?.expiringInsurances?.count {
            val status = it.status.orEmpty().lowercase()
            status !in setOf("expired", "cancelled", "canceled", "inactive")
        } ?: 0

        views.setTextViewText(R.id.widget_updated, snapshot?.loadedAtMillis?.freshLabel() ?: "Sin cache")
        views.setTextViewText(R.id.widget_balance, compactMoney(balance))
        views.setTextViewText(R.id.widget_income, "Ing. ${compactMoney(income)}")
        views.setTextViewText(R.id.widget_expense, "Gas. ${compactMoney(expense)}")
        views.setTextViewText(R.id.widget_net, "Neto ${compactMoney(net)}")
        views.setTextColor(R.id.widget_net, if (net >= 0.0) Color.rgb(45, 212, 191) else Color.rgb(255, 107, 102))
        views.setTextViewText(R.id.widget_attention, attentionText(snapshot != null, pending, debtAlerts, insuranceAlerts))
        views.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
        return views
    }

    private fun attentionText(hasCache: Boolean, pending: Int, debtAlerts: Int, insuranceAlerts: Int): String {
        if (!hasCache) return "Abre ContaApp para cargar el snapshot"
        val parts = buildList {
            if (pending > 0) add("$pending mov. pendientes")
            if (debtAlerts > 0) add("$debtAlerts avisos deuda")
            if (insuranceAlerts > 0) add("$insuranceAlerts seguros")
        }
        return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ") ?: "Sin avisos pendientes"
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

    private fun Long.freshLabel(): String {
        if (this <= 0L) return "Cache"
        return "Act. " + Instant.ofEpochMilli(this)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("HH:mm"))
    }
}
