@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.contaapp.android.domain.CategoryOption
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.Movement
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val ContaCoral = Color(0xFFFF5A52)
val ContaTeal = Color(0xFF0F766E)
val ContaAmber = Color(0xFFF59E0B)
val ContaDanger = Color(0xFFDC2626)
val ContaInk = Color(0xFF0F172A)
val ContaMuted = Color(0xFF64748B)
val ContaSurface = Color.White
val ExecutiveBlue = Color(0xFF0D47A1)
val ExecutiveSky = Color(0xFF1E88E5)
val ExecutiveMint = Color(0xFF22C55E)
val ExecutiveOrange = Color(0xFFF59E0B)
val ExecutiveBg = Color(0xFFF1F5F9)

@Composable
fun HeroBalance(title: String, value: String, subtitle: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier
                .background(
                    Brush.horizontalGradient(listOf(Color(0xFFFF5A52), Color(0xFFFF8A65))),
                    RoundedCornerShape(14.dp)
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, color = Color(0xFFFFE4E0), style = MaterialTheme.typography.titleMedium)
            Text(value, color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color.White.copy(alpha = 0.86f))
        }
    }
}

@Composable
fun PeriodHeader(
    title: String = "Periodo actual",
    subtitle: String = "Vista financiera",
    onPrevious: (() -> Unit)? = null,
    onNext: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "<",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold,
                modifier = if (onPrevious != null) Modifier.clickable { onPrevious() } else Modifier
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            }
            Text(
                ">",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontWeight = FontWeight.Bold,
                modifier = if (onNext != null) Modifier.clickable { onNext() } else Modifier
            )
        }
    }
}

@Composable
fun FinanceRingCard(
    income: Double,
    expense: Double,
    debt: Double,
    hidden: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF17212B)),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(18.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FinanceRing(income = income, expense = expense, debt = debt, size = 132.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.commoncomponents_actividad), color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LegendLine(stringResource(R.string.commoncomponents_ingresos), money(income, hidden), Color(0xFF2DD4BF))
                LegendLine(stringResource(R.string.commoncomponents_gastos), money(expense, hidden), Color(0xFFFF5A52))
                LegendLine(stringResource(R.string.commoncomponents_deuda), money(debt, hidden), Color(0xFFFBBF24))
            }
        }
    }
}

