package com.cristinachdr.mitacografo.data.room.descanso

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Consultas SQL de la tabla `descanso`. Añadir un DAO no cambia el esquema de la base de datos. */
@Dao
interface DescansoDao {
    @Query("SELECT * FROM descanso ORDER BY inicio_epoch_ms DESC")
    fun observarTodos(): Flow<List<DescansoEntity>>

    @Query("SELECT * FROM descanso WHERE fin_epoch_ms IS NULL ORDER BY inicio_epoch_ms DESC LIMIT 1")
    fun observarEnCurso(): Flow<DescansoEntity?>

    @Query("SELECT * FROM descanso ORDER BY inicio_epoch_ms DESC")
    suspend fun obtenerTodos(): List<DescansoEntity>

    @Query("SELECT * FROM descanso WHERE id = :id")
    suspend fun obtenerPorId(id: Long): DescansoEntity?

    @Query("SELECT * FROM descanso WHERE fin_epoch_ms IS NULL ORDER BY inicio_epoch_ms DESC LIMIT 1")
    suspend fun obtenerEnCurso(): DescansoEntity?

    /** @return el id generado. */
    @Insert
    suspend fun insertar(descanso: DescansoEntity): Long

    /** @return filas actualizadas (0 si el id no existe). */
    @Update
    suspend fun actualizar(descanso: DescansoEntity): Int

    @Query("DELETE FROM descanso WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM descanso")
    suspend fun borrarTodos()

    @Insert
    suspend fun insertarTodos(descansos: List<DescansoEntity>)

    /** Las dos cosas o ninguna, como en `JornadaDao`. */
    @Transaction
    suspend fun reemplazarTodos(descansos: List<DescansoEntity>) {
        borrarTodos()
        insertarTodos(descansos)
    }
}
