@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.compose.animation.Crossfade
import com.contaapp.android.R
import androidx.compose.ui.res.stringResource
import androidx.compose.runtime.Composable
import com.contaapp.android.ContaUiState
import com.contaapp.android.data.MovementFilters

@Composable
fun AppRoot(
    state: ContaUiState,
    onLogin: (String, String) -> Unit,
    onVerifyTwoFactor: (String) -> Unit,
    onCancelTwoFactor: () -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onServerTest: (String) -> Unit,
    onUnlock: () -> Unit,
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
    Crossfade(
        targetState = when {
            !state.hasStoredSession -> "login"
            !state.isUnlocked -> "locked"
            else -> "app"
        },
        label = stringResource(R.string.approot_root)
    ) { target ->
        when (target) {
            "login" -> LoginScreen(state, onLogin, onVerifyTwoFactor, onCancelTwoFactor, onBaseUrlChanged, onServerTest)
            "locked" -> LockedScreen(state, onUnlock, onLogout)
            else -> MainScaffold(
                state = state,
                onLogout = onLogout,
                onRefresh = onRefresh,
                onHomeFiltersChanged = onHomeFiltersChanged,
                onChartFiltersChanged = onChartFiltersChanged,
                onCalendarMonthChanged = onCalendarMonthChanged,
                onMovementFiltersChanged = onMovementFiltersChanged,
                onTogglePrivacy = onTogglePrivacy,
                onToggleDarkMode = onToggleDarkMode,
                onToggleDynamicColor = onToggleDynamicColor,
                onTogglePrivacyDefault = onTogglePrivacyDefault,
                onCacheTtlChanged = onCacheTtlChanged,
                onRefreshCacheEntry = onRefreshCacheEntry,
                onClearCache = onClearCache,
                onExportMovements = onExportMovements,
                onShowAttentionNotification = onShowAttentionNotification
            )
        }
    }
}



