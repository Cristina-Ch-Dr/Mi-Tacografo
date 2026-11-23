package com.cristinachdr.mitacografo.data.room

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.cristinachdr.mitacografo.data.room.descanso.DescansoDao
import com.cristinachdr.mitacografo.data.room.descanso.DescansoEntity
import com.cristinachdr.mitacografo.data.room.jornada.JornadaDao
import com.cristinachdr.mitacografo.data.room.jornada.JornadaEntity
import com.cristinachdr.mitacografo.data.room.viaje.ViajeDao
import com.cristinachdr.mitacografo.data.room.viaje.ViajeEntity

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
    version = 3,
    exportSchema = true,
    // 1 → 2 (MT-18): tabla `viaje` y clave ajena `jornada.viaje_id`. Room la genera comparando
    // `schemas/1.json` y `2.json`: rehace la tabla `jornada` copiando todas sus filas.
    // Se prueba en `MigracionesTest` con la base de datos de la versión 1.
    // 2 → 3 (MT-53): dos columnas opcionales en `jornada` (conducción «hasta ahora» y antes de medianoche).
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jornadaDao(): JornadaDao

    abstract fun descansoDao(): DescansoDao

    abstract fun viajeDao(): ViajeDao

    companion object {
        const val NOMBRE = "mi_tacografo.db"
    }
}
