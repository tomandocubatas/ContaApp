package com.contaapp.android.ui.screens

import java.text.NumberFormat
import java.util.Locale

fun money(value: Double, hidden: Boolean): String {
    if (hidden) return "•••• €"
    return NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(value)
}

/** Importe con signo explícito: «+» en positivos (el «−» ya lo pone el formato), para no depender solo del color. */
fun signedMoney(value: Double, hidden: Boolean): String {
    val text = money(value, hidden)
    return if (!hidden && value > 0.0) "+$text" else text
}

fun percent(value: Double): String = "${value.toInt()}%"
