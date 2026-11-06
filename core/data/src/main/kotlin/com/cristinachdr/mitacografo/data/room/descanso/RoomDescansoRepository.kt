package com.cristinachdr.mitacografo.data.room.descanso

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/** Adaptador de salida: implementa el puerto [DescansoRepository] del dominio con Room. */
internal class RoomDescansoRepository
    @Inject
    constructor(
        private val dao: DescansoDao,
    ) : DescansoRepository {
        override fun observarTodos(): Flow<List<Descanso>> =
            dao.observarTodos().map { filas ->
                filas.map { it.aDominio() }
            }

        override fun observarEnCurso(): Flow<Descanso?> = dao.observarEnCurso().map { it?.aDominio() }

        override suspend fun obtenerTodos(): List<Descanso> = dao.obtenerTodos().map { it.aDominio() }

        override suspend fun obtenerPorId(id: Long): Descanso? = dao.obtenerPorId(id)?.aDominio()

        override suspend fun obtenerEnCurso(): Descanso? = dao.obtenerEnCurso()?.aDominio()

        override suspend fun guardar(descanso: Descanso): Long {
            if (descanso.id == Descanso.SIN_ID) return dao.insertar(descanso.aEntity())

            val actualizadas = dao.actualizar(descanso.aEntity())
            check(actualizadas == 1) { "No existe el descanso ${descanso.id}" }
            return descanso.id
        }

        override suspend fun borrar(id: Long) = dao.borrar(id)

        override suspend fun reemplazarTodos(descansos: List<Descanso>) =
            dao.reemplazarTodos(descansos.map { it.aEntity() })
    }
