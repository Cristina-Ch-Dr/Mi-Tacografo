package com.cristinachdr.mitacografo.data.room.gasto

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cristinachdr.mitacografo.domain.model.Gasto
import com.cristinachdr.mitacografo.domain.port.GastoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

/**
 * Fila de la tabla `gasto` (desde la versión 5, MT-58). La fecha va como número de día (desde 1970) y el
 * importe en céntimos: así no hay decimales en la base de datos.
 */
@Entity(tableName = "gasto")
data class GastoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "fecha_dia") val fechaDia: Long,
    @ColumnInfo(name = "descripcion") val descripcion: String?,
    @ColumnInfo(name = "importe_centimos") val importeCentimos: Long,
)

@Dao
interface GastoDao {
    @Query("SELECT * FROM gasto ORDER BY fecha_dia DESC, id DESC")
    fun observarTodos(): Flow<List<GastoEntity>>

    @Query("SELECT * FROM gasto ORDER BY fecha_dia DESC, id DESC")
    suspend fun obtenerTodos(): List<GastoEntity>

    @Insert
    suspend fun insertar(gasto: GastoEntity): Long

    @Update
    suspend fun actualizar(gasto: GastoEntity): Int

    @Query("DELETE FROM gasto WHERE id = :id")
    suspend fun borrar(id: Long)

    @Query("DELETE FROM gasto")
    suspend fun borrarTodos()

    @Insert
    suspend fun insertarTodos(gastos: List<GastoEntity>)

    @Transaction
    suspend fun reemplazarTodos(gastos: List<GastoEntity>) {
        borrarTodos()
        insertarTodos(gastos)
    }
}

/** Adaptador de salida: implementa el puerto [GastoRepository] del dominio con Room. */
internal class RoomGastoRepository
    @Inject
    constructor(
        private val dao: GastoDao,
    ) : GastoRepository {
        override fun observarTodos(): Flow<List<Gasto>> =
            dao.observarTodos().map { filas ->
                filas.map { it.aDominio() }
            }

        override suspend fun obtenerTodos(): List<Gasto> = dao.obtenerTodos().map { it.aDominio() }

        override suspend fun guardar(gasto: Gasto): Long {
            if (gasto.id == Gasto.SIN_ID) return dao.insertar(gasto.aEntity())
            check(dao.actualizar(gasto.aEntity()) == 1) { "No existe el gasto ${gasto.id}" }
            return gasto.id
        }

        override suspend fun borrar(id: Long) = dao.borrar(id)

        override suspend fun reemplazarTodos(gastos: List<Gasto>) = dao.reemplazarTodos(gastos.map { it.aEntity() })
    }

private val CIEN = BigDecimal(100)

internal fun GastoEntity.aDominio(): Gasto =
    Gasto(
        id = id,
        fecha = LocalDate.ofEpochDay(fechaDia),
        descripcion = descripcion,
        importe = BigDecimal.valueOf(importeCentimos, 2),
    )

internal fun Gasto.aEntity(): GastoEntity =
    GastoEntity(
        id = id,
        fechaDia = fecha.toEpochDay(),
        descripcion = descripcion,
        importeCentimos = importe.multiply(CIEN).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact(),
    )
