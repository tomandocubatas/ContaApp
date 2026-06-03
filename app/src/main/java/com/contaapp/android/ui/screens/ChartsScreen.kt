@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.contaapp.android.ContaUiState
import com.contaapp.android.R
import com.contaapp.android.core.settings.AppPreferencesStore
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.domain.CategoryBreakdown

@Composable
fun ChartsScreen(state: ContaUiState, onChartFiltersChanged: (MovementFilters) -> Unit) {
    val context = LocalContext.current
    val preferencesStore = remember(context) { AppPreferencesStore(context) }
    var range by remember {
        mutableStateOf(
            runCatching { QuickRange.valueOf(preferencesStore.getString("chartsPeriod", QuickRange.ThisMonth.name)) }
                .getOrDefault(QuickRange.ThisMonth)
        )
    }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var chartMode by remember { mutableStateOf(FinancialChartMode.Accumulated) }
    var section by remember {
        mutableStateOf(
            runCatching { ChartSection.valueOf(preferencesStore.getString("chartsSection", ChartSection.Summary.name)) }
                .getOrDefault(ChartSection.Summary)
        )
    }

    LaunchedEffect(range, dateFrom, dateTo) {
        val dates = range.toDates()
        onChartFiltersChanged(
            MovementFilters(
                dateFrom = dateFrom.takeIf { it.isNotBlank() } ?: dates?.first,
                dateTo = dateTo.takeIf { it.isNotBlank() } ?: dates?.second
            )
        )
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp
        val charts = state.charts
        val financial = financialChart(charts.financialSeries, chartMode)
        val dashboard = state.dashboard

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Graficas", "evolucion economica") }
            item {
                ChartPeriodPanel(
                    range = range,
                    onRange = {
                        range = it
                        preferencesStore.setString("chartsPeriod", it.name)
                    },
                    dateFrom = dateFrom,
                    onDateFrom = { dateFrom = it },
                    dateTo = dateTo,
                    onDateTo = { dateTo = it },
                    chartMode = chartMode,
                    onChartMode = { chartMode = it }
                )
            }
            item {
                ChartSectionSelector(
                    section = section,
                    onSection = {
                        section = it
                        preferencesStore.setString("chartsSection", it.name)
                    }
                )
            }
            item {
                ChartsSummaryCard(state)
            }
            when (section) {
                ChartSection.Summary -> {
                    item {
                        FinancialEvolutionDashboardCard(
                            financial = financial,
                            chartMode = chartMode,
                            granularity = charts.granularity
                        )
                    }
                    item {
                        QuickDistributionDashboardCard(dashboard, state.privacyMode)
                    }
                }
                ChartSection.Categories -> {
                    item {
                        CategoryPeriodDashboardCard(
                            expenses = dashboard?.expenseCategories.orEmpty(),
                            income = dashboard?.incomeCategories.orEmpty(),
                            hidden = state.privacyMode
                        )
                    }
                    item {
                        if (isTablet) {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                                CategoryRankingCard("Top gastos", dashboard?.expenseCategories.orEmpty(), state.privacyMode, ContaCoral, Modifier.weight(1f))
                                CategoryRankingCard("Top ingresos", dashboard?.incomeCategories.orEmpty(), state.privacyMode, ContaTeal, Modifier.weight(1f))
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                CategoryRankingCard("Top gastos", dashboard?.expenseCategories.orEmpty(), state.privacyMode, ContaCoral)
                                CategoryRankingCard("Top ingresos", dashboard?.incomeCategories.orEmpty(), state.privacyMode, ContaTeal)
                            }
                        }
                    }
                    item {
                        HistoricalCategoriesDashboardCard(
                            incomeValues = charts.incomeTrend,
                            expenseValues = charts.expenseTrend,
                            hidden = state.privacyMode
                        )
                    }
                }
                ChartSection.Trends -> {
                    item {
                        ChartOrEmpty(
                            title = "Balance en cuentas",
                            subtitle = "evolucion del patrimonio bancario",
                            values = charts.balanceTrend.map { it.value },
                            labels = charts.balanceTrend.map { it.label.toAxisLabel() },
                            color = ExecutiveBlue
                        )
                    }
                    item {
                        HistoricalCategoriesDashboardCard(
                            incomeValues = charts.incomeTrend,
                            expenseValues = charts.expenseTrend,
                            hidden = state.privacyMode
                        )
                    }
                }
                ChartSection.Debt -> {
                    item {
                        ChartOrEmpty(
                            title = "Evolucion de deuda",
                            subtitle = "capital pendiente historico",
                            values = charts.debtTrend.map { it.value },
                            labels = charts.debtTrend.map { it.label.toAxisLabel() },
                            color = ContaAmber
                        )
                    }
                    item { DebtSnapshotChartCard(state) }
                }
                ChartSection.Rentals -> {
                    item {
                        ChartOrEmpty(
                            title = "Cashflow de alquileres",
                            subtitle = "rentabilidad por periodo",
                            values = charts.rentalCashflow.map { it.value },
                            labels = charts.rentalCashflow.map { it.label.toAxisLabel() },
                            color = ContaTeal
                        )
                    }
                    item { RentalsSnapshotChartCard(state) }
                }
            }
        }
    }
}

