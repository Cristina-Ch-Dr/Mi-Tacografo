package com.cristinachdr.mitacografo.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cristinachdr.mitacografo.data.room.descanso.DescansoDao
import com.cristinachdr.mitacografo.data.room.descanso.DescansoEntity
import com.cristinachdr.mitacografo.data.room.jornada.JornadaDao
import com.cristinachdr.mitacografo.data.room.jornada.JornadaEntity

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
    entities = [JornadaEntity::class, DescansoEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun jornadaDao(): JornadaDao

    abstract fun descansoDao(): DescansoDao

    companion object {
        const val NOMBRE = "mi_tacografo.db"
    }
}
