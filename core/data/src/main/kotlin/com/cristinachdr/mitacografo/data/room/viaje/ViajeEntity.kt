package com.cristinachdr.mitacografo.data.room.viaje

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fila de la tabla `viaje` (desde la versión 2 de la base de datos, MT-18). Los km no se guardan. */
@Entity(
    tableName = "viaje",
    indices = [Index("inicio_epoch_ms"), Index("fin_epoch_ms")],
)
data class ViajeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "nombre") val nombre: String,
    @ColumnInfo(name = "inicio_epoch_ms") val inicioEpochMs: Long,
    @ColumnInfo(name = "zona_inicio") val zonaInicio: String,
    /** `null` = viaje en curso. */
    @ColumnInfo(name = "fin_epoch_ms") val finEpochMs: Long?,
    @ColumnInfo(name = "zona_fin") val zonaFin: String?,
    @ColumnInfo(name = "observaciones") val observaciones: String?,
)
