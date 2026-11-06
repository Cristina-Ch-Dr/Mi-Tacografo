package com.cristinachdr.mitacografo.data.room.descanso

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cristinachdr.mitacografo.data.room.AppDatabase
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
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

/** El adaptador de Room de los descansos, con una base de datos en memoria (Robolectric). */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class RoomDescansoRepositoryTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private lateinit var database: AppDatabase
    private lateinit var repository: RoomDescansoRepository

    @Before
    fun crearBaseDeDatos() {
        database =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        repository = RoomDescansoRepository(database.descansoDao())
    }

    @After
    fun cerrarBaseDeDatos() = database.close()

    private fun hora(
        dia: Int,
        hora: Int,
        zona: ZoneId = madrid,
    ) = LocalDateTime.of(2026, 11, dia, hora, 0).atZone(zona).toInstant()

    private val descanso =
        Descanso(
            inicio = hora(2, 18, kyiv),
            zonaInicio = kyiv,
            fin = hora(3, 5),
            zonaFin = madrid,
            tipo = TipoDescanso.DESCANSO,
            pais = "UA",
            observaciones = "Nota",
        )

    @Test
    fun `guarda y lee un descanso con todos sus datos`() =
        runTest {
            val id = repository.guardar(descanso)

            assertEquals(descanso.copy(id = id), repository.obtenerPorId(id))
        }

    @Test
    fun `la pausa en curso es la que no tiene fin`() =
        runTest {
            repository.guardar(descanso)
            val pausa = Descanso(inicio = hora(3, 10), zonaInicio = madrid, tipo = TipoDescanso.PAUSA)
            val id = repository.guardar(pausa)

            assertEquals(id, repository.obtenerEnCurso()?.id)
            assertEquals(id, repository.observarEnCurso().first()?.id)

            repository.guardar(pausa.copy(id = id, fin = hora(3, 11), zonaFin = madrid))
            assertNull(repository.obtenerEnCurso())
        }

    @Test
    fun `la lista sale de la más reciente a la más antigua`() =
        runTest {
            val primero = repository.guardar(descanso)
            val segundo = repository.guardar(descanso.copy(inicio = hora(4, 18), fin = hora(5, 5)))

            assertEquals(listOf(segundo, primero), repository.observarTodos().first().map { it.id })
        }

    @Test
    fun `borrar y reemplazar todos`() =
        runTest {
            val id = repository.guardar(descanso)
            repository.borrar(id)
            assertEquals(emptyList<Descanso>(), repository.obtenerTodos())

            val copia = listOf(descanso.copy(id = 10), descanso.copy(id = 11, inicio = hora(4, 18), fin = hora(5, 5)))
            repository.reemplazarTodos(copia)
            assertEquals(listOf(11L, 10L), repository.obtenerTodos().map { it.id })
        }
}
