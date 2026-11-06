package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.Descanso
import kotlinx.coroutines.flow.Flow

/** Puerto de salida para guardar y leer pausas y descansos. La implementación con Room vive en `core:data`. */
interface DescansoRepository {
    /** Todos, del más reciente al más antiguo. */
    fun observarTodos(): Flow<List<Descanso>>

    /** El que sigue en curso (sin hora de fin), o `null`. */
    fun observarEnCurso(): Flow<Descanso?>

    suspend fun obtenerTodos(): List<Descanso>

    suspend fun obtenerPorId(id: Long): Descanso?

    suspend fun obtenerEnCurso(): Descanso?

    /**
     * Inserta si su id es [Descanso.SIN_ID] o actualiza si ya existe.
     *
     * @return el id del descanso guardado.
     */
    suspend fun guardar(descanso: Descanso): Long

    suspend fun borrar(id: Long)

    /** Borra todos y guarda estos (con sus ids), de una vez. Lo usa la restauración de la copia. */
    suspend fun reemplazarTodos(descansos: List<Descanso>)
}
