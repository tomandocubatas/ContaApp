package com.contaapp.android

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.contaapp.android.core.cache.FinancialSnapshot
import com.contaapp.android.core.cache.SnapshotStore
import com.contaapp.android.core.network.ApiException
import com.contaapp.android.core.notifications.AttentionNotifier
import com.contaapp.android.core.session.SessionStore
import com.contaapp.android.core.settings.AppPreferencesStore
import com.contaapp.android.core.settings.ServerConfigStore
import com.contaapp.android.core.network.ApiClient
import com.contaapp.android.data.ContaRepository
import com.contaapp.android.data.MovementFilters
import com.contaapp.android.domain.BankAccountBalance
import com.contaapp.android.domain.CategoryOption
import com.contaapp.android.domain.ChartsSnapshot
import com.contaapp.android.domain.DashboardSummary
import com.contaapp.android.domain.DebtDetail
import com.contaapp.android.domain.DebtSummary
import com.contaapp.android.domain.InsuranceSummary
import com.contaapp.android.domain.InsurancePolicy
import com.contaapp.android.domain.InsuranceCoverage
import com.contaapp.android.domain.InsuranceRenewal
import com.contaapp.android.domain.Movement
import com.contaapp.android.domain.RentalProperty
import com.contaapp.android.domain.RentalMonth
import com.contaapp.android.domain.RentalSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CacheStatusUi(
    val key: String,
    val screen: String,
    val detail: String,
    val cachedAt: String,
    val remaining: String,
    val isFresh: Boolean
)

data class ContaUiState(
    val hasStoredSession: Boolean = false,
    val isUnlocked: Boolean = false,
    val isLoading: Boolean = false,
    val isMovementsLoading: Boolean = false,
    val privacyMode: Boolean = false,
    val privacyDefault: Boolean = false,
    val darkMode: Boolean = false,
    val userName: String = "",
    val lastUpdatedLabel: String? = null,
    val baseUrl: String = "",
    val cacheTtlMinutes: Int = 360,
    val refreshTick: Long = 0,
    val serverTestMessage: String? = null,
    val isTestingServer: Boolean = false,
    val lastExportPath: String? = null,
    val error: String? = null,
    val dashboard: DashboardSummary? = null,
    val categories: List<CategoryOption> = emptyList(),
    val accounts: List<BankAccountBalance> = emptyList(),
    val movements: List<Movement> = emptyList(),
    val debts: List<DebtSummary> = emptyList(),
    val debtDetails: List<DebtDetail> = emptyList(),
    val insurance: InsuranceSummary? = null,
    val expiringInsurances: List<InsurancePolicy> = emptyList(),
    val insuranceCoverages: List<InsuranceCoverage> = emptyList(),
    val insuranceRenewals: List<InsuranceRenewal> = emptyList(),
    val rentalSummary: RentalSummary? = null,
    val rentalProperties: List<RentalProperty> = emptyList(),
    val rentalMonths: List<RentalMonth> = emptyList(),
    val charts: ChartsSnapshot = ChartsSnapshot(),
    val cacheStatus: List<CacheStatusUi> = emptyList()
)

private fun initialCacheStatus(snapshot: FinancialSnapshot?, ttlMinutes: Int): List<CacheStatusUi> {
    val loadedAt = snapshot?.loadedAtMillis?.takeIf { it > 0L }?.let(Instant::ofEpochMilli)
    val hasData = snapshot != null && (
        snapshot.dashboard != null ||
            snapshot.categories.isNotEmpty() ||
            snapshot.accounts.isNotEmpty() ||
            snapshot.movements.isNotEmpty() ||
            snapshot.debts.isNotEmpty() ||
            snapshot.debtDetails.isNotEmpty() ||
            snapshot.insurance != null ||
            snapshot.expiringInsurances.isNotEmpty() ||
            snapshot.insuranceCoverages.isNotEmpty() ||
            snapshot.insuranceRenewals.isNotEmpty() ||
            snapshot.rentalSummary != null ||
            snapshot.rentalProperties.isNotEmpty() ||
            snapshot.rentalMonths.isNotEmpty() ||
            snapshot.charts.financialSeries.isNotEmpty()
        )
    val screens = listOf(
        "Inicio" to "Dashboard, KPIs y resumen",
        "Graficas" to "Series y distribuciones",
        "Cuentas" to "Saldos por banco",
        "Deuda" to "Prestamos y financiaciones",
        "Seguros" to "Polizas y renovaciones",
        "Alquileres" to "Resumen, propiedades y meses"
    )
    val shared = screens.map { (screen, detail) ->
        initialCacheStatusRow("snapshot", screen, detail, loadedAt, hasData, ttlMinutes)
    }
    return shared + initialCacheStatusRow("movements", "Movimientos", "Listado con filtros activos", null, snapshot?.movements?.isNotEmpty() == true, ttlMinutes)
}

