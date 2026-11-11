package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Jornada
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
 * Reglamento (CE) 561/2006, art. 6 y 7 (espec. §7.2, §7.3 y casos T1–T6, T20–T22 de §13).
 * Datos inventados: solo horas y km. Las semanas empiezan el lunes 2, 9 y 16 de noviembre de 2026.
 */
class ReglasConduccionTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val lunes2 = LocalDate.of(2026, 11, 2)
    private val lunes9 = LocalDate.of(2026, 11, 9)

    /** "Ahora": muy después de todas las jornadas de los tests. */
    private val ahora: Instant = LocalDateTime.of(2026, 12, 1, 12, 0).atZone(madrid).toInstant()

    private var siguienteId = 1L

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant = LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()

    /** Jornada de [dia] de 06:00 a 06:00 + [dura] (por defecto 12 h), con [conduccion] del tacógrafo. */
    private fun jornada(
        dia: Int,
        conduccion: Duration?,
        dura: Duration = Duration.ofHours(12),
        empieza: Instant = hora(dia, 6),
    ) = Jornada(
        id = siguienteId++,
        inicio = empieza,
        zonaInicio = madrid,
        kmInicio = 0,
        fin = empieza.plus(dura),
        zonaFin = madrid,
        kmFin = 500,
        conduccionTacografo = conduccion,
    )

    private fun h(
        horas: Long,
        minutos: Long = 0,
    ) = Duration.ofHours(horas).plusMinutes(minutos)

    /** Todo apuntado desde mucho antes (para estos tests, la primera semana está completa). */
    private val desdeSiempre: Instant = LocalDateTime.of(2026, 1, 1, 0, 0).atZone(madrid).toInstant()

    private fun evaluar(vararg jornadas: Jornada) =
        MotorReglas().evaluar(ContextoReglas(jornadas.toList(), ahora = ahora, datosDesde = desdeSiempre))

    /** Empieza a usar la app el lunes 2 a las 00:00: esa semana está completa, la anterior no se sabe. */
    private fun desdeElLunes2(jornadas: List<Jornada>) =
        MotorReglas().evaluar(
            ContextoReglas(jornadas, ahora = ahora, datosDesde = lunes2.atStartOfDay(madrid).toInstant()),
        )

    private fun InformeReglas.deJornada(
        jornada: Jornada,
        regla: ReglaId,
    ): Resultado = semanas.flatMap { it.jornadas }.single { it.jornada.id == jornada.id }.resultado(regla)

    private fun InformeReglas.deSemana(
        lunes: LocalDate,
        regla: ReglaId,
    ): Resultado = semanas.single { it.lunes == lunes }.resultado(regla)

    @Nested
    inner class ConduccionDiaria {
        @Test
        fun `T1 - 9 h 00 cumple y no es ampliada`() {
            val j = jornada(2, h(9))
            val r = evaluar(j).deJornada(j, ReglaId.CONDUCCION_DIARIA)

            assertTrue(r is Resultado.Cumple)
            assertEquals(false, r.detalle.ampliada)
        }

        @Test
        fun `T2 - 9 h 01 cumple como ampliada y cuenta 1 de 2 en la semana`() {
            val j = jornada(2, h(9, 1))
            val informe = evaluar(j)

            val diaria = informe.deJornada(j, ReglaId.CONDUCCION_DIARIA)
            assertTrue(diaria is Resultado.Cumple)
            assertEquals(true, diaria.detalle.ampliada)

            val ampliadas = informe.deSemana(lunes2, ReglaId.AMPLIACIONES_SEMANA)
            assertTrue(ampliadas is Resultado.Cumple)
            assertEquals(1, ampliadas.detalle.cuenta)
            assertEquals(2, ampliadas.detalle.maximo)
        }

        @Test
        fun `T4 - 10 h 01 se excede`() {
            val j = jornada(2, h(10, 1))
            assertTrue(evaluar(j).deJornada(j, ReglaId.CONDUCCION_DIARIA) is Resultado.Excedido)
        }

        @Test
        fun `10 h 00 justas todavia cumple (como ampliada)`() {
            val j = jornada(2, h(10))
            assertTrue(evaluar(j).deJornada(j, ReglaId.CONDUCCION_DIARIA) is Resultado.Cumple)
        }

        @Test
        fun `T21 - sin la conduccion del tacografo no se puede saber`() {
            val j = jornada(2, conduccion = null)
            val r = evaluar(j).deJornada(j, ReglaId.CONDUCCION_DIARIA)

            assertEquals(Resultado.NoVerificado::class, r::class)
            assertEquals(MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO, (r as Resultado.NoVerificado).motivo)
        }

        @Test
        fun `sin conduccion apuntada pero en una jornada de 8 h tampoco se puede pasar de 9 h`() {
            // Sin inventar nada: en 8 h de jornada no caben más de 8 h de conducción.
            val j = jornada(2, conduccion = null, dura = h(8))
            assertTrue(evaluar(j).deJornada(j, ReglaId.CONDUCCION_DIARIA) is Resultado.Cumple)
        }
    }

    @Nested
    inner class Ampliaciones {
        @Test
        fun `T3 - la tercera jornada de 10 h en la misma semana se excede`() {
            val informe = evaluar(jornada(2, h(10)), jornada(3, h(10)), jornada(4, h(10)))
            val r = informe.deSemana(lunes2, ReglaId.AMPLIACIONES_SEMANA)

            assertTrue(r is Resultado.Excedido)
            assertEquals(3, r.detalle.cuenta)
        }

        @Test
        fun `dos ampliadas y una sin conduccion apuntada no se puede saber`() {
            val informe = evaluar(jornada(2, h(10)), jornada(3, h(10)), jornada(4, conduccion = null))
            val r = informe.deSemana(lunes2, ReglaId.AMPLIACIONES_SEMANA)

            assertEquals(MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO, (r as Resultado.NoVerificado).motivo)
        }

        @Test
        fun `las ampliadas de otra semana no cuentan`() {
            val informe = evaluar(jornada(2, h(10)), jornada(3, h(10)), jornada(9, h(10)))
            assertTrue(informe.deSemana(lunes2, ReglaId.AMPLIACIONES_SEMANA) is Resultado.Cumple)
            assertTrue(informe.deSemana(lunes9, ReglaId.AMPLIACIONES_SEMANA) is Resultado.Cumple)
        }
    }

    @Nested
    inner class Semanal {
        /** 7 jornadas de 8 h = 56 h (lunes a domingo). */
        private fun semanaDe56h(desde: Int) = (0..6).map { jornada(desde + it, h(8)) }

        @Test
        fun `T5 - 56 h 00 cumple y 56 h 01 se excede`() {
            val justa = evaluar(*semanaDe56h(2).toTypedArray()).deSemana(lunes2, ReglaId.CONDUCCION_SEMANAL)
            assertTrue(justa is Resultado.Cumple)
            assertEquals(Intervalo.exacto(h(56)), justa.detalle.valor)

            siguienteId = 100
            val mas = semanaDe56h(2).toMutableList()
            mas[0] = mas[0].copy(conduccionTacografo = h(8, 1))
            assertTrue(evaluar(*mas.toTypedArray()).deSemana(lunes2, ReglaId.CONDUCCION_SEMANAL) is Resultado.Excedido)
        }

        @Test
        fun `T6 - 56 h una semana y 35 h la siguiente suman 91 h - se excede la bisemana`() {
            val segunda = (0..4).map { jornada(9 + it, h(7)) } // 35 h
            val informe = evaluar(*(semanaDe56h(2) + segunda).toTypedArray())

            val r = informe.deSemana(lunes9, ReglaId.CONDUCCION_BISEMANAL)
            assertTrue(r is Resultado.Excedido)
            assertEquals(Intervalo.exacto(h(91)), r.detalle.valor)
            assertEquals(h(90), r.detalle.limite)
        }

        @Test
        fun `la primera semana registrada no se sabe cuanto se condujo la anterior`() {
            // Antes de usar la app pudo conducir hasta 56 h: 40 + 56 > 90, así que no se puede asegurar.
            val informe = desdeElLunes2((0..4).map { jornada(2 + it, h(8)) })
            val r = informe.deSemana(lunes2, ReglaId.CONDUCCION_BISEMANAL)

            assertEquals(MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO, (r as Resultado.NoVerificado).motivo)
        }

        @Test
        fun `una semana de poca conduccion cumple la bisemana aunque sea la primera`() {
            // 30 h + como mucho 56 h de antes = 86 h ≤ 90 h.
            val informe = desdeElLunes2((0..2).map { jornada(2 + it, h(10)) })
            assertTrue(informe.deSemana(lunes2, ReglaId.CONDUCCION_BISEMANAL) is Resultado.Cumple)
        }

        @Test
        fun `T20 - una jornada que cruza el domingo a las 24 h se reparte con un intervalo`() {
            // Empieza el domingo 8 a las 20:00 y acaba el lunes 9 a las 06:00 con 6 h de conducción:
            // a la semana del 2 le tocan entre 0 h y 4 h; a la del 9, entre 2 h y 6 h.
            val cruza = jornada(8, h(6), dura = h(10), empieza = hora(8, 20))
            val informe = evaluar(cruza)

            assertEquals(Intervalo(h(0), h(4)), informe.semanas.single { it.lunes == lunes2 }.conduccion)
            assertEquals(Intervalo(h(2), h(6)), informe.semanas.single { it.lunes == lunes9 }.conduccion)
            // Aun con el peor reparto, ninguna semana pasa de 56 h: cumple sin inventar nada.
            assertTrue(informe.deSemana(lunes2, ReglaId.CONDUCCION_SEMANAL) is Resultado.Cumple)
        }

        @Test
        fun `si el reparto de una jornada que cruza la semana decide el limite, no se puede saber`() {
            // Semana del 2 con 54 h el lunes–sábado, más una jornada que cruza el domingo con 6 h:
            // a la semana del 2 le tocan entre 54 h y 58 h.
            val semana = (0..5).map { jornada(2 + it, h(9)) }
            val cruza = jornada(8, h(6), dura = h(10), empieza = hora(8, 20))
            val r = evaluar(*(semana + cruza).toTypedArray()).deSemana(lunes2, ReglaId.CONDUCCION_SEMANAL)

            assertEquals(MotivoNoVerificado.JORNADA_CRUZA_SEMANA, (r as Resultado.NoVerificado).motivo)
        }
    }

    @Nested
    inner class Pausas {
        @Test
        fun `T22 - con 4 h 30 de conduccion no hacia falta pausa`() {
            val j = jornada(2, h(4, 30))
            assertTrue(evaluar(j).deJornada(j, ReglaId.PAUSA) is Resultado.Cumple)
        }

        @Test
        fun `T22 - con 4 h 31 no se sabe si se hizo a tiempo (no hay tramos)`() {
            val j = jornada(2, h(4, 31))
            val r = evaluar(j).deJornada(j, ReglaId.PAUSA)

            assertEquals(MotivoNoVerificado.SIN_TRAMOS_CONDUCCION, (r as Resultado.NoVerificado).motivo)
        }
    }

    @Test
    fun `la jornada en curso cuenta en la semana pero aun no tiene resultados propios`() {
        val enCurso = Jornada(id = 50, inicio = ahora.minus(h(3)), zonaInicio = madrid, kmInicio = 0)
        val informe = MotorReglas().evaluar(ContextoReglas(listOf(enCurso), ahora = ahora, datosDesde = desdeSiempre))

        val semana = informe.semanas.single()
        assertEquals(Intervalo(Duration.ZERO, h(3)), semana.conduccion)
        assertTrue(
            semana.jornadas
                .single()
                .resultados
                .isEmpty(),
        )
    }

    @Test
    fun `las semanas salen de la mas reciente a la mas antigua`() {
        val informe = evaluar(jornada(2, h(8)), jornada(16, h(8)))
        assertEquals(listOf(LocalDate.of(2026, 11, 16), lunes2), informe.semanas.map { it.lunes })
    }

    @Test
    fun `si los datos empiezan a mitad de semana, lo de antes no se sabe`() {
        // Empieza a usar la app el jueves 5: de lunes a miércoles pudo conducir sin apuntarlo.
        val jueves = jornada(5, h(9))
        val informe = MotorReglas().evaluar(ContextoReglas(listOf(jueves), ahora = ahora))

        val semana = informe.semanas.single()
        assertEquals(h(9), semana.conduccion.min)
        assertTrue(semana.conduccion.max > h(56))
        val r = semana.resultado(ReglaId.CONDUCCION_SEMANAL)
        assertEquals(MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO, (r as Resultado.NoVerificado).motivo)
    }
}
