package com.cristinachdr.mitacografo.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/** Casos T7–T10 y T12 de la especificación (§13), más los límites de cada clase. */
class ClasificacionDescansosTest {
    private val madrid = ZoneId.of("Europe/Madrid")

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    private var siguienteId = 1L

    private fun descanso(
        inicio: Instant,
        dura: Duration?,
        tipo: TipoDescanso = TipoDescanso.DESCANSO,
    ) = Descanso(
        id = siguienteId++,
        inicio = inicio,
        zonaInicio = madrid,
        fin = dura?.let { inicio.plus(it) },
        zonaFin = dura?.let { madrid },
        tipo = tipo,
    )

    private fun horas(
        h: Long,
        m: Long = 0,
    ) = Duration.ofHours(h).plusMinutes(m)

    /** Clasifica uno solo, sin descansos alrededor. */
    private fun clase(dura: Duration?): ClaseDescanso =
        clasificarDescansos(listOf(descanso(hora(2, 20), dura))).single().clase

    @Nested
    inner class Diarios {
        @Test
        fun `T7 - 11 h es un diario normal`() = assertEquals(ClaseDescanso.DIARIO_NORMAL, clase(horas(11)))

        @Test
        fun `T8 - 10 h 59 es un diario reducido`() = assertEquals(ClaseDescanso.DIARIO_REDUCIDO, clase(horas(10, 59)))

        @Test
        fun `9 h justas ya es reducido, 8 h 59 no llega`() {
            assertEquals(ClaseDescanso.DIARIO_REDUCIDO, clase(horas(9)))
            assertEquals(ClaseDescanso.FRACCIONADO_PRIMERA_PARTE, clase(horas(8, 59)))
        }

        @Test
        fun `menos de 3 h no cuenta como descanso diario`() =
            assertEquals(ClaseDescanso.INSUFICIENTE, clase(horas(2, 59)))
    }

    @Nested
    inner class Fraccionados {
        @Test
        fun `T9 - 3 h y luego 9 h dentro de 24 h es un fraccionado completo (cuenta como normal)`() {
            val primera = descanso(hora(2, 12), horas(3)) // 12:00–15:00
            val segunda = descanso(hora(2, 20), horas(9)) // 20:00–05:00
            val resultado = clasificarDescansos(listOf(segunda, primera))

            assertEquals(
                listOf(ClaseDescanso.FRACCIONADO_PRIMERA_PARTE, ClaseDescanso.FRACCIONADO_COMPLETO),
                resultado.map { it.clase },
            )
        }

        @Test
        fun `T10 - 2 h 59 y luego 9 h no es fraccionado - el de 9 h es reducido`() {
            val primera = descanso(hora(2, 12), horas(2, 59))
            val segunda = descanso(hora(2, 20), horas(9))

            assertEquals(
                listOf(ClaseDescanso.INSUFICIENTE, ClaseDescanso.DIARIO_REDUCIDO),
                clasificarDescansos(listOf(primera, segunda)).map { it.clase },
            )
        }

        @Test
        fun `si la segunda parte termina después de las 24 h desde el último diario, no es fraccionado`() {
            val anterior = descanso(hora(1, 20), horas(11)) // termina el 2 a las 07:00
            val primera = descanso(hora(2, 12), horas(3))
            val segunda = descanso(hora(2, 23), horas(9)) // termina el 3 a las 08:00: 25 h después de las 07:00

            assertEquals(
                listOf(ClaseDescanso.DIARIO_NORMAL, ClaseDescanso.INSUFICIENTE, ClaseDescanso.DIARIO_REDUCIDO),
                clasificarDescansos(listOf(anterior, primera, segunda)).map { it.clase },
            )
        }

        @Test
        fun `una primera parte sin segunda todavía sigue esperando`() {
            val anterior = descanso(hora(1, 20), horas(11))
            val primera = descanso(hora(2, 12), horas(3))

            assertEquals(
                ClaseDescanso.FRACCIONADO_PRIMERA_PARTE,
                clasificarDescansos(listOf(anterior, primera)).last().clase,
            )
        }

        @Test
        fun `las pausas de en medio no cortan el fraccionado`() {
            val primera = descanso(hora(2, 12), horas(3))
            val cafe = descanso(hora(2, 17), Duration.ofMinutes(30), TipoDescanso.PAUSA)
            val segunda = descanso(hora(2, 20), horas(9))

            assertEquals(
                listOf(
                    ClaseDescanso.FRACCIONADO_PRIMERA_PARTE,
                    ClaseDescanso.PAUSA,
                    ClaseDescanso.FRACCIONADO_COMPLETO,
                ),
                clasificarDescansos(listOf(primera, cafe, segunda)).map { it.clase },
            )
        }
    }

    @Nested
    inner class Semanales {
        @Test
        fun `T12 - 44 h 59 es un semanal reducido con 1 min de deuda`() {
            val resultado = clasificarDescansos(listOf(descanso(hora(2, 20), horas(44, 59)))).single()

            assertEquals(ClaseDescanso.SEMANAL_REDUCIDO, resultado.clase)
            assertEquals(Duration.ofMinutes(1), resultado.deuda)
        }

        @Test
        fun `24 h ya es semanal reducido y 45 h normal, sin deuda`() {
            assertEquals(ClaseDescanso.SEMANAL_REDUCIDO, clase(horas(24)))
            assertEquals(ClaseDescanso.DIARIO_NORMAL, clase(horas(23, 59)))

            val normal = clasificarDescansos(listOf(descanso(hora(2, 20), horas(45)))).single()
            assertEquals(ClaseDescanso.SEMANAL_NORMAL, normal.clase)
            assertEquals(Duration.ZERO, normal.deuda)
        }
    }

    @Test
    fun `las pausas son pausas, duren lo que duren`() {
        val larga = descanso(hora(2, 12), horas(12), TipoDescanso.PAUSA)
        assertEquals(ClaseDescanso.PAUSA, clasificarDescansos(listOf(larga)).single().clase)
    }

    @Test
    fun `lo que sigue en curso no se clasifica todavía`() = assertEquals(ClaseDescanso.EN_CURSO, clase(null))

    @Test
    fun `el resultado sale en orden cronológico aunque la lista venga desordenada`() {
        val a = descanso(hora(1, 20), horas(11))
        val b = descanso(hora(3, 20), horas(11))
        val c = descanso(hora(2, 20), horas(9))

        assertEquals(listOf(a, c, b), clasificarDescansos(listOf(b, a, c)).map { it.descanso })
    }
}
