package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Lo que el conductor rellena al terminar la jornada. */
data class DatosFin(
    val fin: Instant,
    val zona: ZoneId,
    val kmFin: Int,
    val pais: String? = null,
    val ciudad: String? = null,
    /** Lo que marca el tacógrafo. `null` si no lo apunta (las reglas dirán «No verificado»). */
    val conduccionTacografo: Duration? = null,
    val observaciones: String? = null,
)

/**
 * Caso de uso: terminar la jornada abierta.
 *
 * Reglas: el fin es posterior al inicio y no futuro, `kmFin ≥ kmInicio`
 * y la conducción no supera la duración de la jornada.
 */
class FinalizarJornada
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val clock: Clock,
    ) {
        /** @return la jornada ya finalizada. */
        suspend operator fun invoke(datos: DatosFin): ResultadoJornada<Jornada> {
            val abierta =
                repository.obtenerAbierta()
                    ?: return ResultadoJornada.Invalida(ErrorJornada.SIN_JORNADA_ABIERTA)

            val finalizada =
                abierta.copy(
                    fin = datos.fin,
                    zonaFin = datos.zona,
                    kmFin = datos.kmFin,
                    paisFin = datos.pais.comoPais(),
                    ciudadFin = datos.ciudad.limpio(),
                    conduccionTacografo = datos.conduccionTacografo,
                    observaciones = datos.observaciones.limpio(),
                )
            val errores = validarJornada(finalizada, clock.instant())
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            repository.guardar(finalizada)
            return ResultadoJornada.Ok(finalizada)
        }
    }
