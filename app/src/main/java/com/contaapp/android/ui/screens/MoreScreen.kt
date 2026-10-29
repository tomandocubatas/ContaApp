package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.contaapp.android.ContaUiState
import com.contaapp.android.R

@Composable
fun MoreScreen(
    state: ContaUiState,
    onOpen: (Tab) -> Unit,
    onOpenResult: (MoreSearchResult) -> Unit = { onOpen(it.tab) }
) {
    var query by remember { mutableStateOf("") }
    val pendingMovements = state.movements.count { !it.reviewed }
    val debtWarnings = state.debtDetails.visibleDebtDetails().count { it.isDebtAlert() }
    val insuranceWarnings = state.expiringInsurances.visibleInsurancePolicies().count { insuranceUrgency(it.renewalDate ?: it.endDate) != InsuranceUrgency.Future }
    val searchResults = remember(query, state) { globalSearchResults(query, state) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { SectionHeader(stringResource(R.string.more_mas_opciones), "navegacion completa") }
        item {
            MoreExecutiveCard(
                balance = money(state.dashboard?.totalBalance ?: 0.0, state.privacyMode),
                pending = pendingMovements,
                alerts = debtWarnings + insuranceWarnings
            )
        }
        item {
            GlobalSearchCard(
                query = query,
                onQuery = { query = it },
                resultCount = searchResults.size
            )
        }
        if (query.isNotBlank()) {
            items(searchResults.take(8)) { result ->
                MoreSearchResultRow(result = result, onOpen = onOpenResult)
            }
            item { EmptyHint(searchResults.isEmpty(), "No hay coincidencias en la cache actual.") }
        }
        item { SectionTitle(stringResource(R.string.more_analisis_y_operaciones)) }
        item { MoreRow(Tab.Charts, "Graficas por resumen, categorias, deuda y alquileres", onOpen) }
        item { MoreRow(Tab.Agenda, "Pagos previstos, recurrentes, seguros y confirmaciones", onOpen) }
        item { MoreRow(Tab.Accounts, "Detalle por banco, cuenta y saldo disponible", onOpen) }
        item { MoreRow(Tab.Movements, "Busqueda, filtros guardables y vinculaciones", onOpen) }
        item { MoreRow(Tab.Debt, "Prestamos, financiaciones y proximos pagos", onOpen) }
        item { MoreRow(Tab.Insurance, "Polizas, primas y renovaciones", onOpen) }
        item { MoreRow(Tab.Rentals, "Rentabilidad y cashflow por propiedad", onOpen) }
        item { SectionTitle(stringResource(R.string.more_sistema)) }
        item { MoreRow(Tab.Settings, "Servidor, privacidad, cache y sesion", onOpen) }
    }
}

@Composable
fun MoreExecutiveCard(balance: String, pending: Int, alerts: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(52.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_brand_mark), contentDescription = null, tint = androidx.compose.ui.graphics.Color.Unspecified, modifier = Modifier.size(42.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(stringResource(R.string.more_contaapp_executive), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.more_vista_financiera_protegida_y_cachead), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.more_balance), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(
                        balance,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MoreCompactMetric(stringResource(R.string.more_pendientes), pending.toString(), Modifier.weight(1f))
                MoreCompactMetric(stringResource(R.string.more_avisos), alerts.toString(), Modifier.weight(1f))
                MoreCompactMetric(stringResource(R.string.more_cache), "TTL", Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MoreCompactMetric(title: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, maxLines = 1)
        }
    }
}

@Composable
fun GlobalSearchCard(query: String, onQuery: (String) -> Unit, resultCount: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                leadingIcon = { Icon(painterResource(R.drawable.ic_menu), contentDescription = null) },
                label = { Text(stringResource(R.string.more_buscar_en_toda_la_cache)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                if (query.isBlank()) "Movimientos, cuentas, deuda, seguros y alquileres" else "$resultCount coincidencias",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun MoreSearchResultRow(result: MoreSearchResult, onOpen: (MoreSearchResult) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen(result) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(result.tab.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(result.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(result.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(result.type, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}

data class MoreSearchResult(
    val title: String,
    val detail: String,
    val type: String,
    val tab: Tab,
    val targetId: String? = null
)

fun globalSearchResults(query: String, state: ContaUiState): List<MoreSearchResult> {
    val term = query.trim()
    if (term.isBlank()) return emptyList()
    fun String?.matchesTerm() = this?.contains(term, ignoreCase = true) == true
    return buildList {
        state.movements.filter {
            it.description.matchesTerm() || it.categoryName.matchesTerm() || it.accountName.matchesTerm()
        }.take(5).forEach {
            add(MoreSearchResult(it.description, "${it.date.take(10)} · ${it.categoryName ?: it.type} · ${money(it.amount, state.privacyMode)}", "Mov", Tab.Movements, it.id))
        }
        state.accounts.filter {
            it.accountName.matchesTerm() || it.bankName.matchesTerm()
        }.take(4).forEach {
            add(MoreSearchResult(it.accountName, "${it.bankName} · ${money(it.balance, state.privacyMode)}", "Cuenta", Tab.Accounts, it.id))
        }
        state.debtDetails.visibleDebtDetails().filter {
            it.name.matchesTerm() || it.entity.matchesTerm() || it.type.matchesTerm()
        }.take(4).forEach {
            add(MoreSearchResult(it.name, "${it.source} · ${it.entity} · ${money(it.pendingAmount, state.privacyMode)}", "Deuda", Tab.Debt, it.id))
        }
        state.expiringInsurances.visibleInsurancePolicies().filter {
            it.name.matchesTerm() || it.company.matchesTerm() || it.type.matchesTerm()
        }.take(4).forEach {
            add(MoreSearchResult(it.name.ifBlank { "Seguro" }, "${it.company ?: it.type} · ${money(it.annualPremium, state.privacyMode)}", "Seguro", Tab.Insurance, it.id))
        }
        state.rentalProperties.filter {
            it.name.matchesTerm()
        }.take(4).forEach {
            add(MoreSearchResult(it.name, "Cashflow ${money(it.profit, state.privacyMode)} · ${percent(it.profitability)}", "Alquiler", Tab.Rentals, it.id))
        }
        state.calendarMonth?.days.orEmpty()
            .flatMap { it.events }
            .filter { it.label.matchesTerm() || it.type.matchesTerm() }
            .take(4)
            .forEach {
                add(MoreSearchResult(it.label, "${it.date.take(10)} · ${money(if (it.direction == "income") kotlin.math.abs(it.amount) else -kotlin.math.abs(it.amount), state.privacyMode)}", "Agenda", Tab.Agenda, it.id))
            }
    }
}

@Composable
fun MoreRow(tab: Tab, detail: String, onOpen: (Tab) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onOpen(tab) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(tab.iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tab.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(">", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
    }
}

