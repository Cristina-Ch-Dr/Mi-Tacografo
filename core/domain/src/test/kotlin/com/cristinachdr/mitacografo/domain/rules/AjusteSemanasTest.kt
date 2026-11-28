package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.AjusteSemanas
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.completarDescansos
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * «Ajuste de semanas» (MT-59): empieza a usar la app el jueves 5 de noviembre de 2026 (semana del lunes 2).
 * Lo de antes lo dice el ajuste, y el motor lo usa como dato conocido. Datos inventados.
 */
class AjusteSemanasTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val lunes2 = LocalDate.of(2026, 11, 2)
    private val ahora: Instant = LocalDateTime.of(2026, 11, 20, 12, 0).atZone(madrid).toInstant()

    private fun hora(
        dia: Int,
        h: Int,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, 0).atZone(madrid).toInstant()

    private fun h(horas: Long) = Duration.ofHours(horas)

    /** Del jueves 5 al sábado 7: tres jornadas de 06:00 a 17:00 con [conduccion]; descansan 13 h entre ellas. */
    private fun jornadas(conduccion: Duration) =
        (5..7).map { dia ->
            Jornada(
                id = dia.toLong(),
                inicio = hora(dia, 6),
                zonaInicio = madrid,
                kmInicio = 0,
                fin = hora(dia, 17),
                zonaFin = madrid,
                kmFin = 500,
                conduccionTacografo = conduccion,
            )
        }

    private fun evaluar(
        jornadas: List<Jornada>,
        ajuste: AjusteSemanas?,
    ) = MotorReglas().evaluar(
        ContextoReglas(
            jornadas = jornadas,
            descansos = completarDescansos(jornadas, emptyList()),
            descansoEntreJornadas = true,
            ahora = ahora,
            ajuste = ajuste,
        ),
    )

    private fun InformeReglas.deLaSemana(regla: ReglaId) = semanas.single { it.lunes == lunes2 }.resultado(regla)

    @Test
    fun `sin ajuste, lo de antes del jueves no se sabe`() {
        val informe = evaluar(jornadas(h(8)), null)

        assertTrue(
            informe.semanas
                .single()
                .conduccion.max > h(56),
        )
        assertTrue(informe.deLaSemana(ReglaId.CONDUCCION_BISEMANAL) is Resultado.NoVerificado)
        assertTrue(informe.deLaSemana(ReglaId.AMPLIACIONES_SEMANA) is Resultado.NoVerificado)
    }

    @Test
    fun `con el ajuste, la semana y las dos semanas son exactas`() {
        // Antes del jueves ya llevaba 27 h; la semana anterior, 50 h. Con 3 × 8 h: 51 h y 101 h en dos semanas.
        val ajuste = AjusteSemanas(semana = lunes2, conduccionSemana = h(27), conduccionSemanaAnterior = h(50))
        val informe = evaluar(jornadas(h(8)), ajuste)

        assertEquals(Intervalo.exacto(h(51)), informe.semanas.single().conduccion)
        assertTrue(informe.deLaSemana(ReglaId.CONDUCCION_SEMANAL) is Resultado.Cumple)
        assertTrue(informe.deLaSemana(ReglaId.CONDUCCION_BISEMANAL) is Resultado.Excedido)
    }

    @Test
    fun `las ampliaciones de antes cuentan`() {
        // Ya usó las 2 ampliaciones a 10 h antes del jueves; el jueves conduce 9 h 30: ya son 3.
        val ajuste = AjusteSemanas(semana = lunes2, ampliaciones = 2)
        val conAmpliada =
            jornadas(h(8)).mapIndexed { i, j ->
                if (i ==
                    0
                ) {
                    j.copy(conduccionTacografo = Duration.ofMinutes(570))
                } else {
                    j
                }
            }
        val informe = evaluar(conAmpliada, ajuste)

        assertTrue(informe.deLaSemana(ReglaId.AMPLIACIONES_SEMANA) is Resultado.Excedido)
        assertTrue(evaluar(jornadas(h(8)), ajuste).deLaSemana(ReglaId.AMPLIACIONES_SEMANA) is Resultado.Cumple)
    }

    @Test
    fun `los reducidos de antes cuentan, aunque no haya ningun semanal apuntado`() {
        // Ya llevaba 3 reducidos desde el último semanal. Con descansos de 13 h no hay más: cumple.
        val ajuste = AjusteSemanas(semana = lunes2, reducidos = 3)
        assertTrue(evaluar(jornadas(h(8)), ajuste).deLaSemana(ReglaId.REDUCIDOS_ENTRE_SEMANALES) is Resultado.Cumple)

        // Si el sábado empieza 9 h 30 después del viernes, es un reducido más: 4.
        val conReducido =
            jornadas(h(8)).map {
                if (it.id ==
                    7L
                ) {
                    it.copy(inicio = hora(6, 17).plusSeconds(9 * 3600 + 1800), fin = hora(7, 17))
                } else {
                    it
                }
            }
        assertTrue(evaluar(conReducido, ajuste).deLaSemana(ReglaId.REDUCIDOS_ENTRE_SEMANALES) is Resultado.Excedido)
    }

    @Test
    fun `un ajuste de otra semana no cambia nada`() {
        val deOtraSemana = AjusteSemanas(semana = lunes2.plusWeeks(1), conduccionSemana = h(27))

        assertTrue(
            evaluar(jornadas(h(8)), deOtraSemana)
                .semanas
                .single()
                .conduccion.max > h(56),
        )
    }
}
