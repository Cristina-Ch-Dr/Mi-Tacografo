package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.port.DescansoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Repositorio de descansos en memoria para los tests (igual que [FakeJornadaRepository]). */
class FakeDescansoRepository(
    iniciales: List<Descanso> = emptyList(),
) : DescansoRepository {
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1
    private val descansos = MutableStateFlow(iniciales)

    /** Todos los guardados, en el orden en que se crearon. */
    val guardados: List<Descanso> get() = descansos.value

    override fun observarTodos(): Flow<List<Descanso>> =
        descansos.map { lista -> lista.sortedByDescending { it.inicio } }

    override fun observarEnCurso(): Flow<Descanso?> = descansos.map { lista -> lista.enCurso() }

    override suspend fun obtenerTodos(): List<Descanso> = descansos.value.sortedByDescending { it.inicio }

    override suspend fun obtenerPorId(id: Long): Descanso? = descansos.value.find { it.id == id }

    override suspend fun obtenerEnCurso(): Descanso? = descansos.value.enCurso()

    override suspend fun guardar(descanso: Descanso): Long {
        if (descanso.id == Descanso.SIN_ID) {
            val nuevo = descanso.copy(id = siguienteId++)
            descansos.update { it + nuevo }
            return nuevo.id
        }
        check(descansos.value.any { it.id == descanso.id }) { "No existe el descanso ${descanso.id}" }
        descansos.update { lista -> lista.map { if (it.id == descanso.id) descanso else it } }
        return descanso.id
    }

    override suspend fun borrar(id: Long) = descansos.update { lista -> lista.filterNot { it.id == id } }

    override suspend fun reemplazarTodos(descansos: List<Descanso>) {
        this.descansos.value = descansos
        siguienteId = (descansos.maxOfOrNull { it.id } ?: 0) + 1
    }

    private fun List<Descanso>.enCurso() = filter { it.enCurso }.maxByOrNull { it.inicio }
}