private fun initialCacheStatusRow(
    key: String,
    screen: String,
    detail: String,
    loadedAt: Instant?,
    hasData: Boolean,
    ttlMinutes: Int
): CacheStatusUi {
    val remainingMillis = loadedAt?.let { ttlMinutes * 60_000L - (Instant.now().toEpochMilli() - it.toEpochMilli()) }
    val isFresh = remainingMillis?.let { it > 0L } ?: hasData
    return CacheStatusUi(
        key = key,
        screen = screen,
        detail = detail,
        cachedAt = loadedAt?.atZone(ZoneId.systemDefault())?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: if (hasData) "Cache local" else "Sin cache",
        remaining = remainingMillis?.let { millis ->
            if (millis <= 0L) {
                "Caducada"
            } else {
                val minutes = (millis / 60_000L).coerceAtLeast(1L)
                val hours = minutes / 60
                val mins = minutes % 60
                if (hours > 0) "${hours}h ${mins}m restantes" else "${mins}m restantes"
            }
        } ?: if (hasData) "Fecha no disponible" else "Pendiente de carga",
        isFresh = isFresh
    )
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionStore = SessionStore(application)
    private val snapshotStore = SnapshotStore(application)
    private val preferencesStore = AppPreferencesStore(application)
    private val serverConfigStore = ServerConfigStore(application)
    private val repository = ContaRepository(ApiClient { serverConfigStore.getBaseUrl() })
    private val initialSnapshot = snapshotStore.load()

    private val _uiState = MutableStateFlow(
        ContaUiState(
            hasStoredSession = sessionStore.hasSession(),
            userName = sessionStore.get()?.name.orEmpty(),
            baseUrl = serverConfigStore.getBaseUrl(),
            cacheTtlMinutes = preferencesStore.cacheTtlMinutes(),
            privacyMode = preferencesStore.isPrivacyDefault(),
            privacyDefault = preferencesStore.isPrivacyDefault(),
            darkMode = preferencesStore.isDarkMode(),
            dashboard = initialSnapshot?.dashboard,
            lastUpdatedLabel = initialSnapshot?.let { "Cache local" },
            categories = initialSnapshot?.categories.orEmpty(),
            accounts = initialSnapshot?.accounts.orEmpty(),
            movements = initialSnapshot?.movements.orEmpty(),
            debts = initialSnapshot?.debts.orEmpty(),
            debtDetails = initialSnapshot?.debtDetails.orEmpty(),
            insurance = initialSnapshot?.insurance,
            expiringInsurances = initialSnapshot?.expiringInsurances.orEmpty(),
            insuranceCoverages = initialSnapshot?.insuranceCoverages.orEmpty(),
            insuranceRenewals = initialSnapshot?.insuranceRenewals.orEmpty(),
            rentalSummary = initialSnapshot?.rentalSummary,
            rentalProperties = initialSnapshot?.rentalProperties.orEmpty(),
            rentalMonths = initialSnapshot?.rentalMonths.orEmpty(),
            charts = initialSnapshot?.charts ?: ChartsSnapshot(),
            cacheStatus = initialCacheStatus(initialSnapshot, preferencesStore.cacheTtlMinutes())
        )
    )
    val uiState: StateFlow<ContaUiState> = _uiState
    private var lastHomeFilters: MovementFilters? = null
    private var lastHomeLoadedAt: Instant? = initialSnapshot?.loadedAtMillis?.takeIf { it > 0L }?.let(Instant::ofEpochMilli)
    private var homeCache: Map<MovementFilters, CachedHomePayload> = emptyMap()
    private var lastMovementFilters: MovementFilters? = null
    private var movementsCache: Map<MovementFilters, CachedMovements> = emptyMap()

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching {
                repository.login(email, password)
            }.onSuccess { session ->
                sessionStore.save(session)
                _uiState.update {
                    it.copy(
                        hasStoredSession = true,
                        isUnlocked = true,
                        isLoading = false,
                        userName = session.name,
                        error = null
                    )
                }
                loadHome()
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isLoading = false, error = throwable.message ?: "No se pudo iniciar sesion")
                }
            }
        }
    }

    fun unlockAfterBiometric() {
        _uiState.update { it.copy(isUnlocked = true, error = null) }
        loadHome()
    }

    fun lockWithError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun logout() {
        sessionStore.clear()
        snapshotStore.clear()
        _uiState.value = ContaUiState()
    }

    fun togglePrivacy() {
        _uiState.update { it.copy(privacyMode = !it.privacyMode) }
    }

    fun toggleDarkMode() {
        val next = !_uiState.value.darkMode
        preferencesStore.setDarkMode(next)
        _uiState.update { it.copy(darkMode = next) }
    }

    fun togglePrivacyDefault() {
        val next = !_uiState.value.privacyDefault
        preferencesStore.setPrivacyDefault(next)
        _uiState.update { it.copy(privacyDefault = next, privacyMode = next) }
    }

    fun setCacheTtlMinutes(minutes: Int) {
        preferencesStore.setCacheTtlMinutes(minutes)
        _uiState.update { it.copy(cacheTtlMinutes = preferencesStore.cacheTtlMinutes(), cacheStatus = buildCacheStatus()) }
    }

    fun clearCache() {
        snapshotStore.clear()
        lastHomeLoadedAt = null
        lastHomeFilters = null
        homeCache = emptyMap()
        lastMovementFilters = null
        movementsCache = emptyMap()
        _uiState.update {
            it.copy(
                dashboard = null,
                categories = emptyList(),
                accounts = emptyList(),
                movements = emptyList(),
                debts = emptyList(),
                debtDetails = emptyList(),
                insurance = null,
                expiringInsurances = emptyList(),
                insuranceCoverages = emptyList(),
                insuranceRenewals = emptyList(),
                rentalSummary = null,
                rentalProperties = emptyList(),
                rentalMonths = emptyList(),
                charts = ChartsSnapshot(),
                lastUpdatedLabel = null,
                error = "Cache local borrada"
            )
        }
        refreshCacheStatus()
    }

    fun showAttentionNotification() {
        AttentionNotifier(getApplication()).show(snapshotStore.load())
        _uiState.update { it.copy(error = "Aviso local solicitado. En Android 13+ debes conceder permiso de notificaciones.") }
    }

    fun updateBaseUrl(baseUrl: String) {
        serverConfigStore.saveBaseUrl(baseUrl)
        _uiState.update { it.copy(baseUrl = serverConfigStore.getBaseUrl(), error = null, serverTestMessage = null) }
    }

    fun testServer(baseUrl: String) {
        updateBaseUrl(baseUrl)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isTestingServer = true, serverTestMessage = null) }
            runCatching { repository.healthCheck() }
                .onSuccess {
                    _uiState.update {
                        it.copy(isTestingServer = false, serverTestMessage = "Conexion correcta")
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isTestingServer = false,
                            serverTestMessage = throwable.message ?: "No se pudo conectar"
                        )
                    }
                }
        }
    }

    fun loadHome(filters: MovementFilters = MovementFilters(), force: Boolean = false) {
        val session = sessionStore.get() ?: return
        if (!force && applyCachedHome(filters)) return
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, error = null) }
            runCatching { loadHomePayload(session.accessToken, filters) }
            .recoverCatching { throwable ->
                if ((throwable as? ApiException)?.statusCode == 401) {
                    val refreshed = repository.refresh(session.refreshToken)
                    val merged = refreshed.copy(
                        name = refreshed.name.ifBlank { session.name },
                        email = refreshed.email.ifBlank { session.email }
                    )
                    sessionStore.save(merged)
                    loadHomePayload(merged.accessToken, filters)
                } else {
                    throw throwable
                }
            }.onSuccess { payload ->
                val loadedAt = Instant.now()
                val label = freshDataLabel()
                val snapshot = payload.toSnapshot(loadedAt)
                snapshotStore.save(snapshot)
                snapshotStore.save(filters.snapshotKey(), snapshot)
                homeCache = homeCache + (filters to CachedHomePayload(payload, loadedAt, label))
                lastHomeFilters = filters
                lastHomeLoadedAt = loadedAt
                applyHomePayload(payload, label, isLoading = false)
            }.onFailure { throwable ->
                val snapshot = snapshotStore.load()
                snapshot?.loadedAtMillis?.takeIf { it > 0L }?.let { lastHomeLoadedAt = Instant.ofEpochMilli(it) }
                _uiState.update {
                    if (snapshot != null) {
                        it.copy(
                            isLoading = false,
                            dashboard = snapshot.dashboard,
                            categories = snapshot.categories,
                            accounts = snapshot.accounts,
                            movements = snapshot.movements,
                            debts = snapshot.debts,
                            debtDetails = snapshot.debtDetails,
                            insurance = snapshot.insurance,
                            expiringInsurances = snapshot.expiringInsurances,
                            insuranceCoverages = snapshot.insuranceCoverages,
                            insuranceRenewals = snapshot.insuranceRenewals,
                            rentalSummary = snapshot.rentalSummary,
                            rentalProperties = snapshot.rentalProperties,
                            rentalMonths = snapshot.rentalMonths,
                            charts = snapshot.charts,
                            lastUpdatedLabel = "Cache local",
                            cacheStatus = buildCacheStatus(),
                            error = "Mostrando ultimos datos guardados. ${throwable.message ?: "No se pudieron cargar los datos"}"
                        )
                    } else {
                        it.copy(isLoading = false, error = throwable.message ?: "No se pudieron cargar los datos")
                    }
                }
                refreshCacheStatus()
            }
        }
    }

    fun refreshHome() {
        movementsCache = emptyMap()
        _uiState.update { it.copy(refreshTick = it.refreshTick + 1) }
        loadHome(lastHomeFilters ?: MovementFilters(), force = true)
    }

    fun refreshCacheEntry(key: String) {
        when (key) {
            "movements" -> {
                movementsCache = movementsCache - (lastMovementFilters ?: MovementFilters())
                loadMovements(lastMovementFilters ?: MovementFilters(), force = true)
            }
            else -> loadHome(lastHomeFilters ?: MovementFilters(), force = true)
        }
    }

    fun loadMovements(filters: MovementFilters, force: Boolean = false) {
        val session = sessionStore.get() ?: return
        lastMovementFilters = filters
        val cached = movementsCache[filters]
        if (!force && cached != null && isFresh(cached.loadedAt)) {
            _uiState.update {
                it.copy(
                    isMovementsLoading = false,
                    movements = cached.movements,
                    lastUpdatedLabel = cached.label,
                    cacheStatus = buildCacheStatus()
                )
            }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isMovementsLoading = true, error = null) }
            runCatching { repository.movements(session.accessToken, filters) }
                .recoverCatching { throwable ->
                    if ((throwable as? ApiException)?.statusCode == 401) {
                        val refreshed = repository.refresh(session.refreshToken)
                        val merged = refreshed.copy(
                            name = refreshed.name.ifBlank { session.name },
                            email = refreshed.email.ifBlank { session.email }
                        )
                        sessionStore.save(merged)
                        repository.movements(merged.accessToken, filters)
                    } else {
                        throw throwable
                    }
                }.onSuccess { movements ->
                    val label = freshDataLabel()
                    movementsCache = movementsCache + (filters to CachedMovements(movements, Instant.now(), label))
                    _uiState.update { it.copy(isMovementsLoading = false, movements = movements, lastUpdatedLabel = label, cacheStatus = buildCacheStatus()) }
                }.onFailure { throwable ->
                    _uiState.update {
                        it.copy(isMovementsLoading = false, error = throwable.message ?: "No se pudieron cargar los movimientos")
                    }
                }
        }
    }

    fun exportMovementsCsv() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val dir = File(getApplication<Application>().getExternalFilesDir(null), "exports")
                dir.mkdirs()
                val file = File(dir, "movimientos-contaapp.csv")
                val header = "fecha,descripcion,importe,tipo,categoria,cuenta,revisado,notas"
                val rows = _uiState.value.movements.map { movement ->
                    listOf(
                        movement.date.take(10),
                        movement.description,
                        movement.amount.toString(),
                        movement.type,
                        movement.categoryName.orEmpty(),
                        movement.accountName.orEmpty(),
                        movement.reviewed.toString(),
                        movement.notes.orEmpty()
                    ).joinToString(",") { it.csvEscape() }
                }
                file.writeText((listOf(header) + rows).joinToString("\n"))
                file.absolutePath
            }.onSuccess { path ->
                _uiState.update { it.copy(lastExportPath = path, error = "CSV exportado en $path") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.message ?: "No se pudo exportar CSV") }
            }
        }
    }

    private fun loadHomePayload(token: String, filters: MovementFilters): HomePayload {
        val dashboardResponse = repository.dashboardResponse(token, filters)
        return HomePayload(
            dashboard = repository.dashboardFrom(dashboardResponse),
            categories = runCatching { repository.categories(token) }.getOrDefault(emptyList()),
            accounts = repository.accountBalances(token),
            movements = repository.movements(token, filters),
            debts = repository.debt(token),
            debtDetails = repository.debtDetails(token),
            insurance = repository.insurances(token),
            expiringInsurances = runCatching { repository.insurancePolicies(token) }
                .getOrElse { repository.expiringInsurances(token) },
            insuranceCoverages = runCatching { repository.insuranceCoverages(token) }.getOrDefault(emptyList()),
            insuranceRenewals = runCatching { repository.insuranceRenewals(token) }.getOrDefault(emptyList()),
            rentalSummary = runCatching { repository.rentalSummary(token, filters) }.getOrDefault(RentalSummary(0.0, 0.0, 0.0, 0.0, 0)),
            rentalProperties = runCatching { repository.rentalProperties(token, filters) }.getOrDefault(emptyList()),
            rentalMonths = runCatching { repository.rentalMonths(token, filters) }.getOrDefault(emptyList()),
            charts = runCatching { repository.charts(token, dashboardResponse, filters) }.getOrDefault(ChartsSnapshot())
        )
    }

    private fun applyCachedHome(filters: MovementFilters): Boolean {
        val cached = homeCache[filters]
        if (cached != null && isFresh(cached.loadedAt)) {
            lastHomeFilters = filters
            lastHomeLoadedAt = cached.loadedAt
            applyHomePayload(cached.payload, cached.label, isLoading = false)
            return true
        }
        val snapshot = snapshotStore.load(filters.snapshotKey()) ?: return false
        val loadedAt = snapshot.loadedAtMillis.takeIf { it > 0L }?.let(Instant::ofEpochMilli) ?: return false
        if (!isFresh(loadedAt)) return false
        val payload = snapshot.toHomePayloadOrNull() ?: return false
        val label = "Cache ${loadedAt.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}"
        homeCache = homeCache + (filters to CachedHomePayload(payload, loadedAt, label))
        lastHomeFilters = filters
        lastHomeLoadedAt = loadedAt
        applyHomePayload(payload, label, isLoading = false)
        return true
    }

    private fun applyHomePayload(payload: HomePayload, label: String, isLoading: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = isLoading,
                dashboard = payload.dashboard,
                categories = payload.categories,
                accounts = payload.accounts,
                movements = payload.movements,
                debts = payload.debts,
                debtDetails = payload.debtDetails,
                insurance = payload.insurance,
                expiringInsurances = payload.expiringInsurances,
                insuranceCoverages = payload.insuranceCoverages,
                insuranceRenewals = payload.insuranceRenewals,
                rentalSummary = payload.rentalSummary,
                rentalProperties = payload.rentalProperties,
                rentalMonths = payload.rentalMonths,
                charts = payload.charts,
                lastUpdatedLabel = label,
                cacheStatus = buildCacheStatus()
            )
        }
    }

    private fun isFresh(loadedAt: Instant): Boolean {
        val ttlMillis = preferencesStore.cacheTtlMinutes() * 60_000L
        return Instant.now().toEpochMilli() - loadedAt.toEpochMilli() < ttlMillis
    }

    private fun freshDataLabel(): String {
        return "Act. ${LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))}"
    }

    private fun refreshCacheStatus() {
        _uiState.update { it.copy(cacheStatus = buildCacheStatus()) }
    }

    private fun buildCacheStatus(): List<CacheStatusUi> {
        val sharedScreens = listOf(
            "Inicio" to "Dashboard, KPIs y resumen",
            "Graficas" to "Series y distribuciones",
            "Cuentas" to "Saldos por banco",
            "Deuda" to "Prestamos y financiaciones",
            "Seguros" to "Polizas y renovaciones",
            "Alquileres" to "Resumen, propiedades y meses"
        )
        val hasSharedSnapshot = hasSharedVisibleData()
        val snapshotDetailSuffix = if (homeCache.isNotEmpty()) " · ${homeCache.size} rango(s) en memoria" else ""
        val shared = sharedScreens.map { (screen, detail) ->
            cacheStatus("snapshot", screen, detail + snapshotDetailSuffix, lastHomeLoadedAt, hasSharedSnapshot)
        }
        val movementLoadedAt = lastMovementFilters?.let { movementsCache[it]?.loadedAt }
        return shared + cacheStatus("movements", "Movimientos", "Listado ${lastMovementFilters?.cacheLabel() ?: "sin filtros activos"}", movementLoadedAt, hasMovementVisibleData())
    }

    private fun cacheStatus(key: String, screen: String, detail: String, loadedAt: Instant?, hasData: Boolean): CacheStatusUi {
        val fresh = loadedAt?.let { isFresh(it) } ?: hasData
        return CacheStatusUi(
            key = key,
            screen = screen,
            detail = detail,
            cachedAt = loadedAt?.let { formatInstant(it) } ?: if (hasData) "Cache local" else "Sin cache",
            remaining = loadedAt?.let { remainingLabel(it) } ?: if (hasData) "Fecha no disponible" else "Pendiente de carga",
            isFresh = fresh
        )
    }

    private fun hasSharedVisibleData(): Boolean {
        val state = _uiState.value
        return state.dashboard != null ||
            state.categories.isNotEmpty() ||
            state.accounts.isNotEmpty() ||
            state.debts.isNotEmpty() ||
            state.debtDetails.isNotEmpty() ||
            state.insurance != null ||
            state.expiringInsurances.isNotEmpty() ||
            state.insuranceCoverages.isNotEmpty() ||
            state.insuranceRenewals.isNotEmpty() ||
            state.rentalSummary != null ||
            state.rentalProperties.isNotEmpty() ||
            state.rentalMonths.isNotEmpty() ||
            state.charts.financialSeries.isNotEmpty() ||
            state.charts.incomeTrend.isNotEmpty() ||
            state.charts.expenseTrend.isNotEmpty()
    }

    private fun hasMovementVisibleData(): Boolean {
        return _uiState.value.movements.isNotEmpty()
    }

    private fun formatInstant(instant: Instant): String {
        return instant.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))
    }

    private fun remainingLabel(instant: Instant): String {
        val ttlMillis = preferencesStore.cacheTtlMinutes() * 60_000L
        val remainingMillis = ttlMillis - (Instant.now().toEpochMilli() - instant.toEpochMilli())
        if (remainingMillis <= 0L) return "Caducada"
        val minutes = (remainingMillis / 60_000L).coerceAtLeast(1L)
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) "${hours}h ${mins}m restantes" else "${mins}m restantes"
    }
}

