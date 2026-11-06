package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.fake.FakeAlarmScheduler
import com.cristinachdr.mitacografo.domain.fake.FakeDescansoRepository
import com.cristinachdr.mitacografo.domain.fake.FakeJornadaRepository
import com.cristinachdr.mitacografo.domain.fake.FakeSettingsRepository
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.port.BackupPort
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class CopiaDeSeguridadTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private val reloj = Clock.fixed(hora(3, 20), madrid)

    private val lunes =
        Jornada(
            id = 1,
            inicio = hora(2, 6),
            zonaInicio = madrid,
            kmInicio = 1_000,
            fin = hora(2, 17),
            zonaFin = madrid,
            kmFin = 1_500,
        )
    private val enCurso = Jornada(id = 2, inicio = hora(3, 19), zonaInicio = madrid, kmInicio = 1_500)

    /** "Formato" falso: guarda la copia en memoria y devuelve una clave. Lo real (JSON) se prueba en core:data. */
    private class FakeBackup : BackupPort {
        private val copias = mutableMapOf<String, CopiaSeguridad>()

        override fun codificar(copia: CopiaSeguridad): String = "copia-${copias.size}".also { copias[it] = copia }

        override fun decodificar(texto: String): CopiaSeguridad =
            copias[texto] ?: throw CopiaInvalidaException(ErrorCopia.NO_ES_UNA_COPIA)
    }

    private val backup = FakeBackup()

    @Test
    fun `exportar y restaurar en otro movil deja los mismos datos y avisos`() =
        runTest {
            val ajustes = Ajustes(paisEmpresa = "ES", nombreConductor = "Conductor")
            val origen = FakeJornadaRepository(listOf(lunes, enCurso))
            val descanso =
                Descanso(
                    id = 4,
                    inicio = hora(2, 17),
                    zonaInicio = madrid,
                    fin = hora(3, 6),
                    zonaFin = madrid,
                    tipo = TipoDescanso.DESCANSO,
                    pais = "FR",
                )
            val texto =
                ExportarCopia(
                    origen,
                    FakeDescansoRepository(listOf(descanso)),
                    FakeSettingsRepository(ajustes),
                    backup,
                    reloj,
                )()

            // Móvil nuevo: vacío.
            val destino = FakeJornadaRepository()
            val descansosDestino = FakeDescansoRepository()
            val settingsDestino = FakeSettingsRepository()
            val alarmas = FakeAlarmScheduler()
            val copia = (LeerCopia(backup)(texto) as ResultadoLectura.Ok).copia
            RestaurarCopia(
                destino,
                descansosDestino,
                settingsDestino,
                GestorAvisos(destino, settingsDestino, alarmas, reloj),
            )(
                copia,
            )

            assertEquals(listOf(lunes, enCurso), destino.guardadas.sortedBy { it.id })
            assertEquals(listOf(descanso), descansosDestino.guardados)
            assertEquals(ajustes, settingsDestino.actuales)
            assertEquals(hora(3, 20), copia.creada)
            // La jornada en curso empezó a las 19:00: los avisos de 12 h 30… vuelven a estar programados.
            assertTrue(alarmas.programados.isNotEmpty())
            assertTrue(alarmas.programados.all { it.jornadaId == enCurso.id })
        }

    @Test
    fun `restaurar reemplaza lo que habia y quita los avisos de la jornada de antes`() =
        runTest {
            val ahoraAbierta = Jornada(id = 7, inicio = hora(3, 18), zonaInicio = madrid, kmInicio = 9_000)
            val repo = FakeJornadaRepository(listOf(ahoraAbierta))
            val settings = FakeSettingsRepository()
            val alarmas = FakeAlarmScheduler()
            val gestor = GestorAvisos(repo, settings, alarmas, reloj)
            gestor.reprogramar(ahoraAbierta)

            RestaurarCopia(
                repo,
                FakeDescansoRepository(),
                settings,
                gestor,
            )(CopiaSeguridad(hora(3, 20), listOf(lunes), Ajustes()))

            assertEquals(listOf(lunes), repo.guardadas)
            assertTrue(alarmas.programados.isEmpty())
        }

    @Test
    fun `un archivo que no es una copia no se puede leer`() {
        assertEquals(ResultadoLectura.Invalida(ErrorCopia.NO_ES_UNA_COPIA), LeerCopia(backup)("hola"))
    }

    @Test
    fun `una copia con dos jornadas abiertas o ids repetidos no se acepta`() {
        val dosAbiertas =
            backup.codificar(
                CopiaSeguridad(hora(3, 20), listOf(enCurso, enCurso.copy(id = 3)), Ajustes()),
            )
        val repetidos = backup.codificar(CopiaSeguridad(hora(3, 20), listOf(lunes, lunes), Ajustes()))

        assertEquals(ResultadoLectura.Invalida(ErrorCopia.DATOS_INCORRECTOS), LeerCopia(backup)(dosAbiertas))
        assertEquals(ResultadoLectura.Invalida(ErrorCopia.DATOS_INCORRECTOS), LeerCopia(backup)(repetidos))
    }

    @Test
    fun `una copia vacia es valida`() {
        val vacia = backup.codificar(CopiaSeguridad(hora(3, 20), emptyList(), Ajustes()))

        assertTrue(LeerCopia(backup)(vacia) is ResultadoLectura.Ok)
    }
}
