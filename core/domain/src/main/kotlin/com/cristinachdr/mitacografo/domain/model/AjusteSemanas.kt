package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.LocalDate

/**
 * «Ajuste de semanas» (MT-59): para quien empieza a usar la app a mitad de semana, lo que ya llevaba antes de
 * apuntar nada. Se rellena una vez, antes de la primera salida; lo que se apunta después ya lo cuenta la app.
 *
 * El motor de reglas lo usa como dato conocido en vez de «No verificado» (como la «Poprávka tyzhniv» de su app).
 *
 * @param semana el lunes de la semana a la que se refiere: la primera con datos en la app.
 */
data class AjusteSemanas(
    val semana: LocalDate,
    /** Conducción de esa semana antes de la primera jornada apuntada. */
    val conduccionSemana: Duration = Duration.ZERO,
    /** Conducción de la semana anterior (para las 90 h de dos semanas). */
    val conduccionSemanaAnterior: Duration = Duration.ZERO,
    /** Ampliaciones a 10 h ya usadas esa semana (como mucho 2). */
    val ampliaciones: Int = 0,
    /** Diarios reducidos ya usados desde el último semanal (como mucho 3). */
    val reducidos: Int = 0,
) {
    companion object {
        const val MAX_AMPLIACIONES = 2
        const val MAX_REDUCIDOS = 3

        /** Lo más que se puede conducir en una semana (art. 6.2). */
        val MAX_CONDUCCION_SEMANA: Duration = Duration.ofHours(56)
    }
}
