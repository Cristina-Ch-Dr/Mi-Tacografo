package com.cristinachdr.mitacografo.data.room.viaje

import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/** Adaptador de salida: implementa el puerto [ViajeRepository] del dominio con Room. */
internal class RoomViajeRepository
    @Inject
    constructor(
        private val dao: ViajeDao,
    ) : ViajeRepository {
        override fun observarTodos(): Flow<List<Viaje>> =
            dao.observarTodos().map { filas ->
                filas.map { it.aDominio() }
            }

        override suspend fun obtenerTodos(): List<Viaje> = dao.obtenerTodos().map { it.aDominio() }

        override suspend fun obtenerPorId(id: Long): Viaje? = dao.obtenerPorId(id)?.aDominio()

        override suspend fun obtenerEnCurso(): Viaje? = dao.obtenerEnCurso()?.aDominio()

        override suspend fun guardar(viaje: Viaje): Long {
            if (viaje.id == Viaje.SIN_ID) return dao.insertar(viaje.aEntity())

            val actualizadas = dao.actualizar(viaje.aEntity())
            check(actualizadas == 1) { "No existe el viaje ${viaje.id}" }
            return viaje.id
        }

        override suspend fun borrar(id: Long) = dao.borrar(id)

        override suspend fun reemplazarTodos(viajes: List<Viaje>) = dao.reemplazarTodos(viajes.map { it.aEntity() })
    }

internal fun ViajeEntity.aDominio(): Viaje =
    Viaje(
        id = id,
        nombre = nombre,
        inicio = Instant.ofEpochMilli(inicioEpochMs),
        zonaInicio = ZoneId.of(zonaInicio),
        fin = finEpochMs?.let(Instant::ofEpochMilli),
        zonaFin = zonaFin?.let(ZoneId::of),
        observaciones = observaciones,
    )

internal fun Viaje.aEntity(): ViajeEntity =
    ViajeEntity(
        id = id,
        nombre = nombre,
        inicioEpochMs = inicio.toEpochMilli(),
        zonaInicio = zonaInicio.id,
        finEpochMs = fin?.toEpochMilli(),
        zonaFin = zonaFin?.id,
        observaciones = observaciones,
    )
