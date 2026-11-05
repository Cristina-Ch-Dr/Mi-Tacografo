package com.cristinachdr.mitacografo.data.datastore

import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.PreferencesSerializer
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.time.Duration
import java.time.ZoneId

/**
 * DataStore no necesita Android: se prueba con un archivo temporal, sin Robolectric.
 *
 * Se guarda con [OkioStorage] en vez de con el almacenamiento de Android: el de Android renombra el
 * archivo con `File.renameTo`, que en Windows no puede sobrescribir uno que ya existe, y la segunda
 * escritura fallaba al pasar los tests en el PC. En el móvil (Linux) los dos funcionan igual.
 */
class DataStoreSettingsRepositoryTest {
    @get:Rule
    val carpeta = TemporaryFolder()

    private fun TestScope.repositorio(archivo: File = carpeta.newFile("ajustes.preferences_pb").also { it.delete() }) =
        DataStoreSettingsRepository(
            PreferenceDataStoreFactory.create(
                storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) { archivo.toOkioPath() },
                scope = backgroundScope,
            ),
        )

    @Test
    fun `sin nada guardado se usan los ajustes por defecto`() =
        runTest {
            assertEquals(Ajustes(), repositorio().observar().first())
        }

    @Test
    fun `los ajustes se guardan y se leen igual`() =
        runTest {
            val repo = repositorio()
            val nuevos =
                Ajustes(
                    avisos =
                        listOf(
                            AvisoConfig(Duration.ofMinutes(750), activo = false),
                            AvisoConfig(Duration.ofHours(15)),
                        ),
                    paisEmpresa = "ES",
                    paisResidencia = "UA",
                    nombreConductor = "Conductor de prueba",
                    zonaReferencia = ZoneId.of("Europe/Madrid"),
                    paisesHabituales = setOf("DE", "FR"),
                    continentes = setOf(Continente.EUROPA, Continente.ASIA),
                )

            repo.actualizar { nuevos }

            assertEquals(nuevos, repo.observar().first())
        }

    @Test
    fun `quitar todos los paises y continentes vuelve a dejarlos vacios`() =
        runTest {
            val repo = repositorio()
            repo.actualizar { it.copy(paisesHabituales = setOf("DE"), continentes = setOf(Continente.AFRICA)) }

            repo.actualizar { it.copy(paisesHabituales = emptySet(), continentes = emptySet()) }

            assertEquals(Ajustes(), repo.observar().first())
        }

    @Test
    fun `un texto vacio borra el valor`() =
        runTest {
            val repo = repositorio()
            repo.actualizar { it.copy(nombreConductor = "Nombre") }

            repo.actualizar { it.copy(nombreConductor = "  ") }

            assertNull(repo.observar().first().nombreConductor)
        }

    @Test
    fun `los avisos se codifican como texto y vuelven igual`() {
        val texto = codificarAvisos(AvisoConfig.POR_DEFECTO.mapIndexed { i, a -> a.copy(activo = i != 1) })

        assertEquals("750:1,780:0,870:1,900:1", texto)
        assertEquals(AvisoConfig.POR_DEFECTO.mapIndexed { i, a -> a.copy(activo = i != 1) }, decodificarAvisos(texto))
    }

    @Test
    fun `sin avisos se queda sin avisos (no vuelven los de por defecto)`() {
        assertEquals(emptyList<AvisoConfig>(), decodificarAvisos(codificarAvisos(emptyList())))
    }

    @Test
    fun `un texto de avisos estropeado no rompe la app`() {
        assertNull(decodificarAvisos("esto:no:vale"))
        assertNull(decodificarAvisos("abc:1"))
    }
}
