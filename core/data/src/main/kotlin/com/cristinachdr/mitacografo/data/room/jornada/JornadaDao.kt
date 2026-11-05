package com.cristinachdr.mitacografo.data.room.jornada

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Consultas SQL de la tabla `jornada`. Room genera la implementación al compilar. */
@Dao
interface JornadaDao {
    @Query("SELECT * FROM jornada WHERE fin_epoch_ms IS NULL ORDER BY inicio_epoch_ms DESC LIMIT 1")
    fun observarAbierta(): Flow<JornadaEntity?>

    @Query("SELECT * FROM jornada ORDER BY inicio_epoch_ms DESC")
    fun observarTodas(): Flow<List<JornadaEntity>>

    @Query("SELECT * FROM jornada WHERE id = :id")
    suspend fun obtenerPorId(id: Long): JornadaEntity?

    @Query("SELECT * FROM jornada WHERE fin_epoch_ms IS NULL ORDER BY inicio_epoch_ms DESC LIMIT 1")
    suspend fun obtenerAbierta(): JornadaEntity?

    @Query("SELECT * FROM jornada WHERE fin_epoch_ms IS NOT NULL ORDER BY fin_epoch_ms DESC LIMIT 1")
    suspend fun obtenerUltimaFinalizada(): JornadaEntity?

    /** @return el id generado. */
    @Insert
    suspend fun insertar(jornada: JornadaEntity): Long

    /** @return filas actualizadas (0 si el id no existe). */
    @Update
    suspend fun actualizar(jornada: JornadaEntity): Int

    @Query("DELETE FROM jornada WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM jornada")
    suspend fun borrarTodas()

    @Insert
    suspend fun insertarTodas(jornadas: List<JornadaEntity>)

    /** Las dos cosas o ninguna: si falla al insertar, no se queda la tabla vacía. */
    @Transaction
    suspend fun reemplazarTodas(jornadas: List<JornadaEntity>) {
        borrarTodas()
        insertarTodas(jornadas)
    }
}
