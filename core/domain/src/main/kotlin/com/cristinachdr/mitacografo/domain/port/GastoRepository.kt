package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.Gasto
import kotlinx.coroutines.flow.Flow

/** Puerto de salida: los gastos de su bolsillo (MT-58). */
interface GastoRepository {
    /** Del más reciente al más antiguo; se actualiza solo al añadir o borrar. */
    fun observarTodos(): Flow<List<Gasto>>

    suspend fun obtenerTodos(): List<Gasto>

    /** @return el id del gasto guardado. */
    suspend fun guardar(gasto: Gasto): Long

    suspend fun borrar(id: Long)

    /** Para restaurar una copia: borra todos y guarda estos, de una vez. */
    suspend fun reemplazarTodos(gastos: List<Gasto>)
}
