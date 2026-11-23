package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.model.sinConduccionSobrante
import com.cristinachdr.mitacografo.domain.model.validarJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.first
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
    /** Si la jornada cruza el domingo a las 24:00: cuánta fue antes (MT-50). */
    val conduccionAntesMedianoche: Duration? = null,
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
        private val avisos: GestorAvisos,
        private val settings: SettingsRepository,
        private val clock: Clock,
    ) {
        /** @return la jornada ya finalizada. */
        suspend operator fun invoke(datos: DatosFin): ResultadoJornada<Jornada> {
            val abierta =
                repository.obtenerAbierta()
                    ?: return ResultadoJornada.Invalida(ErrorJornada.SIN_JORNADA_ABIERTA)

            val zonaReferencia = settings.observar().first().zonaReferencia
            val finalizada =
                abierta
                    .copy(
                        fin = datos.fin,
                        zonaFin = datos.zona,
                        kmFin = datos.kmFin,
                        paisFin = datos.pais.comoPais(),
                        ciudadFin = datos.ciudad.limpio(),
                        conduccionTacografo = datos.conduccionTacografo,
                        conduccionAntesMedianoche = datos.conduccionAntesMedianoche,
                        observaciones = datos.observaciones.limpio(),
                    ).sinConduccionSobrante(zonaReferencia)
            val errores = validarJornada(finalizada, clock.instant(), zonaReferencia = zonaReferencia)
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)

            repository.guardar(finalizada)
            avisos.reprogramar(finalizada) // jornada cerrada → cancela todos sus avisos
            return ResultadoJornada.Ok(finalizada)
        }
    }

/**
 * Caso de uso: apuntar, con la jornada en curso, la conducción que marca el tacógrafo hasta ahora (MT-53).
 *
 * Así «Lo que te queda» de Inicio cuenta con ella, y al finalizar el campo ya sale relleno.
 */
class ApuntarConduccion
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val clock: Clock,
    ) {
        /** @return la jornada en curso con la conducción apuntada. */
        suspend operator fun invoke(conduccion: Duration): ResultadoJornada<Jornada> {
            val abierta =
                repository.obtenerAbierta()
                    ?: return ResultadoJornada.Invalida(ErrorJornada.SIN_JORNADA_ABIERTA)
            val ahora = clock.instant()
            val apuntada = abierta.copy(conduccionTacografo = conduccion, conduccionApuntadaEn = ahora)
            val errores = validarJornada(apuntada, ahora)
            if (errores.isNotEmpty()) return ResultadoJornada.Invalida(errores)
            repository.guardar(apuntada)
            return ResultadoJornada.Ok(apuntada)
        }
    }
