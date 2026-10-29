package com.contaapp.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.contaapp.android.core.session.BiometricGate
import com.contaapp.android.ui.screens.AppRoot
import com.contaapp.android.ui.theme.ContaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    /**
     * Con la privacidad activa, la vista de apps recientes no muestra la pantalla y se bloquean las capturas.
     * `setRecentsScreenshotEnabled` (API 33+) no lo respetaba en todos los fabricantes, así que se usa FLAG_SECURE, que sí.
     */
    private fun applyRecentsPrivacy(hidden: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            setRecentsScreenshotEnabled(!hidden)
        }
        if (hidden) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val state by viewModel.uiState.collectAsState()
            SideEffect { applyRecentsPrivacy(state.privacyMode) }
            ContaTheme(darkMode = state.darkMode) {
                AppRoot(
                    state = state,
                    onLogin = viewModel::login,
                    onVerifyTwoFactor = viewModel::verifyTwoFactor,
                    onCancelTwoFactor = viewModel::cancelTwoFactor,
                    onBaseUrlChanged = viewModel::updateBaseUrl,
                    onServerTest = viewModel::testServer,
                    onUnlock = {
                        BiometricGate(this).authenticate(
                            onSuccess = viewModel::unlockAfterBiometric,
                            onCancelOrError = viewModel::lockWithError
                        )
                    },
                    onLogout = viewModel::logout,
                    onRefresh = viewModel::refreshHome,
                    onHomeFiltersChanged = viewModel::loadHome,
                    onChartFiltersChanged = viewModel::loadHome,
                    onCalendarMonthChanged = viewModel::loadCalendar,
                    onMovementFiltersChanged = viewModel::loadMovements,
                    onTogglePrivacy = viewModel::togglePrivacy,
                    onToggleDarkMode = viewModel::toggleDarkMode,
                    onTogglePrivacyDefault = viewModel::togglePrivacyDefault,
                    onCacheTtlChanged = viewModel::setCacheTtlMinutes,
                    onRefreshCacheEntry = viewModel::refreshCacheEntry,
                    onClearCache = viewModel::clearCache,
                    onExportMovements = viewModel::exportMovementsCsv,
                    onShowAttentionNotification = viewModel::showAttentionNotification
                )
            }
        }
    }
}
