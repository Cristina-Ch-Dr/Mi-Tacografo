package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.ZoneId

/**
 * Preferencias del conductor.
 *
 * El idioma no está aquí: lo gestiona Android con el idioma por app
 * (Ajustes del sistema → Apps → Mi Tacógrafo → Idioma).
 */
data class Ajustes(
    val avisos: List<AvisoConfig> = AvisoConfig.POR_DEFECTO,
    /** País de la empresa (ISO alfa-2). Lo necesitan las reglas de transporte internacional. */
    val paisEmpresa: String? = null,
    /** País de residencia del conductor (ISO alfa-2). */
    val paisResidencia: String? = null,
    /** Nombre que aparece en la cabecera del PDF. */
    val nombreConductor: String? = null,
    /** Zona que marca el lunes 00:00 de la semana legal. `null` = la del móvil. */
    val zonaReferencia: ZoneId? = null,
)

/**
 * Un aviso que salta cuando ha pasado [desdeInicio] desde que empezó la jornada.
 *
 * Es un recordatorio personal, no un aviso de incumplimiento.
 */
data class AvisoConfig(
    val desdeInicio: Duration,
    val activo: Boolean = true,
) {
    companion object {
        /** 12:30 y 13:00 (descanso diario normal) y 14:30 y 15:00 (descanso reducido). */
        val POR_DEFECTO: List<AvisoConfig> =
            listOf(
                AvisoConfig(Duration.ofHours(12).plusMinutes(30)),
                AvisoConfig(Duration.ofHours(13)),
                AvisoConfig(Duration.ofHours(14).plusMinutes(30)),
                AvisoConfig(Duration.ofHours(15)),
            )
    }
}
