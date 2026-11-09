package com.cristinachdr.mitacografo.data.room.viaje

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cristinachdr.mitacografo.data.room.AppDatabase
import com.cristinachdr.mitacografo.domain.model.Viaje
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RoomViajeRepositoryTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private lateinit var database: AppDatabase
    private lateinit var repository: RoomViajeRepository

    @Before
    fun crearBaseDeDatos() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomViajeRepository(database.viajeDao())
    }

    @After
    fun cerrarBaseDeDatos() = database.close()

    private fun hora(dia: Int) = LocalDateTime.of(2026, 11, dia, 6, 0).atZone(madrid).toInstant()

    @Test
    fun `guarda, lee y termina un viaje`() =
        runTest {
            val id =
                repository.guardar(
                    Viaje(nombre = "Ida", inicio = hora(2), zonaInicio = madrid, observaciones = "Nota"),
                )
            assertEquals(id, repository.obtenerEnCurso()?.id)
            assertEquals("Nota", repository.obtenerPorId(id)?.observaciones)

            repository.guardar(repository.obtenerPorId(id)!!.copy(fin = hora(5), zonaFin = madrid))
            assertNull(repository.obtenerEnCurso())
        }

    @Test
    fun `la lista sale del mas reciente al mas antiguo y se puede reemplazar entera`() =
        runTest {
            val primero =
                repository.guardar(
                    Viaje(nombre = "A", inicio = hora(2), zonaInicio = madrid, fin = hora(4), zonaFin = madrid),
                )
            val segundo = repository.guardar(Viaje(nombre = "B", inicio = hora(6), zonaInicio = madrid))
            assertEquals(listOf(segundo, primero), repository.observarTodos().first().map { it.id })

            repository.reemplazarTodos(listOf(Viaje(id = 7, nombre = "C", inicio = hora(1), zonaInicio = madrid)))
            assertEquals(listOf(7L), repository.obtenerTodos().map { it.id })
        }
}
