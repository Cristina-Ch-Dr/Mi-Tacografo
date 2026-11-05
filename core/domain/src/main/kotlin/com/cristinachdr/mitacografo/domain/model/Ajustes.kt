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
    /** Países donde suele cargar y descargar: salen primero en el selector. Vacío = ninguno elegido. */
    val paisesHabituales: Set<String> = emptySet(),
    /** Continentes por los que suele trabajar: sus países salen primero. Vacío = todos. */
    val continentes: Set<Continente> = emptySet(),
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
        /** Lo antes que puede sonar un aviso: antes de 1 h de jornada no tiene sentido. */
        val MINIMO: Duration = Duration.ofHours(1)

        /** Lo más tarde: una jornada no puede pasar de 24 h. */
        val MAXIMO: Duration = Duration.ofHours(24)

        /** Avisos que caben como mucho (el sistema de alarmas reserva un hueco para cada uno). */
        const val MAX_AVISOS = 8

        /**
         * 12:30 y 13:00 (descanso diario normal) y 14:30 y 15:00 (descanso reducido).
         *
         * Salen de restar el descanso al día: 24 h − 11 h = 13 h y 24 h − 9 h = 15 h.
         * Los de 12 h 30 y 14 h 30 avisan media hora antes.
         */
        val POR_DEFECTO: List<AvisoConfig> =
            listOf(
                AvisoConfig(Duration.ofHours(12).plusMinutes(30)),
                AvisoConfig(Duration.ofHours(13)),
                AvisoConfig(Duration.ofHours(14).plusMinutes(30)),
                AvisoConfig(Duration.ofHours(15)),
            )
    }
}
