@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
import com.contaapp.android.ui.theme.ContaSpacing
import com.contaapp.android.ui.theme.ContaRadius
import androidx.compose.ui.res.stringResource
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
fun InsuranceScreen(
    state: ContaUiState,
    focusInsuranceId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    var range by remember { mutableStateOf(InsuranceRange.All) }
    var selectedPolicy by remember { mutableStateOf<InsurancePolicy?>(null) }
    val visiblePolicies = state.expiringInsurances.visibleInsurancePolicies()
    val filteredPolicies = visiblePolicies.filter { policy ->
        val matchesText = query.isBlank() ||
            policy.name.contains(query, ignoreCase = true) ||
            (policy.company ?: "").contains(query, ignoreCase = true) ||
            policy.type.contains(query, ignoreCase = true)
        val matchesRange = range.matches(policy.renewalDate ?: policy.endDate)
        matchesText && matchesRange
    }.sortedBy { it.renewalDate ?: it.endDate ?: "9999-12-31" }

    LaunchedEffect(focusInsuranceId, visiblePolicies) {
        focusInsuranceId?.let { id ->
            selectedPolicy = visiblePolicies.firstOrNull { it.id == id }
            onFocusConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        LazyColumn(Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            item { SectionHeader(stringResource(R.string.insurance_seguros), "${filteredPolicies.size} polizas") }
            item {
                val insurance = state.insurance
                if (insurance != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
                        InsuranceHeroCard(
                            active = insurance.activeCount,
                            expiring = insurance.expiringSoon,
                            annualPremium = insurance.annualPremium,
                            hidden = state.privacyMode
                        )
                        InsuranceCalendarSummary(filteredPolicies, insurance.annualPremium, state.privacyMode)
                    }
                } else {
                    EmptyHint(true, "No hay resumen de seguros cargado todavia.")
                }
            }
            item {
                InsuranceCoverageCard(state.insuranceCoverages, state.privacyMode)
            }
            item {
                InsuranceRenewalCalendarCard(state.insuranceRenewals)
            }
            item {
                FilterPanel {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.insurance_buscar_seguro)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompactFilterSection(R.drawable.ic_shield, "Vencimiento") {
                        FilterButton(stringResource(R.string.insurance_todos), range == InsuranceRange.All) { range = InsuranceRange.All }
                        FilterButton(stringResource(R.string.insurance_vencidas), range == InsuranceRange.Overdue) { range = InsuranceRange.Overdue }
                        FilterButton(stringResource(R.string.insurance_30_dias), range == InsuranceRange.Days30) { range = InsuranceRange.Days30 }
                        FilterButton(stringResource(R.string.insurance_60_dias), range == InsuranceRange.Days60) { range = InsuranceRange.Days60 }
                    }
                }
            }
            if (state.isLoading && filteredPolicies.isEmpty()) {
                item { SkeletonCard(lines = 4) }
                item { SkeletonCard(lines = 4) }
            }
            item { SectionTitle(stringResource(R.string.insurance_detalle_de_seguros)) }
            items(filteredPolicies, key = { it.id.ifBlank { "${it.name}-${it.policyNumber.orEmpty()}" } }) { policy ->
                InsurancePolicyRow(policy, state.privacyMode) { selectedPolicy = policy }
            }
            item { EmptyHint(filteredPolicies.isEmpty(), "No hay polizas para los filtros actuales.") }
        }
    }

    selectedPolicy?.let { policy ->
        InsurancePolicyDetailDialog(
            policy = policy,
            hidden = state.privacyMode,
            onDismiss = { selectedPolicy = null }
        )
    }
}

enum class InsuranceRange(val days: Long?) {
    All(null),
    Overdue(0),
    Days30(30),
    Days60(60);

    fun matches(rawDate: String?): Boolean {
        val limit = days ?: return true
        val date = rawDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val today = LocalDate.now()
        return when (this) {
            Overdue -> date.isBefore(today)
            else -> !date.isBefore(today) && !date.isAfter(today.plusDays(limit))
        }
    }
}

@Composable
private fun insuranceStatusLabel(status: String?): String? = when (status?.lowercase()) {
    null, "" -> null
    "active" -> stringResource(R.string.insurance_status_active)
    "pending_renewal" -> stringResource(R.string.insurance_status_pending_renewal)
    "expired" -> stringResource(R.string.insurance_status_expired)
    "cancelled", "canceled" -> stringResource(R.string.insurance_status_cancelled)
    "inactive" -> stringResource(R.string.insurance_status_inactive)
    else -> status
}

