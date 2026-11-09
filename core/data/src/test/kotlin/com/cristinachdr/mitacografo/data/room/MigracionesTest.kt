package com.cristinachdr.mitacografo.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cristinachdr.mitacografo.data.room.jornada.RoomJornadaRepository
import com.cristinachdr.mitacografo.data.room.viaje.RoomViajeRepository
import com.cristinachdr.mitacografo.domain.model.Viaje
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.ZoneId

/**
 * Las migraciones con la base de datos de verdad de cada versión (los esquemas de `core/data/schemas/`).
 *
 * Es lo que pasa en el móvil del conductor al actualizar la app: si este test falla,
 * perdería sus jornadas.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class MigracionesTest {
    // La ruta completa, no solo el nombre: con Robolectric, MigrationTestHelper compara el nombre
    // con la ruta del archivo y, si no coinciden, no abre la base de datos.
    private val nombre: String =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getDatabasePath("migracion-test.db")
            .absolutePath

    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    fun `de la version 1 a la 2 se conservan las jornadas y los descansos, y ya hay viajes`() =
        runTest {
            // Una base de datos de la v0.1 con una jornada terminada, otra abierta y un descanso (inventados).
            helper.createDatabase(nombre, 1).use { db ->
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, pais_inicio, fin_epoch_ms, " +
                        "zona_fin, km_fin, pais_fin, conduccion_min, viaje_id) VALUES " +
                        "(1, 1000, 'Europe/Madrid', 250000, 'ES', 40000000, 'Europe/Madrid', 250480, 'FR', 480, NULL)",
                )
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, viaje_id) " +
                        "VALUES (2, 90000000, 'Europe/Kyiv', 250480, NULL)",
                )
                db.execSQL(
                    "INSERT INTO descanso (id, inicio_epoch_ms, zona_inicio, fin_epoch_ms, zona_fin, tipo) " +
                        "VALUES (1, 40000000, 'Europe/Madrid', 89000000, 'Europe/Madrid', 'DESCANSO')",
                )
            }

            // Room comprueba que el resultado es igual al esquema 2.json.
            helper.runMigrationsAndValidate(nombre, 2, true).close()

            val database =
                Room
                    .databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, nombre)
                    .allowMainThreadQueries()
                    .build()
            try {
                val jornadas = RoomJornadaRepository(database.jornadaDao())
                assertEquals(480, jornadas.obtenerPorId(1)?.kmRecorridos)
                assertEquals(ZoneId.of("Europe/Kyiv"), jornadas.obtenerAbierta()?.zonaInicio)
                assertEquals(1, database.descansoDao().obtenerTodos().size)

                // La tabla nueva funciona y la clave ajena también.
                val viajes = RoomViajeRepository(database.viajeDao())
                val id =
                    viajes.guardar(
                        Viaje(
                            nombre = "Ida",
                            inicio = Instant.ofEpochMilli(500),
                            zonaInicio = ZoneId.of("Europe/Madrid"),
                        ),
                    )
                jornadas.guardar(jornadas.obtenerPorId(1)!!.copy(viajeId = id))
                viajes.borrar(id)
                assertNull(jornadas.obtenerPorId(1)?.viajeId) // ON DELETE SET NULL: la jornada sigue ahí, sin viaje
            } finally {
                database.close()
            }
        }
}
