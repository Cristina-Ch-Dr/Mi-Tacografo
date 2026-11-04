package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.AvisoProgramado

/**
 * Puerto de salida para los avisos de la jornada.
 *
 * La implementación con `AlarmManager` vive en `app` (necesita Android).
 */
interface AlarmScheduler {
    /** Programa los avisos (y los recuerda para reprogramarlos tras reiniciar el móvil). */
    suspend fun programar(avisos: List<AvisoProgramado>)

    /** Cancela todos los avisos pendientes de una jornada. */
    suspend fun cancelar(jornadaId: Long)
}
