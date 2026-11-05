package com.cristinachdr.mitacografo.ui

import java.time.Duration

/** Conducción escrita en dos casillas (horas y minutos). Si las dos están vacías, `null` (no apuntada). */
fun leerConduccion(
    horas: String,
    minutos: String,
): Duration? {
    if (horas.isBlank() && minutos.isBlank()) return null
    return Duration.ofHours(horas.toLongOrNull() ?: 0).plusMinutes(minutos.toLongOrNull() ?: 0)
}

/** Lo contrario de [leerConduccion]: 9 h 05 min → ("9", "5"). Sin conducción, casillas vacías. */
fun casillasConduccion(conduccion: Duration?): Pair<String, String> {
    if (conduccion == null) return "" to ""
    val minutos = conduccion.toMinutes()
    return (minutos / 60).toString() to (minutos % 60).toString()
}

/** Minutos de 0 a 59; si no, el error para mostrar debajo de la casilla. */
fun errorMinutos(minutos: String): ErrorCampo? {
    val valor = minutos.toIntOrNull()
    return if (valor != null && valor > 59) ErrorCampo.MINUTOS_INVALIDOS else null
}
