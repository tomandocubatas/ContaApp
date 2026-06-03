@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.contaapp.android.ContaUiState
import com.contaapp.android.R
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CategoryBreakdown
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.Movement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
@Composable
fun DebtScreen(
    state: ContaUiState,
    focusDebtId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var selectedDebt by remember { mutableStateOf<DebtDetail?>(null) }
    var query by remember { mutableStateOf("") }
    var source by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    val visibleDetails = state.debtDetails.visibleDebtDetails()
    val visibleSummaries = if (visibleDetails.isNotEmpty()) {
        debtSummariesFromDetails(visibleDetails)
    } else {
        state.debts
    }
    val filteredDetails = visibleDetails.filter { debt ->
        val matchesText = query.isBlank() ||
            debt.name.contains(query, ignoreCase = true) ||
            debt.entity.contains(query, ignoreCase = true)
        val matchesSource = source == null || debt.source == source
        val matchesStatus = status == null || debt.status.equals(status, ignoreCase = true)
        matchesText && matchesSource && matchesStatus
    }

    LaunchedEffect(focusDebtId, visibleDetails) {
        focusDebtId?.let { id ->
            selectedDebt = visibleDetails.firstOrNull { it.id == id }
            onFocusConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        LazyColumn(Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { SectionHeader("Posicion de deuda", "${filteredDetails.size} detalles") }
            item {
                DebtHeroCard(debts = visibleSummaries, hidden = state.privacyMode)
            }
            if (visibleDetails.isNotEmpty()) {
                item {
                    DebtNextPaymentCard(debts = visibleDetails, hidden = state.privacyMode)
                }
                item {
                    DebtCalendarCard(debts = visibleDetails, hidden = state.privacyMode)
                }
                item {
                    FilterPanel {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            label = { Text("Buscar deuda o entidad") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Origen", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterButton("Todo", source == null) { source = null }
                            FilterButton("Prestamos", source == "Prestamo") { source = "Prestamo" }
                            FilterButton("Financ.", source == "Financiacion") { source = "Financiacion" }
                        }
                        Text("Estado", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            FilterButton("Estados", status == null) { status = null }
                            FilterButton("Avisos", status == "warning") { status = "warning" }
                            FilterButton("Vencidas", status == "overdue") { status = "overdue" }
                        }
                    }
                }
                if (state.isLoading && filteredDetails.isEmpty()) {
                    item { SkeletonCard(lines = 4) }
                    item { SkeletonCard(lines = 4) }
                }
                debtSourceGroups(filteredDetails).forEach { (label, rows) ->
                    item {
                        DebtSourceSummaryCard(label = label, debts = rows, hidden = state.privacyMode)
                    }
                    items(rows, key = { it.id.ifBlank { "${it.source}-${it.name}-${it.entity}" } }) { debt -> DebtDetailRow(debt, state.privacyMode) { selectedDebt = debt } }
                }
            } else {
                items(visibleSummaries, key = { it.title }) { debt -> DebtRow(debt, state.privacyMode) }
            }
            item { EmptyHint(visibleSummaries.isEmpty() && filteredDetails.isEmpty(), "No hay deuda activa para los filtros actuales.") }
        }
    }

    selectedDebt?.let { debt ->
        DebtDetailDialog(
            debt = debt,
            hidden = state.privacyMode,
            onDismiss = { selectedDebt = null }
        )
    }
}

fun debtSourceGroups(debts: List<DebtDetail>): List<Pair<String, List<DebtDetail>>> {
    return listOf(
        "Prestamos" to debts.filter { it.source.equals("Prestamo", true) },
        "Financiaciones" to debts.filter { it.source.equals("Financiacion", true) }
    ).filter { it.second.isNotEmpty() }
}

fun debtSummariesFromDetails(debts: List<DebtDetail>): List<DebtSummary> {
    return debtSourceGroups(debts).map { (label, rows) ->
        DebtSummary(
            title = label,
            pending = rows.sumOf { it.pendingAmount },
            paid = 0.0,
            interest = 0.0
        )
    }
}

@Composable
fun DebtSourceSummaryCard(label: String, debts: List<DebtDetail>, hidden: Boolean) {
    val pending = debts.sumOf { it.pendingAmount }
    val payment = debts.sumOf { it.monthlyPayment }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                Text("${debts.size} operaciones · cuota ${money(payment, hidden)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            StatusChip(money(pending, hidden), ContaDanger)
        }
    }
}

@Composable
fun DebtCalendarCard(debts: List<DebtDetail>, hidden: Boolean) {
    val next30 = debts.filter { debt ->
        val days = debt.daysUntilPayment
        days != null && days <= 30
    }
    val total = next30.sumOf { it.monthlyPayment }
    AttentionCard(
        title = "Pagos proximos 30 dias",
        value = money(total, hidden),
        detail = "${next30.size} cuotas previstas o vencidas",
        color = if (next30.any { (it.daysUntilPayment ?: 99) < 0 }) ContaDanger else ContaAmber
    )
}

