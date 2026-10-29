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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
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
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AccountsScreen(
    state: ContaUiState,
    focusAccountId: String? = null,
    onFocusConsumed: () -> Unit = {}
) {
    var selectedAccount by remember { mutableStateOf<BankAccountBalance?>(null) }
    var query by remember { mutableStateOf("") }
    var bankFilter by remember { mutableStateOf<String?>(null) }
    var balanceFilter by remember { mutableStateOf(AccountBalanceFilter.All) }
    val banks = state.accounts.map { it.bankName.ifBlank { "Banco" } }.distinct().sorted()
    val filteredAccounts = state.accounts
        .filter { account ->
            val matchesText = query.isBlank() ||
                account.accountName.contains(query, ignoreCase = true)
            val matchesBank = bankFilter == null || account.bankName.ifBlank { "Banco" } == bankFilter
            val matchesBalance = when (balanceFilter) {
                AccountBalanceFilter.All -> true
                AccountBalanceFilter.Positive -> account.balance >= 0.0
                AccountBalanceFilter.Negative -> account.balance < 0.0
            }
            matchesText && matchesBank && matchesBalance
        }
        .sortedByDescending { kotlin.math.abs(it.balance) }

    LaunchedEffect(focusAccountId, state.accounts) {
        focusAccountId?.let { id ->
            selectedAccount = state.accounts.firstOrNull { it.id == id }
            onFocusConsumed()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp

        LazyColumn(Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            item { SectionHeader(stringResource(R.string.accounts_cuentas), "${filteredAccounts.size} visibles") }
            item {
                AccountsTotalCard(accounts = filteredAccounts, hidden = state.privacyMode)
            }
            item {
                FilterPanel {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(painterResource(R.drawable.ic_bank), contentDescription = null) },
                        label = { Text(stringResource(R.string.accounts_buscar_cuenta)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    BankCombo(
                        banks = banks,
                        selected = bankFilter,
                        onSelected = { bankFilter = it }
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.xs), verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs), modifier = Modifier.fillMaxWidth()) {
                        FilterButton(stringResource(R.string.accounts_todas), balanceFilter == AccountBalanceFilter.All) { balanceFilter = AccountBalanceFilter.All }
                        FilterButton(stringResource(R.string.accounts_positivas), balanceFilter == AccountBalanceFilter.Positive) { balanceFilter = AccountBalanceFilter.Positive }
                        FilterButton(stringResource(R.string.accounts_negativas), balanceFilter == AccountBalanceFilter.Negative) { balanceFilter = AccountBalanceFilter.Negative }
                    }
                }
            }
            if (state.isLoading && state.accounts.isEmpty()) {
                item { SkeletonCard(lines = 4) }
                item { SkeletonCard(lines = 4) }
            }
            filteredAccounts.groupBy { it.bankName.ifBlank { "Banco" } }.forEach { (bank, accounts) ->
                item { BankAccountsCard(bank, accounts, state.privacyMode, onClick = { selectedAccount = it }) }
            }
            item { EmptyHint(filteredAccounts.isEmpty(), "No hay cuentas para los filtros actuales.") }
        }
    }

    selectedAccount?.let { account ->
        AccountDetailDialog(
            account = account,
            movements = state.movements.filter { movement -> movement.accountId == account.id || movement.accountName == account.accountName },
            hidden = state.privacyMode,
            onDismiss = { selectedAccount = null }
        )
    }
}

enum class AccountBalanceFilter {
    All,
    Positive,
    Negative
}

@Composable
fun BankCombo(
    banks: List<String>,
    selected: String?,
    onSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(ContaRadius.md),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = ContaSpacing.md, vertical = ContaSpacing.md)
        ) {
            Icon(painterResource(R.drawable.ic_bank), contentDescription = null, tint = ContaTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(selected ?: "Todos los bancos", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(stringResource(R.string.accounts_v), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.accounts_todos_los_bancos)) },
                onClick = {
                    onSelected(null)
                    expanded = false
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_bank), contentDescription = null) }
            )
            banks.forEach { bank ->
                DropdownMenuItem(
                    text = { Text(bank) },
                    onClick = {
                        onSelected(bank)
                        expanded = false
                    },
                    leadingIcon = { Icon(painterResource(R.drawable.ic_bank), contentDescription = null) }
                )
            }
        }
    }
}