@Composable
fun FinanceRing(income: Double, expense: Double, debt: Double, size: Dp) {
    val values = listOf(income.coerceAtLeast(0.0), expense.coerceAtLeast(0.0))
    val total = values.sum().takeIf { it > 0.0 } ?: 1.0
    val colors = listOf(Color(0xFF2DD4BF), Color(0xFFFF5A52))

    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
            val diameter = this.size.minDimension - stroke.width
            val topLeft = (this.size.minDimension - diameter) / 2
            var start = -90f

            drawArc(
                color = Color.White.copy(alpha = 0.12f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                size = Size(diameter, diameter),
                style = stroke
            )

            values.forEachIndexed { index, value ->
                val sweep = ((value / total) * 360f).toFloat().coerceAtLeast(8f)
                drawArc(
                    color = colors[index],
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(topLeft, topLeft),
                    size = Size(diameter, diameter),
                    style = stroke
                )
                start += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.commoncomponents_periodo), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
            Text(if (debt > 0.0) "Activo" else "100%", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LegendLine(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(8.dp)))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.labelMedium)
            Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun CategoryRankingCard(
    title: String,
    categories: List<CategoryBreakdown>,
    hidden: Boolean,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val dark = MaterialTheme.colorScheme.background.red < 0.12f && MaterialTheme.colorScheme.background.green < 0.12f
    val cardColor = if (dark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface
    val top = categories
        .sortedByDescending { kotlin.math.abs(it.total) }
        .take(5)
    val max = top.maxOfOrNull { kotlin.math.abs(it.total) }?.takeIf { it > 0.0 } ?: 1.0

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${top.sumOf { it.count }} mov.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            }
            if (top.isEmpty()) {
                Text(stringResource(R.string.commoncomponents_sin_categorias_en_este_periodo), color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                top.forEachIndexed { index, category ->
                    CategoryBarRow(
                        index = index + 1,
                        category = category,
                        progress = kotlin.math.abs(category.total) / max,
                        hidden = hidden,
                        accent = accent
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryBarRow(
    index: Int,
    category: CategoryBreakdown,
    progress: Double,
    hidden: Boolean,
    accent: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(28.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(index.toString(), color = accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
                Text(category.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
            }
            Text(money(kotlin.math.abs(category.total), hidden), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
        }
        ProgressBar(progress = progress, color = accent)
    }
}

@Composable
fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FitText(title, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            FitText(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun MovementRow(movement: Movement, hidden: Boolean, onClick: () -> Unit) {
    val accent = when {
        movement.amount > 0 -> Color(0xFF0F766E)
        movement.amount < 0 -> Color(0xFFFF5A52)
        else -> Color(0xFF64748B)
    }
    val icon = movementIcon(movement)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(icon), contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(movement.description, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${movement.date.take(10)} - ${movement.categoryName ?: movement.type}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(money(movement.amount, hidden), color = accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MovementDetailDialog(
    movement: Movement,
    hidden: Boolean,
    categories: List<CategoryOption> = emptyList(),
    accounts: List<BankAccountBalance> = emptyList(),
    debts: List<DebtDetail> = emptyList(),
    insurances: List<InsurancePolicy> = emptyList(),
    onDismiss: () -> Unit
) {
    val category = movementCategory(movement, categories)
    val accent = categoryColor(category, if (movement.amount >= 0.0) ContaTeal else ContaDanger)
    val accountName = movementAccountName(movement, accounts)
    val link = movementLinkLabel(movement, debts, insurances)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.commoncomponents_cerrar)) }
        },
        title = { Text(stringResource(R.string.commoncomponents_detalle_del_movimiento), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.76f)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                Modifier.size(50.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painterResource(categoryIcon(category, movement)), contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(movement.description, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${movement.date.take(10)} · ${category?.name ?: movement.categoryName ?: movement.type}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(if (movement.amount >= 0.0) "Ingreso" else "Gasto", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                                Text(money(movement.amount, hidden), color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.headlineSmall)
                            }
                            StatusChip(if (movement.reviewed) "Revisado" else "Pendiente", if (movement.reviewed) ContaTeal else ContaAmber)
                        }
                        ProgressBar(
                            progress = if (movement.amount >= 0.0) 1.0 else 0.72,
                            color = accent
                        )
                    }
                }
                QualityBadges(movement = movement, accountName = accountName, category = category, link = link)
                MovementContextCard(
                    type = movement.type,
                    account = accountName ?: "Cuenta no informada por la API",
                    category = category?.name ?: movement.categoryName ?: "Sin categoria",
                    link = link ?: "Sin vinculacion detectada",
                    accent = accent
                )
                if (movement.hasSplits) DetailLine(stringResource(R.string.commoncomponents_desglose), "Movimiento con splits")
                if (movement.isDuplicate) DetailLine(stringResource(R.string.commoncomponents_duplicado), "Marcado como posible duplicado")
                movement.notes?.let { DetailLine(stringResource(R.string.commoncomponents_notas), it) }
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun MovementContextCard(type: String, account: String, category: String, link: String, accent: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.commoncomponents_contexto), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
            DetailLine(stringResource(R.string.commoncomponents_tipo), type)
            DetailLine(stringResource(R.string.commoncomponents_cuenta), account)
            DetailLine(stringResource(R.string.commoncomponents_categoria), category)
            Row(
                Modifier.fillMaxWidth().background(accent.copy(alpha = 0.09f), RoundedCornerShape(12.dp)).padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(painterResource(R.drawable.ic_receipt), contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.commoncomponents_vinculacion), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(link, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun DetailLine(label: String, value: String) {
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun ProgressBar(progress: Double, color: Color) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
    ) {
        Box(
            Modifier
                .fillMaxWidth(progress.coerceIn(0.0, 1.0).toFloat())
                .height(8.dp)
                .background(color, RoundedCornerShape(8.dp))
        )
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@DrawableRes
fun movementIcon(movement: Movement): Int {
    val text = "${movement.description} ${movement.categoryName.orEmpty()} ${movement.type}".lowercase()
    return when {
        movement.amount > 0 -> R.drawable.ic_bank
        "seguro" in text || "alarma" in text -> R.drawable.ic_shield
        "hipoteca" in text || "prestamo" in text || "pres." in text -> R.drawable.ic_home
        "visa" in text || "payment" in text || "tarjeta" in text -> R.drawable.ic_credit_card
        else -> R.drawable.ic_receipt
    }
}

@DrawableRes
fun categoryIcon(category: CategoryOption?, movement: Movement): Int {
    val icon = category?.icon.orEmpty().lowercase()
    val name = "${category?.name.orEmpty()} ${movement.description}".lowercase()
    return when {
        "🏠" in icon || "home" in icon || "house" in icon || "apartment" in icon || "hipoteca" in name || "hogar" in name || "comunidad" in name || "alquiler" in name -> R.drawable.ic_home
        "🏦" in icon || "bank" in icon || "account_balance" in icon || "impuesto" in name || "banco" in name || "interes" in name || "interés" in name -> R.drawable.ic_bank
        "🛡" in icon || "shield" in icon || "seguro" in name || "alarma" in name || "security" in icon || "proteccion" in name -> R.drawable.ic_shield
        "💳" in icon || "card" in icon || "credit" in icon || "visa" in name || "tarjeta" in name || "payment" in name || "pago" in name -> R.drawable.ic_credit_card
        "chart" in icon || "analytics" in icon || "graf" in name || "inversion" in name -> R.drawable.ic_chart
        "settings" in icon || "gear" in icon || "servicio" in name || "suministro" in name -> R.drawable.ic_settings
        else -> movementIcon(movement)
    }
}

fun categoryColor(category: CategoryOption?, fallback: Color): Color {
    val raw = category?.color?.trim() ?: return fallback
    return runCatching {
        Color(android.graphics.Color.parseColor(if (raw.startsWith("#")) raw else "#$raw"))
    }.getOrDefault(fallback)
}

fun movementAccountName(movement: Movement, accounts: List<BankAccountBalance>): String? {
    return movement.accountName?.takeIf { it.isNotBlank() && it != "Cuenta desconocida" }
        ?: accounts.firstOrNull { it.id == movement.accountId }?.let { account ->
            listOf(account.bankName, account.accountName).filter { it.isNotBlank() }.joinToString(" · ")
        }
}

fun movementCategory(movement: Movement, categories: List<CategoryOption>): CategoryOption? {
    return categories.firstOrNull { it.id == movement.categoryId }
        ?: categories.firstOrNull { it.name.equals(movement.categoryName, ignoreCase = true) }
}

fun movementLinkLabel(
    movement: Movement,
    debts: List<DebtDetail> = emptyList(),
    insurances: List<InsurancePolicy> = emptyList()
): String? {
    val debt = debts.firstOrNull { it.id == movement.loanId || it.id == movement.financingId }
    val insurance = insurances.firstOrNull { it.id == movement.insuranceId }
    return when {
        movement.loanId != null -> "Prestamo · ${debt?.let { "${it.name} (${it.entity})" } ?: movement.loanId}"
        movement.financingId != null -> "Financiacion · ${debt?.let { "${it.name} (${it.entity})" } ?: movement.financingId}"
        movement.propertyId != null -> "Propiedad · ${movement.propertyId}"
        movement.insuranceId != null -> "Seguro · ${insurance?.name ?: movement.insuranceId}"
        movement.parentMovementId != null || movement.hasSplits -> "Split · ${movement.parentMovementId ?: "movimiento desglosado"}"
        movement.isDuplicate -> "Duplicado · posible movimiento repetido"
        else -> null
    }
}

fun Movement.hasBusinessLink(): Boolean {
    return loanId != null ||
        financingId != null ||
        propertyId != null ||
        insuranceId != null ||
        parentMovementId != null ||
        hasSplits
}

fun List<DebtDetail>.visibleDebtDetails(): List<DebtDetail> {
    return filter { debt ->
        debt.pendingAmount > 0.0 &&
            debt.status.lowercase() !in setOf("paid", "cancelled", "canceled", "closed", "expired", "inactive")
    }
}

fun DebtDetail.isDebtAlert(): Boolean {
    return status.equals("overdue", true) || status.equals("warning", true)
}

fun List<InsurancePolicy>.visibleInsurancePolicies(): List<InsurancePolicy> {
    return filter { policy ->
        val status = policy.status.orEmpty().lowercase()
        // Las de fecha pasada se muestran (como "Vencido"): son las que mas atencion necesitan.
        status !in setOf("expired", "cancelled", "canceled", "inactive")
    }
}

@Composable
fun PremiumPeriodSelector(
    selected: QuickRange,
    onSelected: (QuickRange) -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Periodo",
    detail: String = "Vista financiera"
) {
    var open by remember { mutableStateOf(false) }
    val dates = selected.toDates()
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Selector de periodo ${selected.homeLabel()}" }
            .clickable { open = true }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_chart), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black, maxLines = 1)
                Text(
                    dates?.let { "${selected.homeLabel()} · ${it.first} / ${it.second}" } ?: detail,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2
                )
            }
            StatusChip(stringResource(R.string.commoncomponents_cambiar), MaterialTheme.colorScheme.primary)
        }
    }

    if (open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            PeriodSelectorSheet(
                selected = selected,
                onSelected = {
                    onSelected(it)
                    open = false
                },
                onDismiss = { open = false }
            )
        }
    }
}

@Composable
fun PeriodSelectorSheet(
    selected: QuickRange,
    onSelected: (QuickRange) -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SectionHeader(stringResource(R.string.commoncomponents_seleccionar_periodo), "afecta a KPIs, graficas y listados")
        premiumPeriodOptions().forEach { option ->
            PeriodOptionRow(
                option = option,
                selected = selected == option,
                onClick = { onSelected(option) }
            )
        }
        WalletActionButton(stringResource(R.string.commoncomponents_cerrar), onDismiss, Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 18.dp))
    }
}

@Composable
fun PeriodOptionRow(option: QuickRange, selected: Boolean, onClick: () -> Unit) {
    val dates = option.toDates()
    val accent = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(38.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_credit_card), contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(option.homeLabel(), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                Text(
                    dates?.let { "${it.first} - ${it.second}" } ?: "Sin limite de fechas",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (selected) StatusChip(stringResource(R.string.commoncomponents_activo), MaterialTheme.colorScheme.primary)
        }
    }
}

fun premiumPeriodOptions(): List<QuickRange> = listOf(
    QuickRange.ThisMonth,
    QuickRange.LastMonth,
    QuickRange.ThisQuarter,
    QuickRange.ThisYear,
    QuickRange.LastYear,
    QuickRange.Last30Days,
    QuickRange.All
)

@Composable
fun PremiumPeriodChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(50))
            .border(1.dp, border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
fun FilterButton(text: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = ContaCoral),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) { Text(text, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, maxLines = 1) }
    } else {
        OutlinedButton(
            onClick = onClick,
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        ) { Text(text, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
    }
}

@Composable
fun StatusChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QualityBadges(
    movement: Movement,
    accountName: String?,
    category: CategoryOption?,
    link: String?,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (accountName.isNullOrBlank()) StatusChip(stringResource(R.string.commoncomponents_sin_cuenta), ContaAmber)
        if (category == null && movement.categoryName.isNullOrBlank()) StatusChip(stringResource(R.string.commoncomponents_sin_categoria), ContaAmber)
        if (!movement.reviewed) StatusChip(stringResource(R.string.commoncomponents_pendiente), ContaAmber)
        if (link != null) StatusChip(stringResource(R.string.commoncomponents_vinculado), ExecutiveBlue)
        if (movement.isDuplicate) StatusChip(stringResource(R.string.commoncomponents_duplicado), ContaDanger)
    }
}

@Composable
fun AttentionCard(title: String, value: String, detail: String, color: Color, modifier: Modifier = Modifier) {
    val compactValue = value.length <= 3 && !value.contains(" ")
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .height(42.dp)
                    .widthIn(min = if (compactValue) 42.dp else 82.dp, max = 116.dp)
                    .background(color.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    value,
                    color = color,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, detail: String? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        if (!detail.isNullOrBlank()) {
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun WalletActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, danger: Boolean = false) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text,
            color = if (danger) ContaDanger else ContaCoral,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ExecutiveKpiCard(
    title: String,
    value: String,
    detail: String,
    @DrawableRes iconRes: Int,
    color: Color,
    modifier: Modifier = Modifier,
    sparkValues: List<Double> = emptyList()
) {
    val surface = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(42.dp).background(color.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, color = onSurface.copy(alpha = 0.62f), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                FitText(value, color = onSurface, fontWeight = FontWeight.Black)
                Text(detail, color = color, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (sparkValues.isNotEmpty()) {
                MiniSparkline(sparkValues, color, Modifier.size(width = 58.dp, height = 34.dp))
            }
        }
    }
}

@Composable
fun MiniSparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 1.0
        val range = (max - min).takeIf { it > 0.0 } ?: 1.0
        val step = size.width / (values.size - 1)
        values.zipWithNext().forEachIndexed { index, pair ->
            val y1 = size.height - (((pair.first - min) / range).toFloat() * size.height)
            val y2 = size.height - (((pair.second - min) / range).toFloat() * size.height)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(step * index, y1),
                end = androidx.compose.ui.geometry.Offset(step * (index + 1), y2),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun ExecutiveProgressCard(
    title: String,
    value: String,
    detail: String,
    progress: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
                Text(value, color = color, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
            }
            ProgressBar(progress = progress, color = color)
        }
    }
}

@Composable
fun FinancialLineChart(
    title: String,
    subtitle: String,
    values: List<Double>,
    color: Color,
    labels: List<String> = emptyList(),
    xAxisLabel: String = "Periodo",
    yAxisLabel: String = "Importe",
    hidden: Boolean = false,
    modifier: Modifier = Modifier
) {
    val surface = MaterialTheme.colorScheme.surface
    val grid = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val cleanValues = values.map { if (it.isFinite()) it else 0.0 }
    val minValue = cleanValues.minOrNull() ?: 0.0
    val maxValue = cleanValues.maxOrNull() ?: 0.0
    val midValue = (minValue + maxValue) / 2.0
    Card(
        colors = CardDefaults.cardColors(containerColor = surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title, subtitle)
            if (cleanValues.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.commoncomponents_inicio, money(cleanValues.first(), hidden)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.commoncomponents_fin, money(cleanValues.last(), hidden)), color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(if (cleanValues.size == 1) R.string.commoncomponents_punto else R.string.commoncomponents_puntos, cleanValues.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(stringResource(R.string.commoncomponents_rango, axisMoney(minValue, hidden), axisMoney(maxValue, hidden)), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(
                    Modifier.height(150.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    Text(axisMoney(maxValue, hidden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(axisMoney(midValue, hidden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                    Text(axisMoney(minValue, hidden), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                }
                Canvas(Modifier.weight(1f).height(150.dp)) {
                    repeat(4) { line ->
                        val y = size.height * (line + 1) / 5f
                        drawLine(grid, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                    if (cleanValues.size >= 2) {
                    val min = cleanValues.minOrNull() ?: 0.0
                    val max = cleanValues.maxOrNull() ?: 1.0
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
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.commoncomponents_eje_x, xAxisLabel), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Text(stringResource(R.string.commoncomponents_eje_y, yAxisLabel), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            }
            if (labels.isNotEmpty()) {
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
        }
    }
}

private fun axisMoney(value: Double, hidden: Boolean): String {
    if (hidden) return "•••• €"
    val abs = kotlin.math.abs(value)
    val scaled = when {
        abs >= 1_000_000.0 -> value / 1_000_000.0
        abs >= 1_000.0 -> value / 1_000.0
        else -> value
    }
    val suffix = when {
        abs >= 1_000_000.0 -> "M"
        abs >= 1_000.0 -> "K"
        else -> ""
    }
    return String.format(java.util.Locale("es", "ES"), "%.1f%s €", scaled, suffix)
}

@Composable
fun ActiveFilterBar(filters: List<String>, onOpenFilters: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(painterResource(R.drawable.ic_settings), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.commoncomponents_vista_filtrada), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black)
                Text(
                    if (filters.isEmpty()) "Todos los datos" else filters.joinToString(" · "),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Button(
                onClick = onOpenFilters,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    if (filters.isEmpty()) "Filtrar" else "Filtrar · ${filters.size}",
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun FilterPanel(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            content()
        }
    }
}

@Composable
fun InlineLoading(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun SkeletonCard(lines: Int = 3, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(lines) { index ->
                Box(
                    Modifier
                        .fillMaxWidth(if (index == lines - 1) 0.62f else 1f)
                        .height(if (index == 0) 22.dp else 14.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(50))
                )
            }
        }
    }
}

@Composable
fun ErrorText(error: String?) {
    if (!error.isNullOrBlank()) {
        Text(error, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun EmptyHint(show: Boolean, text: String) {
    if (show) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(Modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
                Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun PremiumEmptyState(show: Boolean, title: String, detail: String, @DrawableRes iconRes: Int = R.drawable.ic_receipt) {
    if (show) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)
                    Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DarkMiniMetric(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier.background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(label, color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
        FitText(value, color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
    }
}




/** Texto de una linea que reduce su tamano hasta caber, en lugar de cortarse con "...". */
@Composable
fun FitText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
    fontWeight: FontWeight? = null
) {
    var scale by remember(text) { androidx.compose.runtime.mutableFloatStateOf(1f) }
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = style,
        fontSize = style.fontSize * scale,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            if (result.hasVisualOverflow && scale > 0.55f) scale *= 0.9f
        }
    )
}
