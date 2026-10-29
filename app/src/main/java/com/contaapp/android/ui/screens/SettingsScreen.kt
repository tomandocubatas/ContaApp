@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.compose.foundation.background
import com.contaapp.android.ui.theme.ContaCardDefaults
import com.contaapp.android.ui.theme.ContaSpacing
import com.contaapp.android.ui.theme.ContaRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.contaapp.android.CacheStatusUi
import com.contaapp.android.ContaUiState
import com.contaapp.android.R

@Composable
fun SettingsScreen(
    state: ContaUiState,
    onRefresh: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onTogglePrivacyDefault: () -> Unit,
    onCacheTtlChanged: (Int) -> Unit,
    onRefreshCacheEntry: (String) -> Unit,
    onClearCache: () -> Unit,
    onExportMovements: () -> Unit,
    onShowAttentionNotification: () -> Unit,
    onLogout: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 760.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(if (isTablet) 24.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)
        ) {
            item { SectionHeader(stringResource(R.string.settings_ajustes), "seguridad y datos") }
            item {
                SettingsHero(baseUrl = state.baseUrl)
            }
            item { SectionHeader(stringResource(R.string.settings_apariencia_y_seguridad)) }
            item {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_modo_oscuro),
                    detail = "Paleta nocturna premium para revisar datos con menos brillo",
                    checked = state.darkMode,
                    iconRes = R.drawable.ic_visibility,
                    onChange = onToggleDarkMode
                )
            }
            if (android.os.Build.VERSION.SDK_INT >= 31) {
                item {
                    SettingsToggleRow(
                        title = "Color dinamico",
                        detail = "Toma el acento del fondo de pantalla del sistema; los colores de ingreso y gasto no cambian",
                        checked = state.dynamicColor,
                        iconRes = R.drawable.ic_settings,
                        onChange = onToggleDynamicColor
                    )
                }
            }
            item {
                SettingsToggleRow(
                    title = stringResource(R.string.settings_privacidad_por_defecto),
                    detail = "Oculta importes al abrir la app y mantiene el layout estable",
                    checked = state.privacyDefault,
                    iconRes = R.drawable.ic_shield,
                    onChange = onTogglePrivacyDefault
                )
            }
            item { SectionHeader(stringResource(R.string.settings_datos)) }
            item {
                CacheTtlCard(
                    selectedMinutes = state.cacheTtlMinutes,
                    onSelected = onCacheTtlChanged
                )
            }
            item {
                OfflinePolicyCard(state.cacheTtlMinutes)
            }
            item {
                CacheStatusCard(
                    entries = state.cacheStatus,
                    onRefreshEntry = onRefreshCacheEntry
                )
            }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_datos_y_cache),
                    detail = "Refresca datos, exporta movimientos o limpia el snapshot offline",
                    primary = "Actualizar",
                    secondary = "Exportar CSV",
                    danger = "Borrar cache",
                    onPrimary = onRefresh,
                    onSecondary = onExportMovements,
                    onDanger = onClearCache
                )
            }
            item {
                SettingsRoadmapCard(onShowAttentionNotification)
            }
            item { SectionHeader(stringResource(R.string.settings_sesion)) }
            item {
                SettingsActionCard(
                    title = stringResource(R.string.settings_sesion),
                    detail = "Cerrar sesion borra credenciales locales y cache financiera",
                    primary = "Cerrar sesion",
                    secondary = null,
                    danger = null,
                    onPrimary = onLogout
                )
            }
            item {
                state.lastExportPath?.let {
                    StatusNote(stringResource(R.string.settings_ultima_exportacion), it)
                }
            }
        }
    }
}

@Composable
fun OfflinePolicyCard(ttlMinutes: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.settings_modo_consulta_offline), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Si no hay servidor, la app usa el ultimo snapshot disponible. La informacion se considera fresca durante ${ttlMinutes / 60}h, salvo refresco manual desde cabecera.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                StatusChip(stringResource(R.string.settings_snapshot_financiero), ContaTeal, Modifier.weight(1f))
                StatusChip(stringResource(R.string.settings_refresco_forzado), ExecutiveBlue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CacheStatusCard(entries: List<CacheStatusUi>, onRefreshEntry: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.settings_estado_de_cache_por_pantalla), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "La foto financiera se reutiliza durante el TTL configurado. Puedes refrescar una pantalla concreta o usar la cabecera para forzar todo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            entries.forEach { entry ->
                CacheStatusRow(entry = entry, onRefresh = { onRefreshEntry(entry.key) })
            }
        }
    }
}

