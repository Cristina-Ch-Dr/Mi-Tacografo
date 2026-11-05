package com.cristinachdr.mitacografo.feature.settings

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.BackupPort
import com.cristinachdr.mitacografo.domain.usecase.ExportarCopia
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import com.cristinachdr.mitacografo.domain.usecase.LeerCopia
import com.cristinachdr.mitacografo.domain.usecase.RestaurarCopia
import com.cristinachdr.mitacografo.platform.ArchivosCopia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

class CopiaViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 5, 10, 0).atZone(madrid).toInstant(), madrid)

    private fun jornada(id: Long) =
        Jornada(
            id = id,
            inicio = LocalDateTime.of(2026, 11, id.toInt(), 6, 0).atZone(madrid).toInstant(),
            zonaInicio = madrid,
            kmInicio = 1_000,
            fin = LocalDateTime.of(2026, 11, id.toInt(), 17, 0).atZone(madrid).toInstant(),
            zonaFin = madrid,
            kmFin = 1_500,
        )

    /** Formato falso: el "archivo" es un número que apunta a la copia guardada en memoria. */
    private class FakeBackup : BackupPort {
        val copias = mutableListOf<CopiaSeguridad>()

        override fun codificar(copia: CopiaSeguridad): String {
            copias += copia
            return (copias.size - 1).toString()
        }

        override fun decodificar(texto: String): CopiaSeguridad =
            texto.toIntOrNull()?.let { copias.getOrNull(it) }
                ?: throw CopiaInvalidaException(ErrorCopia.NO_ES_UNA_COPIA)
    }

    /** Carpeta falsa: un mapa de "rutas" a contenido. */
    private class FakeArchivos : ArchivosCopia {
        val archivos = mutableMapOf<String, String>()

        override suspend fun escribir(
            destino: String,
            texto: String,
        ) {
            if (destino.startsWith("solo-lectura")) throw IOException("No se puede escribir")
            archivos[destino] = texto
        }

        override suspend fun leer(origen: String): String = archivos[origen] ?: throw IOException("No existe")
    }

    private val backup = FakeBackup()
    private val archivos = FakeArchivos()

    private fun viewModel(
        repo: FakeJornadaRepository,
        settings: FakeSettingsRepository = FakeSettingsRepository(),
    ): CopiaViewModel {
        val gestor = GestorAvisos(repo, settings, FakeAlarmScheduler(), reloj)
        return CopiaViewModel(
            ExportarCopia(repo, settings, backup, reloj),
            LeerCopia(backup),
            RestaurarCopia(repo, settings, gestor),
            repo,
            archivos,
            reloj,
        )
    }

    @Test
    fun `el nombre propuesto lleva la fecha`() {
        assertEquals("mi-tacografo-2026-11-05.json", viewModel(FakeJornadaRepository()).nombreArchivo())
    }

    @Test
    fun `guardar escribe la copia en el archivo elegido`() {
        val vm = viewModel(FakeJornadaRepository(listOf(jornada(2), jornada(3))))

        vm.onGuardarEn("content://descargas/copia.json")

        assertEquals(MensajeCopia.Guardada, vm.uiState.value.mensaje)
        val guardada = backup.decodificar(archivos.archivos.getValue("content://descargas/copia.json"))
        assertEquals(2, guardada.jornadas.size)
        assertFalse(vm.uiState.value.trabajando)
    }

    @Test
    fun `si no se puede escribir se dice`() {
        val vm = viewModel(FakeJornadaRepository())

        vm.onGuardarEn("solo-lectura/copia.json")

        assertEquals(MensajeCopia.NoSePudoGuardar, vm.uiState.value.mensaje)
    }

    @Test
    fun `restaurar pide confirmacion con el resumen y despues reemplaza los datos`() {
        archivos.archivos["copia"] =
            backup.codificar(
                CopiaSeguridad(reloj.instant(), listOf(jornada(2), jornada(3)), Ajustes(paisEmpresa = "ES")),
            )
        val repo = FakeJornadaRepository(listOf(jornada(4)))
        val settings = FakeSettingsRepository()
        val vm = viewModel(repo, settings)

        vm.onAbrir("copia")

        // Todavía no se ha tocado nada: solo se enseña el resumen.
        assertEquals(
            2,
            vm.uiState.value.porRestaurar
                ?.jornadas
                ?.size,
        )
        assertEquals(1, vm.uiState.value.jornadasActuales)
        assertEquals(listOf(jornada(4)), repo.guardadas)

        vm.onConfirmarRestaurar()

        assertEquals(listOf(jornada(2), jornada(3)), repo.guardadas)
        assertEquals("ES", settings.actuales.paisEmpresa)
        assertEquals(MensajeCopia.Restaurada, vm.uiState.value.mensaje)
        assertNull(vm.uiState.value.porRestaurar)
    }

    @Test
    fun `cancelar no cambia nada`() {
        archivos.archivos["copia"] = backup.codificar(CopiaSeguridad(reloj.instant(), emptyList(), Ajustes()))
        val repo = FakeJornadaRepository(listOf(jornada(4)))
        val vm = viewModel(repo)

        vm.onAbrir("copia")
        vm.onCancelarRestaurar()

        assertNull(vm.uiState.value.porRestaurar)
        assertEquals(listOf(jornada(4)), repo.guardadas)
    }

    @Test
    fun `un archivo que no es una copia se dice y no se toca nada`() {
        archivos.archivos["foto.jpg"] = "no soy una copia"
        val repo = FakeJornadaRepository(listOf(jornada(4)))
        val vm = viewModel(repo)

        vm.onAbrir("foto.jpg")
        vm.onAbrir("no-existe")

        assertEquals(MensajeCopia.NoSePudoLeer, vm.uiState.value.mensaje)
        vm.onAbrir("foto.jpg")
        assertEquals(MensajeCopia.NoValida(ErrorCopia.NO_ES_UNA_COPIA), vm.uiState.value.mensaje)
        assertNull(vm.uiState.value.porRestaurar)
        assertEquals(listOf(jornada(4)), repo.guardadas)
    }
}
