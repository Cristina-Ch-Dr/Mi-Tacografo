package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reglamento (CE) 561/2006, art. 8.6 ter y 8.7 (espec. §8.4; casos T13, T14 y T17e de §13). Datos inventados.
 *
 * Semanas de 4 en 4 desde el lunes 2 de noviembre de 2026. Cada semana: trabajo de lunes a sábado sin
 * huecos y el semanal el sábado a las 06:00. La deuda de un reducido de la semana del 2 vence al acabar
 * la 3ª semana siguiente: el lunes 30 de noviembre a las 00:00.
 */
class CompensacionesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val lunes2 = LocalDate.of(2026, 11, 2)
    private val lunes9 = LocalDate.of(2026, 11, 9)
    private val lunes16 = LocalDate.of(2026, 11, 16)

    private fun hora(
        dia: Int,
        h: Int,
    ): Instant =
        if (dia > 30) {
            LocalDateTime.of(2026, 12, dia - 30, h, 0).atZone(madrid).toInstant()
        } else {
            LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()
        }

    private fun h(horas: Long) = Duration.ofHours(horas)

    private var ids = 1L

    private fun descanso(
        inicio: Instant,
        dura: Duration,
        pais: String? = "DE",
    ) = Descanso(
        id = ids++,
        inicio = inicio,
        zonaInicio = madrid,
        fin = inicio.plus(dura),
        zonaFin = madrid,
        tipo = TipoDescanso.DESCANSO,
        pais = pais,
    )

    private fun jornada(
        inicio: Instant,
        dura: Duration,
    ) = Jornada(
        id = ids++,
        inicio = inicio,
        zonaInicio = madrid,
        kmInicio = 0,
        fin = inicio.plus(dura),
        zonaFin = madrid,
        kmFin = 400,
        conduccionTacografo = h(7),
    )

    /** Jornadas de 13 h y diarios de 11 h de [desde] a [hasta], sin huecos. */
    private fun trabajo(
        desde: Instant,
        hasta: Instant,
    ): Pair<List<Jornada>, List<Descanso>> {
        val jornadas = mutableListOf<Jornada>()
        val descansos = mutableListOf<Descanso>()
        var t = desde
        while (t < hasta) {
            val finJornada = minOf(t.plus(h(13)), hasta)
            jornadas += jornada(t, Duration.between(t, finJornada))
            t = finJornada
            if (t >= hasta) break
            val finDescanso = minOf(t.plus(h(11)), hasta)
            descansos += descanso(t, Duration.between(t, finDescanso), pais = "ES")
            t = finDescanso
        }
        return jornadas to descansos
    }

    /** Una semana tras otra con estos semanales (cada uno, el sábado de su semana a las 06:00). */
    private fun informe(
        semanales: List<Duration>,
        ahora: Instant = LocalDateTime.of(2027, 2, 1, 12, 0).atZone(madrid).toInstant(),
    ): InformeReglas {
        val jornadas = mutableListOf<Jornada>()
        val descansos = mutableListOf(descanso(hora(2, 6).minus(h(48)), h(48)))
        var finAnterior = hora(2, 6)
        semanales.forEachIndexed { i, dura ->
            val sabado = hora(7 + 7 * i, 6)
            val (j, d) = trabajo(finAnterior, sabado)
            jornadas += j
            descansos += d
            descansos += descanso(sabado, dura)
            finAnterior = sabado.plus(dura)
        }
        val (j, d) = trabajo(finAnterior, hora(7 + 7 * semanales.size, 6))
        jornadas += j
        descansos += d
        return MotorReglas().evaluar(
            ContextoReglas(jornadas, descansos, ahora = ahora, paisEmpresa = "ES", paisResidencia = "UA"),
        )
    }

    private fun InformeReglas.de(
        lunes: LocalDate,
        regla: ReglaId,
    ): Resultado? = semanas.single { it.lunes == lunes }.resultados.singleOrNull { it.regla == regla }

    @Test
    fun `T13 - reducido de 24 h compensado con 21 h unidas al semanal dos semanas despues`() {
        // Semana del 2: reducido de 24 h (deuda 21 h). Semana del 9: normal. Semana del 16: 45 + 21 = 66 h.
        val informe = informe(listOf(h(24), h(45), h(66)))
        val deuda = informe.deudas.single()

        assertEquals(h(21), deuda.horas)
        assertEquals(EstadoDeuda.COMPENSADA, deuda.estado)
        assertTrue(informe.de(lunes2, ReglaId.COMPENSACION) is Resultado.Cumple)
    }

    @Test
    fun `T14 - el mismo reducido sin compensar al acabar la 3a semana siguiente vence`() {
        val informe = informe(listOf(h(24), h(45), h(45), h(45)))

        assertEquals(EstadoDeuda.VENCIDA, informe.deudas.single().estado)
        assertEquals(
            LocalDateTime.of(2026, 11, 30, 0, 0).atZone(madrid).toInstant(),
            informe.deudas.single().fechaLimite,
        )
        assertTrue(informe.de(lunes2, ReglaId.COMPENSACION) is Resultado.Excedido)
    }

    @Test
    fun `mientras no ha vencido la deuda esta pendiente y aun no hay resultado`() {
        val pronto = LocalDateTime.of(2026, 11, 20, 12, 0).atZone(madrid).toInstant()
        val informe = informe(listOf(h(24), h(45)), ahora = pronto)

        assertEquals(EstadoDeuda.PENDIENTE, informe.deudas.single().estado)
        assertNull(informe.de(lunes2, ReglaId.COMPENSACION))
    }

    @Test
    fun `la compensacion va en bloque - 10 h de mas no pagan una deuda de 21 h`() {
        val informe = informe(listOf(h(24), h(55), h(45), h(45)))
        assertEquals(EstadoDeuda.VENCIDA, informe.deudas.single().estado)
    }

    @Test
    fun `D-3 - un diario de 20 h compensa 9 h (lo que pasa de 11 h)`() {
        // Reducido de 36 h (deuda 9 h) del sábado 7 al domingo 8 a las 18:00; el miércoles 11, un diario de 20 h.
        val previo = descanso(hora(2, 6).minus(h(48)), h(48))
        val (j1, d1) = trabajo(hora(2, 6), hora(7, 6))
        val reducido = descanso(hora(7, 6), h(36))
        val (j2, d2) = trabajo(hora(8, 18), hora(11, 6))
        val largo = descanso(hora(11, 6), h(20))
        val (j3, d3) = trabajo(hora(12, 2), hora(14, 6))
        val normal = descanso(hora(14, 6), h(45))
        val (j4, d4) = trabajo(hora(16, 3), hora(30, 6))
        val ahora = LocalDateTime.of(2027, 2, 1, 12, 0).atZone(madrid).toInstant()
        val informe =
            MotorReglas().evaluar(
                ContextoReglas(
                    j1 + j2 + j3 + j4,
                    d1 + d2 + d3 + d4 + previo + reducido + largo + normal,
                    ahora = ahora,
                    paisEmpresa = "ES",
                    paisResidencia = "UA",
                ),
            )

        assertEquals(h(9), informe.deudas.single().horas)
        assertEquals(EstadoDeuda.COMPENSADA, informe.deudas.single().estado)
    }

    @Test
    fun `dos deudas se pagan de la mas antigua a la mas nueva`() {
        // Dos reducidos de 30 h fuera de casa (15 h + 15 h) y un semanal de 75 h que paga las dos.
        val informe = informe(listOf(h(30), h(30), h(75), h(45)))

        assertEquals(listOf(EstadoDeuda.COMPENSADA, EstadoDeuda.COMPENSADA), informe.deudas.map { it.estado })
        assertTrue(informe.de(lunes16, ReglaId.COMPENSACION_ANTES_DEL_SEMANAL) is Resultado.Cumple)
    }

    @Test
    fun `T17e - dos reducidos fuera de casa sin compensar antes del siguiente semanal se excede`() {
        val informe = informe(listOf(h(30), h(30), h(45), h(75)))

        assertTrue(informe.de(lunes16, ReglaId.COMPENSACION_ANTES_DEL_SEMANAL) is Resultado.Excedido)
        // El de 75 h de la semana del 23 acaba el 1/12: tarde para la primera deuda (vence el 30/11),
        // a tiempo para la segunda (vence el 7/12).
        assertEquals(listOf(EstadoDeuda.VENCIDA, EstadoDeuda.COMPENSADA), informe.deudas.map { it.estado })
    }

    @Test
    fun `con un hueco donde pudo compensar sin apuntarlo, la deuda vencida no se puede saber`() {
        // Reducido de 24 h y luego nada apuntado hasta diciembre.
        val descansos = listOf(descanso(hora(2, 6).minus(h(48)), h(48)), descanso(hora(7, 6), h(24)))
        val jornadas = listOf(jornada(hora(2, 6), h(12)))
        val ahora = LocalDateTime.of(2027, 2, 1, 12, 0).atZone(madrid).toInstant()
        val informe =
            MotorReglas().evaluar(
                ContextoReglas(jornadas, descansos, ahora = ahora, paisEmpresa = "ES", paisResidencia = "UA"),
            )

        assertEquals(EstadoDeuda.NO_VERIFICADO, informe.deudas.single().estado)
        val r = informe.de(lunes2, ReglaId.COMPENSACION)
        assertEquals(MotivoNoVerificado.DESCANSO_NO_REGISTRADO, (r as Resultado.NoVerificado).motivo)
    }

    @Test
    fun `un semanal normal no deja deuda`() {
        assertTrue(informe(listOf(h(45), h(45))).deudas.isEmpty())
        assertNull(informe(listOf(h(45), h(45))).de(lunes9, ReglaId.COMPENSACION))
    }
}
