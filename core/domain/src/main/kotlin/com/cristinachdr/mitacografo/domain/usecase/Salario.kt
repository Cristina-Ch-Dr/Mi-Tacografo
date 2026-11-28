package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ConfigSalario
import com.cristinachdr.mitacografo.domain.model.Gasto
import com.cristinachdr.mitacografo.domain.model.ResultadoSalario
import com.cristinachdr.mitacografo.domain.model.calcularSalario
import com.cristinachdr.mitacografo.domain.model.periodoSalario
import com.cristinachdr.mitacografo.domain.port.GastoRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/**
 * Caso de uso: el salario de un periodo (MT-58). [desplazamiento] 0 = el periodo de hoy, -1 = el anterior…
 * Se actualiza solo al cambiar viajes, jornadas, gastos o lo que cobra.
 */
class ObtenerSalario
    @Inject
    constructor(
        private val obtenerViajes: ObtenerViajes,
        private val gastos: GastoRepository,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        operator fun invoke(desplazamiento: Int): Flow<ResultadoSalario> =
            combine(obtenerViajes(), gastos.observarTodos(), settings.observar()) { viajes, todos, ajustes ->
                val config = ajustes.salario
                val periodo = periodoSalario(LocalDate.now(clock), config.diaInicio, desplazamiento)
                calcularSalario(periodo, config, viajes, todos)
            }
    }

/** Caso de uso: cambiar lo que cobra. Un día de inicio fuera de 1–28 o un importe negativo no se guardan. */
class CambiarConfigSalario
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) {
        suspend operator fun invoke(cambio: (ConfigSalario) -> ConfigSalario) {
            settings.actualizar { ajustes ->
                val nueva = cambio(ajustes.salario)
                val valida =
                    nueva.diaInicio in ConfigSalario.DIAS_INICIO &&
                        listOf(nueva.nomina, nueva.porKm, nueva.porDia).none { it.signum() < 0 }
                if (valida) ajustes.copy(salario = nueva) else ajustes
            }
        }
    }

/**
 * Caso de uso: apuntar un gasto de su bolsillo.
 *
 * @return `false` si el importe no es mayor que cero (no se guarda).
 */
class AnadirGasto
    @Inject
    constructor(
        private val gastos: GastoRepository,
    ) {
        suspend operator fun invoke(gasto: Gasto): Boolean {
            if (gasto.importe <= BigDecimal.ZERO) return false
            gastos.guardar(gasto.copy(id = Gasto.SIN_ID, descripcion = gasto.descripcion.limpio()))
            return true
        }
    }

class BorrarGasto
    @Inject
    constructor(
        private val gastos: GastoRepository,
    ) {
        suspend operator fun invoke(id: Long) = gastos.borrar(id)
    }
