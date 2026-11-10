package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Jornada
import java.time.Duration
import java.time.LocalDate

// Reglamento (CE) 561/2006, art. 6 (conducción) y 7 (pausas), consolidado a 22/05/2024.
// Revisado con EUR-Lex antes de escribirlas (MT-24).

private val NUEVE_HORAS: Duration = Duration.ofHours(9)
private val DIEZ_HORAS: Duration = Duration.ofHours(10)
private val SEMANAL: Duration = Duration.ofHours(56)
private val BISEMANAL: Duration = Duration.ofHours(90)
private val ANTES_DE_PAUSA: Duration = Duration.ofMinutes(270) // 4 h 30
private const val AMPLIADAS_POR_SEMANA = 2

/**
 * R-COND-1 (art. 6.1): conducción diaria ≤ 9 h. Hasta 10 h se permite como ampliada
 * (cuántas hay en la semana lo comprueba [Ampliaciones]).
 */
internal object ConduccionDiaria : ReglaJornada {
    override fun evaluar(
        jornada: Jornada,
        contexto: ContextoReglas,
    ): List<Resultado> {
        val conduccion = conduccionPosible(jornada, contexto.ahora)
        val detalle =
            Detalle(
                valor = conduccion,
                limite = if (conduccion.min > NUEVE_HORAS) DIEZ_HORAS else NUEVE_HORAS,
                ampliada = conduccion.min > NUEVE_HORAS,
            )
        return listOf(
            comparar(
                ReglaId.CONDUCCION_DIARIA,
                conduccion,
                DIEZ_HORAS,
                MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO,
                detalle,
            ),
        )
    }
}

/**
 * R-COND-2 (art. 6.1): como mucho 2 jornadas de más de 9 h por semana.
 *
 * Cuenta las que pasan de 9 h seguro y las que podrían pasar (sin conducción apuntada).
 */
internal object Ampliaciones : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val conducciones = semana.jornadas.map { conduccionPosible(it, semana.ahora) }
        val seguras = conducciones.count { it.min > NUEVE_HORAS }
        val posibles = conducciones.count { it.max > NUEVE_HORAS }
        val detalle = Detalle(cuenta = seguras, maximo = AMPLIADAS_POR_SEMANA)
        val resultado =
            when {
                seguras > AMPLIADAS_POR_SEMANA -> {
                    Resultado.Excedido(ReglaId.AMPLIACIONES_SEMANA, detalle)
                }

                posibles <= AMPLIADAS_POR_SEMANA -> {
                    Resultado.Cumple(ReglaId.AMPLIACIONES_SEMANA, detalle)
                }

                else -> {
                    Resultado.NoVerificado(
                        ReglaId.AMPLIACIONES_SEMANA,
                        MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO,
                        detalle,
                    )
                }
            }
        return listOf(resultado)
    }
}

/** R-COND-3 (art. 6.2): conducción de la semana ≤ 56 h. */
internal object ConduccionSemanal : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> =
        listOf(
            comparar(
                ReglaId.CONDUCCION_SEMANAL,
                semana.conduccion,
                SEMANAL,
                semana.motivo ?: MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO,
            ),
        )
}

/**
 * R-COND-4 (art. 6.3): esta semana y la anterior, juntas, ≤ 90 h.
 *
 * La semana anterior a la primera apuntada es desconocida: pudo conducir hasta 56 h (el máximo legal),
 * así que cuenta como [0, 56 h]. Una semana sin jornadas después de la primera cuenta como 0 h.
 */
internal object ConduccionBisemanal : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val lunesAnterior = semana.lunes.minusWeeks(1)
        val anterior =
            when {
                lunesAnterior < primeraSemana -> Intervalo(Duration.ZERO, SEMANAL)
                else -> todas[lunesAnterior]?.conduccion ?: Intervalo.CERO
            }
        val motivo =
            semana.motivo
                ?: todas[lunesAnterior]?.motivo
                ?: MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO
        return listOf(comparar(ReglaId.CONDUCCION_BISEMANAL, semana.conduccion + anterior, BISEMANAL, motivo))
    }
}

/**
 * R-PAUSA-1 (art. 7): tras 4 h 30 de conducción, 45 min de pausa (o 15 + 30).
 *
 * La app no sabe en qué tramos se condujo, solo el total: con 4 h 30 o menos no hacía falta pausa (cumple);
 * con más, aunque haya pausas apuntadas, no se sabe si fueron a tiempo (no verificado, D-4).
 */
internal object Pausa : ReglaJornada {
    override fun evaluar(
        jornada: Jornada,
        contexto: ContextoReglas,
    ): List<Resultado> {
        val conduccion = conduccionPosible(jornada, contexto.ahora)
        val detalle = Detalle(valor = conduccion, limite = ANTES_DE_PAUSA)
        val resultado =
            when {
                conduccion.max <= ANTES_DE_PAUSA -> {
                    Resultado.Cumple(ReglaId.PAUSA, detalle)
                }

                conduccion.exacto -> {
                    Resultado.NoVerificado(
                        ReglaId.PAUSA,
                        MotivoNoVerificado.SIN_TRAMOS_CONDUCCION,
                        detalle,
                    )
                }

                else -> {
                    Resultado.NoVerificado(ReglaId.PAUSA, MotivoNoVerificado.SIN_CONDUCCION_TACOGRAFO, detalle)
                }
            }
        return listOf(resultado)
    }
}
