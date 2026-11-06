package com.cristinachdr.mitacografo.data.room.descanso

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fila de la tabla `descanso`: pausas dentro de la jornada y descansos entre jornadas.
 *
 * La tabla se creó (vacía) en la primera versión de la base de datos, para no tener que migrar
 * la del móvil del conductor. Desde MT-20 la usan [DescansoDao] y [RoomDescansoRepository].
 *
 * La clasificación (diario normal, reducido, fraccionado, semanal…) no se guarda: la calcula
 * el motor de reglas a partir de las horas.
 */
@Entity(
    tableName = "descanso",
    indices = [
        Index("inicio_epoch_ms"),
        Index("fin_epoch_ms"),
    ],
)
data class DescansoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "inicio_epoch_ms") val inicioEpochMs: Long,
    @ColumnInfo(name = "zona_inicio") val zonaInicio: String,
    /** `null` = pausa en curso (para el futuro botón «Iniciar pausa / Terminar pausa»). */
    @ColumnInfo(name = "fin_epoch_ms") val finEpochMs: Long?,
    @ColumnInfo(name = "zona_fin") val zonaFin: String?,
    /** "PAUSA" (dentro de la jornada) o "DESCANSO" (entre jornadas). */
    @ColumnInfo(name = "tipo") val tipo: String,
    /** País donde descansa (ISO alfa-2): lo necesita la regla de semanales reducidos fuera de casa. */
    @ColumnInfo(name = "pais") val pais: String?,
    @ColumnInfo(name = "observaciones") val observaciones: String?,
)
