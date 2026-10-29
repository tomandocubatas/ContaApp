package com.contaapp.android.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/** Escala de espaciado: separaciones y rellenos de la interfaz salen de estos cinco valores. */
object ContaSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

/** Escala de radios: sm para insignias, md para mosaicos, lg para tarjetas internas, xl para tarjetas, xxl para marca. */
object ContaRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp
}

/** Criterio único de tarjeta: elevación 1 dp más un filo de 1 dp, para que se distingan también en tema oscuro, donde la sombra apenas se ve. */
object ContaCardDefaults {
    private val elevation = 1.dp
    private val borderWidth = 1.dp

    @Composable
    fun elevation(): CardElevation = CardDefaults.cardElevation(defaultElevation = elevation)

    @Composable
    fun border(): BorderStroke = BorderStroke(borderWidth, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
}