enum class ChartSection(val label: String) {
    Summary("Resumen"),
    Categories("Categorias"),
    Trends("Tendencias"),
    Debt("Deuda"),
    Rentals("Alquileres")
}

@Composable
fun ChartSectionSelector(section: ChartSection, onSection: (ChartSection) -> Unit) {
    FilterPanel {
        CompactFilterSection(R.drawable.ic_chart, "Vista de analisis") {
            ChartSection.entries.forEach { option ->
                FilterButton(option.label, section == option) { onSection(option) }
            }
        }
    }
}

@Composable
fun DebtSnapshotChartCard(state: ContaUiState) {
    val visibleDebt = state.debtDetails.visibleDebtDetails()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChartCardHeader("Detalle de deuda", "prestamos y financiaciones")
            visibleDebt.take(5).forEach { debt ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(debt.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${debt.source} · ${debt.entity}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(money(debt.pendingAmount, state.privacyMode), color = ContaAmber, fontWeight = FontWeight.Bold)
                }
            }
            EmptyHint(visibleDebt.isEmpty(), "No hay detalle de deuda activa disponible.")
        }
    }
}

@Composable
fun RentalsSnapshotChartCard(state: ContaUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ChartCardHeader("Alquileres por propiedad", "rentabilidad y cashflow")
            state.rentalProperties.take(5).forEach { property ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(property.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Margen ${percent(property.profitability)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(money(property.profit, state.privacyMode), color = if (property.profit >= 0.0) ContaTeal else ContaDanger, fontWeight = FontWeight.Bold)
                }
            }
            EmptyHint(state.rentalProperties.isEmpty(), "No hay propiedades con actividad en el periodo.")
        }
    }
}

@Composable
fun FinancialEvolutionDashboardCard(
    financial: FinancialChartData,
    chartMode: FinancialChartMode,
    granularity: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartCardHeader("Evolucion financiera", "Ingresos, gastos y neto del periodo")
            Text(
                "Modo ${chartMode.label.lowercase()} · eje X ${if (granularity == "day") "dias" else "meses"}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium
            )
            InlineChartPanel("Neto", "Balance del periodo", financial.net, financial.labels, MaterialTheme.colorScheme.primary)
            InlineChartPanel("Ingresos", "Serie ${chartMode.label.lowercase()}", financial.income, financial.labels, ContaTeal)
            InlineChartPanel("Gastos", "Serie ${chartMode.label.lowercase()}", financial.expense, financial.labels, ContaCoral)
        }
    }
}

@Composable
fun QuickDistributionDashboardCard(dashboard: com.contaapp.android.domain.DashboardSummary?, hidden: Boolean) {
    val income = dashboard?.income ?: 0.0
    val expense = dashboard?.expense ?: 0.0
    val net = dashboard?.net ?: 0.0
    val total = (income + expense).takeIf { it > 0.0 } ?: 1.0
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartCardHeader("Distribucion rapida", "Resumen de ingresos y gastos")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight((income / total).toFloat().coerceAtLeast(0.08f)).background(ContaTeal, RoundedCornerShape(50)).size(height = 10.dp, width = 1.dp))
                Box(Modifier.weight((expense / total).toFloat().coerceAtLeast(0.08f)).background(ContaCoral, RoundedCornerShape(50)).size(height = 10.dp, width = 1.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PulseLine("Ingresos", money(income, hidden), ContaTeal, Modifier.weight(1f))
                PulseLine("Gastos", money(expense, hidden), ContaCoral, Modifier.weight(1f))
                PulseLine("Balance", money(net, hidden), if (net >= 0.0) ContaTeal else ContaDanger, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip("${dashboard?.expenseCategories?.size ?: 0} cat. gasto", ContaCoral, Modifier.weight(1f))
                StatusChip("${dashboard?.incomeCategories?.size ?: 0} cat. ingreso", ContaTeal, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CategoryPeriodDashboardCard(expenses: List<CategoryBreakdown>, income: List<CategoryBreakdown>, hidden: Boolean) {
    var type by remember { mutableStateOf("expense") }
    val categories = if (type == "expense") expenses else income
    val accent = if (type == "expense") ContaCoral else ContaTeal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartCardHeader("Categorias del periodo", "Distribucion por categoria")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton("Gastos", type == "expense") { type = "expense" }
                FilterButton("Ingresos", type == "income") { type = "income" }
            }
            CategoryDistributionBars(categories, hidden, accent)
        }
    }
}

@Composable
fun CategoryDistributionBars(categories: List<CategoryBreakdown>, hidden: Boolean, accent: androidx.compose.ui.graphics.Color) {
    val sorted = categories.sortedByDescending { kotlin.math.abs(it.total) }.take(8)
    val max = sorted.maxOfOrNull { kotlin.math.abs(it.total) }?.takeIf { it > 0.0 } ?: 1.0
    if (sorted.isEmpty()) {
        EmptyHint(true, "Sin categorias en este periodo.")
    } else {
        sorted.forEach { category ->
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(money(kotlin.math.abs(category.total), hidden), color = accent, fontWeight = FontWeight.Bold)
                }
                ProgressBar(kotlin.math.abs(category.total) / max, accent)
            }
        }
    }
}

