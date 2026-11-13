package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.model.Jornada
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

// Reglamento (CE) 561/2006, art. 8.2 y 8.4 (MT-27). Texto revisado antes de escribirlas:
// - 8.2: en cada periodo de 24 h desde el final del descanso diario o semanal anterior, un nuevo descanso
//   diario; si la parte dentro de esas 24 h es ≥ 9 h y < 11 h, es reducido.
// - 8.4: como mucho 3 descansos diarios reducidos entre dos descansos semanales.

private val PLAZO: Duration = Duration.ofHours(24)
private val MINIMO_DIARIO: Duration = Duration.ofHours(9)
private const val MAX_REDUCIDOS = 3

/** Clases que cuentan como descanso diario o semanal (las pausas y las primeras partes no). */
private val QUE_CUENTAN =
    setOf(
        ClaseDescanso.DIARIO_NORMAL,
        ClaseDescanso.DIARIO_REDUCIDO,
        ClaseDescanso.FRACCIONADO_COMPLETO,
        ClaseDescanso.SEMANAL_NORMAL,
        ClaseDescanso.SEMANAL_REDUCIDO,
    )

private val SEMANALES = setOf(ClaseDescanso.SEMANAL_NORMAL, ClaseDescanso.SEMANAL_REDUCIDO)

/** Los descansos terminados que cuentan como diario o semanal, en orden. */
private fun ContextoReglas.queCuentan(): List<DescansoClasificado> =
    clasificados.filter { it.clase in QUE_CUENTAN && it.descanso.fin != null }

/**
 * El tramo más largo de [desde, hasta) en el que no hay ninguna jornada ni ningún descanso apuntado:
 * ahí podría haber un descanso sin apuntar.
 */
internal fun ContextoReglas.huecoMasLargo(
    desde: Instant,
    hasta: Instant,
): Duration {
    val ocupado =
        (jornadas.map { it.inicio to (it.fin ?: ahora) } + descansos.map { it.inicio to (it.fin ?: ahora) })
            .filter { (i, f) -> f > desde && i < hasta }
            .sortedBy { it.first }
    var cursor = desde
    var mayor = Duration.ZERO
    ocupado.forEach { (i, f) ->
        if (i > cursor) mayor = maxOf(mayor, Duration.between(cursor, minOf(i, hasta)))
        cursor = maxOf(cursor, f)
    }
    if (hasta > cursor) mayor = maxOf(mayor, Duration.between(cursor, hasta))
    return mayor
}

/**
 * R-DD-1 (art. 8.2): después del descanso que precede a la jornada, en 24 h tiene que haber otro descanso
 * diario, con al menos 9 h dentro del plazo.
 *
 * - Si el descanso anterior no está apuntado, no se sabe cuándo empezó el plazo: no verificado.
 * - Si el siguiente descanso no deja 9 h dentro del plazo: se excede, salvo que en el plazo quede un hueco
 *   sin nada apuntado de 9 h o más (podría haber descansado ahí): entonces no verificado (T23).
 * - Si aún no han pasado las 24 h y no hay descanso, todavía no hay resultado.
 */
