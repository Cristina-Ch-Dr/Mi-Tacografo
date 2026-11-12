package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class DescansosEntreJornadasTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    private fun jornada(
        id: Long,
        inicio: Instant,
        fin: Instant?,
        paisFin: String? = "FR",
    ) = Jornada(
        id = id,
        inicio = inicio,
        zonaInicio = madrid,
        kmInicio = 0,
        fin = fin,
        zonaFin = fin?.let { madrid },
        kmFin = fin?.let { 500 },
        paisInicio = "ES",
        paisFin = fin?.let { paisFin },
    )

    private val lunes = jornada(1, hora(2, 6), hora(2, 18))
    private val martes = jornada(2, hora(3, 6), hora(3, 17))

    @Test
    fun `el hueco entre dos jornadas es un descanso calculado`() {
        val todos = completarDescansos(listOf(martes, lunes), emptyList())

        val descanso = todos.single()
        assertTrue(descanso.calculado)
        assertEquals(TipoDescanso.DESCANSO, descanso.tipo)
        assertEquals(lunes.fin, descanso.inicio)
        assertEquals(martes.inicio, descanso.fin)
        assertEquals("FR", descanso.pais)
        assertEquals(ClaseDescanso.DIARIO_NORMAL, clasificarDescansos(todos).single().clase)
    }

    @Test
    fun `si hay un descanso apuntado en el hueco manda el apuntado`() {
        val apuntado =
            Descanso(
                id = 7,
                inicio = hora(2, 19),
                zonaInicio = madrid,
                fin = hora(3, 5),
                zonaFin = madrid,
                tipo = TipoDescanso.DESCANSO,
            )

        val todos = completarDescansos(listOf(lunes, martes), listOf(apuntado))

        assertEquals(listOf(apuntado), todos)
        assertFalse(todos.single().calculado)
    }

    @Test
    fun `una pausa no tapa el descanso entre jornadas`() {
        val pausa =
            Descanso(
                id = 3,
                inicio = hora(2, 12),
                zonaInicio = madrid,
                fin = hora(2, 12, 45),
                zonaFin = madrid,
                tipo = TipoDescanso.PAUSA,
            )

        val todos = completarDescansos(listOf(lunes, martes), listOf(pausa))

        assertEquals(2, todos.size)
        assertTrue(todos.last().calculado)
    }

    @Test
    fun `con una jornada en curso, el descanso va hasta su inicio y no hay descanso actual`() {
        val enCurso = jornada(2, hora(3, 6), null)

        val todos = completarDescansos(listOf(lunes, enCurso), emptyList())

        assertEquals(Duration.ofHours(12), todos.single().duracion)
        assertNull(descansoActual(listOf(lunes, enCurso)))
    }

    @Test
    fun `sin jornada en curso descansa desde el fin de la ultima`() {
        assertEquals(martes.fin, descansoActual(listOf(lunes, martes)))
        assertNull(descansoActual(emptyList()))
    }

    @Test
    fun `lo descansado antes de cada jornada, con su clase`() {
        val miercoles = jornada(3, hora(4, 4), hora(4, 15))
        val jornadas = listOf(lunes, martes, miercoles)

        val previos =
            descansoAntesDeCadaJornada(jornadas, clasificarDescansos(completarDescansos(jornadas, emptyList())))

        assertNull(previos[1])
        assertEquals(Duration.ofHours(12), previos.getValue(2).total)
        assertEquals(ClaseDescanso.DIARIO_NORMAL, previos.getValue(2).principal.clase)
        assertEquals(Duration.ofHours(11), previos.getValue(3).total)
    }

    @Test
    fun `el resumen suma el descanso calculado`() {
        val jornadas = listOf(lunes, martes)

        val resumen = resumirPeriodo(jornadas, completarDescansos(jornadas, emptyList()), lunesDe2(), 1, madrid)

        assertEquals(Duration.ofHours(12), resumen.tiempoDescanso)
    }

    private fun lunesDe2() = java.time.LocalDate.of(2026, 11, 2)
}
