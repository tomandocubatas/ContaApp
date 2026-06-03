package com.contaapp.android.ui.screens

import java.text.NumberFormat
import java.util.Locale

fun money(value: Double, hidden: Boolean): String {
    if (hidden) return "•••• €"
    return NumberFormat.getCurrencyInstance(Locale("es", "ES")).format(value)
}

fun percent(value: Double): String = "${value.toInt()}%"
