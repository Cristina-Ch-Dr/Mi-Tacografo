package com.cristinachdr.mitacografo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import com.cristinachdr.mitacografo.R
import java.text.NumberFormat
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

// Formatos de fechas, horas, duraciones y km que se repiten en varias pantallas.
// Usan el idioma en el que se ve la app (español: "245.310 km"; ucraniano: "245 310 км"),
// no el del móvil: si el móvil está en inglés, la app sale en español y las fechas también.

/** "HH:mm" (24 h, como el tacógrafo). */
val FormatoHora: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** El idioma en el que se ve la app (sale de `strings.xml`, así que cambia con el idioma de la app). */
@Composable
fun idiomaApp(): Locale {
    val codigo = stringResource(R.string.idioma_app)
    return remember(codigo) { Locale.forLanguageTag(codigo) }
}

/** Día de la semana abreviado: "lun", "пн". */
@Composable
fun formatoDiaSemana(): DateTimeFormatter {
    val idioma = idiomaApp()
    return remember(idioma) { DateTimeFormatter.ofPattern("EEE", idioma) }
}

/** Fecha corta, p. ej. "2 nov 2026". */
@Composable
fun formatoFecha(): DateTimeFormatter {
    val idioma = idiomaApp()
    return remember(idioma) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(idioma) }
}

/** "8 h 05 min". No usa `toMinutesPart()` porque no existe en Android 8 (API 26). */
@Composable
fun textoDuracion(duracion: Duration): String {
    val minutos = duracion.toMinutes().coerceAtLeast(0)
    return stringResource(R.string.formato_duracion, minutos / 60, minutos % 60)
}

/** Km con separador de miles: "245.310". */
@Composable
fun textoKm(km: Int): String {
    val idioma = idiomaApp()
    val formato = remember(idioma) { NumberFormat.getIntegerInstance(idioma) }
    return formato.format(km)
}