@Composable
fun DebtNextPaymentCard(debts: List<DebtDetail>, hidden: Boolean) {
    val next = debts
        .filter { it.daysUntilPayment != null }
        .sortedWith(compareBy<DebtDetail> { it.daysUntilPayment ?: Int.MAX_VALUE }.thenByDescending { it.monthlyPayment })
        .firstOrNull()
    if (next != null) {
        AttentionCard(
            title = "Proximo pago: ${next.name}",
            value = money(next.monthlyPayment, hidden),
            detail = "${next.entity} - ${nextPaymentText(next)}",
            color = when (next.status.lowercase()) {
                "overdue" -> ContaDanger
                "warning" -> ContaAmber
                else -> ContaTeal
            }
        )
    }
}

@Composable
fun DebtHeroCard(debts: List<DebtSummary>, hidden: Boolean) {
    val pending = debts.sumOf { it.pending }
    val paid = debts.sumOf { it.paid }
    val interest = debts.sumOf { it.interest }
    val total = (pending + paid).takeIf { it > 0.0 } ?: 1.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Exposicion financiera", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(money(pending, hidden), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text("Pendiente sobre deuda historica", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip("${((pending / total) * 100).toInt()}%", ContaDanger)
            }
            ProgressBar(progress = pending / total, color = Color(0xFFFF5A52))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                LightHeroMiniMetric("Pagado", money(paid, hidden), ContaTeal, Modifier.weight(1f))
                LightHeroMiniMetric("Intereses", money(interest, hidden), ContaAmber, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun DebtDetailRow(debt: DebtDetail, hidden: Boolean, onClick: () -> Unit) {
    val statusColor = when (debt.status.lowercase()) {
        "overdue" -> Color(0xFFDC2626)
        "warning" -> Color(0xFFF59E0B)
        else -> Color(0xFF0F766E)
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_credit_card), contentDescription = null, tint = statusColor, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(debt.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${debt.entity} - ${nextPaymentText(debt)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                ProgressBar(
                    progress = debt.monthlyPayment / (debt.pendingAmount.takeIf { it > 0 } ?: 1.0),
                    color = statusColor
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(debt.pendingAmount, hidden), fontWeight = FontWeight.Bold)
                Text(money(debt.monthlyPayment, hidden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun DebtDetailDialog(
    debt: DebtDetail,
    hidden: Boolean,
    onDismiss: () -> Unit
) {
    val monthsLeft = if (debt.monthlyPayment > 0.0) kotlin.math.ceil(debt.pendingAmount / debt.monthlyPayment).toInt() else null
    val monthlyRatio = debt.monthlyPayment / (debt.pendingAmount.takeIf { it > 0.0 } ?: 1.0)
    val statusColor = when {
        debt.status.equals("overdue", true) -> ContaDanger
        debt.status.equals("warning", true) -> ContaAmber
        else -> ContaTeal
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text(debt.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(50.dp).background(statusColor.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_credit_card), contentDescription = null, tint = statusColor, modifier = Modifier.size(26.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(debt.entity, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${debt.source} · ${debt.type}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                            StatusChip(debt.status.ifBlank { "Activo" }, statusColor)
                        }
                        Text("Pendiente", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(money(debt.pendingAmount, hidden), color = statusColor, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                        ProgressBar(progress = monthlyRatio.coerceIn(0.0, 1.0), color = statusColor)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            LightHeroMiniMetric("Meses est.", monthsLeft?.toString() ?: "N/D", ContaAmber, Modifier.weight(1f))
                            LightHeroMiniMetric("Cuota", money(debt.monthlyPayment, hidden), ContaTeal, Modifier.weight(1f))
                        }
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailLine("Proximo pago", nextPaymentText(debt))
                        DetailLine("Cuota mensual", money(debt.monthlyPayment, hidden))
                        DetailLine("Origen", debt.source)
                        DetailLine("Tipo", debt.type)
                    }
                }
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun nextPaymentText(debt: DebtDetail): String {
    val days = debt.daysUntilPayment
    val date = debt.nextPaymentDate?.take(10)
    return when {
        days == null && date == null -> "sin proximo pago"
        days == null -> date.orEmpty()
        days < 0 -> "vencido hace ${kotlin.math.abs(days)} dias"
        days == 0 -> "vence hoy"
        days == 1 -> "vence manana"
        else -> "$date en $days dias"
    }
}

@Composable
fun DebtRow(debt: DebtSummary, hidden: Boolean) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(debt.title, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Pendiente", money(debt.pending, hidden), Modifier.weight(1f))
                MetricCard("Pagado", money(debt.paid, hidden), Modifier.weight(1f))
            }
            ProgressBar(
                progress = debt.pending / ((debt.pending + debt.paid).takeIf { it > 0 } ?: 1.0),
                color = Color(0xFFFF5A52)
            )
            Text("Intereses: ${money(debt.interest, hidden)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}



