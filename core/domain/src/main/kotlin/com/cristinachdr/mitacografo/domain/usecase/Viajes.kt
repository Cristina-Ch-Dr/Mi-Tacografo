package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorViaje
import com.cristinachdr.mitacografo.domain.model.ResultadoViaje
import com.cristinachdr.mitacografo.domain.model.ResumenViaje
import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.model.resumirViajes
import com.cristinachdr.mitacografo.domain.model.validarViaje
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import javax.inject.Inject

/** Caso de uso: los viajes con sus jornadas y km, del más reciente al más antiguo. */
class ObtenerViajes
    @Inject
    constructor(
        private val viajes: ViajeRepository,
        private val jornadas: JornadaRepository,
    ) {
        operator fun invoke(): Flow<List<ResumenViaje>> =
            combine(viajes.observarTodos(), jornadas.observarTodas()) { v, j -> resumirViajes(v, j) }

        /** Un viaje concreto (o `null` si ya no existe), para su detalle. */
        fun uno(id: Long): Flow<ResumenViaje?> = invoke().map { lista -> lista.find { it.viaje.id == id } }
    }

/**
 * Caso de uso: empezar un viaje ahora.
 *
 * Si ya hay una jornada en curso (sin viaje), se apunta en él: así no hace falta esperar a la siguiente.
 */
class IniciarViaje
    @Inject
    constructor(
        private val viajes: ViajeRepository,
        private val jornadas: JornadaRepository,
        private val clock: Clock,
    ) {
        /** @return el id del viaje creado. */
        suspend operator fun invoke(nombre: String): ResultadoViaje<Long> {
            val viaje = Viaje(nombre = nombre.trim(), inicio = clock.instant(), zonaInicio = clock.zone)
            val errores = validarViaje(viaje, viajes.obtenerTodos(), clock.instant())
            if (errores.isNotEmpty()) return ResultadoViaje.Invalido(errores)

            val id = viajes.guardar(viaje)
            jornadas.obtenerAbierta()?.takeIf { it.viajeId == null }?.let { jornadas.guardar(it.copy(viajeId = id)) }
            return ResultadoViaje.Ok(id)
        }
    }

/** Caso de uso: terminar el viaje en curso ahora. Las jornadas siguientes ya no se apuntan en él. */
class FinalizarViaje
    @Inject
    constructor(
        private val viajes: ViajeRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(): ResultadoViaje<Unit> {
            val enCurso = viajes.obtenerEnCurso() ?: return ResultadoViaje.Invalido(ErrorViaje.SIN_VIAJE_EN_CURSO)
            val terminado = enCurso.copy(fin = clock.instant(), zonaFin = clock.zone)
            val errores = validarViaje(terminado, emptyList(), clock.instant())
            if (errores.isNotEmpty()) return ResultadoViaje.Invalido(errores)
            viajes.guardar(terminado)
            return ResultadoViaje.Ok(Unit)
        }
    }

/** Caso de uso: corregir el nombre, las fechas o las observaciones de un viaje. */
class GuardarViaje
    @Inject
    constructor(
        private val viajes: ViajeRepository,
        private val clock: Clock,
    ) {
        suspend operator fun invoke(viaje: Viaje): ResultadoViaje<Long> {
            if (viaje.id != Viaje.SIN_ID && viajes.obtenerPorId(viaje.id) == null) {
                return ResultadoViaje.Invalido(ErrorViaje.VIAJE_NO_EXISTE)
            }
            val limpio = viaje.copy(nombre = viaje.nombre.trim(), observaciones = viaje.observaciones.limpio())
            val otros = viajes.obtenerTodos().filter { it.id != limpio.id }
            val errores = validarViaje(limpio, otros, clock.instant())
            if (errores.isNotEmpty()) return ResultadoViaje.Invalido(errores)
            return ResultadoViaje.Ok(viajes.guardar(limpio))
        }
    }

/** Caso de uso: borrar un viaje. Sus jornadas **no** se borran: se quedan sin viaje. */
class BorrarViaje
    @Inject
    constructor(
        private val viajes: ViajeRepository,
    ) {
        suspend operator fun invoke(id: Long): ResultadoViaje<Unit> {
            if (viajes.obtenerPorId(id) == null) return ResultadoViaje.Invalido(ErrorViaje.VIAJE_NO_EXISTE)
            viajes.borrar(id)
            return ResultadoViaje.Ok(Unit)
        }
    }
