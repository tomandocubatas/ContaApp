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
fun InsuranceScreen(state: ContaUiState) {
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        LazyColumn(Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SectionHeader("Seguros", "${filteredPolicies.size} polizas") }
            item {
                val insurance = state.insurance
                if (insurance != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                FilterPanel {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Buscar seguro") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompactFilterSection(R.drawable.ic_shield, "Vencimiento") {
                        FilterButton("Todos", range == InsuranceRange.All) { range = InsuranceRange.All }
                        FilterButton("30 dias", range == InsuranceRange.Days30) { range = InsuranceRange.Days30 }
                        FilterButton("60 dias", range == InsuranceRange.Days60) { range = InsuranceRange.Days60 }
                    }
                }
            }
            item { SectionTitle("Detalle de seguros") }
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
    Days30(30),
    Days60(60);

    fun matches(rawDate: String?): Boolean {
        val limit = days ?: return true
        val date = rawDate?.take(10)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return false
        val today = LocalDate.now()
        return !date.isBefore(today) && !date.isAfter(today.plusDays(limit))
    }
}

@Composable
fun InsuranceMonthlyCard(annualPremium: Double, hidden: Boolean) {
    AttentionCard(
        title = "Coste mensual equivalente",
        value = money(annualPremium / 12.0, hidden),
        detail = "Calculado desde la prima anual total",
        color = ContaTeal
    )
}

@Composable
fun InsuranceCalendarSummary(policies: List<InsurancePolicy>, annualPremium: Double, hidden: Boolean) {
    val next30 = policies.count { InsuranceRange.Days30.matches(it.renewalDate ?: it.endDate) }
    val next60 = policies.count { InsuranceRange.Days60.matches(it.renewalDate ?: it.endDate) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MetricCard("Mensual", money(annualPremium / 12.0, hidden), Modifier.weight(1f))
            MetricCard("30 dias", next30.toString(), Modifier.weight(1f))
            MetricCard("60 dias", next60.toString(), Modifier.weight(1f))
        }
    }
}

@Composable
fun InsuranceHeroCard(active: Int, expiring: Int, annualPremium: Double, hidden: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17212B)),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Prima anual", color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
                    Text(money(annualPremium, hidden), color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier.size(54.dp).background(Color(0xFFFF5A52), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(expiring.toString(), color = Color.White, fontWeight = FontWeight.Black)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                DarkMiniMetric("Activos", active.toString(), Color(0xFF2DD4BF), Modifier.weight(1f))
                DarkMiniMetric("Vencen pronto", expiring.toString(), Color(0xFFFBBF24), Modifier.weight(1f))
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
        InsuranceUrgency.Soon -> ContaAmber
        InsuranceUrgency.Future -> ContaTeal
        InsuranceUrgency.Unknown -> MaterialTheme.colorScheme.primary
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(44.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(policy.name.ifBlank { "Seguro" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(policy.company ?: policy.type, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    policy.policyNumber?.let {
                        Text("Poliza $it", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(money(policy.annualPremium, hidden), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    Text(policy.status ?: urgency.label, color = accent, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(policy.type.ifBlank { "Poliza" }, accent, Modifier.weight(1f))
                StatusChip("Renueva ${(date ?: "sin fecha").take(10)}", accent, Modifier.weight(1f))
            }
            policy.coverage?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Toca para ver coberturas, prima y estado", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text("Ver detalle", color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
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
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        },
        title = { Text(policy.name.ifBlank { "Detalle del seguro" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                DetailLine("Compania", policy.company ?: "No informada")
                DetailLine("Tipo", policy.type.ifBlank { "No informado" })
                DetailLine("Numero de poliza", policy.policyNumber ?: "No informado")
                DetailLine("Estado tecnico", policy.status ?: "No informado")
                DetailLine("Prima anual", money(policy.annualPremium, hidden))
                DetailLine("Prima mensual", money(policy.annualPremium / 12.0, hidden))
                DetailLine("Periodicidad", policy.periodicity ?: "No informada")
                DetailLine("Renovacion", date?.take(10) ?: "Sin fecha")
                DetailLine("Estado", urgency.label)
                DetailLine("Cobertura", policy.coverage ?: "No informada")
                DetailLine("Identificador", policy.id.ifBlank { "No informado" })
            }
        },
        shape = RoundedCornerShape(18.dp)
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