@Composable
fun BankAccountsCard(
    bank: String,
    accounts: List<BankAccountBalance>,
    hidden: Boolean,
    onClick: (BankAccountBalance) -> Unit
) {
    val total = accounts.sumOf { it.balance }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.lg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.weight(1f)) {
                    Box(
                        Modifier.size(38.dp).background(ContaTeal.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_bank), contentDescription = null, tint = ContaTeal, modifier = Modifier.size(21.dp))
                    }
                    Column {
                        Text(bank, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(stringResource(R.string.accounts_cuentas_2, accounts.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                StatusChip(money(total, hidden), if (total >= 0.0) ContaIncome else ContaDanger)
            }
            accounts.forEach { account ->
                Row(
                    Modifier.fillMaxWidth().clickable { onClick(account) }.padding(vertical = ContaSpacing.xs),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(account.accountName.ifBlank { "Cuenta" }, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(account.currency, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(money(account.balance, hidden), color = if (account.balance >= 0.0) MaterialTheme.colorScheme.onSurface else ContaDanger, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AccountBankHeader(bank: String, total: Double, hidden: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = ContaSpacing.xs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(bank, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        StatusChip(money(total, hidden), if (total >= 0.0) ContaIncome else ContaDanger)
    }
}

@Composable
fun AccountsTotalCard(accounts: List<BankAccountBalance>, hidden: Boolean) {
    val total = accounts.sumOf { it.balance }
    val negative = accounts.count { it.balance < 0.0 }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.md),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(ContaSpacing.lg),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(stringResource(R.string.accounts_patrimonio_en_cuentas), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                Text(money(total, hidden), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.accounts_cuentas_en_negativo, negative), color = if (negative > 0) ContaDanger else MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Box(
                Modifier.size(48.dp).background(ContaCoral.copy(alpha = 0.16f), RoundedCornerShape(ContaRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Text(accounts.size.toString(), color = ContaCoral, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
fun AccountRow(account: BankAccountBalance, hidden: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.md)
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = ContaSpacing.md, vertical = ContaSpacing.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(ExecutiveSky.copy(alpha = 0.14f), RoundedCornerShape(ContaRadius.md)),
                contentAlignment = Alignment.Center
            ) {
                Text(account.bankName.take(1).ifBlank { "C" }, color = ExecutiveSky, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(account.accountName.ifBlank { "Cuenta" }, fontWeight = FontWeight.SemiBold)
                Text(account.bankName.ifBlank { "Banco" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(money(account.balance, hidden), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AccountDetailDialog(
    account: BankAccountBalance,
    movements: List<com.contaapp.android.domain.Movement>,
    hidden: Boolean,
    onDismiss: () -> Unit
) {
    val income = movements.filter { it.amount > 0.0 }.sumOf { it.amount }
    val expense = movements.filter { it.amount < 0.0 }.sumOf { kotlin.math.abs(it.amount) }
    val net = income - expense
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.accounts_cerrar)) }
        },
        title = { Text(account.accountName.ifBlank { "Cuenta" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (account.balance >= 0.0) ContaIncome.copy(alpha = 0.12f) else ContaDanger.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(ContaRadius.xl)
                ) {
                    Column(Modifier.fillMaxWidth().padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(account.bankName.ifBlank { "Banco" }, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Text(money(account.balance, hidden), color = if (account.balance >= 0.0) ContaIncome else ContaDanger, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Box(
                                Modifier.size(48.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(ContaRadius.lg)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(R.drawable.ic_bank), contentDescription = null, tint = if (account.balance >= 0.0) ContaIncome else ContaDanger)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                            StatusChip(account.currency, MaterialTheme.colorScheme.primary)
                            StatusChip(if (account.balance >= 0.0) "Saldo positivo" else "En negativo", if (account.balance >= 0.0) ContaIncome else ContaDanger)
                        }
                    }
                }
                DetailLine(stringResource(R.string.accounts_cuenta), account.accountName.ifBlank { "Cuenta" })
                DetailLine(stringResource(R.string.accounts_identificador), account.id.ifBlank { "No informado" })
                if (movements.isNotEmpty()) {
                    Text(stringResource(R.string.accounts_resumen_visible), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                        AccountDetailMetric(stringResource(R.string.accounts_ingresos), money(income, hidden), ContaIncome, Modifier.weight(1f))
                        AccountDetailMetric(stringResource(R.string.accounts_gastos), money(expense, hidden), ContaExpense, Modifier.weight(1f))
                        AccountDetailMetric(stringResource(R.string.accounts_neto), money(net, hidden), if (net >= 0.0) ContaIncome else ContaDanger, Modifier.weight(1f))
                    }
                    Text(stringResource(R.string.accounts_actividad_reciente), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    movements.sortedByDescending { it.date }.take(4).forEach { movement ->
                        AccountMovementMiniRow(movement, hidden)
                    }
                } else {
                    EmptyHint(true, "No hay movimientos visibles vinculados a esta cuenta en la cache actual.")
                }
            }
        },
        shape = RoundedCornerShape(ContaRadius.xl)
    )
}

@Composable
fun AccountMovementMiniRow(movement: com.contaapp.android.domain.Movement, hidden: Boolean) {
    val accent = if (movement.amount >= 0.0) ContaIncome else ContaDanger
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)), contentAlignment = Alignment.Center) {
            Icon(painterResource(movementIcon(movement)), contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(movement.description, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(movement.date.take(10), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
        }
        Text(signedMoney(movement.amount, hidden), color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun AccountDetailMetric(title: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(color.copy(alpha = 0.10f), RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.sm),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text(value, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}



