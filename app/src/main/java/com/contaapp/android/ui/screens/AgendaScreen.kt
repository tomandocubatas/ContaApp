package com.contaapp.android.ui.screens

import androidx.compose.foundation.background
import com.contaapp.android.ui.theme.ContaCardDefaults
import com.contaapp.android.ui.theme.ContaSpacing
import com.contaapp.android.ui.theme.ContaRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.contaapp.android.ContaUiState
import com.contaapp.android.R
import com.contaapp.android.domain.CalendarDay
import com.contaapp.android.domain.CalendarEvent
import com.contaapp.android.domain.CalendarMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AgendaScreen(
    state: ContaUiState,
    onMonthChanged: (Int, Int) -> Unit
) {
    val initial = state.calendarMonth
    var visibleMonth by remember(initial?.year, initial?.month) {
        mutableStateOf(
            if (initial != null && initial.year > 0 && initial.month > 0) {
                YearMonth.of(initial.year, initial.month)
            } else {
                YearMonth.now()
            }
        )
    }
    var selectedDay by remember { mutableStateOf<CalendarDay?>(null) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    LaunchedEffect(visibleMonth) {
        val current = state.calendarMonth
        if (current?.year != visibleMonth.year || current.month != visibleMonth.monthValue) {
            onMonthChanged(visibleMonth.year, visibleMonth.monthValue)
        }
    }

    val calendar = state.calendarMonth?.takeIf {
        it.year == visibleMonth.year && it.month == visibleMonth.monthValue
    } ?: CalendarMonth(visibleMonth.year, visibleMonth.monthValue)
    val allEvents = calendar.days.flatMap { it.events }.sortedBy { it.date }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(ContaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)
    ) {
        item {
            AgendaHeader(
                month = visibleMonth,
                isLoading = state.isCalendarLoading,
                onPrevious = {
                    selectedDay = null
                    visibleMonth = visibleMonth.minusMonths(1)
                },
                onToday = {
                    selectedDay = null
                    visibleMonth = YearMonth.now()
                },
                onNext = {
                    selectedDay = null
                    visibleMonth = visibleMonth.plusMonths(1)
                }
            )
        }
        item { AgendaSummary(calendar = calendar, hidden = state.privacyMode) }
        item {
            AgendaMonthGrid(
                month = visibleMonth,
                calendar = calendar,
                selectedDate = selectedDay?.date,
                onDaySelected = { day -> selectedDay = day }
            )
        }
        selectedDay?.let { day ->
            item { SectionHeader(stringResource(R.string.agenda_dia_seleccionado), day.date.toReadableDate()) }
            if (day.events.isEmpty()) {
                item { EmptyHint(true, "No hay eventos previstos para este dia.") }
            } else {
                items(day.events, key = { "selected-${it.stableAgendaKey()}" }) { event ->
                    AgendaEventRow(event = event, hidden = state.privacyMode, onClick = { selectedEvent = event })
                }
            }
        }
        item { SectionHeader(stringResource(R.string.agenda_proximos_eventos), "${allEvents.size} en el mes") }
        if (allEvents.isEmpty()) {
            item { EmptyHint(true, "No hay eventos financieros para este mes.") }
        } else {
            calendar.days.filter { it.events.isNotEmpty() }.sortedBy { it.date }.forEach { day ->
                stickyHeader(key = "day-${day.date}") {
                    DayHeader(
                        label = dayHeaderLabel(day.date),
                        count = day.events.size,
                        total = day.totalIncome - day.totalExpense,
                        hidden = state.privacyMode
                    )
                }
                items(day.events, key = { "month-${it.stableAgendaKey()}" }) { event ->
                    AgendaEventRow(event = event, hidden = state.privacyMode, onClick = { selectedEvent = event })
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

    selectedEvent?.let { event ->
        AgendaEventDialog(
            event = event,
            hidden = state.privacyMode,
            onDismiss = { selectedEvent = null }
        )
    }
}

@Composable
fun AgendaHeader(
    month: YearMonth,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    onToday: () -> Unit,
    onNext: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.agenda_agenda_financiera), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    Text(month.monthLabel(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                }
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onPrevious, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(50)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior")
                }
                OutlinedButton(onClick = onToday, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(50)) { Text(stringResource(R.string.agenda_hoy)) }
                OutlinedButton(onClick = onNext, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(50)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente")
                }
            }
        }
    }
}

