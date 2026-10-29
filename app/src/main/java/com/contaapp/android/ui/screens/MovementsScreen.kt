@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ModalBottomSheet
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
import com.contaapp.android.ui.theme.amountSmall
import com.contaapp.android.core.settings.AppPreferencesStore
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.data.MovementTypeGroups
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CategoryBreakdown
import com.contaapp.android.domain.CategoryOption
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.Movement
import java.time.LocalDate
import java.time.format.DateTimeFormatter
@Composable
fun MovementsScreen(
    state: ContaUiState,
    onMovementFiltersChanged: (MovementFilters) -> Unit,
    focusMovementId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesStore = remember(context) { AppPreferencesStore(context) }
    var text by remember { mutableStateOf("") }
    var type by remember { mutableStateOf<String?>(null) }
    var reviewed by remember { mutableStateOf<Boolean?>(null) }
    var range by remember { mutableStateOf(QuickRange.ThisMonth) }
    var groupMode by remember { mutableStateOf(MovementGroupMode.Date) }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }
    var onlyLinked by remember { mutableStateOf(false) }
    var onlyDuplicates by remember { mutableStateOf(false) }
    var savedFilterToken by remember { mutableStateOf(preferencesStore.getString("movementSavedFilter", "")) }
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }
    var showFilters by remember { mutableStateOf(false) }

    val selectedCategoryName = state.categories.firstOrNull { it.id == categoryId }?.name
    val selectedAccountName = state.accounts.firstOrNull { it.id == accountId }?.accountName
    val visibleMovements = state.movements
        .filter { movement -> !onlyLinked || movement.hasBusinessLink() }
        .filter { movement -> !onlyDuplicates || movement.isDuplicate }
    val pendingQuickActive = reviewed == false && range == QuickRange.ThisMonth && type == null
    val monthExpensesActive = type == MovementTypeGroups.Expense && reviewed == null && range == QuickRange.ThisMonth
    val monthIncomeActive = type == MovementTypeGroups.Income && reviewed == null && range == QuickRange.ThisMonth

    LaunchedEffect(text, type, reviewed, range, categoryId, accountId, dateFrom, dateTo, minAmount, maxAmount, state.refreshTick) {
        val dates = range.toDates()
        onMovementFiltersChanged(
            MovementFilters(
                text = text.trim(),
                type = type,
                reviewed = reviewed,
                dateFrom = dateFrom.takeIf { it.isNotBlank() } ?: dates?.first,
                dateTo = dateTo.takeIf { it.isNotBlank() } ?: dates?.second,
                accountId = accountId,
                categoryId = categoryId,
                minAmount = minAmount.toDoubleOrNull(),
                maxAmount = maxAmount.toDoubleOrNull()
            )
        )
    }

    LaunchedEffect(focusMovementId, state.movements) {
        focusMovementId?.let { id ->
            selectedMovement = state.movements.firstOrNull { it.id == id }
            onFocusConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        LazyColumn(
            Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { SectionHeader(stringResource(R.string.movements_movimientos_recientes), "${visibleMovements.size} resultados") }
            item {
                MovementsInsightCard(
                    movements = visibleMovements,
                    hidden = state.privacyMode
                )
            }
            item {
                MovementQuickActions(
                    onlyLinked = onlyLinked,
                    onOnlyLinked = { onlyLinked = !onlyLinked },
                    onlyDuplicates = onlyDuplicates,
                    onOnlyDuplicates = { onlyDuplicates = !onlyDuplicates },
                    pendingActive = pendingQuickActive,
                    onPending = {
                        if (pendingQuickActive) {
                            reviewed = null
                            range = QuickRange.All
                        } else {
                            type = null
                            reviewed = false
                            range = QuickRange.ThisMonth
                        }
                    },
                    monthExpensesActive = monthExpensesActive,
                    onMonthExpenses = {
                        if (monthExpensesActive) {
                            type = null
                            range = QuickRange.All
                        } else {
                            type = MovementTypeGroups.Expense
                            reviewed = null
                            range = QuickRange.ThisMonth
                        }
                    },
                    monthIncomeActive = monthIncomeActive,
                    onMonthIncome = {
                        if (monthIncomeActive) {
                            type = null
                            range = QuickRange.All
                        } else {
                            type = MovementTypeGroups.Income
                            reviewed = null
                            range = QuickRange.ThisMonth
                        }
                    },
                    hasSavedFilter = savedFilterToken.isNotBlank(),
                    onSaveFilter = {
                        savedFilterToken = encodeMovementSavedFilter(
                            MovementSavedFilter(
                                text = text,
                                type = type,
                                reviewed = reviewed,
                                range = range,
                                groupMode = groupMode,
                                categoryId = categoryId,
                                accountId = accountId,
                                dateFrom = dateFrom,
                                dateTo = dateTo,
                                minAmount = minAmount,
                                maxAmount = maxAmount,
                                onlyLinked = onlyLinked,
                                onlyDuplicates = onlyDuplicates
                            )
                        )
                        preferencesStore.setString("movementSavedFilter", savedFilterToken)
                    },
                    onRestoreFilter = {
                        decodeMovementSavedFilter(savedFilterToken)?.let { saved ->
                            text = saved.text
                            type = saved.type
                            reviewed = saved.reviewed
                            range = saved.range
                            groupMode = saved.groupMode
                            categoryId = saved.categoryId
                            accountId = saved.accountId
                            dateFrom = saved.dateFrom
                            dateTo = saved.dateTo
                            minAmount = saved.minAmount
                            maxAmount = saved.maxAmount
                            onlyLinked = saved.onlyLinked
                            onlyDuplicates = saved.onlyDuplicates
                        }
                    }
                )
            }
            item {
                ActiveFilterBar(
                    filters = movementFilterLabels(text, type, reviewed, range, selectedCategoryName, selectedAccountName, dateFrom, dateTo, minAmount, maxAmount, groupMode) +
                        buildList {
                            if (onlyLinked) add("Vinculados")
                            if (onlyDuplicates) add("Duplicados")
                        },
                    onOpenFilters = { showFilters = true }
                )
            }
            if (state.isMovementsLoading) {
                item { InlineLoading(stringResource(R.string.movements_actualizando_movimientos)) }
                if (state.movements.isEmpty()) {
                    item { SkeletonCard(lines = 5) }
                    item { SkeletonCard(lines = 4) }
                }
            }
            val groups = visibleMovements.groupBy {
                when (groupMode) {
                    MovementGroupMode.Date -> movementGroupLabel(it.date)
                    MovementGroupMode.Category -> it.categoryName ?: "Sin categoria"
                    MovementGroupMode.Account -> it.accountName ?: "Sin cuenta"
                }
            }
            groups.forEach { (label, movements) ->
                item { MovementGroupHeader(label, movements, state.privacyMode) }
                items(movements, key = { it.id.ifBlank { "${it.date}-${it.description}-${it.amount}" } }) { movement ->
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
            item {
                PremiumEmptyState(
                    show = state.movements.isEmpty() && !state.isMovementsLoading,
                    title = stringResource(R.string.movements_sin_movimientos_visibles),
                    detail = "Prueba a ampliar el periodo, limpiar filtros o refrescar desde la cabecera.",
                    iconRes = R.drawable.ic_receipt
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

    if (showFilters) {
        ModalBottomSheet(onDismissRequest = { showFilters = false }) {
            MovementFilterSheet(
                text = text,
                onText = { text = it },
                minAmount = minAmount,
                onMinAmount = { minAmount = it },
                maxAmount = maxAmount,
                onMaxAmount = { maxAmount = it },
                categories = state.categories,
                categoryId = categoryId,
                onCategoryId = { categoryId = it },
                accounts = state.accounts,
                accountId = accountId,
                onAccountId = { accountId = it },
                dateFrom = dateFrom,
                onDateFrom = { dateFrom = it },
                dateTo = dateTo,
                onDateTo = { dateTo = it },
                type = type,
                onType = { type = it },
                range = range,
                onRange = { range = it },
                reviewed = reviewed,
                onReviewed = { reviewed = it },
                groupMode = groupMode,
                onGroupMode = { groupMode = it },
                onClear = {
                    text = ""
                    type = null
                    reviewed = null
                    range = QuickRange.All
                    groupMode = MovementGroupMode.Date
                    categoryId = null
                    accountId = null
                    dateFrom = ""
                    dateTo = ""
                    minAmount = ""
                    maxAmount = ""
                    onlyLinked = false
                    onlyDuplicates = false
                },
                onApply = { showFilters = false }
            )
        }
    }
}

@Composable
fun MovementQuickActions(
    onlyLinked: Boolean,
    onOnlyLinked: () -> Unit,
    onlyDuplicates: Boolean,
    onOnlyDuplicates: () -> Unit,
    pendingActive: Boolean,
    onPending: () -> Unit,
    monthExpensesActive: Boolean,
    onMonthExpenses: () -> Unit,
    monthIncomeActive: Boolean,
    onMonthIncome: () -> Unit,
    hasSavedFilter: Boolean,
    onSaveFilter: () -> Unit,
    onRestoreFilter: () -> Unit
) {
    FilterPanel {
        CompactFilterSection(R.drawable.ic_receipt, "Accesos rapidos") {
            FilterButton(stringResource(R.string.movements_pendientes), pendingActive, onPending)
            FilterButton(stringResource(R.string.movements_gastos_mes), monthExpensesActive, onMonthExpenses)
            FilterButton(stringResource(R.string.movements_ingresos_mes), monthIncomeActive, onMonthIncome)
            FilterButton(stringResource(R.string.movements_vinculados), onlyLinked, onOnlyLinked)
            FilterButton(stringResource(R.string.movements_duplicados), onlyDuplicates, onOnlyDuplicates)
        }
        CompactFilterSection(R.drawable.ic_settings, "Vista guardada") {
            FilterButton(stringResource(R.string.movements_guardar_actual), false, onSaveFilter)
            FilterButton(stringResource(R.string.movements_restaurar), hasSavedFilter, onRestoreFilter)
        }
    }
}

enum class MovementGroupMode {
    Date,
    Category,
    Account
}

@Composable
fun MovementDateHeader(label: String, count: Int) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        StatusChip("$count", MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun MovementGroupHeader(label: String, movements: List<Movement>, hidden: Boolean) {
    val total = movements.sumOf { it.amount }
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.movements_movimientos, movements.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        StatusChip(money(total, hidden), if (total >= 0.0) ContaIncome else ContaDanger)
    }
}

@Composable
fun WalletMovementRow(
    movement: Movement,
    hidden: Boolean,
    categories: List<CategoryOption> = emptyList(),
    accounts: List<BankAccountBalance> = emptyList(),
    debts: List<DebtDetail> = emptyList(),
    insurances: List<InsurancePolicy> = emptyList(),
    onClick: () -> Unit
) {
    val category = movementCategory(movement, categories)
    val accent = categoryColor(category, if (movement.amount >= 0.0) ContaIncome else ContaDanger)
    val accountName = movementAccountName(movement, accounts)
    val link = movementLinkLabel(movement, debts, insurances)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.md)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(categoryIcon(category, movement)), contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(movement.description, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(movement.date.take(10), category?.name ?: movement.categoryName ?: movement.type, accountName).joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                link?.let {
                    Text(
                        "Vinculado: $it",
                        color = ExecutiveBlue,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(money(movement.amount, hidden), color = accent, style = MaterialTheme.typography.amountSmall, fontWeight = FontWeight.Black)
                if (!movement.reviewed) {
                    StatusChip(stringResource(R.string.movements_pend), ContaWarning)
                }
            }
        }
    }
}

@Composable
fun MovementFilterSheet(
    text: String,
    onText: (String) -> Unit,
    minAmount: String,
    onMinAmount: (String) -> Unit,
    maxAmount: String,
    onMaxAmount: (String) -> Unit,
    categories: List<CategoryOption>,
    categoryId: String?,
    onCategoryId: (String?) -> Unit,
    accounts: List<BankAccountBalance>,
    accountId: String?,
    onAccountId: (String?) -> Unit,
    dateFrom: String,
    onDateFrom: (String) -> Unit,
    dateTo: String,
    onDateTo: (String) -> Unit,
    type: String?,
    onType: (String?) -> Unit,
    range: QuickRange,
    onRange: (QuickRange) -> Unit,
    reviewed: Boolean?,
    onReviewed: (Boolean?) -> Unit,
    groupMode: MovementGroupMode,
    onGroupMode: (MovementGroupMode) -> Unit,
    onClear: () -> Unit,
    onApply: () -> Unit
) {
    val topCategories = categories.take(8)
    val topAccounts = accounts.take(8)
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 720.dp)
            .navigationBarsPadding()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(stringResource(R.string.movements_filtros_de_movimientos), "busqueda avanzada")
        Text(
            "Combina texto, fechas, cuenta, categoria, importe y agrupacion.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
        LazyColumn(
            modifier = Modifier.weight(1f, fill = false),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                OutlinedTextField(
                    value = text,
                    onValueChange = onText,
                    leadingIcon = { Icon(painterResource(R.drawable.ic_receipt), contentDescription = null) },
                    label = { Text(stringResource(R.string.movements_buscar_concepto_comercio_o_texto)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                CompactFilterSection(R.drawable.ic_credit_card, "Periodo") {
                    premiumPeriodOptions().forEach { option ->
                        FilterButton(
                            option.homeLabel()
                                .replace("Todo el historico", "Historico")
                                .replace("Mes actual", "Mes")
                                .replace("Trimestre actual", "Trimestre")
                                .replace("Anio actual", "Anio"),
                            range == option
                        ) { onRange(option) }
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = dateFrom, onValueChange = onDateFrom, label = { Text(stringResource(R.string.movements_desde)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = dateTo, onValueChange = onDateTo, label = { Text(stringResource(R.string.movements_hasta)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                CompactFilterSection(R.drawable.ic_receipt, "Tipo") {
                    FilterButton(stringResource(R.string.movements_todos), type == null) { onType(null) }
                    FilterButton(stringResource(R.string.movements_ingresos), type == MovementTypeGroups.Income) { onType(MovementTypeGroups.Income) }
                    FilterButton(stringResource(R.string.movements_gastos), type == MovementTypeGroups.Expense) { onType(MovementTypeGroups.Expense) }
                }
            }
            item {
                CompactFilterSection(R.drawable.ic_bank, "Cuenta") {
                    FilterButton(stringResource(R.string.movements_todas), accountId == null) { onAccountId(null) }
                    topAccounts.forEach { account ->
                        FilterButton(account.accountName.take(14), accountId == account.id) { onAccountId(account.id) }
                    }
                }
            }
            item {
                CompactFilterSection(R.drawable.ic_settings, "Categoria") {
                    FilterButton(stringResource(R.string.movements_todas), categoryId == null) { onCategoryId(null) }
                    topCategories.forEach { category ->
                        FilterButton(category.name.take(14), categoryId == category.id) { onCategoryId(category.id) }
                    }
                }
            }
            item {
                CompactFilterSection(R.drawable.ic_shield, "Revision") {
                    FilterButton(stringResource(R.string.movements_todas), reviewed == null) { onReviewed(null) }
                    FilterButton(stringResource(R.string.movements_pendientes), reviewed == false) { onReviewed(false) }
                    FilterButton(stringResource(R.string.movements_revisadas), reviewed == true) { onReviewed(true) }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = minAmount, onValueChange = onMinAmount, label = { Text(stringResource(R.string.movements_importe_min)) }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = maxAmount, onValueChange = onMaxAmount, label = { Text(stringResource(R.string.movements_importe_max)) }, singleLine = true, modifier = Modifier.weight(1f))
                }
            }
            item {
                CompactFilterSection(R.drawable.ic_bank, "Agrupar") {
                    FilterButton(stringResource(R.string.movements_fecha), groupMode == MovementGroupMode.Date) { onGroupMode(MovementGroupMode.Date) }
                    FilterButton(stringResource(R.string.movements_categoria), groupMode == MovementGroupMode.Category) { onGroupMode(MovementGroupMode.Category) }
                    FilterButton(stringResource(R.string.movements_cuenta), groupMode == MovementGroupMode.Account) { onGroupMode(MovementGroupMode.Account) }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            WalletActionButton(stringResource(R.string.movements_limpiar), onClear, Modifier.weight(1f))
            Button(onClick = onApply, shape = RoundedCornerShape(50), modifier = Modifier.weight(1f)) { Text(stringResource(R.string.movements_aplicar)) }
        }
    }
}

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
fun CompactFilterSection(@DrawableRes iconRes: Int, title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            content()
        }
    }
}

fun movementFilterLabels(
    text: String,
    type: String?,
    reviewed: Boolean?,
    range: QuickRange,
    categoryName: String?,
    accountName: String?,
    dateFrom: String,
    dateTo: String,
    minAmount: String,
    maxAmount: String,
    groupMode: MovementGroupMode
): List<String> = buildList {
    if (text.isNotBlank()) add("Texto")
    type?.let { add(if (it == MovementTypeGroups.Income || it == "ingreso") "Ingresos" else "Gastos") }
    if (reviewed != null) add(if (reviewed) "Revisadas" else "Pendientes")
    if (range != QuickRange.All) add(
        when (range) {
            QuickRange.Last30Days -> "30 dias"
            QuickRange.LastMonth -> "Mes pasado"
            QuickRange.ThisQuarter -> "Trimestre"
            QuickRange.ThisYear -> "Anio"
            QuickRange.LastYear -> "Anio pasado"
            QuickRange.ThisMonth -> "Mes"
            QuickRange.All -> "Todo"
        }
    )
    categoryName?.let { add("Categoria: $it") }
    accountName?.let { add("Cuenta: $it") }
    if (dateFrom.isNotBlank() || dateTo.isNotBlank()) add("Fechas")
    if (minAmount.isNotBlank() || maxAmount.isNotBlank()) add("Importe")
    if (groupMode != MovementGroupMode.Date) add("Agr. ${if (groupMode == MovementGroupMode.Category) "categoria" else "cuenta"}")
}

data class MovementSavedFilter(
    val text: String,
    val type: String?,
    val reviewed: Boolean?,
    val range: QuickRange,
    val groupMode: MovementGroupMode,
    val categoryId: String?,
    val accountId: String?,
    val dateFrom: String,
    val dateTo: String,
    val minAmount: String,
    val maxAmount: String,
    val onlyLinked: Boolean,
    val onlyDuplicates: Boolean
)

fun encodeMovementSavedFilter(filter: MovementSavedFilter): String {
    return listOf(
        filter.text,
        filter.type.orEmpty(),
        filter.reviewed?.toString().orEmpty(),
        filter.range.name,
        filter.groupMode.name,
        filter.categoryId.orEmpty(),
        filter.accountId.orEmpty(),
        filter.dateFrom,
        filter.dateTo,
        filter.minAmount,
        filter.maxAmount,
        filter.onlyLinked.toString(),
        filter.onlyDuplicates.toString()
    ).joinToString("|") { it.urlPart() }
}

fun decodeMovementSavedFilter(raw: String): MovementSavedFilter? {
    if (raw.isBlank()) return null
    return runCatching {
        val parts = raw.split("|").map { java.net.URLDecoder.decode(it, Charsets.UTF_8.name()) }
        MovementSavedFilter(
            text = parts.getOrNull(0).orEmpty(),
            type = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
            reviewed = parts.getOrNull(2)?.takeIf { it.isNotBlank() }?.toBooleanStrictOrNull(),
            range = parts.getOrNull(3)?.let { QuickRange.valueOf(it) } ?: QuickRange.All,
            groupMode = parts.getOrNull(4)?.let { MovementGroupMode.valueOf(it) } ?: MovementGroupMode.Date,
            categoryId = parts.getOrNull(5)?.takeIf { it.isNotBlank() },
            accountId = parts.getOrNull(6)?.takeIf { it.isNotBlank() },
            dateFrom = parts.getOrNull(7).orEmpty(),
            dateTo = parts.getOrNull(8).orEmpty(),
            minAmount = parts.getOrNull(9).orEmpty(),
            maxAmount = parts.getOrNull(10).orEmpty(),
            onlyLinked = parts.getOrNull(11)?.toBooleanStrictOrNull() ?: false,
            onlyDuplicates = parts.getOrNull(12)?.toBooleanStrictOrNull() ?: false
        )
    }.getOrNull()
}

private fun String.urlPart(): String = java.net.URLEncoder.encode(this, Charsets.UTF_8.name())

fun movementGroupLabel(rawDate: String): String {
    val date = runCatching { LocalDate.parse(rawDate.take(10)) }.getOrNull() ?: return "Sin fecha"
    val today = LocalDate.now()
    return when {
        date == today -> "Hoy"
        date == today.minusDays(1) -> "Ayer"
        !date.isBefore(today.minusDays(7)) -> "Ultimos 7 dias"
        date.year == today.year && date.month == today.month -> "Este mes"
        else -> "${date.monthValue.toString().padStart(2, '0')}/${date.year}"
    }
}

enum class QuickRange {
    All,
    Last30Days,
    LastMonth,
    ThisQuarter,
    ThisYear,
    LastYear,
    ThisMonth;

    fun toDates(): Pair<String, String>? {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        return when (this) {
            All -> null
            Last30Days -> today.minusDays(30).format(formatter) to today.format(formatter)
            LastMonth -> {
                val start = today.minusMonths(1).withDayOfMonth(1)
                start.format(formatter) to start.withDayOfMonth(start.lengthOfMonth()).format(formatter)
            }
            ThisQuarter -> {
                val quarterStartMonth = (((today.monthValue - 1) / 3) * 3) + 1
                val start = LocalDate.of(today.year, quarterStartMonth, 1)
                start.format(formatter) to today.format(formatter)
            }
            ThisYear -> LocalDate.of(today.year, 1, 1).format(formatter) to today.format(formatter)
            LastYear -> {
                val start = LocalDate.of(today.year - 1, 1, 1)
                start.format(formatter) to LocalDate.of(today.year - 1, 12, 31).format(formatter)
            }
            ThisMonth -> today.withDayOfMonth(1).format(formatter) to today.format(formatter)
        }
    }
}

@Composable
fun MovementsInsightCard(movements: List<Movement>, hidden: Boolean) {
    val income = movements.filter { it.amount > 0 }.sumOf { it.amount }
    val expense = movements.filter { it.amount < 0 }.sumOf { kotlin.math.abs(it.amount) }
    val net = income - expense
    val total = (income + expense).takeIf { it > 0.0 } ?: 1.0

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(stringResource(R.string.movements_resultado_visible), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(money(net, hidden), color = if (net >= 0.0) ContaIncome else ContaDanger, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                StatusChip(stringResource(R.string.movements_mov, movements.size), ExecutiveBlue)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .weight((income / total).toFloat().coerceAtLeast(0.08f))
                        .height(8.dp)
                        .background(ContaIncome, RoundedCornerShape(50))
                )
                Box(
                    Modifier
                        .weight((expense / total).toFloat().coerceAtLeast(0.08f))
                        .height(8.dp)
                        .background(ContaExpense, RoundedCornerShape(50))
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PulseLine(stringResource(R.string.movements_ingresos), money(income, hidden), ContaIncome, Modifier.weight(1f))
                PulseLine(stringResource(R.string.movements_gastos), money(expense, hidden), ContaExpense, Modifier.weight(1f))
            }
        }
    }
}



