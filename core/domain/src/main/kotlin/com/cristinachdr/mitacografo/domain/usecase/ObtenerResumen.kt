package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ResumenPeriodo
import com.cristinachdr.mitacografo.domain.model.completarDescansos
import com.cristinachdr.mitacografo.domain.model.resumirPeriodo
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Caso de uso: el resumen de una semana o bisemana. Se actualiza solo al guardar jornadas o descansos. */
class ObtenerResumen
    @Inject
    constructor(
        private val jornadas: JornadaRepository,
        private val descansos: DescansoRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(
            desde: LocalDate,
            semanas: Int,
        ): Flow<ResumenPeriodo> =
            combine(jornadas.observarTodas(), descansos.observarTodos(), settings.observar()) { j, d, ajustes ->
                resumirPeriodo(j, completarDescansos(j, d), desde, semanas, ajustes.zonaReferencia)
            }

        /** El lunes de la semana de hoy (en la zona de referencia, o la del móvil). */
        suspend fun lunesActual(): LocalDate {
            val zona = settings.observar().first().zonaReferencia ?: clock.zone
            return LocalDate.now(clock.withZone(zona)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        }
    }

/**
 * Lo que va en el PDF: el resumen del periodo, el nombre del conductor (opcional, de Ajustes)
 * y cuándo se generó. El dominio no sabe de idiomas ni de PDF: eso lo hace el adaptador de la app.
 */
data class Informe(
    val resumen: ResumenPeriodo,
    val conductor: String?,
    val generado: Instant,
    /** Zona en la que se muestra la fecha de generación. */
    val zona: ZoneId,
)

/** Caso de uso: preparar el informe del periodo visible para exportarlo. */
class PrepararInforme
    @Inject
    constructor(
        private val obtenerResumen: ObtenerResumen,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            desde: LocalDate,
            semanas: Int,
        ): Informe =
            Informe(
                resumen = obtenerResumen(desde, semanas).first(),
                conductor = settings.observar().first().nombreConductor,
                generado = clock.instant(),
                zona = clock.zone,
            )
    }