@Composable
fun AgendaSummary(calendar: CalendarMonth, hidden: Boolean) {
    val income = calendar.days.sumOf { it.totalIncome }
    val expense = calendar.days.sumOf { it.totalExpense }
    val events = calendar.days.sumOf { it.events.size }
    val pending = calendar.days.sumOf { day -> day.events.count { !it.confirmed } }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            SectionHeader(stringResource(R.string.agenda_resumen_mensual), "$events eventos")
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                ComparisonMetric(stringResource(R.string.agenda_ingresos), income, ContaIncome, hidden, Modifier.weight(1f))
                ComparisonMetric(stringResource(R.string.agenda_gastos), expense, ContaExpense, hidden, Modifier.weight(1f))
                ComparisonMetric(stringResource(R.string.agenda_previstos), pending.toDouble(), ContaWarning, false, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun AgendaMonthGrid(
    month: YearMonth,
    calendar: CalendarMonth,
    selectedDate: String?,
    onDaySelected: (CalendarDay) -> Unit
) {
    val dayMap = calendar.days.associateBy { it.date }
    val cells = remember(month, calendar.days) { buildAgendaCells(month, dayMap) }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.md), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                listOf("L", "M", "X", "J", "V", "S", "D").forEach { label ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                    week.forEach { cell ->
                        AgendaDayCell(
                            cell = cell,
                            selected = cell.day?.date == selectedDate,
                            onClick = { cell.day?.let(onDaySelected) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaDayCell(cell: AgendaCell, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val day = cell.day
    val hasEvents = day?.events?.isNotEmpty() == true
    val accent = when {
        day == null -> Color.Transparent
        day.totalIncome > day.totalExpense -> ContaIncome
        hasEvents -> ContaCoral
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
    }
    Box(
        modifier
            .height(48.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
                RoundedCornerShape(ContaRadius.md)
            )
            .clickable(enabled = day != null, onClick = onClick)
            .padding(ContaSpacing.xs),
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                Text(cell.number.toString(), color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.labelMedium, fontWeight = if (selected) FontWeight.Black else FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(day.events.take(3).size) {
                        Box(Modifier.size(5.dp).background(accent, RoundedCornerShape(50)))
                    }
                }
            }
        }
    }
}

@Composable
fun AgendaEventRow(event: CalendarEvent, hidden: Boolean, onClick: () -> Unit) {
    val accent = eventAccent(event)
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.md),
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(ContaSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(42.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)), contentAlignment = Alignment.Center) {
                Icon(painterResource(eventIcon(event)), contentDescription = null, tint = accent, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(event.label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${event.date.toReadableDate()} · ${event.typeLabel()}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)) {
                Text(money(event.signedAmount(), hidden), color = accent, fontWeight = FontWeight.Black, maxLines = 1)
                StatusChip(if (event.confirmed) "Confirmado" else "Previsto", if (event.confirmed) ContaIncome else ContaWarning)
            }
        }
    }
}

@Composable
fun AgendaEventDialog(event: CalendarEvent, hidden: Boolean, onDismiss: () -> Unit) {
    val accent = eventAccent(event)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.agenda_cerrar)) } },
        title = { Text(event.label, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
                Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(46.dp).background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)), contentAlignment = Alignment.Center) {
                        Icon(painterResource(eventIcon(event)), contentDescription = null, tint = accent)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(event.typeLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                        Text(money(event.signedAmount(), hidden), color = accent, fontWeight = FontWeight.Black, style = MaterialTheme.typography.titleLarge)
                    }
                }
                StatusChip(if (event.confirmed) "Movimiento confirmado" else "Proyeccion pendiente", if (event.confirmed) ContaTeal else ContaAmber)
                Text(stringResource(R.string.agenda_fecha, event.date.toReadableDate()), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(ContaRadius.xl)
    )
}

data class AgendaCell(val number: Int?, val day: CalendarDay?)

private fun buildAgendaCells(month: YearMonth, dayMap: Map<String, CalendarDay>): List<AgendaCell> {
    val first = month.atDay(1)
    val leading = first.dayOfWeek.value - 1
    val cells = mutableListOf<AgendaCell>()
    repeat(leading) { cells.add(AgendaCell(null, null)) }
    for (day in 1..month.lengthOfMonth()) {
        val date = month.atDay(day).toString()
        cells.add(AgendaCell(day, dayMap[date] ?: CalendarDay(date, emptyList(), 0.0, 0.0)))
    }
    while (cells.size % 7 != 0) cells.add(AgendaCell(null, null))
    return cells
}

private fun YearMonth.monthLabel(): String {
    val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es", "ES"))
    return atDay(1).format(formatter).replaceFirstChar { it.uppercase() }
}

private fun String.toReadableDate(): String {
    return runCatching {
        LocalDate.parse(take(10)).format(DateTimeFormatter.ofPattern("dd MMM", Locale("es", "ES")))
    }.getOrDefault(take(10))
}

private fun CalendarEvent.signedAmount(): Double {
    val value = kotlin.math.abs(amount)
    return if (direction == "income") value else -value
}

private fun CalendarEvent.typeLabel(): String {
    return when (type) {
        "recurring" -> "Recurrente"
        "loan" -> "Prestamo"
        "financing" -> "Financiacion"
        "insurance" -> "Seguro"
        else -> "Evento"
    }
}

@Composable
private fun eventAccent(event: CalendarEvent): Color {
    return when {
        event.direction == "income" -> ContaIncome
        event.type == "loan" || event.type == "financing" -> ExecutiveBlue
        event.type == "insurance" -> ContaAmber
        else -> ContaCoral
    }
}

private fun eventIcon(event: CalendarEvent): Int {
    return when (event.type) {
        "loan", "financing" -> R.drawable.ic_credit_card
        "insurance" -> R.drawable.ic_shield
        "recurring" -> R.drawable.ic_refresh
        else -> R.drawable.ic_calendar
    }
}

private fun CalendarEvent.stableAgendaKey(): String {
    return listOf(id, type, sourceId, date, label).joinToString("|")
}
