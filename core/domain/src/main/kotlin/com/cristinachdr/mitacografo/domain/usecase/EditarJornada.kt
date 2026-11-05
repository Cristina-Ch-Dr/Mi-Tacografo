package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: corregir una jornada ya guardada (desde el historial o la jornada en curso).
 *
 * Aplica las mismas reglas que al crearla y, además, impide dejar dos jornadas abiertas.
 */
class EditarJornada
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val avisos: GestorAvisos,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(jornada: Jornada): ResultadoJornada<Jornada> {
            if (repository.obtenerPorId(jornada.id) == null) {
                return ResultadoJornada.Invalida(ErrorJornada.JORNADA_NO_EXISTE)
            }
            if (jornada.abierta) {
                val abierta = repository.obtenerAbierta()
                if (abierta != null && abierta.id != jornada.id) {
                    return ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA)
                }
            }

            val limpia =
                jornada.copy(
                    paisInicio = jornada.paisInicio.comoPais(),
                    ciudadInicio = jornada.ciudadInicio.limpio(),
                    paisFin = jornada.paisFin.comoPais(),
                    ciudadFin = jornada.ciudadFin.limpio(),
                    observaciones = jornada.observaciones.limpio(),
                )
            val errores = validarJornada(limpia, clock.instant())
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            repository.guardar(limpia)
            avisos.reprogramar(limpia) // si se cambió la hora de inicio, los avisos se mueven
            return ResultadoJornada.Ok(limpia)
        }
    }
