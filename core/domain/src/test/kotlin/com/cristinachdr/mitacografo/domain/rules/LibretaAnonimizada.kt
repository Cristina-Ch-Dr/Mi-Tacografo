package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Dos semanas y media de la libreta del conductor (1–17 de octubre de 2026), **anonimizadas** (MT-30):
 * solo horas, conducción y km recorridos. Sin lugares ni países, y con el cuentakilómetros desplazado
 * (empieza en 100 000) para que no se pueda reconocer el camión.
 *
 * La libreta no se sube al repositorio; esto sí, porque ya no tiene nada que identifique a nadie.
 *
 * Única suposición: el 04/10 se le olvidó apuntar el fin. Se pone a las 06:39 del 05/10, que es lo que
 * sale de las 11 h de descanso que apuntó antes de la jornada del 05/10 a las 17:39.
 *
 * Lo que él sumó a mano (S = semana, BS = bisemana), para comparar con el motor:
 * - semana del 5/10: S = 43 h 08; bisemana: BS = 90 h 01 (contando la semana del 28/09, que no está entera aquí);
 * - semana del 12/10: S = 29 h 16; BS = 72 h 24.
 */
internal object LibretaAnonimizada {
    val zona: ZoneId = ZoneId.of("Europe/Madrid")

    private fun t(
        dia: Int,
        h: Int,
        m: Int,
    ) = LocalDateTime.of(2026, 10, dia, h, m).atZone(zona).toInstant()

    private fun d(
        h: Long,
        m: Long,
    ) = Duration.ofHours(h).plusMinutes(m)

    /** (inicio, conducción, fin, km recorridos) de cada jornada, tal y como están en la libreta. */
    private val filas =
        listOf(
            Fila(t(1, 9, 10), d(8, 21), t(1, 19, 6), 631),
            Fila(t(2, 10, 50), d(8, 57), t(2, 22, 30), 691),
            Fila(t(3, 8, 5), d(9, 40), t(3, 20, 0), 780),
            Fila(t(4, 20, 0), d(8, 21), t(5, 6, 39), 697), // fin supuesto (se le olvidó)
            Fila(t(5, 17, 39), d(2, 14), t(5, 19, 56), 191),
            Fila(t(6, 8, 1), d(4, 43), t(6, 16, 20), 322),
            Fila(t(7, 8, 14), d(9, 12), t(7, 20, 16), 721),
            Fila(t(8, 7, 45), d(8, 4), t(8, 18, 15), 624),
            Fila(t(9, 7, 55), d(8, 3), t(9, 18, 33), 519),
            Fila(t(10, 3, 55), d(6, 23), t(10, 11, 41), 538),
            Fila(t(12, 2, 1), d(9, 23), t(12, 13, 36), 799),
            Fila(t(13, 9, 0), d(6, 45), t(13, 18, 30), 540),
            Fila(t(16, 9, 1), d(6, 34), t(16, 19, 35), 538),
            Fila(t(17, 8, 11), d(6, 34), t(17, 19, 24), 530),
        )

    private data class Fila(
        val inicio: java.time.Instant,
        val conduccion: Duration,
        val fin: java.time.Instant,
        val km: Int,
    )

    val jornadas: List<Jornada> =
        filas
            .runningFold(100_000 to (null as Jornada?)) { (km, _), fila ->
                val jornada =
                    Jornada(
                        inicio = fila.inicio,
                        zonaInicio = zona,
                        kmInicio = km,
                        fin = fila.fin,
                        zonaFin = zona,
                        kmFin = km + fila.km,
                        conduccionTacografo = fila.conduccion,
                    )
                (km + fila.km) to jornada
            }.mapNotNull { it.second }
            .mapIndexed { i, j -> j.copy(id = i + 1L) }

    /** Los descansos entre una jornada y la siguiente (lo que apuntaría con «¿Descansaste desde…?»). */
    val descansos: List<Descanso> =
        jornadas.zipWithNext().mapIndexed { i, (a, b) ->
            Descanso(
                id = 100L + i,
                inicio = a.fin!!,
                zonaInicio = zona,
                fin = b.inicio,
                zonaFin = zona,
                tipo = TipoDescanso.DESCANSO,
            )
        }
}
