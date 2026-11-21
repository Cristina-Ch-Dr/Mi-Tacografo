package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class PlazoSemanalTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private fun clasificado(
        id: Long,
        inicio: Instant,
        fin: Instant?,
        clase: ClaseDescanso,
    ) = DescansoClasificado(
        Descanso(
            id = id,
            inicio = inicio,
            zonaInicio = madrid,
            fin = fin,
            zonaFin =
                fin?.let {
                    madrid
                },
            tipo = TipoDescanso.DESCANSO,
        ),
        clase,
    )

    @Test
    fun `144 h despues del fin del ultimo semanal`() {
        val descansos =
            listOf(
                clasificado(1, hora(7, 16), hora(9, 6), ClaseDescanso.SEMANAL_NORMAL),
                clasificado(2, hora(9, 17), hora(10, 6), ClaseDescanso.DIARIO_NORMAL),
                clasificado(3, hora(14, 16), hora(15, 20), ClaseDescanso.SEMANAL_REDUCIDO),
                clasificado(4, hora(16, 17), hora(17, 6), ClaseDescanso.DIARIO_NORMAL),
            )

        // El reducido acabó el domingo 15 a las 20:00: el siguiente, como muy tarde el sábado 21 a las 20:00.
        assertEquals(hora(21, 20), plazoSiguienteSemanal(descansos))
    }

    @Test
    fun `sin ningun semanal terminado no hay plazo`() {
        val descansos =
            listOf(
                clasificado(1, hora(9, 17), hora(10, 6), ClaseDescanso.DIARIO_NORMAL),
                clasificado(2, hora(14, 16), null, ClaseDescanso.EN_CURSO),
            )

        assertNull(plazoSiguienteSemanal(descansos))
    }
}
