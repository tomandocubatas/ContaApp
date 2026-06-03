@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import com.contaapp.android.ContaUiState
import com.contaapp.android.data.MovementFilters

@Composable
fun AppRoot(
    state: ContaUiState,
    onLogin: (String, String) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onServerTest: (String) -> Unit,
    onUnlock: () -> Unit,
    onLogout: () -> Unit,
    onRefresh: () -> Unit,
    onHomeFiltersChanged: (MovementFilters) -> Unit,
    onChartFiltersChanged: (MovementFilters) -> Unit,
    onMovementFiltersChanged: (MovementFilters) -> Unit,
    onTogglePrivacy: () -> Unit,
    onToggleDarkMode: () -> Unit,
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
        label = "root"
    ) { target ->
        when (target) {
            "login" -> LoginScreen(state, onLogin, onBaseUrlChanged, onServerTest)
            "locked" -> LockedScreen(state, onUnlock, onLogout)
            else -> MainScaffold(
                state = state,
                onLogout = onLogout,
                onRefresh = onRefresh,
                onHomeFiltersChanged = onHomeFiltersChanged,
                onChartFiltersChanged = onChartFiltersChanged,
                onMovementFiltersChanged = onMovementFiltersChanged,
                onTogglePrivacy = onTogglePrivacy,
                onToggleDarkMode = onToggleDarkMode,
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