@Composable
fun CacheStatusRow(entry: CacheStatusUi, onRefresh: () -> Unit) {
    val accent = if (entry.isFresh) ContaIncome else ContaWarning
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f), RoundedCornerShape(ContaRadius.md))
            .padding(ContaSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.background(accent.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.sm),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(painterResource(R.drawable.ic_refresh), contentDescription = null, tint = accent)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(entry.screen, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(entry.detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${entry.cachedAt} · ${entry.remaining}", color = accent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
        WalletActionButton(stringResource(R.string.settings_refrescar), onRefresh)
    }
}

@Composable
fun CacheTtlCard(selectedMinutes: Int, onSelected: (Int) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.settings_cache_de_datos), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Evita recargas automaticas al navegar. El boton superior de actualizar fuerza una carga inmediata.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(ContaSpacing.sm), modifier = Modifier.fillMaxWidth()) {
                listOf(60, 180, 360, 720).forEach { minutes ->
                    val label = if (minutes < 60) "${minutes}m" else "${minutes / 60}h"
                    FilterButton(label, selectedMinutes == minutes) { onSelected(minutes) }
                }
            }
        }
    }
}

@Composable
fun SettingsRoadmapCard(onShowAttentionNotification: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.settings_avisos_y_widget), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            StatusChip(stringResource(R.string.settings_notificacion_local_de_atencion), ContaWarning)
            StatusChip(stringResource(R.string.settings_widget_android_de_balance_y_alertas), ContaTeal)
            Text(
                "El widget se puede anadir desde el launcher. En Android 13+ la notificacion requiere conceder permiso del sistema.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(onClick = onShowAttentionNotification, shape = RoundedCornerShape(50)) {
                Text(stringResource(R.string.settings_probar_aviso))
            }
        }
    }
}

@Composable
fun SettingsHero(baseUrl: String) {
    val dark = MaterialTheme.colorScheme.background.red < 0.1f
    Card(
        colors = CardDefaults.cardColors(containerColor = if (dark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF17212B)),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
            Text(stringResource(R.string.settings_entorno_api), color = Color.White.copy(alpha = 0.68f), style = MaterialTheme.typography.labelMedium)
            Text(baseUrl, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.settings_http_permitido_temporalmente_hasta_d), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun SettingsToggleRow(title: String, detail: String, checked: Boolean, iconRes: Int, onChange: () -> Unit) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(ContaSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.background(ExecutiveBlue.copy(alpha = 0.10f), RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.sm),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(painterResource(iconRes), contentDescription = null, tint = ExecutiveBlue)
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = onSurface)
                Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = checked, onCheckedChange = { onChange() })
        }
    }
}

@Composable
fun SettingsActionCard(
    title: String,
    detail: String,
    primary: String,
    secondary: String?,
    danger: String?,
    onPrimary: () -> Unit,
    onSecondary: (() -> Unit)? = null,
    onDanger: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = ContaCardDefaults.elevation(), border = ContaCardDefaults.border(),
        shape = RoundedCornerShape(ContaRadius.xl)
    ) {
        Column(Modifier.fillMaxWidth().padding(ContaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Column(verticalArrangement = Arrangement.spacedBy(ContaSpacing.sm)) {
                OutlinedButton(onClick = onPrimary, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) { Text(primary) }
                if (secondary != null && onSecondary != null) {
                    OutlinedButton(onClick = onSecondary, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) { Text(secondary) }
                }
                if (danger != null && onDanger != null) {
                    OutlinedButton(onClick = onDanger, shape = RoundedCornerShape(50), modifier = Modifier.fillMaxWidth()) { Text(danger, color = ContaDanger) }
                }
            }
        }
    }
}

@Composable
fun StatusNote(title: String, detail: String) {
    Column(
        Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(ContaRadius.md)).padding(ContaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ContaSpacing.xs)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}