@Composable
private fun insuranceTypeLabel(type: String): String = when (type.lowercase()) {
    "" -> stringResource(R.string.insurance_poliza_generica)
    "home" -> stringResource(R.string.insurance_type_home)
    "auto" -> stringResource(R.string.insurance_type_auto)
    "life" -> stringResource(R.string.insurance_type_life)
    "health" -> stringResource(R.string.insurance_type_health)
    "other" -> stringResource(R.string.insurance_type_other)
    else -> type
}

@Composable
private fun renewalChipText(urgency: InsuranceUrgency, date: String?): String {
    val shown = date?.take(10) ?: return stringResource(R.string.insurance_sin_fecha)
    return if (urgency == InsuranceUrgency.Expired) stringResource(R.string.insurance_vencio, shown)
    else stringResource(R.string.insurance_renueva, shown)
}

@Composable
fun InsuranceCoverageCard(coverages: List<com.contaapp.android.domain.InsuranceCoverage>, hidden: Boolean) {
    if (coverages.isEmpty()) return
    val sorted = coverages.sortedByDescending { it.maxCoverage }.take(4)
    val max = sorted.maxOfOrNull { it.maxCoverage }?.takeIf { it > 0.0 } ?: 1.0
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            SectionHeader(stringResource(R.string.insurance_coberturas), "comparativa por tipo")
            sorted.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(item.type, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stringResource(R.string.insurance_prima_ratio_x, money(item.totalPremium, hidden), item.ratio.toInt()), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(money(item.maxCoverage, hidden), color = ContaTeal, fontWeight = FontWeight.Black)
                    }
                    ProgressBar(item.maxCoverage / max, ContaTeal)
                }
            }
        }
    }
}

@Composable
fun InsuranceRenewalCalendarCard(renewals: List<com.contaapp.android.domain.InsuranceRenewal>) {
    val upcoming = renewals
        .filter { renewal ->
            val date = runCatching { LocalDate.parse(renewal.date.take(10)) }.getOrNull()
            date != null && !date.isBefore(LocalDate.now())
        }
        .sortedBy { it.date }
        .take(4)
    if (upcoming.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            SectionHeader(stringResource(R.string.insurance_calendario_de_renovaciones), "${upcoming.size} fechas")
            upcoming.forEach { renewal ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).background(ContaWarning.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(renewal.date.takeLast(2), color = ContaWarning, fontWeight = FontWeight.Black)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(renewal.date.take(10), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        Text(renewal.names.take(2).joinToString(", "), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    StatusChip("${renewal.count}", ContaWarning)
                }
            }
        }
    }
}

@Composable
fun InsuranceMonthlyCard(annualPremium: Double, hidden: Boolean) {
    AttentionCard(
        title = stringResource(R.string.insurance_coste_mensual_equivalente),
        value = money(annualPremium / 12.0, hidden),
        detail = "Calculado desde la prima anual total",
        color = ContaTeal
    )
}

