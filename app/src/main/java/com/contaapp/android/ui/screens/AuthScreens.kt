@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.contaapp.android.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.CircularProgressIndicator
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
@Composable
fun LoginScreen(
    state: ContaUiState,
    onLogin: (String, String) -> Unit,
    onVerifyTwoFactor: (String) -> Unit,
    onCancelTwoFactor: () -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onServerTest: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var twoFactorCode by remember { mutableStateOf("") }
    var showServerDialog by remember { mutableStateOf(false) }
    val waitingForTwoFactor = state.pendingTwoFactorToken != null

    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackdrop()
        ServerAccessButton(
            onClick = { showServerDialog = true },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 22.dp)
        )
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AuthBrandMark()
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.auths_acceso_seguro), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFF081320))
                    Text(
                        "Tu posicion financiera, movimientos y alertas en una vista privada y cacheada.",
                        color = Color(0xFF64748B),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    AuthTrustStrip()
                    ServerStatusPill(state.baseUrl)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(stringResource(R.string.auths_email)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(stringResource(R.string.auths_password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (waitingForTwoFactor) {
                        OutlinedTextField(
                            value = twoFactorCode,
                            onValueChange = { twoFactorCode = it.filter(Char::isDigit).take(6) },
                            label = { Text(stringResource(R.string.auths_codigo_2fa)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = {
                            if (waitingForTwoFactor) {
                                onVerifyTwoFactor(twoFactorCode)
                            } else {
                                onLogin(email.trim(), password)
                            }
                        },
                        enabled = !state.isLoading &&
                            if (waitingForTwoFactor) twoFactorCode.length >= 6 else email.isNotBlank() && password.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        contentPadding = PaddingValues(vertical = 15.dp)
                    ) {
                        Text(
                            when {
                                state.isLoading && waitingForTwoFactor -> "Verificando..."
                                state.isLoading -> "Entrando..."
                                waitingForTwoFactor -> "Verificar codigo"
                                else -> "Entrar"
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (waitingForTwoFactor) {
                        TextButton(
                            onClick = {
                                twoFactorCode = ""
                                onCancelTwoFactor()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.auths_usar_otras_credenciales))
                        }
                    }
                    ErrorText(state.error)
                }
            }
        }
    }

    if (showServerDialog) {
        ServerDialog(
            state = state,
            currentBaseUrl = state.baseUrl,
            onDismiss = { showServerDialog = false },
            onTest = onServerTest,
            onSave = {
                onBaseUrlChanged(it)
                showServerDialog = false
            }
        )
    }
}

@Composable
fun AuthBackdrop() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF06111E), Color(0xFF081B2E), Color(0xFF0F2A3E))
                )
            )
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val teal = Color(0xFF00DDB4).copy(alpha = 0.28f)
            val blue = Color(0xFF4F8BFF).copy(alpha = 0.20f)
            val grid = Color.White.copy(alpha = 0.055f)
            repeat(8) { index ->
                val y = size.height * (index + 1) / 9f
                drawLine(grid, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            val points = listOf(
                androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.72f),
                androidx.compose.ui.geometry.Offset(size.width * 0.22f, size.height * 0.64f),
                androidx.compose.ui.geometry.Offset(size.width * 0.40f, size.height * 0.68f),
                androidx.compose.ui.geometry.Offset(size.width * 0.58f, size.height * 0.52f),
                androidx.compose.ui.geometry.Offset(size.width * 0.76f, size.height * 0.46f),
                androidx.compose.ui.geometry.Offset(size.width * 0.96f, size.height * 0.34f)
            )
            points.zipWithNext().forEach { (a, b) ->
                drawLine(teal, a, b, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
            }
            drawCircle(blue, radius = size.minDimension * 0.36f, center = androidx.compose.ui.geometry.Offset(size.width * 0.92f, size.height * 0.04f))
            drawCircle(teal.copy(alpha = 0.16f), radius = size.minDimension * 0.28f, center = androidx.compose.ui.geometry.Offset(size.width * 0.04f, size.height * 0.86f))
        }
    }
}

@Composable
fun ServerAccessButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.size(56.dp),
        shape = RoundedCornerShape(28.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    ) {
        Icon(painterResource(R.drawable.ic_settings), contentDescription = stringResource(R.string.auths_configurar_servidor))
    }
}

@Composable
fun ServerStatusPill(baseUrl: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.auths_servidor_activo), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
            Text(baseUrl, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
        }
        Box(
            Modifier
                .size(34.dp)
                .background(Color(0xFFE0F2F1), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_wifi), contentDescription = null, tint = Color(0xFF0F766E), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun AuthBrandMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier
                .size(82.dp)
                .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_brand_mark), contentDescription = null, tint = Color.Unspecified, modifier = Modifier.size(68.dp))
        }
        Text(stringResource(R.string.auths_contaapp_executive), color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(stringResource(R.string.auths_informacion_clara_acceso_protegido), color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AuthTrustStrip() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        AuthMiniPill(stringResource(R.string.auths_cache_6h), ContaTeal, Modifier.weight(1f))
        AuthMiniPill(stringResource(R.string.auths_biometria), ExecutiveBlue, Modifier.weight(1f))
        AuthMiniPill(stringResource(R.string.auths_privado), ContaAmber, Modifier.weight(1f))
    }
}

@Composable
fun AuthMiniPill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ServerDialog(
    state: ContaUiState,
    currentBaseUrl: String,
    onDismiss: () -> Unit,
    onTest: (String) -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(currentBaseUrl) { mutableStateOf(currentBaseUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.auths_servidor_api)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.auths_selecciona_la_api_a_la_que_se_conect))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { value = "http://10.0.2.2:4000/api" },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.auths_emulador))
                    }
                    OutlinedButton(
                        onClick = { value = "http://192.168.1.X:4000/api" },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.auths_movil_fisico))
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(stringResource(R.string.auths_url_base)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(stringResource(R.string.auths_emulador_http_10_0_2_2_4000_api), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.auths_movil_fisico_http_ip_del_pc_4000_api), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                state.serverTestMessage?.let {
                    Text(it, color = if (it == stringResource(R.string.vm_connection_ok)) Color(0xFF0F766E) else MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(value) }) {
                Text(stringResource(R.string.auths_guardar))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onTest(value) }, enabled = !state.isTestingServer) {
                    Text(if (state.isTestingServer) "Probando..." else "Probar")
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.auths_cancelar))
                }
            }
        },
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
fun LockedScreen(
    state: ContaUiState,
    onUnlock: () -> Unit,
    onLogout: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AuthBackdrop()
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .widthIn(max = 420.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            AuthBrandMark()
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        Modifier.size(76.dp).background(Color(0xFF00DDB4).copy(alpha = 0.12f), RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(R.drawable.ic_fingerprint), contentDescription = null, tint = Color(0xFF00A884), modifier = Modifier.size(44.dp))
                    }
                    Text(stringResource(R.string.auths_datos_protegidos), color = Color(0xFF081320), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.auths_confirma_tu_identidad_para_abrir_el), color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
                    Button(
                        onClick = onUnlock,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A884)),
                        contentPadding = PaddingValues(vertical = 15.dp)
                    ) {
                        Text(stringResource(R.string.auths_desbloquear_con_huella), fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = onLogout) {
                        Text(stringResource(R.string.auths_cerrar_sesion))
                    }
                    ErrorText(state.error)
                }
            }
        }
    }
}



