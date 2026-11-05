package com.cristinachdr.mitacografo.data.room.descanso

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cristinachdr.mitacografo.data.room.AppDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** La tabla `descanso` existe desde la versión 1 de la base de datos, aunque aún no se use. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class TablaDescansoTest {
    private lateinit var database: AppDatabase

    @Before
    fun crearBaseDeDatos() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }

    @After
    fun cerrarBaseDeDatos() = database.close()

    @Test
    fun `la tabla descanso tiene sus columnas y admite una pausa en curso`() {
        val db = database.openHelper.writableDatabase
        val columnas =
            db.query("PRAGMA table_info(descanso)").use { c ->
                buildList { while (c.moveToNext()) add(c.getString(c.getColumnIndexOrThrow("name"))) }
            }
        assertEquals(
            listOf("id", "inicio_epoch_ms", "zona_inicio", "fin_epoch_ms", "zona_fin", "tipo", "pais", "observaciones"),
            columnas,
        )

        // Sin hora de fin = pausa todavía en curso.
        db.execSQL(
            "INSERT INTO descanso (inicio_epoch_ms, zona_inicio, fin_epoch_ms, zona_fin, tipo, pais, observaciones) " +
                "VALUES (1000, 'Europe/Madrid', NULL, NULL, 'PAUSA', 'ES', NULL)",
        )
        db.query("SELECT COUNT(*) FROM descanso WHERE fin_epoch_ms IS NULL").use { c ->
            c.moveToFirst()
            assertEquals(1, c.getInt(0))
        }
    }
}
