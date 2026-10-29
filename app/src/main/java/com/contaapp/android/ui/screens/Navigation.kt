@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
enum class Tab(val label: String, val shortLabel: String, @DrawableRes val iconRes: Int) {
    Home("Inicio", "Inicio", R.drawable.ic_home),
    Agenda("Agenda", "Agenda", R.drawable.ic_calendar),
    Movements("Movimientos", "Movs", R.drawable.ic_receipt),
    Accounts("Cuentas", "Cuentas", R.drawable.ic_bank),
    Charts("Graficas", "Grafs", R.drawable.ic_chart),
    Debt("Deuda", "Deuda", R.drawable.ic_credit_card),
    Insurance("Seguros", "Seguro", R.drawable.ic_shield),
    Rentals("Alquileres", "Alquiler", R.drawable.ic_home),
    Settings("Ajustes", "Ajustes", R.drawable.ic_settings),
    More("Mas", "Mas", R.drawable.ic_menu)
}

private data class DetailTarget(val tab: Tab, val id: String?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScaffold(
    state: ContaUiState,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onHomeFiltersChanged: (MovementFilters) -> Unit,
    onChartFiltersChanged: (MovementFilters) -> Unit,
    onCalendarMonthChanged: (Int, Int) -> Unit,
    onMovementFiltersChanged: (MovementFilters) -> Unit,
    onTogglePrivacy: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onToggleDynamicColor: () -> Unit,
    onTogglePrivacyDefault: () -> Unit,
    onCacheTtlChanged: (Int) -> Unit,
    onRefreshCacheEntry: (String) -> Unit,
    onClearCache: () -> Unit,
    onExportMovements: () -> Unit,
    onShowAttentionNotification: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(Tab.Home) }
    var detailTarget by remember { mutableStateOf<DetailTarget?>(null) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val isTablet = maxWidth >= 720.dp
        LaunchedEffect(isTablet, selectedTab) {
            if (isTablet && selectedTab == Tab.More) {
                selectedTab = Tab.Home
            }
        }

        Scaffold(
            topBar = {
                AppTopBar(
                    selectedTab = selectedTab,
                    userName = state.userName,
                    lastUpdatedLabel = state.lastUpdatedLabel,
                    privacyMode = state.privacyMode,
                    onTogglePrivacy = onTogglePrivacy,
                    onRefresh = onRefresh,
                    onLogout = onLogout
                )
            },
            bottomBar = {
                if (!isTablet) {
                    ContaBottomBar(selectedTab = selectedTab, onSelected = { selectedTab = it })
                }
            }
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(padding)
            ) {
                if (isTablet) {
                    ContaNavigationRail(selectedTab = selectedTab, onSelected = { selectedTab = it })
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    ErrorBanner(state.error, onRetry = onRefresh)
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                    PullToRefreshBox(
                        isRefreshing = state.isLoading || state.isMovementsLoading || state.isCalendarLoading,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        AdaptiveScreenFrame(isTablet = isTablet) {
                            when (selectedTab) {
                                Tab.Home -> HomeScreen(state, onHomeFiltersChanged, onNavigate = { selectedTab = it })
                                Tab.Agenda -> AgendaScreen(
                                    state = state,
                                    onMonthChanged = onCalendarMonthChanged
                                )
                                Tab.Movements -> MovementsScreen(
                                    state = state,
                                    onMovementFiltersChanged = onMovementFiltersChanged,
                                    focusMovementId = detailTarget.takeIf { it?.tab == Tab.Movements }?.id,
                                    onFocusConsumed = { detailTarget = null }
                                )
                                Tab.Accounts -> AccountsScreen(
                                    state = state,
                                    focusAccountId = detailTarget.takeIf { it?.tab == Tab.Accounts }?.id,
                                    onFocusConsumed = { detailTarget = null }
                                )
                                Tab.Charts -> ChartsScreen(state, onChartFiltersChanged)
                                Tab.Debt -> DebtScreen(
                                    state = state,
                                    focusDebtId = detailTarget.takeIf { it?.tab == Tab.Debt }?.id,
                                    onFocusConsumed = { detailTarget = null }
                                )
                                Tab.Insurance -> InsuranceScreen(
                                    state = state,
                                    focusInsuranceId = detailTarget.takeIf { it?.tab == Tab.Insurance }?.id,
                                    onFocusConsumed = { detailTarget = null }
                                )
                                Tab.Rentals -> RentalsScreen(
                                    state = state,
                                    onRentalsFiltersChanged = onChartFiltersChanged,
                                    focusPropertyId = detailTarget.takeIf { it?.tab == Tab.Rentals }?.id,
                                    onFocusConsumed = { detailTarget = null }
                                )
                                Tab.Settings -> SettingsScreen(
                                    state = state,
                                    onRefresh = onRefresh,
                                    onToggleDarkMode = onToggleDarkMode,
                                    onToggleDynamicColor = onToggleDynamicColor,
                                    onTogglePrivacyDefault = onTogglePrivacyDefault,
                                    onCacheTtlChanged = onCacheTtlChanged,
                                    onRefreshCacheEntry = onRefreshCacheEntry,
                                    onClearCache = onClearCache,
                                    onExportMovements = onExportMovements,
                                    onShowAttentionNotification = onShowAttentionNotification,
                                    onLogout = onLogout
                                )
                                Tab.More -> MoreScreen(
                                    state = state,
                                    onOpen = { selectedTab = it },
                                    onOpenResult = { result ->
                                        selectedTab = result.tab
                                        detailTarget = DetailTarget(result.tab, result.targetId)
                                    }
                                )
                            }
                        }
                    }
                }
                }
            }
        }
    }
}

