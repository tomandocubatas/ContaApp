@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.compose.foundation.background
import com.contaapp.android.ui.theme.ContaRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.domain.RentalProperty
import java.time.LocalDate

@Composable
fun RentalsScreen(
    state: ContaUiState,
    onRentalsFiltersChanged: (MovementFilters) -> Unit,
    focusPropertyId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var year by remember { mutableIntStateOf(LocalDate.now().year) }
    var selectedProperty by remember { mutableStateOf<RentalProperty?>(null) }

    LaunchedEffect(year) {
        onRentalsFiltersChanged(MovementFilters(dateFrom = "$year-01-01", dateTo = "$year-12-31"))
    }

    LaunchedEffect(focusPropertyId, state.rentalProperties) {
        focusPropertyId?.let { id ->
            selectedProperty = state.rentalProperties.firstOrNull { it.id == id }
            onFocusConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp
        val summary = state.rentalSummary
        val cashflowPoints = state.charts.rentalCashflow
        val cashflow = cashflowPoints.map { it.value }
        val cashflowLabels = cashflowPoints.map { it.label.toAxisLabel() }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader(stringResource(R.string.rentals_alquileres), "${summary?.activeProperties ?: state.rentalProperties.size} activos") }
            item {
                RentalYearPanel(year = year, onYear = { year = it })
            }
            item {
                if (summary == null) {
                    EmptyHint(true, "No hay informacion de alquileres disponible.")
                } else {
                    RentalHeroCard(
                        income = summary.income,
                        expense = summary.expense,
                        profit = summary.profit,
                        profitability = summary.profitability,
                        activeProperties = summary.activeProperties,
                        hidden = state.privacyMode
                    )
                }
            }
            if (cashflow.isNotEmpty()) {
                item {
                    FinancialLineChart(
                        title = stringResource(R.string.rentals_cashflow_de_alquileres),
                        subtitle = stringResource(R.string.rentals_evolucion_por_mes),
                        values = cashflow,
                        color = ContaTeal,
                        labels = cashflowLabels,
                        xAxisLabel = "Meses",
                        yAxisLabel = "Cashflow",
                        hidden = state.privacyMode
                    )
                }
            }
            item {
                RentalMonthsCard(state.rentalMonths, state.privacyMode)
            }
            item {
                SectionHeader(stringResource(R.string.rentals_propiedades), "${state.rentalProperties.size} con actividad")
            }
            if (state.isLoading && state.rentalProperties.isEmpty()) {
                item { SkeletonCard(lines = 4) }
                item { SkeletonCard(lines = 4) }
            }
            items(state.rentalProperties.take(12)) { property ->
                RentalPropertyRow(property, state.privacyMode) { selectedProperty = property }
            }
            item {
                EmptyHint(state.rentalProperties.isEmpty(), "No hay propiedades con movimientos en el periodo.")
            }
        }
    }

    selectedProperty?.let { property ->
        RentalPropertyDetailDialog(
            property = property,
            months = state.rentalMonths,
            hidden = state.privacyMode,
            onDismiss = { selectedProperty = null }
        )
    }
}

@Composable
fun RentalYearPanel(year: Int, onYear: (Int) -> Unit) {
    val current = LocalDate.now().year
    FilterPanel {
        CompactFilterSection(R.drawable.ic_home, "Ejercicio") {
            (current downTo current - 4).forEach { option ->
                FilterButton(option.toString(), year == option) { onYear(option) }
            }
        }
    }
}

@Composable
fun RentalHeroCard(
    income: Double,
    expense: Double,
    profit: Double,
    profitability: Double,
    activeProperties: Int,
    hidden: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.rentals_rentabilidad_neta), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(money(profit, hidden), color = if (profit >= 0.0) ContaIncome else ContaDanger, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.rentals_de_margen_propiedades_activas, percent(profitability), activeProperties), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Box(
                    Modifier.size(52.dp).background(ContaTeal.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.lg)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_home), contentDescription = null, tint = ContaTeal)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PulseLine(stringResource(R.string.rentals_ingresos), money(income, hidden), ContaIncome, Modifier.weight(1f))
                PulseLine(stringResource(R.string.rentals_gastos), money(expense, hidden), ContaExpense, Modifier.weight(1f))
            }
            ProgressBar(
                progress = income.takeIf { it > 0.0 }?.let { (profit / it).coerceIn(0.0, 1.0) } ?: 0.0,
                color = if (profit >= 0.0) ContaIncome else ContaDanger
            )
        }
    }
}

