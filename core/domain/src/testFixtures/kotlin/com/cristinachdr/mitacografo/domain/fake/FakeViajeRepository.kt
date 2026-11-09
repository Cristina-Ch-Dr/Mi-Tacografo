package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.Viaje
import com.cristinachdr.mitacografo.domain.port.ViajeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * Repositorio de viajes en memoria para los tests.
 *
 * @param jornadas si se pasa, al borrar un viaje sus jornadas se quedan sin viaje, como hace la clave
 *   ajena de Room (`ON DELETE SET NULL`).
 */
class FakeViajeRepository(
    iniciales: List<Viaje> = emptyList(),
    private val jornadas: FakeJornadaRepository? = null,
) : ViajeRepository {
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1
    private val viajes = MutableStateFlow(iniciales)

    val guardados: List<Viaje> get() = viajes.value

    override fun observarTodos(): Flow<List<Viaje>> = viajes.map { lista -> lista.sortedByDescending { it.inicio } }

    override suspend fun obtenerTodos(): List<Viaje> = viajes.value.sortedByDescending { it.inicio }

    override suspend fun obtenerPorId(id: Long): Viaje? = viajes.value.find { it.id == id }

    override suspend fun obtenerEnCurso(): Viaje? = viajes.value.filter { it.enCurso }.maxByOrNull { it.inicio }

    override suspend fun guardar(viaje: Viaje): Long {
        if (viaje.id == Viaje.SIN_ID) {
            val nuevo = viaje.copy(id = siguienteId++)
            viajes.update { it + nuevo }
            return nuevo.id
        }
        check(viajes.value.any { it.id == viaje.id }) { "No existe el viaje ${viaje.id}" }
        viajes.update { lista -> lista.map { if (it.id == viaje.id) viaje else it } }
        return viaje.id
    }

    override suspend fun borrar(id: Long) {
        viajes.update { lista -> lista.filterNot { it.id == id } }
        jornadas?.let { repo ->
            repo.guardadas.filter { it.viajeId == id }.forEach { repo.guardar(it.copy(viajeId = null)) }
        }
    }

    override suspend fun reemplazarTodos(viajes: List<Viaje>) {
        this.viajes.value = viajes
        siguienteId = (viajes.maxOfOrNull { it.id } ?: 0) + 1
    }
}
