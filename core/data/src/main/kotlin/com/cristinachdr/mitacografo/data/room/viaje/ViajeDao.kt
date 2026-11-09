package com.cristinachdr.mitacografo.data.room.viaje

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ViajeDao {
    @Query("SELECT * FROM viaje ORDER BY inicio_epoch_ms DESC")
    fun observarTodos(): Flow<List<ViajeEntity>>

    @Query("SELECT * FROM viaje ORDER BY inicio_epoch_ms DESC")
    suspend fun obtenerTodos(): List<ViajeEntity>

    @Query("SELECT * FROM viaje WHERE id = :id")
    suspend fun obtenerPorId(id: Long): ViajeEntity?

    @Query("SELECT * FROM viaje WHERE fin_epoch_ms IS NULL ORDER BY inicio_epoch_ms DESC LIMIT 1")
    suspend fun obtenerEnCurso(): ViajeEntity?

    @Insert
    suspend fun insertar(viaje: ViajeEntity): Long

    @Update
    suspend fun actualizar(viaje: ViajeEntity): Int

    /** La clave ajena de `jornada` deja sus jornadas con `viaje_id = NULL` (no las borra). */
    @Query("DELETE FROM viaje WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM viaje")
    suspend fun borrarTodos()

    @Insert
    suspend fun insertarTodos(viajes: List<ViajeEntity>)

    @Transaction
    suspend fun reemplazarTodos(viajes: List<ViajeEntity>) {
        borrarTodos()
        insertarTodos(viajes)
    }
}
