package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.model.ErrorDescanso
import com.cristinachdr.mitacografo.domain.model.ResultadoDescanso
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.model.clasificarDescansos
import com.cristinachdr.mitacografo.domain.model.validarDescanso
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Caso de uso: la lista de descansos con su clase, del más reciente al más antiguo. */
class ObtenerDescansos
    @Inject
    constructor(
        private val repository: DescansoRepository,
    ) {
        operator fun invoke(): Flow<List<DescansoClasificado>> =
            repository.observarTodos().map { clasificarDescansos(it).asReversed() }
    }

/**
 * Caso de uso: añadir o corregir una pausa o un descanso a mano.
 *
 * No puede solaparse con otro ni acabar en el futuro, y solo puede haber uno en curso.
 */
class GuardarDescanso
    @Inject
    constructor(
        private val repository: DescansoRepository,
        private val clock: Clock,
    ) {
        /** @return el id del descanso guardado. */
        suspend operator fun invoke(descanso: Descanso): ResultadoDescanso<Long> {
            if (descanso.id != Descanso.SIN_ID && repository.obtenerPorId(descanso.id) == null) {
                return ResultadoDescanso.Invalido(ErrorDescanso.DESCANSO_NO_EXISTE)
            }
            val limpio = descanso.copy(pais = descanso.pais.comoPais(), observaciones = descanso.observaciones.limpio())
            val otros = repository.obtenerTodos().filter { it.id != limpio.id }
            val errores = validarDescanso(limpio, otros, clock.instant())
            if (errores.isNotEmpty()) return ResultadoDescanso.Invalido(errores)
            return ResultadoDescanso.Ok(repository.guardar(limpio))
        }
    }

class BorrarDescanso
    @Inject
    constructor(
        private val repository: DescansoRepository,
    ) {
        suspend operator fun invoke(id: Long): ResultadoDescanso<Unit> {
            if (repository.obtenerPorId(id) == null) return ResultadoDescanso.Invalido(ErrorDescanso.DESCANSO_NO_EXISTE)
            repository.borrar(id)
            return ResultadoDescanso.Ok(Unit)
        }
    }

/**
 * Caso de uso: botón «Iniciar pausa» de la jornada en curso. Empieza ahora, en el país de la jornada.
 */
class IniciarPausa
    @Inject
    constructor(
        private val descansos: DescansoRepository,
        private val jornadas: JornadaRepository,
        private val guardarDescanso: GuardarDescanso,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): ResultadoDescanso<Long> {
            val jornada =
                jornadas.obtenerAbierta() ?: return ResultadoDescanso.Invalido(ErrorDescanso.SIN_JORNADA_ABIERTA)
            if (descansos.obtenerEnCurso() != null) return ResultadoDescanso.Invalido(ErrorDescanso.YA_HAY_UNO_EN_CURSO)
            val pausa =
                Descanso(
                    inicio = clock.instant(),
                    zonaInicio = clock.zone,
                    tipo = TipoDescanso.PAUSA,
                    pais = jornada.paisInicio,
                )
            return guardarDescanso(pausa)
        }
    }

/**
 * Caso de uso: terminar la pausa en curso.
 *
 * @param hasta cuándo termina; por defecto, ahora. Al finalizar la jornada se cierra a la hora de fin.
 *   Si esa hora no es posterior al inicio de la pausa, la pausa no tiene sentido y se borra.
 */
class TerminarPausa
    @Inject
    constructor(
        private val repository: DescansoRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(
            hasta: Instant? = null,
            zona: ZoneId? = null,
        ): ResultadoDescanso<Unit> {
            val enCurso =
                repository.obtenerEnCurso() ?: return ResultadoDescanso.Invalido(ErrorDescanso.SIN_PAUSA_EN_CURSO)
            val fin = hasta ?: clock.instant()
            if (!fin.isAfter(enCurso.inicio)) {
                repository.borrar(enCurso.id)
            } else {
                repository.guardar(enCurso.copy(fin = fin, zonaFin = zona ?: clock.zone))
            }
            return ResultadoDescanso.Ok(Unit)
        }
    }

/** Lo que propone la pantalla de Iniciar jornada: un descanso desde que terminó la jornada anterior. */
data class PropuestaDescanso(
    val desde: Instant,
    val zona: ZoneId,
    val pais: String?,
)

/**
 * Caso de uso: «¿Descansaste desde ayer 19:40?» (espec. §8.5).
 *
 * Si la última jornada terminó y después no hay ningún descanso apuntado, propone uno desde ese fin.
 * No crea nada: solo se guarda si el conductor lo confirma.
 */
class ProponerDescansoPrevio
    @Inject
    constructor(
        private val jornadas: JornadaRepository,
        private val descansos: DescansoRepository,
    ) {
        suspend operator fun invoke(): PropuestaDescanso? {
            if (jornadas.obtenerAbierta() != null) return null
            val ultima = jornadas.obtenerUltimaFinalizada() ?: return null
            val fin = ultima.fin ?: return null
            val yaApuntado =
                descansos.obtenerTodos().any { it.tipo == TipoDescanso.DESCANSO && (it.fin ?: Instant.MAX) > fin }
            if (yaApuntado) return null
            return PropuestaDescanso(desde = fin, zona = ultima.zonaFin ?: ultima.zonaInicio, pais = ultima.paisFin)
        }
    }