@Composable
fun InsuranceCalendarSummary(policies: List<InsurancePolicy>, annualPremium: Double, hidden: Boolean) {
    val overdue = policies.count { InsuranceRange.Overdue.matches(it.renewalDate ?: it.endDate) }
    val next30 = policies.count { InsuranceRange.Days30.matches(it.renewalDate ?: it.endDate) }
    val next60 = policies.count { InsuranceRange.Days60.matches(it.renewalDate ?: it.endDate) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(ContaSpacing.md), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            MetricCard(stringResource(R.string.insurance_mensual), money(annualPremium / 12.0, hidden), Modifier.weight(1.4f))
            MetricCard(stringResource(R.string.insurance_vencidas), overdue.toString(), Modifier.weight(1f))
            MetricCard(stringResource(R.string.insurance_30_dias), next30.toString(), Modifier.weight(1f))
            MetricCard(stringResource(R.string.insurance_60_dias), next60.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun InsuranceHeroCard(active: Int, expiring: Int, annualPremium: Double, hidden: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17212B)),
        shape = RoundedCornerShape(ContaRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.insurance_prima_anual), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
                    Text(money(annualPremium, hidden), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(54.dp).background(Color(0xFFFF5A52), RoundedCornerShape(ContaRadius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(expiring.toString(), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md), modifier = Modifier.fillMaxWidth()) {
                DarkMiniMetric(stringResource(R.string.insurance_activos), active.toString(), Color(0xFF2DD4BF), Modifier.weight(1f))
                DarkMiniMetric(stringResource(R.string.insurance_por_renovar), expiring.toString(), Color(0xFFFBBF24), Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun InsurancePolicyRow(policy: InsurancePolicy, hidden: Boolean, onClick: () -> Unit) {
    val date = policy.renewalDate ?: policy.endDate
    val urgency = insuranceUrgency(date)
    val accent = when (urgency) {
        InsuranceUrgency.Expired -> ContaDanger
        InsuranceUrgency.Soon -> ContaWarning
        InsuranceUrgency.Future -> ContaTeal
        InsuranceUrgency.Unknown -> MaterialTheme.colorScheme.primary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(ContaRadius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(policy.name.ifBlank { "Seguro" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(policy.company ?: policy.type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    policy.policyNumber?.let {
                        Text(stringResource(R.string.insurance_poliza, it), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(policy.annualPremium, hidden), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    Text(insuranceStatusLabel(policy.status) ?: urgency.label, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
                StatusChip(insuranceTypeLabel(policy.type), accent, Modifier.weight(1f))
                StatusChip(renewalChipText(urgency, date), accent, Modifier.weight(1f))
            }
            policy.coverage?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.insurance_toca_para_ver_coberturas_prima_y_est), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.insurance_ver_detalle), color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun InsurancePolicyDetailDialog(policy: InsurancePolicy, hidden: Boolean, onDismiss: () -> Unit) {
    val date = policy.renewalDate ?: policy.endDate
    val urgency = insuranceUrgency(date)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.insurance_cerrar)) }
        },
        title = { Text(policy.name.ifBlank { "Detalle del seguro" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
                val accent = when (urgency) {
                    InsuranceUrgency.Expired -> ContaDanger
                    InsuranceUrgency.Soon -> ContaWarning
                    InsuranceUrgency.Future -> ContaTeal
                    InsuranceUrgency.Unknown -> MaterialTheme.colorScheme.primary
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(ContaRadius.xl)
                ) {
                    Column(Modifier.fillMaxWidth().padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(policy.company ?: "Compania no informada", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Text(money(policy.annualPremium, hidden), color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(ContaRadius.lg)), contentAlignment = Alignment.Center) {
                                Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = accent)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                            StatusChip(insuranceStatusLabel(policy.status) ?: urgency.label, accent)
                            StatusChip(renewalChipText(urgency, date), accent)
                        }
                    }
                }
                DetailLine(stringResource(R.string.insurance_compania), policy.company ?: "No informada")
                DetailLine(stringResource(R.string.insurance_tipo), policy.type.ifBlank { "No informado" })
                DetailLine(stringResource(R.string.insurance_numero_de_poliza), policy.policyNumber ?: "No informado")
                DetailLine(stringResource(R.string.insurance_estado_tecnico), insuranceStatusLabel(policy.status) ?: stringResource(R.string.insurance_no_informado))
                DetailLine(stringResource(R.string.insurance_prima_anual), money(policy.annualPremium, hidden))
                DetailLine(stringResource(R.string.insurance_prima_mensual), money(policy.annualPremium / 12.0, hidden))
                DetailLine(stringResource(R.string.insurance_periodicidad), policy.periodicity ?: "No informada")
                DetailLine(stringResource(R.string.insurance_renovacion), date?.take(10) ?: "Sin fecha")
                DetailLine(stringResource(R.string.insurance_estado), urgency.label)
                DetailLine(stringResource(R.string.insurance_cobertura), policy.coverage ?: "No informada")
                DetailLine(stringResource(R.string.insurance_identificador), policy.id.ifBlank { "No informado" })
            }
        },
        shape = RoundedCornerShape(ContaRadius.xl)
    )
}

enum class InsuranceUrgency(val label: String) {
    Expired("Vencido"),
    Soon("Revisar"),
    Future("Vigente"),
    Unknown("Sin fecha")
}

fun insuranceUrgency(rawDate: String?): InsuranceUrgency {
    val date = rawDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return InsuranceUrgency.Unknown
    val today = LocalDate.now()
    return when {
        date.isBefore(today) -> InsuranceUrgency.Expired
        !date.isAfter(today.plusDays(30)) -> InsuranceUrgency.Soon
        else -> InsuranceUrgency.Future
    }
}



