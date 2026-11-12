package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import kotlinx.coroutines.flow.first
import java.time.Clock
import javax.inject.Inject

/**
 * Caso de uso: añadir a mano una jornada ya terminada (MT-47).
 *
 * Para lo que registró el tacógrafo pero no la app, y para pasar la libreta atrasada.
 * Las mismas reglas que al corregir: tiene que estar terminada y no solaparse con ninguna otra.
 * Si cae dentro de un viaje (entre su inicio y su fin, o hasta ahora si sigue en curso), entra en él.
 * No programa avisos: ya ha pasado.
 */
class AnadirJornada
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val viajes: ViajeRepository,
        private val clock: Clock,
    ) {
        /** @return el id de la jornada creada. */
        suspend operator fun invoke(jornada: Jornada): ResultadoJornada<Long> {
            if (jornada.fin == null) return ResultadoJornada.Invalida(ErrorJornada.FALTA_EL_FIN)

            val ahora = clock.instant()
            val limpia =
                jornada.copy(
                    id = Jornada.SIN_ID,
                    paisInicio = jornada.paisInicio.comoPais(),
                    ciudadInicio = jornada.ciudadInicio.limpio(),
                    paisFin = jornada.paisFin.comoPais(),
                    ciudadFin = jornada.ciudadFin.limpio(),
                    observaciones = jornada.observaciones.limpio(),
                )
            val errores = validarJornada(limpia, ahora).toMutableSet()
            val otras = repository.observarTodas().first()
            if (otras.any { it.solapaCon(limpia, ahora) }) errores += ErrorJornada.SOLAPA_CON_OTRA_JORNADA
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            val viaje =
                viajes.obtenerTodos().firstOrNull { viaje ->
                    viaje.inicio <= limpia.inicio && limpia.inicio < (viaje.fin ?: ahora)
                }
            return ResultadoJornada.Ok(repository.guardar(limpia.copy(viajeId = viaje?.id)))
        }
    }
