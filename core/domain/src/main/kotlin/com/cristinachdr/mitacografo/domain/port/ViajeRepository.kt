package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.Viaje
import kotlinx.coroutines.flow.Flow

/** Puerto de salida para guardar y leer viajes. La implementación con Room vive en `core:data`. */
interface ViajeRepository {
    /** Todos, del más reciente al más antiguo. */
    fun observarTodos(): Flow<List<Viaje>>

    suspend fun obtenerTodos(): List<Viaje>

    suspend fun obtenerPorId(id: Long): Viaje?

    /** El que sigue en curso, o `null`. */
    suspend fun obtenerEnCurso(): Viaje?

    /** Inserta si su id es [Viaje.SIN_ID] o actualiza si ya existe. @return el id. */
    suspend fun guardar(viaje: Viaje): Long

    /** Borra el viaje. Sus jornadas no se borran: se quedan sin viaje. */
    suspend fun borrar(id: Long)

    /** Borra todos y guarda estos (con sus ids), de una vez. Lo usa la restauración de la copia. */
    suspend fun reemplazarTodos(viajes: List<Viaje>)
}
