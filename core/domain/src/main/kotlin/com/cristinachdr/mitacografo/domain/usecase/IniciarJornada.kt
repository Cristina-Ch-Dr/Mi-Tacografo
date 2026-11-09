package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Lo que el conductor rellena al empezar la jornada. */
data class DatosInicio(
    val inicio: Instant,
    val zona: ZoneId,
    val kmInicio: Int,
    val pais: String? = null,
    val ciudad: String? = null,
    val observaciones: String? = null,
)

/**
 * Caso de uso: empezar una jornada.
 *
 * Reglas: no puede haber otra jornada abierta, la hora no puede ser futura
 * y no puede empezar antes de que terminara la anterior.
 * Al guardarla se programan sus avisos (12 h 30, 13 h…) y, si hay un viaje en curso, se apunta en él.
 */
class IniciarJornada
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val viajes: ViajeRepository,
        private val avisos: GestorAvisos,
        private val clock: Clock,
    ) {
        /** @return el id de la jornada creada. */
        suspend operator fun invoke(datos: DatosInicio): ResultadoJornada<Long> {
            if (repository.obtenerAbierta() != null) return ResultadoJornada.Invalida(ErrorJornada.JORNADA_YA_ABIERTA)

            val jornada =
                Jornada(
                    inicio = datos.inicio,
                    zonaInicio = datos.zona,
                    kmInicio = datos.kmInicio,
                    paisInicio = datos.pais.comoPais(),
                    ciudadInicio = datos.ciudad.limpio(),
                    observaciones = datos.observaciones.limpio(),
                    viajeId = viajes.obtenerEnCurso()?.id,
                )
            val finAnterior = repository.obtenerUltimaFinalizada()?.fin
            val errores = validarJornada(jornada, clock.instant(), finAnterior)
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            val id = repository.guardar(jornada)
            avisos.reprogramar(jornada.copy(id = id))
            return ResultadoJornada.Ok(id)
        }
    }
