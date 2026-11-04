package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Repositorio en memoria para los tests: se comporta como el de Room, pero sin base de datos.
 *
 * Es un "fake" (implementación real y sencilla) en lugar de un mock: los tests comprueban
 * el resultado final, no qué métodos se llamaron, y así no se rompen al refactorizar.
 */
class FakeJornadaRepository(
    iniciales: List<Jornada> = emptyList(),
) : JornadaRepository {
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1
    private val jornadas = MutableStateFlow(iniciales)

    /** Todas las jornadas guardadas, en el orden en que se crearon. */
    val guardadas: List<Jornada> get() = jornadas.value

    override fun observarAbierta(): Flow<Jornada?> = jornadas.map { lista -> lista.abierta() }

    override fun observarTodas(): Flow<List<Jornada>> = jornadas.map { lista -> lista.sortedByDescending { it.inicio } }

    override suspend fun obtenerPorId(id: Long): Jornada? = jornadas.value.find { it.id == id }

    override suspend fun obtenerAbierta(): Jornada? = jornadas.value.abierta()

    override suspend fun obtenerUltimaFinalizada(): Jornada? {
        val finalizadas = jornadas.value.filterNot { it.abierta }
        return finalizadas.maxByOrNull { it.fin!! }
    }

    override suspend fun guardar(jornada: Jornada): Long {
        if (jornada.id == Jornada.SIN_ID) {
            val nueva = jornada.copy(id = siguienteId++)
            jornadas.update { it + nueva }
            return nueva.id
        }
        check(jornadas.value.any { it.id == jornada.id }) { "No existe la jornada ${jornada.id}" }
        jornadas.update { lista -> lista.map { if (it.id == jornada.id) jornada else it } }
        return jornada.id
    }

    override suspend fun borrar(id: Long) = jornadas.update { lista -> lista.filterNot { it.id == id } }

    private fun List<Jornada>.abierta() = filter { it.abierta }.maxByOrNull { it.inicio }
}
