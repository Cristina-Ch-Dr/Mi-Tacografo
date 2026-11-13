package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
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
 * Reglamento (CE) 561/2006, art. 8.6 y 8.9 (espec. §7.5 y §7.7; casos T15–T17d de §13). Datos inventados.
 *
 * Semanas que empiezan el lunes 2, 9, 16 y 23 de noviembre de 2026. La empresa está en ES y el conductor
 * vive en UA; «fuera de casa» es cualquier otro país (DE, FR…).
 */
class ReglasDescansoSemanalTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val ahora: Instant = LocalDateTime.of(2027, 1, 15, 12, 0).atZone(madrid).toInstant()
    private val lunes2 = LocalDate.of(2026, 11, 2)
    private val lunes9 = LocalDate.of(2026, 11, 9)

    private fun hora(
        dia: Int,
        h: Int,
        m: Int = 0,
    ): Instant =
        if (dia > 30) {
            LocalDateTime.of(2026, 12, dia - 30, h, m).atZone(madrid).toInstant()
        } else {
            LocalDateTime.of(2026, 11, dia, h, m).atZone(madrid).toInstant()
        }

    private fun h(
        horas: Long,
        minutos: Long = 0,
    ) = Duration.ofHours(horas).plusMinutes(minutos)

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

    /**
     * Rellena de [desde] a [hasta] con jornadas de 13 h y descansos diarios de 11 h, sin dejar huecos:
     * así ninguna regla puede pensar que hubo un semanal sin apuntar.
     */
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

    /**
     * Semanas seguidas, cada una con trabajo de lunes a sábado y su semanal el fin de semana.
     * @param semanales duración y país de cada semanal (empieza el sábado de su semana a las 06:00).
     */
    private fun semanas(
        semanales: List<Pair<Duration, String?>>,
        empresa: String? = "ES",
        residencia: String? = "UA",
    ): InformeReglas {
        val jornadas = mutableListOf<Jornada>()
        val descansos = mutableListOf<Descanso>()
        // Un semanal normal antes de empezar, que acaba el lunes 2 a las 06:00.
        descansos += descanso(hora(2, 6).minus(h(48)), h(48))
        var finAnterior = hora(2, 6)
        semanales.forEachIndexed { i, (dura, pais) ->
            val sabado = hora(7 + 7 * i, 6)
            val (j, d) = trabajo(finAnterior, sabado)
            jornadas += j
            descansos += d
            descansos += descanso(sabado, dura, pais)
            finAnterior = sabado.plus(dura)
        }
        val (j, d) = trabajo(finAnterior, hora(7 + 7 * semanales.size, 6))
        jornadas += j
        descansos += d
        return MotorReglas().evaluar(
            ContextoReglas(jornadas, descansos, ahora = ahora, paisEmpresa = empresa, paisResidencia = residencia),
        )
    }

    private fun InformeReglas.de(
        lunes: LocalDate,
        regla: ReglaId,
    ): Resultado = semanas.single { it.lunes == lunes }.resultados.single { it.regla == regla }

    private val normal = h(45) to "DE"
    private val reducidoFuera = h(30) to "DE"

    @Nested
    inner class DosSemanas {
        @Test
        fun `un normal y un reducido en dos semanas seguidas cumplen`() {
            assertTrue(
                semanas(listOf(normal, reducidoFuera)).de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS) is Resultado.Cumple,
            )
        }

        @Test
        fun `dos normales cumplen`() {
            assertTrue(semanas(listOf(normal, normal)).de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS) is Resultado.Cumple)
        }

        @Test
        fun `T17 - dos reducidos seguidos y uno en el pais de la empresa (caso nacional) se excede`() {
            val informe = semanas(listOf(h(30) to "ES", reducidoFuera))
            assertTrue(informe.de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS) is Resultado.Excedido)
        }

        @Test
        fun `sin semanal apuntado en una semana con huecos no se puede saber`() {
            // Semana del 9 sin semanal y sin nada apuntado del sábado 14 al domingo 15.
            val descansos =
                listOf(descanso(hora(2, 6).minus(h(48)), h(48)), descanso(hora(7, 6), h(45)))
            val jornadas = listOf(jornada(hora(9, 6), h(12)), jornada(hora(10, 6), h(12)))
            val informe =
                MotorReglas().evaluar(
                    ContextoReglas(jornadas, descansos, ahora = ahora, paisEmpresa = "ES", paisResidencia = "UA"),
                )

            val r = informe.de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS)
            assertEquals(MotivoNoVerificado.DESCANSO_NO_REGISTRADO, (r as Resultado.NoVerificado).motivo)
        }
    }

    @Nested
    inner class SeisPeriodos {
        @Test
        fun `T15 - el semanal empieza 144 h 01 despues del anterior y se excede`() {
            // El semanal previo acaba el lunes 2 a las 06:00; 144 h después es el domingo 8 a las 06:00.
            val previo = descanso(hora(2, 6).minus(h(48)), h(48))
            val (jornadas, diarios) = trabajo(hora(2, 6), hora(8, 6, 1))
            val tarde = descanso(hora(8, 6, 1), h(45))
            val informe =
                MotorReglas().evaluar(
                    ContextoReglas(
                        jornadas,
                        diarios + previo + tarde,
                        ahora = ahora,
                        paisEmpresa = "ES",
                        paisResidencia = "UA",
                    ),
                )

            assertTrue(informe.de(lunes2, ReglaId.SEIS_PERIODOS) is Resultado.Excedido)
        }

        @Test
        fun `a las 144 h justas todavia cumple`() {
            val previo = descanso(hora(2, 6).minus(h(48)), h(48))
            val (jornadas, diarios) = trabajo(hora(2, 6), hora(8, 6))
            val justo = descanso(hora(8, 6), h(45))
            val informe =
                MotorReglas().evaluar(
                    ContextoReglas(
                        jornadas,
                        diarios + previo + justo,
                        ahora = ahora,
                        paisEmpresa = "ES",
                        paisResidencia = "UA",
                    ),
                )

            assertTrue(informe.de(lunes2, ReglaId.SEIS_PERIODOS) is Resultado.Cumple)
        }

        @Test
        fun `si entre medias queda un hueco de 24 h sin nada apuntado, no se puede saber`() {
            val previo = descanso(hora(2, 6).minus(h(48)), h(48))
            // Del lunes a las 18:00 al jueves a las 06:00 no hay nada apuntado.
            val jornadas = listOf(jornada(hora(2, 6), h(12)), jornada(hora(5, 6), h(12)))
            val tarde = descanso(hora(8, 12), h(45))
            val informe =
                MotorReglas().evaluar(
                    ContextoReglas(
                        jornadas,
                        listOf(previo, tarde),
                        ahora = ahora,
                        paisEmpresa = "ES",
                        paisResidencia = "UA",
                    ),
                )

            val r = informe.de(lunes2, ReglaId.SEIS_PERIODOS)
            assertEquals(MotivoNoVerificado.DESCANSO_NO_REGISTRADO, (r as Resultado.NoVerificado).motivo)
        }
    }

    @Nested
    inner class Internacional {
        @Test
        fun `T16 - dos reducidos seguidos fuera de casa y dos normales en las 4 semanas cumplen`() {
            val informe = semanas(listOf(reducidoFuera, reducidoFuera, normal, normal))

            assertTrue(informe.de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS) is Resultado.Cumple)
            assertTrue(informe.de(lunes9, ReglaId.REDUCIDOS_FUERA_DE_CASA) is Resultado.Cumple)
        }

        @Test
        fun `T17b - dos reducidos fuera de casa pero solo un normal en las 4 semanas se excede`() {
            val informe = semanas(listOf(reducidoFuera, reducidoFuera, normal, reducidoFuera))
            assertTrue(informe.de(lunes9, ReglaId.REDUCIDOS_FUERA_DE_CASA) is Resultado.Excedido)
        }

        @Test
        fun `T17c - uno de los dos reducidos sin pais no se puede saber`() {
            val informe = semanas(listOf(h(30) to null, reducidoFuera, normal, normal))

            val r = informe.de(lunes9, ReglaId.REDUCIDOS_FUERA_DE_CASA)
            assertEquals(MotivoNoVerificado.SIN_PAIS_DESCANSO, (r as Resultado.NoVerificado).motivo)
        }

        @Test
        fun `T17d - sin los paises de la empresa y de residencia en Ajustes no se puede saber`() {
            val informe = semanas(listOf(reducidoFuera, reducidoFuera, normal, normal), empresa = null)

            val r = informe.de(lunes9, ReglaId.REDUCIDOS_FUERA_DE_CASA)
            assertEquals(MotivoNoVerificado.PAISES_NO_CONFIGURADOS, (r as Resultado.NoVerificado).motivo)
            assertEquals(
                MotivoNoVerificado.PAISES_NO_CONFIGURADOS,
                (informe.de(lunes9, ReglaId.SEMANAL_DOS_SEMANAS) as Resultado.NoVerificado).motivo,
            )
        }

        @Test
        fun `el pais de residencia tampoco cuenta como fuera de casa`() {
            val informe = semanas(listOf(h(30) to "UA", reducidoFuera, normal, normal))
            assertTrue(informe.de(lunes9, ReglaId.REDUCIDOS_FUERA_DE_CASA) is Resultado.Excedido)
        }
    }

    @Test
    fun `D-2 - un semanal cuenta en la semana en que empieza salvo que ya tenga uno - entonces en la siguiente`() {
        val primero = descanso(hora(2, 6), h(45)) // del lunes 2 al miércoles 4
        val segundo = descanso(hora(7, 6), h(45)) // del sábado 7 al lunes 9: la semana del 2 ya tiene uno
        val asignados = asignarSemanales(ContextoReglas(emptyList(), listOf(primero, segundo), ahora = ahora))

        assertEquals(listOf(primero.id), asignados[lunes2]?.map { it.descanso.id })
        assertEquals(listOf(segundo.id), asignados[lunes9]?.map { it.descanso.id })
    }
}
