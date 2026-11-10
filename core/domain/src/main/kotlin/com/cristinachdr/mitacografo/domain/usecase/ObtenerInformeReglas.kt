package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import com.cristinachdr.mitacografo.domain.rules.ContextoReglas
import com.cristinachdr.mitacografo.domain.rules.InformeReglas
import com.cristinachdr.mitacografo.domain.rules.MotorReglas
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: el informe de reglas de todas las semanas. Se recalcula solo al guardar o corregir
 * una jornada o un descanso (T24: editar una jornada antigua cambia el informe de su semana).
 */
class ObtenerInformeReglas
    @Inject
    constructor(
        private val jornadas: JornadaRepository,
        private val descansos: DescansoRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        private val motor = MotorReglas()

        operator fun invoke(): Flow<InformeReglas> =
            combine(jornadas.observarTodas(), descansos.observarTodos(), settings.observar()) { j, d, ajustes ->
                motor.evaluar(ContextoReglas(j, d, ajustes.zonaReferencia, clock.instant()))
            }
    }
