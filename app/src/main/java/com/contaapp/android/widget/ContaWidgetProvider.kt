package com.contaapp.android.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.contaapp.android.R
import com.contaapp.android.core.cache.SnapshotStore
import java.text.NumberFormat
import java.util.Locale

class ContaWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val snapshot = SnapshotStore(context).load()
        val balance = snapshot?.dashboard?.totalBalance ?: 0.0
        val pending = snapshot?.movements?.count { !it.reviewed } ?: 0
        val debtAlerts = snapshot?.debtDetails?.count {
            it.pendingAmount > 0.0 &&
                it.status.lowercase() !in setOf("paid", "cancelled", "canceled", "closed", "expired", "inactive") &&
                (it.status.equals("overdue", true) || it.status.equals("warning", true))
        } ?: 0

        appWidgetIds.forEach { appWidgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_conta_summary)
            views.setTextViewText(R.id.widget_balance, money(balance))
            views.setTextViewText(R.id.widget_pending, "$pending pendientes")
            views.setTextViewText(R.id.widget_alerts, "$debtAlerts avisos deuda")
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    private fun money(value: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(value)
    }
}
