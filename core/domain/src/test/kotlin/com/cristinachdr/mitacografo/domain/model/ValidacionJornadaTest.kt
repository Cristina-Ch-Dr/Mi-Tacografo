package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ValidacionJornadaTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora: Instant = hora(20, 0)

    private fun hora(
        h: Int,
        m: Int,
    ): Instant = LocalDateTime.of(2026, 11, 2, h, m).atZone(madrid).toInstant()

    private val valida =
        Jornada(
            inicio = hora(6, 0),
            zonaInicio = madrid,
            kmInicio = 1000,
            fin = hora(17, 0),
            zonaFin = madrid,
            kmFin = 1500,
            conduccionTacografo = Duration.ofHours(9),
        )

    private fun errores(
        jornada: Jornada,
        finAnterior: Instant? = null,
    ) = validarJornada(jornada, ahora, finAnterior)

    @Test
    fun `una jornada correcta no tiene errores`() {
        assertEquals(emptySet<ErrorJornada>(), errores(valida))
        assertEquals(
            emptySet<ErrorJornada>(),
            errores(valida.copy(fin = null, kmFin = null, conduccionTacografo = null)),
        )
    }

    @Test
    fun `km negativos`() {
        assertEquals(setOf(ErrorJornada.KM_NEGATIVOS), errores(valida.copy(kmInicio = -1, kmFin = null)))
    }

    @Test
    fun `km final menor que el inicial`() {
        assertEquals(setOf(ErrorJornada.KM_FIN_MENOR_QUE_INICIO), errores(valida.copy(kmFin = 999)))
    }

    @Test
    fun `los mismos km al inicio y al final son validos`() {
        assertEquals(emptySet<ErrorJornada>(), errores(valida.copy(kmFin = 1000)))
    }

    @Test
    fun `inicio en el futuro, con 5 minutos de margen`() {
        val abierta = valida.copy(fin = null, kmFin = null, conduccionTacografo = null)

        assertEquals(emptySet<ErrorJornada>(), errores(abierta.copy(inicio = hora(20, 5))))
        assertEquals(setOf(ErrorJornada.INICIO_EN_EL_FUTURO), errores(abierta.copy(inicio = hora(20, 6))))
    }

    @Test
    fun `no puede empezar antes de que terminara la anterior`() {
        assertEquals(setOf(ErrorJornada.INICIO_ANTES_DEL_FIN_ANTERIOR), errores(valida, finAnterior = hora(6, 30)))
        assertEquals(emptySet<ErrorJornada>(), errores(valida, finAnterior = hora(6, 0)))
    }

    @Test
    fun `el fin tiene que ser posterior al inicio`() {
        val errores = errores(valida.copy(fin = hora(6, 0), conduccionTacografo = null))

        assertEquals(setOf(ErrorJornada.FIN_NO_POSTERIOR_AL_INICIO), errores)
    }

    @Test
    fun `fin en el futuro`() {
        assertEquals(setOf(ErrorJornada.FIN_EN_EL_FUTURO), errores(valida.copy(fin = hora(21, 0))))
    }

    @Test
    fun `la conduccion no puede superar la duracion de la jornada`() {
        // La jornada dura 11 h.
        assertEquals(emptySet<ErrorJornada>(), errores(valida.copy(conduccionTacografo = Duration.ofHours(11))))
        assertEquals(
            setOf(ErrorJornada.CONDUCCION_MAYOR_QUE_JORNADA),
            errores(valida.copy(conduccionTacografo = Duration.ofHours(11).plusMinutes(1))),
        )
    }

    @Test
    fun `conduccion negativa`() {
        assertEquals(
            setOf(ErrorJornada.CONDUCCION_NEGATIVA),
            errores(valida.copy(conduccionTacografo = Duration.ofMinutes(-1))),
        )
    }

    @Test
    fun `se devuelven todos los errores a la vez`() {
        val mal = valida.copy(kmFin = 10, fin = hora(5, 0), conduccionTacografo = null)

        assertEquals(setOf(ErrorJornada.KM_FIN_MENOR_QUE_INICIO, ErrorJornada.FIN_NO_POSTERIOR_AL_INICIO), errores(mal))
    }
}
