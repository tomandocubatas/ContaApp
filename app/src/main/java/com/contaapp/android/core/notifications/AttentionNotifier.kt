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
                date != null && !date.isAfter(LocalDate.now().plusDays(30))
        } ?: 0

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(context.getString(R.string.notification_text, pendingMovements, debtAlerts, insuranceAlerts))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.notification_big_text, pendingMovements, debtAlerts, insuranceAlerts))
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
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = context.getString(R.string.notification_channel_description)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "conta_attention"
        private const val ATTENTION_ID = 4101
    }
}
