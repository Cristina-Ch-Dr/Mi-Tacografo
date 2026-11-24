package com.cristinachdr.mitacografo.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cristinachdr.mitacografo.data.room.descanso.DescansoDao
import com.cristinachdr.mitacografo.data.room.descanso.DescansoEntity
import com.cristinachdr.mitacografo.data.room.jornada.JornadaDao
import com.cristinachdr.mitacografo.data.room.jornada.JornadaEntity
import com.cristinachdr.mitacografo.data.room.viaje.ViajeDao
import com.cristinachdr.mitacografo.data.room.viaje.ViajeEntity
import com.cristinachdr.mitacografo.domain.model.PAIS_CON_REGIONES
import com.cristinachdr.mitacografo.domain.model.REGIONES_ESPANA

/**
 * Base de datos SQLite de la app.
 *
 * Cada vez que cambie una tabla hay que subir [version] y escribir una migración,
 * para que el conductor no pierda sus datos al actualizar. El esquema de cada versión
 * queda guardado en `core/data/schemas/`.
 */
@Database(
    // La tabla `descanso` se creó vacía en la versión 1 para no tener que migrar
    // la base de datos del conductor; desde MT-20 se usa (el DAO no cambia el esquema).
    entities = [JornadaEntity::class, DescansoEntity::class, ViajeEntity::class],
    version = 4,
    exportSchema = true,
    // 1 → 2 (MT-18): tabla `viaje` y clave ajena `jornada.viaje_id`. Room la genera comparando
    // `schemas/1.json` y `2.json`: rehace la tabla `jornada` copiando todas sus filas.
    // Se prueba en `MigracionesTest` con la base de datos de la versión 1.
    // 2 → 3 (MT-53): dos columnas opcionales en `jornada` (conducción «hasta ahora» y antes de medianoche).
    // 3 → 4 (MT-55): dos columnas opcionales en `jornada` (comunidad autónoma al inicio y al final).
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4, spec = AppDatabase.ComunidadDesdeCiudad::class),
    ],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jornadaDao(): JornadaDao

    abstract fun descansoDao(): DescansoDao

    abstract fun viajeDao(): ViajeDao

    /**
     * 3 → 4 (MT-55): antes el conductor apuntaba la comunidad en «Ciudad» («NA», «CAT»…). En España, si la
     * ciudad es solo un código del tacógrafo, pasa a la columna nueva y la ciudad se queda vacía.
     * Lo mismo que `conComunidadDeCiudad()` del dominio, que se usa al restaurar copias antiguas.
     */
    class ComunidadDesdeCiudad : AutoMigrationSpec {
        override fun onPostMigrate(db: SupportSQLiteDatabase) {
            val codigos = REGIONES_ESPANA.joinToString(",") { "'$it'" }
            listOf("inicio", "fin").forEach { lado ->
                db.execSQL(
                    "UPDATE jornada SET region_$lado = UPPER(TRIM(ciudad_$lado)), ciudad_$lado = NULL " +
                        "WHERE pais_$lado = '$PAIS_CON_REGIONES' AND region_$lado IS NULL " +
                        "AND UPPER(TRIM(ciudad_$lado)) IN ($codigos)",
                )
            }
        }
    }

    companion object {
        const val NOMBRE = "mi_tacografo.db"
    }
}