@Composable
fun RentalPropertyRow(property: RentalProperty, hidden: Boolean, onClick: () -> Unit) {
    val accent = if (property.profit >= 0.0) ContaIncome else ContaDanger
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(44.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_home), contentDescription = null, tint = accent)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(property.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "Ingresos ${money(property.income, hidden)} · Gastos ${money(property.expense, hidden)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(property.profit, hidden), color = accent, fontWeight = FontWeight.Black)
                Text(percent(property.profitability), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun RentalPropertyDetailDialog(
    property: RentalProperty,
    months: List<com.contaapp.android.domain.RentalMonth>,
    hidden: Boolean,
    onDismiss: () -> Unit
) {
    val accent = if (property.profit >= 0.0) ContaIncome else ContaDanger
    val activeMonths = months.filter { it.income > 0.0 || it.expense > 0.0 }
    val bestMonth = activeMonths.maxByOrNull { it.profit }
    val worstMonth = activeMonths.minByOrNull { it.profit }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.rentals_cerrar)) }
        },
        title = { Text(property.name.ifBlank { "Propiedad" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)),
                    shape = RoundedCornerShape(ContaRadius.xl)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(50.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(ContaRadius.lg)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_home), contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                            }
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.rentals_cashflow_anual), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Text(money(property.profit, hidden), color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                            }
                            StatusChip(percent(property.profitability), accent)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            LightHeroMiniMetric(stringResource(R.string.rentals_ingresos), money(property.income, hidden), ContaIncome, Modifier.weight(1f))
                            LightHeroMiniMetric(stringResource(R.string.rentals_gastos), money(property.expense, hidden), ContaExpense, Modifier.weight(1f))
                        }
                        ProgressBar(progress = property.income.takeIf { it > 0.0 }?.let { (property.profit / it).coerceIn(0.0, 1.0) } ?: 0.0, color = accent)
                    }
                }
                if (bestMonth != null || worstMonth != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(ContaRadius.lg)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            bestMonth?.let { DetailLine(stringResource(R.string.rentals_mejor_mes), "${it.monthName} · ${money(it.profit, hidden)}") }
                            worstMonth?.let { DetailLine(stringResource(R.string.rentals_mes_mas_flojo), "${it.monthName} · ${money(it.profit, hidden)}") }
                        }
                    }
                }
                if (activeMonths.isNotEmpty()) {
                    Text(stringResource(R.string.rentals_detalle_mensual), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    activeMonths.take(6).forEach { month ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(ContaRadius.md),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(month.monthName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                                    Text(money(month.profit, hidden), color = if (month.profit >= 0.0) ContaIncome else ContaDanger, fontWeight = FontWeight.Black)
                                }
                                Text(
                                    "Ingresos ${money(month.income, hidden)} · gastos ${money(month.expense, hidden)}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelMedium
                                )
                                month.categories.take(3).forEach { category ->
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(category.categoryName, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(money(category.total, hidden), color = ContaExpense, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        shape = RoundedCornerShape(ContaRadius.xl)
    )
}
@Composable
fun RentalMonthsCard(months: List<com.contaapp.android.domain.RentalMonth>, hidden: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(stringResource(R.string.rentals_detalle_mensual), "${months.count { it.income > 0.0 || it.expense > 0.0 }} meses activos")
            if (months.isEmpty()) {
                Text(stringResource(R.string.rentals_no_hay_detalle_mensual_para_este_eje), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                months.forEach { month ->
                    RentalMonthRow(month, hidden)
                }
            }
        }
    }
}

@Composable
fun RentalMonthRow(month: com.contaapp.android.domain.RentalMonth, hidden: Boolean) {
    val accent = if (month.profit >= 0.0) ContaIncome else ContaDanger
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(month.monthName, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(
                    "Ingresos ${money(month.income, hidden)} · Gastos ${money(month.expense, hidden)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(money(month.profit, hidden), color = accent, fontWeight = FontWeight.Black)
                Text(percent(month.profitability), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
        }
        ProgressBar(progress = month.income.takeIf { it > 0.0 }?.let { (month.profit / it).coerceIn(0.0, 1.0) } ?: 0.0, color = accent)
        month.categories.take(4).forEach { category ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    category.categoryName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(money(category.total, hidden), color = ContaExpense, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

