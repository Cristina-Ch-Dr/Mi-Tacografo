package com.cristinachdr.mitacografo.data.room.jornada

import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Adaptador de salida: implementa el puerto [JornadaRepository] del dominio con Room. */
internal class RoomJornadaRepository
    @Inject
    constructor(
        private val dao: JornadaDao,
    ) : JornadaRepository {
        override fun observarAbierta(): Flow<Jornada?> = dao.observarAbierta().map { it?.aDominio() }

        override fun observarTodas(): Flow<List<Jornada>> =
            dao.observarTodas().map { filas -> filas.map { it.aDominio() } }

        override suspend fun obtenerPorId(id: Long): Jornada? = dao.obtenerPorId(id)?.aDominio()

        override suspend fun obtenerAbierta(): Jornada? = dao.obtenerAbierta()?.aDominio()

        override suspend fun obtenerUltimaFinalizada(): Jornada? = dao.obtenerUltimaFinalizada()?.aDominio()

        override suspend fun guardar(jornada: Jornada): Long {
            if (jornada.id == Jornada.SIN_ID) return dao.insertar(jornada.aEntity())

            val actualizadas = dao.actualizar(jornada.aEntity())
            check(actualizadas == 1) { "No existe la jornada ${jornada.id}" }
            return jornada.id
        }

        override suspend fun borrar(id: Long) = dao.borrar(id)
    }
