package com.cristinachdr.mitacografo.domain.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * El motor con las semanas reales de la libreta (anonimizadas, [LibretaAnonimizada]) comparado con lo que
 * el conductor sumó a mano y sabe que pasó (MT-30). Sin países: la libreta anonimizada no los tiene.
 */
class LibretaRealTest {
    private val zona = LibretaAnonimizada.zona
    private val lunes28 = LocalDate.of(2026, 9, 28)
    private val lunes5 = LocalDate.of(2026, 10, 5)
    private val lunes12 = LocalDate.of(2026, 10, 12)

    private fun d(
        h: Long,
        m: Long,
    ) = Duration.ofHours(h).plusMinutes(m)

    private fun evaluar(ahora: LocalDateTime = LocalDateTime.of(2026, 12, 1, 12, 0)) =
        MotorReglas().evaluar(
            ContextoReglas(
                LibretaAnonimizada.jornadas,
                LibretaAnonimizada.descansos,
                ahora = ahora.atZone(zona).toInstant(),
                paisEmpresa = "ES",
                paisResidencia = "UA",
            ),
        )

    private fun InformeReglas.semana(lunes: LocalDate) = semanas.single { it.lunes == lunes }

    private operator fun Intervalo.contains(valor: Duration) = valor in min..max

    @Test
    fun `la conduccion de cada semana cuadra con la S que sumo a mano`() {
        val informe = evaluar()

        // Semana del 12/10: todo apuntado, el valor es exacto y es su S.
        assertEquals(Intervalo.exacto(d(29, 16)), informe.semana(lunes12).conduccion)
        // Semana del 5/10: la jornada del domingo 4 cruza la semana. Él la repartió 3 h 52 / 4 h 29;
        // el motor da un intervalo que contiene su S (43 h 08) y no se inventa el reparto.
        assertTrue(d(43, 8) in informe.semana(lunes5).conduccion)
        assertTrue(informe.semana(lunes5).resultado(ReglaId.CONDUCCION_SEMANAL) is Resultado.Cumple)
    }

    @Test
    fun `la bisemana del 12 cumple y contiene su BS de 72 h 24`() {
        val semana = evaluar().semana(lunes12)
        val bisemanal = semana.resultado(ReglaId.CONDUCCION_BISEMANAL)

        assertTrue(bisemanal is Resultado.Cumple)
        assertTrue(d(72, 24) in bisemanal.detalle.valor!!)
    }

    @Test
    fun `la bisemana del 5 (BS 90 h 01) no se puede comprobar - la semana del 28 no esta entera`() {
        // Sus 90 h 01 se pasan 1 min del límite, pero de lunes 28 a miércoles 30 no hay nada apuntado:
        // el motor no puede saberlo y lo dice, en vez de dar «Cumple».
        val bisemanal = evaluar().semana(lunes5).resultado(ReglaId.CONDUCCION_BISEMANAL)

        assertEquals(MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO, (bisemanal as Resultado.NoVerificado).motivo)
        assertTrue(d(90, 1) in bisemanal.detalle.valor!!)
    }

    @Test
    fun `las jornadas de mas de 9 h son las del 3, el 7 y el 12 de octubre`() {
        val ampliadas =
            evaluar()
                .semanas
                .flatMap { it.jornadas }
                .filter { it.resultado(ReglaId.CONDUCCION_DIARIA).detalle.ampliada == true }
                .map {
                    it.jornada.inicio
                        .atZone(zona)
                        .dayOfMonth
                }

        assertEquals(listOf(3, 7, 12), ampliadas.sorted())
        assertTrue(
            evaluar()
                .semanas
                .flatMap {
                    it.jornadas
                }.none { it.resultado(ReglaId.CONDUCCION_DIARIA) is Resultado.Excedido },
        )
    }

    @Test
    fun `los dos semanales reducidos dejan 21 h y 6 h 40 de deuda`() {
        val deudas = evaluar().deudas

        assertEquals(listOf(d(21, 0), d(6, 40)), deudas.map { it.horas })
        // La de 6 h 40 la paga el diario de 19 h 24 del 12/10 (8 h 24 de más). La de 21 h no se ve pagada,
        // pero tras el 17/10 se fue de vacaciones sin apuntarlas: no se puede saber.
        assertEquals(listOf(EstadoDeuda.NO_VERIFICADO, EstadoDeuda.COMPENSADA), deudas.map { it.estado })
    }

    @Test
    fun `dos reducidos seguidos sin pais apuntado no se pueden comprobar`() {
        val r = evaluar().semana(lunes5).resultado(ReglaId.SEMANAL_DOS_SEMANAS)
        assertEquals(MotivoNoVerificado.SIN_PAIS_DESCANSO, (r as Resultado.NoVerificado).motivo)
    }

    @Test
    fun `los semanales empiezan dentro de las 144 h`() {
        val informe = evaluar()
        listOf(lunes5, lunes12).forEach { lunes ->
            assertTrue(informe.semana(lunes).resultado(ReglaId.SEIS_PERIODOS) is Resultado.Cumple, "semana del $lunes")
        }
        assertTrue(informe.semanas.none { it.lunes == lunes28.minusWeeks(1) })
    }
}
