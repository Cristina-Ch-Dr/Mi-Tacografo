package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.calcularAvisos
import com.cristinachdr.mitacografo.domain.port.AlarmScheduler
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Mantiene los avisos del sistema de acuerdo con la jornada y los ajustes.
 *
 * Los avisos no se guardan en la base de datos: siempre se pueden recalcular a partir
 * de la hora de inicio de la jornada abierta y de los ajustes (con [calcularAvisos]).
 * Por eso tras reiniciar el móvil basta con llamar a [reprogramarJornadaAbierta].
 */
class GestorAvisos
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val settings: SettingsRepository,
        private val scheduler: AlarmScheduler,
        private val clock: Clock,
    ) {
        /** Borra los avisos de la jornada y, si sigue abierta, programa los que falten. */
        suspend fun reprogramar(jornada: Jornada) {
            scheduler.cancelar(jornada.id)
            if (!jornada.abierta) return
            val avisos = calcularAvisos(jornada, settings.observar().first().avisos, clock.instant())
            if (avisos.isNotEmpty()) scheduler.programar(avisos)
        }

        /** Al reiniciar el móvil, al cambiar los ajustes o al conceder el permiso de alarmas exactas. */
        suspend fun reprogramarJornadaAbierta() {
            repository.obtenerAbierta()?.let { reprogramar(it) }
        }
    }
