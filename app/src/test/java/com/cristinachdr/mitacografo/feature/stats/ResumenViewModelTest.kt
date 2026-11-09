package com.cristinachdr.mitacografo.feature.stats

import com.cristinachdr.mitacografo.MainDispatcherRule
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.usecase.Informe
import com.cristinachdr.mitacografo.domain.usecase.ObtenerResumen
import com.cristinachdr.mitacografo.domain.usecase.PrepararInforme
import com.cristinachdr.mitacografo.platform.ArchivosInforme
import com.cristinachdr.mitacografo.platform.GeneradorPdf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ResumenViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val madrid = ZoneId.of("Europe/Madrid")

    /** "Ahora": jueves 12/11/2026. La semana actual empieza el lunes 9. */
    private val reloj = Clock.fixed(LocalDateTime.of(2026, 11, 12, 20, 0).atZone(madrid).toInstant(), madrid)
    private val lunes = LocalDate.of(2026, 11, 9)

    private fun jornada(
        id: Long,
        dia: Int,
        km: Int,
    ) = Jornada(
        id = id,
        inicio = LocalDateTime.of(2026, 11, dia, 6, 0).atZone(madrid).toInstant(),
        zonaInicio = madrid,
        kmInicio = 0,
        fin = LocalDateTime.of(2026, 11, dia, 17, 0).atZone(madrid).toInstant(),
        zonaFin = madrid,
        kmFin = km,
    )

    private class FakePdf : GeneradorPdf {
        val informes = mutableListOf<Informe>()

        override suspend fun generar(informe: Informe): ByteArray {
            informes += informe
            return byteArrayOf(1, 2, 3)
        }
    }

    private class FakeArchivos(
        val falla: Boolean = false,
    ) : ArchivosInforme {
        val guardados = mutableMapOf<String, ByteArray>()

        override suspend fun guardar(
            destino: String,
            bytes: ByteArray,
        ) {
            if (falla) throw IOException("lleno")
            guardados[destino] = bytes
        }

        override suspend fun paraCompartir(
            nombre: String,
            bytes: ByteArray,
        ): String = "content://prueba/$nombre"
    }

    private val pdf = FakePdf()

    private fun viewModel(archivos: ArchivosInforme = FakeArchivos()): ResumenViewModel {
        val jornadas = FakeJornadaRepository(listOf(jornada(1, 3, 400), jornada(2, 10, 500)))
        val settings = FakeSettingsRepository()
        val obtener = ObtenerResumen(jornadas, FakeDescansoRepository(), settings, reloj)
        return ResumenViewModel(obtener, PrepararInforme(obtener, settings, reloj), pdf, archivos)
    }

    @Test
    fun `empieza en la semana de hoy y no deja pasar al futuro`() {
        val estado = viewModel().uiState.value

        assertEquals(Periodo(lunes, 1), estado.periodo)
        assertEquals(500, estado.resumen?.km)
        assertFalse(estado.puedeAvanzar)
    }

    @Test
    fun `con dos semanas se ve esta y la anterior`() {
        val vm = viewModel()
        vm.onSemanas(2)

        assertEquals(Periodo(lunes.minusWeeks(1), 2), vm.uiState.value.periodo)
        assertEquals(
            900,
            vm.uiState.value.resumen
                ?.km,
        )
    }

    @Test
    fun `ir a la semana anterior y volver`() {
        val vm = viewModel()
        vm.onAnterior()
        assertEquals(
            400,
            vm.uiState.value.resumen
                ?.km,
        )
        assertTrue(vm.uiState.value.puedeAvanzar)

        vm.onSiguiente()
        assertEquals(
            lunes,
            vm.uiState.value.periodo
                ?.desde,
        )
    }

    @Test
    fun `guardar el PDF escribe el del periodo que se ve`() {
        val archivos = FakeArchivos()
        val vm = viewModel(archivos)
        vm.onSemanas(2)

        vm.onGuardarEn("content://descargas/informe.pdf")

        assertEquals(3, archivos.guardados["content://descargas/informe.pdf"]?.size)
        assertEquals(
            LocalDate.of(2026, 11, 15),
            pdf.informes
                .single()
                .resumen.hasta,
        )
        assertEquals(MensajeResumen.PDF_GUARDADO, vm.uiState.value.mensaje)
        assertEquals("mi-tacografo-2026-11-02_2026-11-15.pdf", vm.nombreArchivo())
    }

    @Test
    fun `compartir deja el PDF listo para el menu de compartir`() {
        val vm = viewModel()
        vm.onCompartir()

        assertEquals("content://prueba/mi-tacografo-2026-11-09_2026-11-15.pdf", vm.uiState.value.compartir)
        assertNull(vm.uiState.value.mensaje)

        vm.onCompartido()
        assertNull(vm.uiState.value.compartir)
    }

    @Test
    fun `si no se puede guardar se avisa`() {
        val vm = viewModel(FakeArchivos(falla = true))
        vm.onGuardarEn("content://lleno")

        assertEquals(MensajeResumen.PDF_ERROR, vm.uiState.value.mensaje)
        assertFalse(vm.uiState.value.exportando)
    }
}
