package com.cristinachdr.mitacografo.data.room

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cristinachdr.mitacografo.data.room.gasto.RoomGastoRepository
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
import java.time.Duration
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
    private val nombre: String = ruta("migracion-test.db")

    /** Cada test con su archivo: con el mismo, el segundo no puede abrirlo. */
    private fun ruta(archivo: String): String =
        ApplicationProvider
            .getApplicationContext<Context>()
            .getDatabasePath(archivo)
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

    @Test
    fun `de la version 2 a la 3 se conservan las jornadas y se pueden guardar los datos de conduccion nuevos`() =
        runTest {
            val nombre = ruta("migracion-2-a-3.db")
            helper.createDatabase(nombre, 2).use { db ->
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, fin_epoch_ms, zona_fin, " +
                        "km_fin, conduccion_min) VALUES (1, 1000, 'Europe/Madrid', 250000, 40000000, " +
                        "'Europe/Madrid', 250480, 480)",
                )
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio) " +
                        "VALUES (2, 90000000, 'Europe/Kyiv', 250480)",
                )
            }

            helper.runMigrationsAndValidate(nombre, 3, true).close()

            val database =
                Room
                    .databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, nombre)
                    .allowMainThreadQueries()
                    // Sin WAL: con Robolectric en Windows, Room no puede abrir en WAL la base de datos que
                    // MigrationTestHelper acaba de dejar en la última versión (SQLITE_CANTOPEN).
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
            try {
                val jornadas = RoomJornadaRepository(database.jornadaDao())
                val terminada = jornadas.obtenerPorId(1)!!
                assertEquals(Duration.ofHours(8), terminada.conduccionTacografo)
                assertNull(terminada.conduccionAntesMedianoche)

                val abierta = jornadas.obtenerAbierta()!!
                assertNull(abierta.conduccionApuntadaEn)
                jornadas.guardar(
                    abierta.copy(
                        conduccionTacografo = Duration.ofMinutes(200),
                        conduccionApuntadaEn = Instant.ofEpochMilli(100_000_000),
                    ),
                )
                assertEquals(Instant.ofEpochMilli(100_000_000), jornadas.obtenerAbierta()?.conduccionApuntadaEn)
            } finally {
                database.close()
            }
        }

    @Test
    fun `de la version 3 a la 4 se conservan las jornadas y se puede guardar la comunidad`() =
        runTest {
            val nombre = ruta("migracion-3-a-4.db")
            helper.createDatabase(nombre, 3).use { db ->
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, pais_inicio, fin_epoch_ms, " +
                        "zona_fin, km_fin, pais_fin, conduccion_min, conduccion_antes_medianoche_min) VALUES " +
                        "(1, 1000, 'Europe/Madrid', 250000, 'ES', 40000000, 'Europe/Madrid', 250480, 'ES', 480, 120)",
                )
                // Como en la copia del conductor: la comunidad apuntada en «Ciudad».
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, pais_inicio, ciudad_inicio, " +
                        "fin_epoch_ms, zona_fin, km_fin, pais_fin, ciudad_fin) VALUES (2, 50000000, 'Europe/Madrid', " +
                        "250480, 'ES', ' na ', 60000000, 'Europe/Paris', 250900, 'FR', 'CAT')",
                )
            }

            helper.runMigrationsAndValidate(nombre, 4, true).close()

            val database =
                Room
                    .databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, nombre)
                    .allowMainThreadQueries()
                    // Sin WAL, por lo mismo que en la migración de 2 a 3.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
            try {
                val jornadas = RoomJornadaRepository(database.jornadaDao())
                val jornada = jornadas.obtenerPorId(1)!!
                assertEquals(Duration.ofMinutes(120), jornada.conduccionAntesMedianoche)
                assertNull(jornada.regionInicio)

                jornadas.guardar(jornada.copy(regionInicio = "AN", regionFin = "M"))
                assertEquals("M", jornadas.obtenerPorId(1)?.regionFin)

                // En España pasa de «Ciudad» a su columna; en Francia («CAT» como ciudad) no se toca.
                val antigua = jornadas.obtenerPorId(2)!!
                assertEquals("NA", antigua.regionInicio)
                assertNull(antigua.ciudadInicio)
                assertNull(antigua.regionFin)
                assertEquals("CAT", antigua.ciudadFin)
            } finally {
                database.close()
            }
        }

    @Test
    fun `de la version 4 a la 5 se conservan las jornadas y ya se pueden guardar gastos`() =
        runTest {
            val nombre = ruta("migracion-4-a-5.db")
            helper.createDatabase(nombre, 4).use { db ->
                db.execSQL(
                    "INSERT INTO jornada (id, inicio_epoch_ms, zona_inicio, km_inicio, pais_inicio, region_inicio) " +
                        "VALUES (1, 1000, 'Europe/Madrid', 250000, 'ES', 'NA')",
                )
            }

            helper.runMigrationsAndValidate(nombre, 5, true).close()

            val database =
                Room
                    .databaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java, nombre)
                    .allowMainThreadQueries()
                    // Sin WAL, por lo mismo que en la migración de 2 a 3.
                    .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                    .build()
            try {
                assertEquals("NA", RoomJornadaRepository(database.jornadaDao()).obtenerPorId(1)?.regionInicio)

                val gastos = RoomGastoRepository(database.gastoDao())
                val gasto =
                    com.cristinachdr.mitacografo.domain.model.Gasto(
                        fecha = java.time.LocalDate.of(2026, 11, 22),
                        descripcion = "Parking",
                        importe = java.math.BigDecimal("12.50"),
                    )
                val id = gastos.guardar(gasto)
                assertEquals(listOf(gasto.copy(id = id)), gastos.obtenerTodos())
            } finally {
                database.close()
            }
        }
}
