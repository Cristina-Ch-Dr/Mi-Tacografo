package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/**
 * El tiempo entre dos jornadas es descanso (MT-46): el conductor no tiene que apuntarlo.
 *
 * Por cada hueco entre el fin de una jornada y el inicio de la siguiente en el que no hay ningún descanso
 * apuntado, añade uno calculado (id negativo, ver [Descanso.calculado]). Si el conductor apuntó algún
 * descanso en ese hueco, manda lo apuntado. Las pausas no cuentan: son de dentro de la jornada.
 *
 * El país del descanso calculado es donde acabó la jornada anterior (o, si no se apuntó, donde empezó la
 * siguiente): lo necesita la regla de los semanales reducidos fuera de casa.
 *
 * Antes de la primera jornada no se sabe nada, y después de la última aún no hay descanso terminado
 * (lo que lleva descansando lo da [descansoActual]).
 *
 * @return los descansos apuntados más los calculados, en orden cronológico.
 */
fun completarDescansos(
    jornadas: List<Jornada>,
    descansos: List<Descanso>,
): List<Descanso> {
    val apuntados = descansos.filter { it.tipo == TipoDescanso.DESCANSO }
    val calculados =
        jornadas
            .sortedBy { it.inicio }
            .zipWithNext()
            .mapIndexedNotNull { i, (anterior, siguiente) ->
                val desde = anterior.fin ?: return@mapIndexedNotNull null
                val hasta = siguiente.inicio
                if (hasta <= desde) return@mapIndexedNotNull null
                val hayApuntado = apuntados.any { it.inicio < hasta && (it.fin ?: hasta) > desde }
                if (hayApuntado) return@mapIndexedNotNull null
                Descanso(
                    id = -(i + 1).toLong(),
                    inicio = desde,
                    zonaInicio = anterior.zonaFin ?: anterior.zonaInicio,
                    fin = hasta,
                    zonaFin = siguiente.zonaInicio,
                    tipo = TipoDescanso.DESCANSO,
                    pais = anterior.paisFin ?: siguiente.paisInicio,
                )
            }
    return (descansos + calculados).sortedBy { it.inicio }
}

/** `true` si no lo apuntó el conductor: lo calculó la app con el hueco entre dos jornadas. */
val Descanso.calculado: Boolean get() = id < 0

/** Lo que descansó entre la jornada anterior y una jornada: el total y el descanso más largo, con su clase. */
data class DescansoPrevio(
    val total: Duration,
    val principal: DescansoClasificado,
)

/**
 * Lo que descansó antes de cada jornada (por id): los descansos, apuntados o calculados, entre el fin de la
 * jornada anterior y su inicio. Sin entrada para la primera (no se sabe qué hubo antes) ni si no hay ninguno.
 *
 * @param clasificados los descansos ya completados con [completarDescansos] y clasificados.
 */
fun descansoAntesDeCadaJornada(
    jornadas: List<Jornada>,
    clasificados: List<DescansoClasificado>,
): Map<Long, DescansoPrevio> =
    jornadas
        .sortedBy { it.inicio }
        .zipWithNext()
        .mapNotNull { (anterior, siguiente) ->
            val desde = anterior.fin ?: return@mapNotNull null
            val delHueco =
                clasificados.filter {
                    it.descanso.tipo == TipoDescanso.DESCANSO &&
                        it.descanso.fin != null &&
                        it.descanso.inicio >= desde &&
                        it.descanso.fin <= siguiente.inicio
                }
            val principal = delHueco.maxByOrNull { it.descanso.duracion!! } ?: return@mapNotNull null
            val total = delHueco.mapNotNull { it.descanso.duracion }.fold(Duration.ZERO, Duration::plus)
            siguiente.id to DescansoPrevio(total, principal)
        }.toMap()

/** Desde cuándo descansa, si no hay jornada en curso: el fin de la última. `null` si sigue trabajando o no hay ninguna. */
fun descansoActual(jornadas: List<Jornada>): Instant? = jornadas.maxByOrNull { it.inicio }?.fin
