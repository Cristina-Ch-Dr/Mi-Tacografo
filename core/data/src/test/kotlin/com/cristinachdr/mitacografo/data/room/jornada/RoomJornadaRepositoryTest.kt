package com.cristinachdr.mitacografo.data.room.jornada

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cristinachdr.mitacografo.data.room.AppDatabase
import com.cristinachdr.mitacografo.domain.model.Jornada
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Prueba el adaptador de Room de verdad (SQL incluido), con una base de datos en memoria.
 * Robolectric simula Android en el ordenador, así que no hace falta emulador.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RoomJornadaRepositoryTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomJornadaRepository

    @Before
    fun crearBaseDeDatos() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomJornadaRepository(database.jornadaDao())
    }

    @After
    fun cerrarBaseDeDatos() = database.close()

    private fun hora(
        dia: Int,
        hora: Int,
        zona: ZoneId = madrid,
    ) = LocalDateTime.of(2026, 11, dia, hora, 0).atZone(zona).toInstant()

    private fun abierta(
        dia: Int = 2,
        kmInicio: Int = 100_000,
    ) = Jornada(inicio = hora(dia, 6), zonaInicio = madrid, kmInicio = kmInicio)

    private fun finalizada(
        dia: Int,
        kmInicio: Int,
        kmFin: Int,
    ) = abierta(dia, kmInicio).copy(fin = hora(dia, 17), zonaFin = madrid, kmFin = kmFin)

    @Test
    fun `una jornada completa se guarda y se lee sin perder ningun dato`() =
        runTest {
            val original =
                Jornada(
                    inicio = hora(2, 6),
                    zonaInicio = madrid,
                    kmInicio = 245_310,
                    paisInicio = "ES",
                    ciudadInicio = "Zaragoza",
                    fin = hora(2, 18, kyiv),
                    zonaFin = kyiv,
                    kmFin = 245_872,
                    paisFin = "UA",
                    ciudadFin = "Lviv",
                    conduccionTacografo = Duration.ofHours(8).plusMinutes(45),
                    observaciones = "Carga en muelle 3",
                )

            val id = repository.guardar(original)

            assertNotEquals(Jornada.SIN_ID, id)
            assertEquals(original.copy(id = id), repository.obtenerPorId(id))
        }

    @Test
    fun `guardar una jornada que ya existe la actualiza`() =
        runTest {
            val id = repository.guardar(abierta())
            val terminada = repository.obtenerPorId(id)!!.copy(fin = hora(2, 17), zonaFin = madrid, kmFin = 100_500)

            assertEquals(id, repository.guardar(terminada))
            assertEquals(500, repository.obtenerPorId(id)?.kmRecorridos)
            assertEquals(1, repository.observarTodas().first().size)
        }

    @Test(expected = IllegalStateException::class)
    fun `actualizar una jornada que no existe es un error`() =
        runTest {
            repository.guardar(abierta().copy(id = 999))
        }

    @Test
    fun `la jornada abierta es la que no tiene fin`() =
        runTest {
            repository.guardar(finalizada(dia = 2, kmInicio = 100_000, kmFin = 100_400))
            assertNull(repository.obtenerAbierta())
            assertNull(repository.observarAbierta().first())

            val id = repository.guardar(abierta(dia = 3, kmInicio = 100_400))

            assertEquals(id, repository.obtenerAbierta()?.id)
            assertEquals(id, repository.observarAbierta().first()?.id)
        }

    @Test
    fun `la ultima finalizada sirve para proponer el km de inicio`() =
        runTest {
            repository.guardar(finalizada(dia = 2, kmInicio = 100_000, kmFin = 100_400))
            repository.guardar(finalizada(dia = 3, kmInicio = 100_400, kmFin = 100_950))
            repository.guardar(abierta(dia = 4, kmInicio = 100_950))

            assertEquals(100_950, repository.obtenerUltimaFinalizada()?.kmFin)
        }

    @Test
    fun `todas las jornadas salen de la mas reciente a la mas antigua`() =
        runTest {
            repository.guardar(finalizada(dia = 3, kmInicio = 0, kmFin = 1))
            repository.guardar(finalizada(dia = 2, kmInicio = 0, kmFin = 1))
            repository.guardar(finalizada(dia = 4, kmInicio = 0, kmFin = 1))

            val dias = repository.observarTodas().first().map { it.inicio.atZone(madrid).dayOfMonth }

            assertEquals(listOf(4, 3, 2), dias)
        }

    @Test
    fun `borrar elimina la jornada`() =
        runTest {
            val id = repository.guardar(abierta())

            repository.borrar(id)

            assertNull(repository.obtenerPorId(id))
            assertEquals(emptyList<Jornada>(), repository.observarTodas().first())
        }
}
