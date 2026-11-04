package com.cristinachdr.mitacografo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.cristinachdr.mitacografo.R
import java.text.NumberFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// Formatos de fechas, horas, duraciones y km que se repiten en varias pantallas.
// Usan el idioma del móvil (español: "245.310 km"; ucraniano: "245 310 км").

/** "HH:mm" (24 h, como el tacógrafo). */
val FormatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** Fecha corta según el idioma, p. ej. "2 nov 2026". */
val FormatoFecha: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** "8 h 05 min". No usa `toMinutesPart()` porque no existe en Android 8 (API 26). */
@Composable
fun textoDuracion(duracion: Duration): String {
    val minutos = duracion.toMinutes().coerceAtLeast(0)
    return stringResource(R.string.formato_duracion, minutos / 60, minutos % 60)
}

/** Km con separador de miles: "245.310". */
@Composable
fun textoKm(km: Int): String {
    val formato = remember { NumberFormat.getIntegerInstance() }
    return formato.format(km)
}
