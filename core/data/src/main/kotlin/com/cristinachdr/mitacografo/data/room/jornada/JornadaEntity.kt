package com.cristinachdr.mitacografo.data.room.jornada

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `jornada`.
 *
 * Solo usa tipos simples (números y textos) para que el esquema sea fácil de leer y de migrar.
 * La conversión a [com.cristinachdr.mitacografo.domain.model.Jornada] está en `JornadaMapper.kt`.
 * Esta clase nunca sale del módulo de datos.
 */
@Entity(
    tableName = "jornada",
    indices = [
        Index("inicio_epoch_ms"),
        Index("fin_epoch_ms"),
        Index("viaje_id"),
    ],
)
data class JornadaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "inicio_epoch_ms") val inicioEpochMs: Long,
    @ColumnInfo(name = "zona_inicio") val zonaInicio: String,
    @ColumnInfo(name = "km_inicio") val kmInicio: Int,
    @ColumnInfo(name = "pais_inicio") val paisInicio: String?,
    @ColumnInfo(name = "ciudad_inicio") val ciudadInicio: String?,
    @ColumnInfo(name = "fin_epoch_ms") val finEpochMs: Long?,
    @ColumnInfo(name = "zona_fin") val zonaFin: String?,
    @ColumnInfo(name = "km_fin") val kmFin: Int?,
    @ColumnInfo(name = "pais_fin") val paisFin: String?,
    @ColumnInfo(name = "ciudad_fin") val ciudadFin: String?,
    @ColumnInfo(name = "conduccion_min") val conduccionMin: Int?,
    // La clave ajena a `viaje` se añadirá con la tabla de viajes (MT-18).
    @ColumnInfo(name = "viaje_id") val viajeId: Long?,
    @ColumnInfo(name = "observaciones") val observaciones: String?,
)
