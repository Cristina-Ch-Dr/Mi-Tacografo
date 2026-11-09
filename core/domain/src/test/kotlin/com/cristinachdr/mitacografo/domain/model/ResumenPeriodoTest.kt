package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Datos inventados: solo horas y km. El lunes 2/11/2026 empieza la semana. */
class ResumenPeriodoTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val lunes = LocalDate.of(2026, 11, 2)

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    private fun jornada(
        id: Long,
        dia: Int,
        km: Int,
        conduccionMin: Long? = 480,
    ) = Jornada(
        id = id,
        inicio = hora(dia, 6),
        zonaInicio = madrid,
        kmInicio = 100_000 + id.toInt() * 1_000,
        fin = hora(dia, 17),
        zonaFin = madrid,
        kmFin = 100_000 + id.toInt() * 1_000 + km,
        conduccionTacografo = conduccionMin?.let(Duration::ofMinutes),
    )

    private fun descanso(
        id: Long,
        dia: Int,
        h: Int,
        horas: Long,
        tipo: TipoDescanso = TipoDescanso.DESCANSO,
    ) = Descanso(
        id = id,
        inicio = hora(dia, h),
        zonaInicio = madrid,
        fin = hora(dia, h).plus(Duration.ofHours(horas)),
        zonaFin = madrid,
        tipo = tipo,
    )

    private val cincoJornadas =
        listOf(
            jornada(1, 2, 500),
            jornada(2, 3, 450),
            jornada(3, 4, 600),
            jornada(4, 5, 400, conduccionMin = null),
            jornada(5, 6, 550),
        )

    private val seisDescansos =
        listOf(
            descanso(1, 2, 17, 13), // diario normal
            descanso(2, 3, 17, 10), // reducido
            descanso(3, 4, 17, 13), // normal
            descanso(4, 5, 17, 13), // normal
            descanso(5, 6, 17, 30), // semanal reducido: deuda de 15 h
            descanso(6, 4, 11, 1, TipoDescanso.PAUSA),
        )

    @Test
    fun `T25 - semana con 5 jornadas y 6 descansos - totales correctos y filas en orden cronologico`() {
        val resumen = resumirPeriodo(cincoJornadas.shuffled(), seisDescansos.shuffled(), lunes, semanas = 1)

        assertEquals(2_500, resumen.km)
        assertEquals(5, resumen.numJornadas)
        // La del jueves no tiene conducción apuntada: la suma se queda corta y se avisa.
        assertEquals(Duration.ofMinutes(480 * 4), resumen.conduccion)
        assertFalse(resumen.conduccionCompleta)
        assertEquals(Duration.ofHours(11 * 5), resumen.tiempoJornadas)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), resumen.jornadas.map { it.id })
        assertEquals(listOf(1L, 2L, 6L, 3L, 4L, 5L), resumen.descansos.map { it.descanso.id })

        assertEquals(3, resumen.cuantos(ClaseDescanso.DIARIO_NORMAL))
        assertEquals(1, resumen.cuantos(ClaseDescanso.DIARIO_REDUCIDO))
        assertEquals(1, resumen.cuantos(ClaseDescanso.SEMANAL_REDUCIDO))
        assertEquals(1, resumen.cuantos(ClaseDescanso.PAUSA))
        assertEquals(Duration.ofHours(15), resumen.deudaSemanal)
    }

    @Test
    fun `km por dia salen los 7 dias de la semana, tambien los que no se trabajo`() {
        val resumen = resumirPeriodo(cincoJornadas, emptyList(), lunes, semanas = 1)

        assertEquals((0L..6L).map { lunes.plusDays(it) }, resumen.kmPorDia.map { it.dia })
        assertEquals(listOf(500, 450, 600, 400, 550, 0, 0), resumen.kmPorDia.map { it.km })
    }

    @Test
    fun `solo cuenta lo que empieza dentro del periodo`() {
        val antes = jornada(9, 1, 300) // domingo de la semana anterior
        val despues = Jornada(id = 10, inicio = hora(9, 6), zonaInicio = madrid, kmInicio = 0) // lunes siguiente

        val resumen = resumirPeriodo(cincoJornadas + antes + despues, emptyList(), lunes, semanas = 1)

        assertEquals(LocalDate.of(2026, 11, 8), resumen.hasta)
        assertEquals(5, resumen.numJornadas)
    }

    @Test
    fun `la bisemana suma las dos semanas y tambien da cada una por separado`() {
        val segunda = listOf(jornada(6, 9, 700), jornada(7, 10, 300))
        val resumen = resumirPeriodo(cincoJornadas + segunda, emptyList(), lunes, semanas = 2)

        assertEquals(LocalDate.of(2026, 11, 15), resumen.hasta)
        assertEquals(3_500, resumen.km)
        assertEquals(14, resumen.kmPorDia.size)
        assertEquals(listOf(2_500, 1_000), resumen.porSemana.map { it.km })
        assertEquals(listOf(lunes, lunes.plusWeeks(1)), resumen.porSemana.map { it.desde })
    }

    @Test
    fun `una jornada en curso cuenta pero no suma km ni tiempo`() {
        val enCurso = Jornada(id = 8, inicio = hora(7, 6), zonaInicio = madrid, kmInicio = 0)
        val resumen = resumirPeriodo(listOf(jornada(1, 2, 500), enCurso), emptyList(), lunes, semanas = 1)

        assertEquals(2, resumen.numJornadas)
        assertEquals(500, resumen.km)
        assertEquals(Duration.ofHours(11), resumen.tiempoJornadas)
        assertTrue(resumen.conduccionCompleta)
    }

    @Test
    fun `el dia de cada jornada se mira en la zona de referencia si hay`() {
        // Empieza el domingo 8 a las 23:30 en Kyiv: en Madrid aún es domingo; en Kyiv, domingo también.
        // El lunes 9 a las 00:30 en Kyiv es domingo 23:30 en Madrid.
        val kyiv = ZoneId.of("Europe/Kyiv")
        val deMadrugada =
            Jornada(
                id = 1,
                inicio = LocalDateTime.of(2026, 11, 9, 0, 30).atZone(kyiv).toInstant(),
                zonaInicio = kyiv,
                kmInicio = 0,
            )

        assertEquals(0, resumirPeriodo(listOf(deMadrugada), emptyList(), lunes, 1).numJornadas)
        assertEquals(1, resumirPeriodo(listOf(deMadrugada), emptyList(), lunes, 1, zonaReferencia = madrid).numJornadas)
    }
}
