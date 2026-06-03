package com.contaapp.android

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val state by viewModel.uiState.collectAsState()
            ContaTheme(darkMode = state.darkMode) {
                AppRoot(
                    state = state,
                    onLogin = viewModel::login,
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