private data class CachedMovements(
    val movements: List<Movement>,
    val loadedAt: Instant,
    val label: String
)

private data class CachedHomePayload(
    val payload: HomePayload,
    val loadedAt: Instant,
    val label: String
)

private fun MovementFilters.cacheLabel(): String {
    val parts = buildList {
        if (text.isNotBlank()) add("texto")
        if (type != null) add("tipo")
        if (reviewed != null) add("revision")
        if (dateFrom != null || dateTo != null) add("fechas")
        if (accountId != null) add("cuenta")
        if (categoryId != null) add("categoria")
        if (minAmount != null || maxAmount != null) add("importe")
    }
    return if (parts.isEmpty()) "sin filtros activos" else "filtros: ${parts.joinToString(", ")}"
}

private fun String.csvEscape(): String {
    val escaped = replace("\"", "\"\"")
    return "\"$escaped\""
}

private data class HomePayload(
    val dashboard: DashboardSummary,
    val categories: List<CategoryOption>,
    val accounts: List<BankAccountBalance>,
    val movements: List<Movement>,
    val debts: List<DebtSummary>,
    val debtDetails: List<DebtDetail>,
    val insurance: InsuranceSummary,
    val expiringInsurances: List<InsurancePolicy>,
    val insuranceCoverages: List<InsuranceCoverage>,
    val insuranceRenewals: List<InsuranceRenewal>,
    val rentalSummary: RentalSummary,
    val rentalProperties: List<RentalProperty>,
    val rentalMonths: List<RentalMonth>,
    val charts: ChartsSnapshot
)

