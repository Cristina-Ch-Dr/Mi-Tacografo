package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Caso de uso: corregir una jornada ya guardada (desde el historial o la jornada en curso).
 *
 * Aplica las mismas reglas que al crearla y, además, impide dejar dos jornadas abiertas
 * o que la jornada corregida se solape con otra.
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
            val ahora = clock.instant()
            val errores = validarJornada(limpia, ahora).toMutableSet()
            val otras = repository.observarTodas().first().filter { it.id != limpia.id }
            if (otras.any { it.solapaCon(limpia, ahora) }) errores += ErrorJornada.SOLAPA_CON_OTRA_JORNADA
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            repository.guardar(limpia)
            avisos.reprogramar(limpia) // si se cambió la hora de inicio, los avisos se mueven
            return ResultadoJornada.Ok(limpia)
        }
    }

/** Dos jornadas se solapan si una empieza antes de que termine la otra. Una abierta dura hasta [ahora]. */
private fun Jornada.solapaCon(
    otra: Jornada,
    ahora: Instant,
): Boolean = inicio < (otra.fin ?: ahora) && otra.inicio < (fin ?: ahora)
