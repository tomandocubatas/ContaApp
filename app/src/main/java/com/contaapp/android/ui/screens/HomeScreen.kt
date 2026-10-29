@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
import com.contaapp.android.ui.theme.ContaCardDefaults
import com.contaapp.android.ui.theme.ContaSpacing
import com.contaapp.android.ui.theme.ContaRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.contaapp.android.ContaUiState
import com.contaapp.android.R
import com.contaapp.android.ui.theme.amountHero
import com.contaapp.android.ui.theme.amountMedium
import com.contaapp.android.core.settings.AppPreferencesStore
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CalendarEvent
import com.contaapp.android.domain.CategoryBreakdown
import com.contaapp.android.domain.DashboardSummary
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.Movement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
@Composable
fun HomeScreen(
    state: ContaUiState,
    onMovementFiltersChanged: (MovementFilters) -> Unit,
    onNavigate: (Tab) -> Unit = {}
) {
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }
    val context = LocalContext.current
    val preferencesStore = remember(context) { AppPreferencesStore(context) }
    var period by remember {
        mutableStateOf(
            runCatching { QuickRange.valueOf(preferencesStore.getString("homePeriod", QuickRange.ThisMonth.name)) }
                .getOrDefault(QuickRange.ThisMonth)
        )
    }
    LaunchedEffect(period) {
        val dates = period.toDates()
        onMovementFiltersChanged(MovementFilters(dateFrom = dates?.first, dateTo = dates?.second))
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp
        val dashboard = state.dashboard

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isTablet) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)
        ) {
            item {
                PremiumPeriodSelector(
                    selected = period,
                    onSelected = {
                        period = it
                        preferencesStore.setString("homePeriod", it.name)
                    },
                    title = stringResource(R.string.home_vista_financiera),
                    detail = "Cambia periodo sin salir del dashboard"
                )
            }
            item { HomeAttentionSection(state = state, isTablet = isTablet, onNavigate = onNavigate) }
            item {
                if (dashboard == null) {
                    if (state.isLoading) {
                        SkeletonCard(lines = 4)
                    } else {
                        WelcomeEmptyState(state.error)
                    }
                } else {
                    HomeHeroDashboard(dashboard, state.privacyMode)
                }
            }
            item { HomeShortcuts(onNavigate) }
            item {
                HomeQuickMetrics(dashboard, state.privacyMode, isTablet, state.debtDetails.visibleDebtDetails().sumOf { it.pendingAmount }, state.movements.count { !it.reviewed })
            }
            item {
                HomePeriodComparisonCard(state)
            }
            if (dashboard != null) {
                item {
                    FinancialLineChart(
                        title = stringResource(R.string.home_ingresos_vs_gastos),
                        subtitle = stringResource(R.string.home_tendencia_estimada),
                        values = executiveTrend(dashboard.income, dashboard.expense, dashboard.net),
                        color = ExecutiveBlue,
                        hidden = state.privacyMode
                    )
                }
            }
            item {
                if (isTablet && dashboard != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md), modifier = Modifier.fillMaxWidth()) {
                        FinancialPulseCard(dashboard, state.privacyMode, Modifier.weight(1f))
                        CategoryRankingCard(
                            title = stringResource(R.string.home_top_gastos),
                            categories = dashboard.expenseCategories,
                            hidden = state.privacyMode,
                            accent = ContaCoral,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    dashboard?.let {
                        FinancialPulseCard(it, state.privacyMode)
                    }
                }
            }
            if (!isTablet && dashboard != null) {
                item {
                    CategoryRankingCard(
                        title = stringResource(R.string.home_top_gastos),
                        categories = dashboard.expenseCategories,
                        hidden = state.privacyMode,
                        accent = ContaCoral
                    )
                }
            }
            item { HomeHealthCard(dashboard, state) }
            item {
                HomeAgendaCard(state)
            }
            item { SectionHeader(stringResource(R.string.home_actividad_reciente), "${state.movements.size} mov.") }
            items(
                state.movements.take(if (isTablet) 8 else 5),
                key = { it.id.ifBlank { "${it.date}-${it.description}-${it.amount}" } }
            ) { movement ->
                WalletMovementRow(
                    movement = movement,
                    hidden = state.privacyMode,
                    categories = state.categories,
                    accounts = state.accounts,
                    debts = state.debtDetails,
                    insurances = state.expiringInsurances,
                    onClick = { selectedMovement = movement }
                )
            }
        }
    }

    selectedMovement?.let { movement ->
        MovementDetailDialog(
            movement = movement,
            hidden = state.privacyMode,
            categories = state.categories,
            accounts = state.accounts,
            debts = state.debtDetails,
            insurances = state.expiringInsurances,
            onDismiss = { selectedMovement = null }
        )
    }
}

