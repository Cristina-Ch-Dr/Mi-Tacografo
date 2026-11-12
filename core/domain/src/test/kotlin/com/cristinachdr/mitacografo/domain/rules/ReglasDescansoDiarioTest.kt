package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import com.cristinachdr.mitacografo.domain.model.completarDescansos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reglamento (CE) 561/2006, art. 8.2 y 8.4 (espec. §7.4; casos T11 y T23 de §13). Datos inventados.
 *
 * Patrón de los tests: el domingo 1 acaba un semanal a las 06:00; cada jornada va de 06:00 a 18:00
 * y el descanso diario, de 18:00 a las 06:00 del día siguiente (12 h), salvo que se diga otra cosa.
 */
class ReglasDescansoDiarioTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora: Instant = LocalDateTime.of(2026, 12, 1, 12, 0).atZone(madrid).toInstant()

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    private fun h(horas: Long) = Duration.ofHours(horas)

    private var ids = 1L

    private fun jornada(
        dia: Int,
        desde: Int = 6,
        hasta: Int = 18,
    ) = Jornada(
        id = ids++,
        inicio = hora(dia, desde),
        zonaInicio = madrid,
        kmInicio = 0,
        fin = hora(dia, hasta),
        zonaFin = madrid,
        kmFin = 500,
        conduccionTacografo = h(8),
    )

    private fun descanso(
        inicio: Instant,
        dura: Duration,
    ) = Descanso(
        id = ids++,
        inicio = inicio,
        zonaInicio = madrid,
        fin = inicio.plus(dura),
        zonaFin = madrid,
        tipo = TipoDescanso.DESCANSO,
    )

    /** Semanal de 48 h que acaba el lunes 2 a las 06:00. */
    private val semanalPrevio = descanso(hora(2, 6).minus(h(48)), h(48))

    private fun evaluar(
        jornadas: List<Jornada>,
        descansos: List<Descanso>,
    ) = MotorReglas().evaluar(ContextoReglas(jornadas, descansos, ahora = ahora))

    private fun InformeReglas.diaria(jornada: Jornada) =
        semanas
            .flatMap { it.jornadas }
            .single { it.jornada.id == jornada.id }
            .resultado(ReglaId.DESCANSO_DIARIO)

    @Nested
    inner class Plazo24Horas {
        @Test
        fun `con un descanso de 12 h despues de la jornada se cumple (normal)`() {
            val lunes = jornada(2)
            val r = evaluar(listOf(lunes), listOf(semanalPrevio, descanso(hora(2, 18), h(12)))).diaria(lunes)

            assertTrue(r is Resultado.Cumple)
            assertEquals(Intervalo.exacto(h(12)), r.detalle.valor)
        }

        @Test
        fun `con 10 h dentro de las 24 h se cumple como reducido`() {
            val lunes = jornada(2, hasta = 20)
            val r = evaluar(listOf(lunes), listOf(semanalPrevio, descanso(hora(2, 20), h(10)))).diaria(lunes)

            assertTrue(r is Resultado.Cumple)
            assertEquals(h(9), r.detalle.limite)
        }

        @Test
        fun `si el siguiente descanso empieza tan tarde que no caben 9 h en las 24 h, se excede`() {
            // Desde las 06:00 del lunes, las 24 h acaban el martes a las 06:00.
            // Trabaja hasta las 23:00 y descansa desde ahí: solo caben 7 h dentro del plazo.
            val lunes = jornada(2, hasta = 23)
            val r = evaluar(listOf(lunes), listOf(semanalPrevio, descanso(hora(2, 23), h(11)))).diaria(lunes)

            assertTrue(r is Resultado.Excedido)
            assertEquals(Intervalo.exacto(h(7)), r.detalle.valor)
        }

        @Test
        fun `T23 - un hueco de 30 h entre jornadas sin descanso apuntado no se puede comprobar`() {
            val lunes = jornada(2)
            val miercoles = jornada(4) // del lunes 18:00 al miércoles 06:00 hay 36 h sin nada apuntado
            val informe = evaluar(listOf(lunes, miercoles), listOf(semanalPrevio))

            val r = informe.diaria(lunes)
            assertEquals(MotivoNoVerificado.DESCANSO_NO_REGISTRADO, (r as Resultado.NoVerificado).motivo)
            // Y la del miércoles tampoco: no se sabe cuándo acabó su descanso anterior.
            assertEquals(
                MotivoNoVerificado.DESCANSO_NO_REGISTRADO,
                (informe.diaria(miercoles) as Resultado.NoVerificado).motivo,
            )
        }

        @Test
        fun `sin descanso apuntado pero sin hueco donde pudiera haberlo, se excede`() {
            // El lunes de 06:00 a 18:00 y otra jornada de 18:00 del lunes a 04:00 del martes, seguidas:
            // en las 24 h no queda ningún hueco de 9 h.
            val lunes = jornada(2)
            val noche =
                Jornada(
                    id = ids++,
                    inicio = hora(2, 18),
                    zonaInicio = madrid,
                    kmInicio = 0,
                    fin = hora(3, 4),
                    zonaFin = madrid,
                    kmFin = 300,
                    conduccionTacografo = h(6),
                )
            val r = evaluar(listOf(lunes, noche), listOf(semanalPrevio)).diaria(lunes)

            assertTrue(r is Resultado.Excedido)
        }

        @Test
        fun `el fraccionado 3 mas 9 cumple`() {
            // Pausa larga de 3 h a mediodía y 9 h por la noche, todo dentro de las 24 h.
            val lunes = jornada(2, hasta = 20)
            val descansos = listOf(semanalPrevio, descanso(hora(2, 11), h(3)), descanso(hora(2, 20), h(9)))
            assertTrue(evaluar(listOf(lunes), descansos).diaria(lunes) is Resultado.Cumple)
        }

        @Test
        fun `si aun no han pasado las 24 h y no hay descanso, todavia no hay resultado`() {
            val reciente =
                Jornada(
                    id = 90,
                    inicio = ahora.minus(h(10)),
                    zonaInicio = madrid,
                    kmInicio = 0,
                    fin = ahora.minus(h(1)),
                    zonaFin = madrid,
                    kmFin = 100,
                    conduccionTacografo = h(7),
                )
            val previo = descanso(ahora.minus(h(22)), h(12)) // acaba justo al empezar la jornada
            val informe = MotorReglas().evaluar(ContextoReglas(listOf(reciente), listOf(previo), ahora = ahora))

            val resultados =
                informe.semanas
                    .single()
                    .jornadas
                    .single()
                    .resultados
            assertTrue(resultados.none { it.regla == ReglaId.DESCANSO_DIARIO })
        }
    }

    @Nested
    inner class MaximoTresReducidos {
        /** Lunes a viernes con descansos de 10 h (reducidos) después de cada jornada. */
        private fun semanaConReducidos(cuantos: Int): Pair<List<Jornada>, List<Descanso>> {
            val jornadas = (0 until cuantos + 1).map { jornada(2 + it, hasta = 20) }
            val reducidos = (0 until cuantos).map { descanso(hora(2 + it, 20), h(10)) }
            return jornadas to listOf(semanalPrevio) + reducidos
        }

        private fun InformeReglas.reducidos(lunes: LocalDate = LocalDate.of(2026, 11, 2)) =
            semanas.single { it.lunes == lunes }.resultado(ReglaId.REDUCIDOS_ENTRE_SEMANALES)

        @Test
        fun `tres reducidos entre dos semanales cumplen`() {
            val (jornadas, descansos) = semanaConReducidos(3)
            val r = evaluar(jornadas, descansos).reducidos()

            assertTrue(r is Resultado.Cumple)
            assertEquals(3, r.detalle.cuenta)
            assertEquals(3, r.detalle.maximo)
        }

        @Test
        fun `T11 - el cuarto reducido entre dos semanales se excede`() {
            val (jornadas, descansos) = semanaConReducidos(4)
            val r = evaluar(jornadas, descansos).reducidos()

            assertTrue(r is Resultado.Excedido)
            assertEquals(4, r.detalle.cuenta)
        }

        @Test
        fun `un semanal en medio vuelve a contar desde cero`() {
            val (jornadas, descansos) = semanaConReducidos(3)
            // Semanal el fin de semana (del sábado 7 a las 06:00 al lunes 9 a las 06:00) y otro reducido después.
            val siguiente = listOf(jornada(9, hasta = 20), jornada(10, hasta = 20))
            val mas = descansos + descanso(hora(7, 6), h(48)) + descanso(hora(9, 20), h(10))
            val informe = evaluar(jornadas + siguiente, mas)

            val r = informe.reducidos(LocalDate.of(2026, 11, 9))
            assertTrue(r is Resultado.Cumple)
            assertEquals(1, r.detalle.cuenta)
        }

        @Test
        fun `sin un semanal apuntado antes no se sabe cuantos reducidos llevaba`() {
            val jornadas = listOf(jornada(2, hasta = 20), jornada(3, hasta = 20))
            val r = evaluar(jornadas, listOf(descanso(hora(2, 20), h(10)))).reducidos()

            assertEquals(MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO, (r as Resultado.NoVerificado).motivo)
        }
    }

    @Nested
    inner class DescansoEntreJornadas {
        private fun evaluarCalculando(jornadas: List<Jornada>) =
            MotorReglas().evaluar(
                ContextoReglas(
                    jornadas,
                    completarDescansos(jornadas, listOf(semanalPrevio)),
                    ahora = ahora,
                    descansoEntreJornadas = true,
                ),
            )

        @Test
        fun `el hueco entre jornadas cuenta como descanso aunque no este apuntado`() {
            val lunes = jornada(2)
            val martes = jornada(3)

            assertTrue(evaluarCalculando(listOf(lunes, martes)).diaria(lunes) is Resultado.Cumple)
        }

        @Test
        fun `despues de la ultima jornada sigue descansando`() {
            val lunes = jornada(2)

            assertTrue(evaluarCalculando(listOf(lunes)).diaria(lunes) is Resultado.Cumple)
        }

        @Test
        fun `si la ultima jornada no deja 9 h dentro del plazo se excede`() {
            // Del lunes 06:00 al martes 00:00: solo quedan 6 h del plazo de 24 h.
            val larga = jornada(2).copy(fin = hora(3, 0))

            assertTrue(evaluarCalculando(listOf(larga)).diaria(larga) is Resultado.Excedido)
        }
    }
}
