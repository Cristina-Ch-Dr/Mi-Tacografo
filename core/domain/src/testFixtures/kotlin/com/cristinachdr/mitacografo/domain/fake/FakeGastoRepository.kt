package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.Gasto
import com.cristinachdr.mitacografo.domain.port.GastoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** Gastos en memoria para los tests. */
class FakeGastoRepository(
    iniciales: List<Gasto> = emptyList(),
) : GastoRepository {
    private var siguienteId = (iniciales.maxOfOrNull { it.id } ?: 0) + 1
    private val gastos = MutableStateFlow(iniciales)

    val guardados: List<Gasto> get() = gastos.value

    override fun observarTodos(): Flow<List<Gasto>> = gastos.map { lista -> lista.sortedByDescending { it.fecha } }

    override suspend fun obtenerTodos(): List<Gasto> = gastos.value.sortedByDescending { it.fecha }

    override suspend fun guardar(gasto: Gasto): Long {
        val id = if (gasto.id == Gasto.SIN_ID) siguienteId++ else gasto.id
        gastos.update { lista -> lista.filterNot { it.id == id } + gasto.copy(id = id) }
        return id
    }

    override suspend fun borrar(id: Long) = gastos.update { lista -> lista.filterNot { it.id == id } }

    override suspend fun reemplazarTodos(gastos: List<Gasto>) {
        this.gastos.value = gastos
    }
}