@Composable
fun HomeAgendaCard(state: ContaUiState) {
    val today = LocalDate.now().toString()
    val entries = state.calendarMonth?.days
        .orEmpty()
        .flatMap { it.events }
        .filter { it.date >= today }
        .sortedBy { it.date }
        .take(5)
        .map { it.toAgendaEntry() }
    if (entries.isEmpty()) return
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            SectionHeader(stringResource(R.string.home_agenda_financiera), "${entries.size} proximos hitos")
            entries.forEach { entry ->
                AgendaRow(entry, state.privacyMode)
            }
        }
    }
}

data class AgendaEntry(
    val title: String,
    val typeLabel: String,
    val amount: Double,
    val date: String,
    @DrawableRes val iconRes: Int,
    val accent: Color
)

@Composable
fun AgendaRow(entry: AgendaEntry, hidden: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).background(entry.accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(entry.iconRes), contentDescription = null, tint = entry.accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.title.privacySafeTitle(hidden), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.typeLabel} · ${money(entry.amount, hidden)}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        StatusChip(entry.date, entry.accent)
    }
}

@Composable
fun HomePeriodComparisonCard(state: ContaUiState) {
    val dashboard = state.dashboard ?: return
    val series = state.charts.financialSeries
    val periodLabel = if (series.size >= 2) {
        "${series.first().label.toAxisLabel()} -> ${series.last().label.toAxisLabel()}"
    } else {
        "Periodo actual"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            SectionHeader(stringResource(R.string.home_resumen_del_periodo), periodLabel)
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                ComparisonMetric(stringResource(R.string.home_ingresos), dashboard.income, ContaIncome, state.privacyMode, Modifier.weight(1f))
                ComparisonMetric(stringResource(R.string.home_gastos), dashboard.expense, ContaExpense, state.privacyMode, Modifier.weight(1f))
                ComparisonMetric(stringResource(R.string.home_balance), dashboard.net, if (dashboard.net >= 0.0) ContaIncome else ContaDanger, state.privacyMode, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CalendarEvent.toAgendaEntry(): AgendaEntry {
    val accent = when {
        direction == "income" -> ContaIncome
        type == "loan" || type == "financing" -> ExecutiveBlue
        type == "insurance" -> ContaAmber
        else -> ContaCoral
    }
    val icon = when (type) {
        "loan", "financing" -> R.drawable.ic_credit_card
        "insurance" -> R.drawable.ic_shield
        "recurring" -> R.drawable.ic_refresh
        else -> R.drawable.ic_calendar
    }
    val typeLabel = when (type) {
        "loan" -> "Prestamo"
        "financing" -> "Financiacion"
        "insurance" -> "Seguro"
        "recurring" -> "Recurrente"
        else -> "Evento"
    }
    val signed = if (direction == "income") kotlin.math.abs(amount) else -kotlin.math.abs(amount)
    return AgendaEntry(
        title = label,
        typeLabel = typeLabel,
        amount = signed,
        date = date.take(10),
        iconRes = icon,
        accent = accent
    )
}

private fun String.privacySafeTitle(hidden: Boolean): String {
    if (!hidden) return this
    return replace(Regex("""[-+]?\d{1,3}(?:[.\s]\d{3})*(?:,\d+)?\s*€|[-+]?\d+(?:[,.]\d+)?\s*€"""), "•••• €")
}

@Composable
fun ComparisonMetric(title: String, value: Double, color: Color, hidden: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier
            .background(color.copy(alpha = 0.10f), RoundedCornerShape(ContaRadius.md))
            .padding(ContaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        FitText(
            money(value, hidden),
            color = color,
            style = MaterialTheme.typography.amountMedium,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun HomePeriodCombo(period: QuickRange, onPeriod: (QuickRange) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = ContaSpacing.lg, vertical = ContaSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(38.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(ContaRadius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_credit_card), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(stringResource(R.string.home_vista_financiera), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(period.homeLabel(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            }
            Text(stringResource(R.string.home_v), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Black)
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                QuickRange.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.homeLabel()) },
                        onClick = {
                            onPeriod(option)
                            expanded = false
                        },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_credit_card), contentDescription = null) }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeHeroDashboard(dashboard: DashboardSummary, hidden: Boolean) {
    val netColor = if (dashboard.net >= 0.0) ContaIncome else ContaDanger
    val colors = MaterialTheme.colorScheme
    val heroBrush = Brush.horizontalGradient(listOf(colors.primaryContainer, colors.secondaryContainer))
    val heroText = colors.onSurface
    val heroMuted = colors.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(ContaRadius.xl),
        border = ContaCardDefaults.border(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(
                    heroBrush,
                    RoundedCornerShape(ContaRadius.xl)
                )
                .padding(ContaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(ContaSpacing.lg)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                    Text(stringResource(R.string.home_balance_disponible), color = heroMuted, style = MaterialTheme.typography.labelLarge)
                    Text(money(dashboard.totalBalance, hidden), color = heroText, style = MaterialTheme.typography.amountHero, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(stringResource(R.string.home_resumen_ejecutivo_del_periodo_actual), color = heroMuted, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(if (dashboard.net >= 0.0) "positivo" else "atencion", if (dashboard.net >= 0.0) ContaIncome else ContaWarning)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                LightHeroMiniMetric(stringResource(R.string.home_neto), money(dashboard.net, hidden), netColor, Modifier.weight(1f))
                LightHeroMiniMetric(stringResource(R.string.home_revisado), percent(dashboard.reviewedPct), ExecutiveBlue, Modifier.weight(1f))
                LightHeroMiniMetric(stringResource(R.string.home_categ), percent(dashboard.categorizedPct), ContaWarning, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun HeroMiniMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
        FitText(value, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
fun LightHeroMiniMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f), RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
        FitText(value, color = color, fontWeight = FontWeight.Black)
    }
}

@Composable
fun HomeQuickMetrics(dashboard: DashboardSummary?, hidden: Boolean, isTablet: Boolean, activeDebt: Double? = null, pendingReview: Int = 0) {
    val columns = if (isTablet) 4 else 2
    val metrics = listOf(
        ExecutiveMetricSpec("Ingresos", money(dashboard?.income ?: 0.0, hidden), "+ actividad", R.drawable.ic_bank, ContaIncome, listOf(2.0, 3.0, 3.4, 4.0, 4.8, 5.2)),
        ExecutiveMetricSpec("Gastos", money(dashboard?.expense ?: 0.0, hidden), "control mensual", R.drawable.ic_receipt, ContaExpense, listOf(3.0, 3.6, 3.1, 4.0, 3.8, 4.2)),
        ExecutiveMetricSpec("Deuda", money(activeDebt?.takeIf { it > 0.0 } ?: dashboard?.totalDebts ?: 0.0, hidden), "exposicion", R.drawable.ic_credit_card, ContaDebt, listOf(5.0, 4.8, 4.6, 4.4, 4.1, 3.9)),
        ExecutiveMetricSpec(stringResource(R.string.home_pendientes), pendingReview.toString(), stringResource(R.string.home_movimientos_por_revisar), R.drawable.ic_shield, ExecutiveBlue, listOf(1.0, 2.0, 2.8, 3.4, 4.2, 5.0))
    )
    Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
        metrics.chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                row.forEach { metric ->
                    ExecutiveKpiCard(
                        title = metric.title,
                        value = metric.value,
                        detail = metric.detail,
                        iconRes = metric.icon,
                        color = metric.color,
                        sparkValues = if (isTablet) metric.spark else emptyList(),
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

data class ExecutiveMetricSpec(
    val title: String,
    val value: String,
    val detail: String,
    @DrawableRes val icon: Int,
    val color: Color,
    val spark: List<Double>
)

@Composable
fun WalletMetricCard(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = modifier
    ) {
        Column(Modifier.padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Box(Modifier.size(28.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(9.dp).background(color, RoundedCornerShape(50)))
            }
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            FitText(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun FinancialPulseCard(dashboard: DashboardSummary, hidden: Boolean, modifier: Modifier = Modifier) {
    val activityTotal = (dashboard.income + dashboard.expense).takeIf { it > 0.0 } ?: 1.0
    val incomeProgress = dashboard.income / activityTotal
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            SectionHeader(stringResource(R.string.home_pulso_financiero), "periodo")
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(incomeProgress.toFloat().coerceAtLeast(0.08f)).height(10.dp).background(ContaIncome, RoundedCornerShape(50)))
                Box(Modifier.weight((1.0 - incomeProgress).toFloat().coerceAtLeast(0.08f)).height(10.dp).background(ContaExpense, RoundedCornerShape(50)))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md), modifier = Modifier.fillMaxWidth()) {
                PulseLine(stringResource(R.string.home_ingresos), money(dashboard.income, hidden), ContaIncome, Modifier.weight(1f))
                PulseLine(stringResource(R.string.home_gastos), money(dashboard.expense, hidden), ContaExpense, Modifier.weight(1f))
            }
            PulseLine(stringResource(R.string.home_neto_del_periodo), money(dashboard.net, hidden), if (dashboard.net >= 0.0) ContaIncome else ContaDanger)
        }
    }
}

@Composable
fun PulseLine(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        FitText(value, color = color, fontWeight = FontWeight.Bold)
    }
}

fun executiveTrend(income: Double, expense: Double, net: Double): List<Double> {
    val base = (income - expense).takeIf { it != 0.0 } ?: net.takeIf { it != 0.0 } ?: 1.0
    return listOf(0.12, 0.22, 0.34, 0.42, 0.57, 0.63, 0.71, 0.82, 0.92, 1.0).mapIndexed { index, factor ->
        base * factor + (income * 0.025 * (index % 3))
    }
}

fun QuickRange.homeLabel(): String {
    return when (this) {
        QuickRange.All -> "Todo el historico"
        QuickRange.Last30Days -> "Ultimos 30 dias"
        QuickRange.LastMonth -> "Mes pasado"
        QuickRange.ThisQuarter -> "Trimestre actual"
        QuickRange.ThisYear -> "Anio actual"
        QuickRange.LastYear -> "Anio pasado"
        QuickRange.ThisMonth -> "Mes actual"
    }
}

fun QuickRange.previousHomeRange(): QuickRange {
    return when (this) {
        QuickRange.ThisMonth -> QuickRange.LastMonth
        QuickRange.ThisQuarter -> QuickRange.LastMonth
        QuickRange.ThisYear -> QuickRange.ThisQuarter
        QuickRange.LastYear -> QuickRange.ThisYear
        QuickRange.LastMonth -> QuickRange.Last30Days
        QuickRange.Last30Days -> QuickRange.All
        QuickRange.All -> QuickRange.ThisMonth
    }
}

fun QuickRange.nextHomeRange(): QuickRange {
    return when (this) {
        QuickRange.All -> QuickRange.Last30Days
        QuickRange.Last30Days -> QuickRange.LastMonth
        QuickRange.LastMonth -> QuickRange.ThisQuarter
        QuickRange.ThisQuarter -> QuickRange.ThisYear
        QuickRange.ThisYear -> QuickRange.LastYear
        QuickRange.LastYear -> QuickRange.ThisMonth
        QuickRange.ThisMonth -> QuickRange.All
    }
}

@Composable
fun HomeHealthCard(dashboard: DashboardSummary?, state: ContaUiState, modifier: Modifier = Modifier) {
    val pendingMovements = state.movements.count { !it.reviewed }
    val urgentDebt = state.debtDetails.visibleDebtDetails().count { it.isDebtAlert() }
    val score = ((dashboard?.reviewedPct ?: 0.0) * 0.45 + (dashboard?.categorizedPct ?: 0.0) * 0.35 + if (urgentDebt == 0) 20.0 else 5.0).coerceIn(0.0, 100.0)
    val color = when {
        score >= 75.0 -> ContaTeal
        score >= 45.0 -> ContaWarning
        else -> ContaDanger
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.home_salud_financiera), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.home_pendientes_avisos, pendingMovements, urgentDebt), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                StatusChip(percent(score), color)
            }
            ProgressBar(progress = score / 100.0, color = color)
        }
    }
}

@Composable
fun HomeAttentionSection(state: ContaUiState, isTablet: Boolean, modifier: Modifier = Modifier, onNavigate: (Tab) -> Unit = {}) {
    val pendingMovements = state.movements.count { !it.reviewed }
    val urgentDebt = state.debtDetails.visibleDebtDetails().count { it.isDebtAlert() }
    val upcomingInsurance = state.expiringInsurances.visibleInsurancePolicies().count {
        val raw = it.renewalDate ?: it.endDate
        val date = raw?.take(10)?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }
        date != null && !date.isBefore(LocalDate.now()) && !date.isAfter(LocalDate.now().plusDays(30))
    }

    val alerts = buildList<@Composable (Modifier) -> Unit> {
        if (pendingMovements > 0) add { m -> AttentionCard(stringResource(R.string.home_movimientos_pendientes), pendingMovements.toString(), "Revisa clasificacion y estado", ContaWarning, m, onClick = { onNavigate(Tab.Movements) }) }
        if (urgentDebt > 0) add { m -> AttentionCard(stringResource(R.string.home_deudas_con_aviso), urgentDebt.toString(), "Vencidas o proximas a pago", ContaDanger, m, onClick = { onNavigate(Tab.Debt) }) }
        if (upcomingInsurance > 0) add { m -> AttentionCard(stringResource(R.string.home_seguros_proximos), upcomingInsurance.toString(), "Renovaciones a 30 dias", ContaTeal, m, onClick = { onNavigate(Tab.Insurance) }) }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
        SectionHeader(stringResource(R.string.home_requiere_atencion), if (alerts.isEmpty()) "todo al dia" else "${alerts.size} avisos")
        if (alerts.isEmpty()) {
            AttentionCard("Sin avisos", "OK", "No hay movimientos pendientes, deudas con aviso ni seguros proximos", ContaTeal)
        } else if (isTablet) {
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md), modifier = Modifier.fillMaxWidth()) {
                alerts.forEach { alert -> alert(Modifier.weight(1f)) }
            }
        } else {
            alerts.forEach { alert -> alert(Modifier) }
        }
    }
}

/** Atajos a las pantallas de uso más frecuente, bajo el resumen. */
@Composable
fun HomeShortcuts(onNavigate: (Tab) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
        listOf(Tab.Movements to "Movimientos", Tab.Accounts to "Cuentas", Tab.Agenda to "Agenda", Tab.Charts to "Graficas").forEach { (tab, label) ->
            OutlinedButton(
                onClick = { onNavigate(tab) },
                modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = ContaSpacing.xs)
            ) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun WelcomeEmptyState(error: String?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(ContaRadius.md),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.home_preparando_tus_datos), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Cuando la API responda, aqui veras el balance, ingresos, gastos y movimientos recientes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



