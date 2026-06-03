package com.contaapp.android.core.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.contaapp.android.R
import com.contaapp.android.core.cache.FinancialSnapshot
import java.time.LocalDate

class AttentionNotifier(private val context: Context) {
    fun show(snapshot: FinancialSnapshot?) {
        ensureChannel()
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pendingMovements = snapshot?.movements?.count { !it.reviewed } ?: 0
        val debtAlerts = snapshot?.debtDetails?.count {
            it.pendingAmount > 0.0 &&
                it.status.lowercase() !in setOf("paid", "cancelled", "canceled", "closed", "expired", "inactive") &&
                (it.status.equals("overdue", true) || it.status.equals("warning", true))
        } ?: 0
        val insuranceAlerts = snapshot?.expiringInsurances?.count {
            val status = it.status.orEmpty().lowercase()
            val raw = it.renewalDate ?: it.endDate
            val date = raw?.take(10)?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
            status !in setOf("expired", "cancelled", "canceled", "inactive") &&
                date != null && !date.isBefore(LocalDate.now()) && !date.isAfter(LocalDate.now().plusDays(30))
        } ?: 0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("ContaApp requiere atencion")
            .setContentText("$pendingMovements movimientos, $debtAlerts deudas y $insuranceAlerts seguros a revisar")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$pendingMovements movimientos pendientes, $debtAlerts deudas vencidas/proximas y $insuranceAlerts seguros con renovacion a 30 dias.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(ATTENTION_ID, notification)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas ContaApp",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Avisos de deuda, seguros y movimientos pendientes"
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "conta_attention"
        private const val ATTENTION_ID = 4101
    }
}