internal object DescansoDiario : ReglaJornada {
    override fun evaluar(
        jornada: Jornada,
        contexto: ContextoReglas,
    ): List<Resultado> {
        val validos = contexto.queCuentan()
        val anteriorJornada =
            contexto.jornadas
                .filter {
                    it.fin != null && it.fin <= jornada.inicio
                }.maxByOrNull { it.fin!! }
        // El descanso que acaba justo antes de la jornada (después de la anterior, si la hay).
        val previo =
            validos
                .filter {
                    it.descanso.fin!! <= jornada.inicio &&
                        (anteriorJornada == null || it.descanso.fin!! > anteriorJornada.fin!!)
                }.maxByOrNull { it.descanso.fin!! }
                ?: return listOf(
                    Resultado.NoVerificado(ReglaId.DESCANSO_DIARIO, MotivoNoVerificado.DESCANSO_NO_REGISTRADO),
                )

        val desde = previo.descanso.fin!!
        val limite = desde.plus(PLAZO)
        val siguiente = validos.firstOrNull { it.descanso.inicio >= desde && it.descanso != previo.descanso }

        if (siguiente == null && limite > contexto.ahora) return emptyList()

        // Después de la última jornada sigue descansando (MT-46): cuenta lo que queda del plazo tras su fin.
        val esLaUltima = contexto.jornadas.none { it.inicio > jornada.inicio }
        if (siguiente == null && contexto.descansoEntreJornadas && esLaUltima) {
            val tras = Duration.between(jornada.fin!!, limite).coerceAtLeast(Duration.ZERO)
            val detalle = Detalle(valor = Intervalo.exacto(tras), limite = MINIMO_DIARIO)
            return listOf(
                if (tras >= MINIMO_DIARIO) {
                    Resultado.Cumple(ReglaId.DESCANSO_DIARIO, detalle)
                } else {
                    Resultado.Excedido(ReglaId.DESCANSO_DIARIO, detalle)
                },
            )
        }

        val dentro =
            siguiente
                ?.takeIf { it.descanso.inicio < limite }
                ?.let { Duration.between(it.descanso.inicio, minOf(it.descanso.fin!!, limite)) }
                ?: Duration.ZERO
        val valor = Intervalo.exacto(dentro)
        val detalle = Detalle(valor = valor, limite = MINIMO_DIARIO)
        val resultado =
            when {
                // Un fraccionado 3 + 9 completo ya se comprobó que acaba dentro de las 24 h al clasificarlo.
                siguiente?.clase == ClaseDescanso.FRACCIONADO_COMPLETO -> {
                    Resultado.Cumple(ReglaId.DESCANSO_DIARIO, detalle)
                }

                dentro >= MINIMO_DIARIO -> {
                    Resultado.Cumple(ReglaId.DESCANSO_DIARIO, detalle)
                }

                contexto.huecoMasLargo(desde, limite) >= MINIMO_DIARIO -> {
                    Resultado.NoVerificado(ReglaId.DESCANSO_DIARIO, MotivoNoVerificado.DESCANSO_NO_REGISTRADO, detalle)
                }

                else -> {
                    Resultado.Excedido(ReglaId.DESCANSO_DIARIO, detalle)
                }
            }
        return listOf(resultado)
    }
}

/**
 * R-DD-3 (art. 8.4): como mucho 3 diarios reducidos entre dos semanales.
 *
 * Para la semana cuenta los reducidos desde el último semanal apuntado hasta el final de la semana.
 * - Sin un semanal apuntado antes, no se sabe cuántos llevaba (salvo que ya pasen de 3).
 * - Si entre dos jornadas del tramo no hay descanso apuntado, podría faltar algún reducido: no verificado.
 */
internal object ReducidosEntreSemanales : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val contexto = semana.contexto
        val zona = contexto.zonaReferencia
        val finSemana = semana.lunes.plusWeeks(1)

        fun dia(
            i: Instant,
            z: ZoneId,
        ) = i.atZone(zona ?: z).toLocalDate()

        val validos = contexto.queCuentan().filter { dia(it.descanso.inicio, it.descanso.zonaInicio) < finSemana }
        val ultimoSemanal = validos.lastOrNull { it.clase in SEMANALES }
        val reducidos =
            validos
                .filter { it.clase == ClaseDescanso.DIARIO_REDUCIDO }
                .filter { ultimoSemanal == null || it.descanso.inicio > ultimoSemanal.descanso.inicio }
        val detalle = Detalle(cuenta = reducidos.size, maximo = MAX_REDUCIDOS)

        // Jornadas del tramo (desde el último semanal hasta el final de la semana) con un hueco sin descanso detrás.
        val desde = ultimoSemanal?.descanso?.fin
        val delTramo =
            contexto.jornadas
                .filter {
                    it.fin != null && (desde == null || it.inicio >= desde) &&
                        dia(it.inicio, it.zonaInicio) < finSemana
                }.sortedBy { it.inicio }
        val faltaAlguno =
            delTramo.zipWithNext().any { (a, b) ->
                validos.none { it.descanso.inicio >= a.fin!! && it.descanso.fin!! <= b.inicio }
            }

        val resultado =
            when {
                reducidos.size > MAX_REDUCIDOS -> {
                    Resultado.Excedido(ReglaId.REDUCIDOS_ENTRE_SEMANALES, detalle)
                }

                ultimoSemanal == null -> {
                    Resultado.NoVerificado(
                        ReglaId.REDUCIDOS_ENTRE_SEMANALES,
                        MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO,
                        detalle,
                    )
                }

                faltaAlguno -> {
                    Resultado.NoVerificado(
                        ReglaId.REDUCIDOS_ENTRE_SEMANALES,
                        MotivoNoVerificado.DESCANSO_NO_REGISTRADO,
                        detalle,
                    )
                }

                else -> {
                    Resultado.Cumple(ReglaId.REDUCIDOS_ENTRE_SEMANALES, detalle)
                }
            }
        return listOf(resultado)
    }
}
