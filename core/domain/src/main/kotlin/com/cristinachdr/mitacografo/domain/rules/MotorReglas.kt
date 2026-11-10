package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.Jornada
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Todo lo que necesitan las reglas. */
data class ContextoReglas(
    val jornadas: List<Jornada>,
    val descansos: List<Descanso> = emptyList(),
    /** Zona en la que empieza la semana el lunes a las 00:00 (D-1). `null` = la de cada jornada. */
    val zonaReferencia: ZoneId? = null,
    val ahora: Instant,
)

/** Una jornada con sus resultados (conducción diaria, pausa…). Vacío mientras sigue en curso. */
data class InformeJornada(
    val jornada: Jornada,
    val resultados: List<Resultado>,
) {
    fun resultado(regla: ReglaId): Resultado = resultados.single { it.regla == regla }
}

/** Una semana legal con su conducción, sus resultados y los de sus jornadas. */
data class InformeSemana(
    val lunes: LocalDate,
    /** Conducción de la semana (intervalo si alguna jornada no la tiene apuntada o cruza la semana). */
    val conduccion: Intervalo,
    val resultados: List<Resultado>,
    /** Las jornadas que **empiezan** en esta semana, en orden cronológico. */
    val jornadas: List<InformeJornada>,
) {
    val domingo: LocalDate get() = lunes.plusDays(6)

    fun resultado(regla: ReglaId): Resultado = resultados.single { it.regla == regla }
}

/** El informe completo: las semanas con jornadas, de la más reciente a la más antigua. */
data class InformeReglas(
    val semanas: List<InformeSemana>,
)

/**
 * Una regla del reglamento. Cada una se puede probar sola y añadir otra no toca las demás (espec. §8.3).
 */
internal interface ReglaJornada {
    /** Resultados de una jornada terminada. */
    fun evaluar(
        jornada: Jornada,
        contexto: ContextoReglas,
    ): List<Resultado>
}

internal interface ReglaSemana {
    /** Resultados de la semana [semana] (con todas las semanas, por si necesita las anteriores). */
    fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado>
}

/** Una semana con la conducción ya repartida. */
internal data class SemanaRepartida(
    val lunes: LocalDate,
    val partes: List<Parte>,
    /** Las jornadas terminadas que empiezan en esta semana (para contar las ampliadas). */
    val jornadas: List<Jornada>,
    val ahora: Instant,
) {
    val conduccion: Intervalo get() = partes.fold(Intervalo.CERO) { acc, p -> acc + p.conduccion }

    /** El motivo de la incertidumbre, si la hay: primero lo que falta apuntar, luego lo que cruza la semana. */
    val motivo: MotivoNoVerificado?
        get() {
            val motivos = partes.mapNotNull { it.motivo }.toSet()
            return PRIORIDAD.firstOrNull { it in motivos }
        }

    private companion object {
        val PRIORIDAD =
            listOf(MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO, MotivoNoVerificado.JORNADA_CRUZA_SEMANA)
    }
}

/**
 * El motor de reglas (espec. §8): reparte la conducción por semanas y pasa cada regla.
 *
 * De momento, conducción (art. 6) y pausas (art. 7). Las de descanso llegan en MT-27 a MT-29.
 */
class MotorReglas {
    private val reglasJornada: List<ReglaJornada> = listOf(ConduccionDiaria, Pausa)
    private val reglasSemana: List<ReglaSemana> = listOf(Ampliaciones, ConduccionSemanal, ConduccionBisemanal)

    fun evaluar(contexto: ContextoReglas): InformeReglas {
        val jornadas = contexto.jornadas.sortedBy { it.inicio }
        if (jornadas.isEmpty()) return InformeReglas(emptyList())

        val partes = repartirPorSemanas(jornadas, contexto.zonaReferencia, contexto.ahora)
        val semanaDeInicio = jornadas.groupBy { lunesDe(it.inicio, contexto.zonaReferencia ?: it.zonaInicio) }
        val lunesConDatos = (partes.keys + semanaDeInicio.keys).sorted()
        val semanas =
            lunesConDatos.associateWith { lunes ->
                SemanaRepartida(
                    lunes = lunes,
                    partes = partes[lunes].orEmpty(),
                    jornadas = semanaDeInicio[lunes].orEmpty().filterNot { it.abierta },
                    ahora = contexto.ahora,
                )
            }
        val primera = lunesConDatos.first()

        val informes =
            lunesConDatos.map { lunes ->
                val semana = semanas.getValue(lunes)
                InformeSemana(
                    lunes = lunes,
                    conduccion = semana.conduccion,
                    resultados = reglasSemana.flatMap { it.evaluar(semana, semanas, primera) },
                    jornadas =
                        semanaDeInicio[lunes].orEmpty().map { jornada ->
                            val resultados =
                                if (jornada.abierta) {
                                    emptyList()
                                } else {
                                    reglasJornada.flatMap {
                                        it.evaluar(
                                            jornada,
                                            contexto,
                                        )
                                    }
                                }
                            InformeJornada(jornada, resultados)
                        },
                )
            }
        return InformeReglas(informes.sortedByDescending { it.lunes })
    }
}