@Composable
fun HistoricalCategoriesDashboardCard(
    incomeValues: List<com.contaapp.android.domain.ChartPoint>,
    expenseValues: List<com.contaapp.android.domain.ChartPoint>,
    hidden: Boolean
) {
    var type by remember { mutableStateOf("expense") }
    val points = if (type == "expense") expenseValues else incomeValues
    val accent = if (type == "expense") ContaCoral else ContaTeal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ChartCardHeader("Historico por categorias", "Evolucion mensual de categorias")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterButton("Gastos", type == "expense") { type = "expense" }
                FilterButton("Ingresos", type == "income") { type = "income" }
            }
            ChartOrEmpty(
                title = if (type == "expense") "Gastos por mes" else "Ingresos por mes",
                subtitle = "total mensual por categorias",
                values = points.map { it.value },
                labels = points.map { it.label.toAxisLabel() },
                color = accent
            )
            points.takeLast(6).reversed().forEach { point ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(point.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                    Text(money(point.value, hidden), color = accent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ChartPeriodPanel(
    range: QuickRange,
    onRange: (QuickRange) -> Unit,
    dateFrom: String,
    onDateFrom: (String) -> Unit,
    dateTo: String,
    onDateTo: (String) -> Unit,
    chartMode: FinancialChartMode,
    onChartMode: (FinancialChartMode) -> Unit
) {
    FilterPanel {
        PremiumPeriodSelector(
            selected = range,
            onSelected = onRange,
            title = "Periodo de graficas",
            detail = "El historico se recalcula con este rango"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = dateFrom, onValueChange = onDateFrom, label = { Text("Desde") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = dateTo, onValueChange = onDateTo, label = { Text("Hasta") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        CompactFilterSection(R.drawable.ic_chart, "Modo de grafica") {
            FinancialChartMode.entries.forEach { mode ->
                FilterButton(mode.label, chartMode == mode) { onChartMode(mode) }
            }
        }
    }
}

@Composable
fun ChartsSummaryCard(state: ContaUiState) {
    val dashboard = state.dashboard
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(48.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_receipt), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Panel de evolucion", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(
                    "Balance ${money(dashboard?.totalBalance ?: 0.0, state.privacyMode)} · Neto ${money(dashboard?.net ?: 0.0, state.privacyMode)}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun ChartCardHeader(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            title,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            subtitle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun InlineChartPanel(
    title: String,
    subtitle: String,
    values: List<Double>,
    labels: List<String>,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val cleanValues = values.map { if (it.isFinite()) it else 0.0 }
    var selectedIndex by remember(cleanValues, labels) { mutableStateOf(cleanValues.lastIndex.coerceAtLeast(0)) }
    val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f), RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                cleanValues.lastOrNull()?.let { money(it, false) } ?: "Sin datos",
                color = color,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (cleanValues.size >= 2) {
            val min = cleanValues.minOrNull() ?: 0.0
            val max = cleanValues.maxOrNull() ?: 0.0
            val safeIndex = selectedIndex.coerceIn(0, cleanValues.lastIndex)
            val selectedLabel = labels.getOrNull(safeIndex).orEmpty()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${cleanValues.size} puntos", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text("Rango ${money(min, false)} / ${money(max, false)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(
                Modifier.fillMaxWidth().background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Punto destacado", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(selectedLabel.ifBlank { "Indice ${safeIndex + 1}" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
                Text(money(cleanValues[safeIndex], false), color = color, fontWeight = FontWeight.Black)
            }
            ChartPointSelector(
                size = cleanValues.size,
                selected = safeIndex,
                onSelected = { selectedIndex = it }
            )
            Canvas(Modifier.fillMaxWidth().height(96.dp)) {
                repeat(3) { line ->
                    val y = size.height * (line + 1) / 4f
                    drawLine(gridColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }
                val range = (max - min).takeIf { it > 0.0 } ?: 1.0
                val step = size.width / (cleanValues.size - 1)
                cleanValues.zipWithNext().forEachIndexed { index, pair ->
                    val y1 = size.height - (((pair.first - min) / range).toFloat() * size.height)
                    val y2 = size.height - (((pair.second - min) / range).toFloat() * size.height)
                    drawLine(
                        color = color,
                        start = androidx.compose.ui.geometry.Offset(step * index, y1),
                        end = androidx.compose.ui.geometry.Offset(step * (index + 1), y2),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            AxisLabels(labels)
        } else if (cleanValues.size == 1) {
            Row(
                Modifier.fillMaxWidth().background(color.copy(alpha = 0.10f), RoundedCornerShape(14.dp)).padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Dato unico del periodo", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(labels.firstOrNull().orEmpty(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            EmptyHint(true, "No hay suficientes datos para esta grafica.")
        }
    }
}

@Composable
fun ChartPointSelector(size: Int, selected: Int, onSelected: (Int) -> Unit) {
    if (size < 2) return
    val options = listOf(0, size / 2, size - 1).distinct()
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        options.forEach { index ->
            val active = selected == index
            Box(
                Modifier
                    .weight(1f)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                        RoundedCornerShape(50)
                    )
                    .clickable { onSelected(index) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (index) {
                        0 -> "Inicio"
                        size - 1 -> "Fin"
                        else -> "Centro"
                    },
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Black else FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun AxisLabels(labels: List<String>) {
    if (labels.isEmpty()) return
    val first = labels.firstOrNull().orEmpty()
    val middle = labels.getOrNull(labels.size / 2).orEmpty()
    val last = labels.lastOrNull().orEmpty()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(first, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        if (middle != first && middle != last) {
            Text(middle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Text(last, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
fun ChartOrEmpty(title: String, subtitle: String, values: List<Double>, labels: List<String>, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    if (values.size >= 2) {
        FinancialLineChart(title = title, subtitle = subtitle, values = values, color = color, labels = labels, modifier = modifier)
    } else if (values.size == 1) {
        InlineChartPanel(title = title, subtitle = subtitle, values = values, labels = labels, color = color, modifier = modifier)
    } else {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            shape = RoundedCornerShape(18.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(title, subtitle)
                EmptyHint(true, "No hay suficientes puntos para dibujar esta evolucion.")
            }
        }
    }
}

enum class FinancialChartMode(val label: String) {
    Daily("Diario"),
    Accumulated("Acumulado"),
    Monthly("Mensual")
}

data class FinancialChartData(
    val labels: List<String>,
    val income: List<Double>,
    val expense: List<Double>,
    val net: List<Double>
)

fun financialChart(points: List<com.contaapp.android.domain.FinancialSeriesPoint>, mode: FinancialChartMode): FinancialChartData {
    if (points.isEmpty()) return FinancialChartData(emptyList(), emptyList(), emptyList(), emptyList())
    if (mode == FinancialChartMode.Daily) {
        return FinancialChartData(
            labels = points.map { it.label.toAxisLabel() },
            income = points.map { it.income },
            expense = points.map { it.expense },
            net = points.map { it.net }
        )
    }
    if (mode == FinancialChartMode.Accumulated) {
        var income = 0.0
        var expense = 0.0
        var net = 0.0
        return FinancialChartData(
            labels = points.map { it.label.toAxisLabel() },
            income = points.map { income += it.income; income },
            expense = points.map { expense += it.expense; expense },
            net = points.map { net += it.net; net }
        )
    }
    val buckets = linkedMapOf<String, Triple<Double, Double, Double>>()
    points.forEach { point ->
        val key = point.label.take(7)
        val current = buckets[key] ?: Triple(0.0, 0.0, 0.0)
        buckets[key] = Triple(current.first + point.income, current.second + point.expense, current.third + point.net)
    }
    return FinancialChartData(
        labels = buckets.keys.map { it.toAxisLabel() },
        income = buckets.values.map { it.first },
        expense = buckets.values.map { it.second },
        net = buckets.values.map { it.third }
    )
}

fun String.toAxisLabel(): String {
    return when {
        length >= 10 -> substring(5, 10)
        length >= 7 -> substring(0, 7)
        else -> this
    }
}