@Composable
fun AppTopBar(
    selectedTab: Tab,
    userName: String,
    lastUpdatedLabel: String?,
    privacyMode: Boolean,
    onTogglePrivacy: () -> Unit,
    onRefresh: () -> Unit,
    onLogout: () -> Unit
) {
    TopAppBar(
        title = {
            WithCappedFontScale {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ContaSpacing.md)) {
                Box(
                    Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f), RoundedCornerShape(ContaRadius.md)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(R.drawable.ic_brand_mark), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(30.dp))
                }
                Column {
                    Text(selectedTab.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        listOfNotNull(
                            userName.ifBlank { "Vista segura" },
                            lastUpdatedLabel?.takeIf { it.isNotBlank() }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            }
        },
        actions = {
            HeaderIconButton(
                iconRes = if (privacyMode) R.drawable.ic_visibility else R.drawable.ic_visibility_off,
                contentDescription = if (privacyMode) "Mostrar importes" else "Ocultar importes",
                onClick = onTogglePrivacy
            )
            HeaderIconButton(iconRes = R.drawable.ic_refresh, contentDescription = stringResource(R.string.navigation_actualizar), onClick = onRefresh)
            HeaderIconButton(iconRes = R.drawable.ic_logout, contentDescription = stringResource(R.string.navigation_salir), onClick = onLogout)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun HeaderIconButton(@DrawableRes iconRes: Int, contentDescription: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(end = ContaSpacing.xs)
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(ContaRadius.md)),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onClick) {
            Icon(painterResource(iconRes), contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
fun AdaptiveScreenFrame(isTablet: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = if (isTablet) Alignment.TopCenter else Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = if (isTablet) 980.dp else Dp.Unspecified)
        ) {
            content()
        }
    }
}

@Composable
fun ContaNavigationRail(selectedTab: Tab, onSelected: (Tab) -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .size(52.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(ContaRadius.md)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_brand_mark), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(18.dp))
        tabletTabs().forEach { tab ->
            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onSelected(tab) },
                icon = { Icon(painterResource(tab.iconRes), contentDescription = null) },
                label = { Text(tab.label, maxLines = 1) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = ContaCoral,
                    selectedTextColor = ContaCoral,
                    indicatorColor = ContaCoral.copy(alpha = 0.16f)
                )
            )
        }
    }
}

/** Barra inferior de Material: 4 destinos principales y «Mas»; «Mas» queda marcado cuando el destino actual está dentro de él. */
@Composable
fun ContaBottomBar(selectedTab: Tab, onSelected: (Tab) -> Unit) {
    val visibleTabs = bottomTabs()
    WithCappedFontScale {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        visibleTabs.forEach { tab ->
            val selected = selectedTab == tab || (tab == Tab.More && selectedTab !in visibleTabs)
            NavigationBarItem(
                selected = selected,
                onClick = { onSelected(tab) },
                icon = { Icon(painterResource(tab.iconRes), contentDescription = null, modifier = Modifier.size(22.dp)) },
                label = { Text(tab.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ContaCoral,
                    selectedTextColor = ContaCoral,
                    indicatorColor = ContaCoral.copy(alpha = 0.16f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
    }
}

private fun bottomTabs(): List<Tab> = listOf(Tab.Home, Tab.Movements, Tab.Accounts, Tab.Charts, Tab.More)

private fun tabletTabs(): List<Tab> = listOf(Tab.Home, Tab.Agenda, Tab.Movements, Tab.Accounts, Tab.Charts, Tab.Debt, Tab.Insurance, Tab.Rentals, Tab.Settings)