private fun HomePayload.toSnapshot(loadedAt: Instant): FinancialSnapshot {
    return FinancialSnapshot(
        loadedAtMillis = loadedAt.toEpochMilli(),
        dashboard = dashboard,
        categories = categories,
        accounts = accounts,
        movements = movements,
        debts = debts,
        debtDetails = debtDetails,
        insurance = insurance,
        expiringInsurances = expiringInsurances,
        insuranceCoverages = insuranceCoverages,
        insuranceRenewals = insuranceRenewals,
        rentalSummary = rentalSummary,
        rentalProperties = rentalProperties,
        rentalMonths = rentalMonths,
        charts = charts
    )
}

private fun FinancialSnapshot.toHomePayloadOrNull(): HomePayload? {
    return HomePayload(
        dashboard = dashboard ?: return null,
        categories = categories,
        accounts = accounts,
        movements = movements,
        debts = debts,
        debtDetails = debtDetails,
        insurance = insurance ?: InsuranceSummary(0, 0.0, 0),
        expiringInsurances = expiringInsurances,
        insuranceCoverages = insuranceCoverages,
        insuranceRenewals = insuranceRenewals,
        rentalSummary = rentalSummary ?: RentalSummary(0.0, 0.0, 0.0, 0.0, 0),
        rentalProperties = rentalProperties,
        rentalMonths = rentalMonths,
        charts = charts
    )
}

private fun MovementFilters.snapshotKey(): String {
    return listOf(
        "home-v2",
        text.trim(),
        type.orEmpty(),
        reviewed?.toString().orEmpty(),
        dateFrom.orEmpty(),
        dateTo.orEmpty(),
        accountId.orEmpty(),
        categoryId.orEmpty(),
        minAmount?.toString().orEmpty(),
        maxAmount?.toString().orEmpty()
    ).joinToString("|")
}
