package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

// Reglamento (CE) 561/2006, art. 8.6 ter y 8.7, con el Reglamento (UE) 2020/1054 (MT-29). Texto revisado:
// - 8.6 ter: cada reducción de un semanal se compensa con un descanso equivalente, en bloque, antes del final
//   de la 3ª semana siguiente. Tras dos reducidos seguidos, el siguiente semanal va precedido de la compensación.
// - 8.7: el descanso de compensación va unido a otro descanso de al menos 9 h.

private val SEMANAL_NORMAL: Duration = Duration.ofHours(45)
private val DIARIO_NORMAL: Duration = Duration.ofHours(11)
private val MINIMO_UNIDO: Duration = Duration.ofHours(9)

/** En qué punto está la deuda de un semanal reducido (espec. §8.4). */
enum class EstadoDeuda {
    /** Aún no se ha compensado, pero todavía hay tiempo. */
    PENDIENTE,
    COMPENSADA,

    /** Acabó el plazo sin compensar. */
    VENCIDA,

    /** Acabó el plazo y no consta la compensación, pero hay huecos sin apuntar donde pudo hacerse. */
    NO_VERIFICADO,
}

/**
 * Lo que le falta a un semanal reducido hasta 45 h, y hasta cuándo hay para devolverlo.
 *
 * @param semana la semana en la que cuenta el reducido (D-2).
 * @param fechaLimite el final de la 3ª semana siguiente (el lunes de 4 semanas después, a las 00:00).
 * @param compensadaCon el descanso con el que se devolvió, si se devolvió.
 */
data class Deuda(
    val semanal: Descanso,
    val semana: LocalDate,
    val horas: Duration,
    val fechaLimite: Instant,
    val estado: EstadoDeuda,
    val compensadaCon: Descanso? = null,
)

/**
 * Lo que un descanso tiene de más y puede servir de compensación (decisión D-3, espec. §16):
 * - de un semanal normal, lo que pasa de 45 h;
 * - de un diario, lo que pasa de 11 h (y lo de 11 h ya es el descanso de ≥ 9 h al que va unida, art. 8.7).
 *
 * Un descanso de 24 h a menos de 45 h se toma como semanal reducido: con lo que apunta la app no se puede
 * distinguir de un diario más una compensación, y la app no se lo inventa.
 */
private fun DescansoClasificado.deMas(): Duration {
    val duracion = descanso.duracion ?: return Duration.ZERO
    val base =
        when (clase) {
            ClaseDescanso.SEMANAL_NORMAL -> SEMANAL_NORMAL
            ClaseDescanso.DIARIO_NORMAL -> DIARIO_NORMAL
            else -> return Duration.ZERO
        }
    check(base >= MINIMO_UNIDO)
    return (duracion - base).coerceAtLeast(Duration.ZERO)
}

/**
 * Calcula las deudas de todos los semanales reducidos y con qué se compensan.
 *
 * Los descansos se recorren en orden; lo que uno tiene de más paga deudas **enteras** (en bloque), de la más
 * antigua a la más nueva (FIFO), siempre que el descanso acabe antes del plazo de la deuda.
 */
internal fun calcularDeudas(contexto: ContextoReglas): List<Deuda> {
    val semanaDe =
        asignarSemanales(contexto)
            .flatMap { (lunes, lista) ->
                lista.map { it.descanso.id to lunes }
            }.toMap()
    val reducidos =
        contexto.clasificados.filter {
            it.clase == ClaseDescanso.SEMANAL_REDUCIDO && it.descanso.fin != null
        }

    data class Abierta(
        val reducido: DescansoClasificado,
        val semana: LocalDate,
        val horas: Duration,
        val limite: Instant,
        var compensadaCon: Descanso? = null,
    )

    val deudas =
        reducidos.map { r ->
            val semana = semanaDe.getValue(r.descanso.id)
            Abierta(r, semana, r.deuda, contexto.inicioSemana(semana.plusWeeks(4)))
        }

    contexto.clasificados.filter { it.descanso.fin != null }.forEach { posible ->
        var deMas = posible.deMas()
        if (deMas.isZero) return@forEach
        deudas
            .filter { it.compensadaCon == null && it.reducido.descanso.fin!! <= posible.descanso.inicio }
            .filter { posible.descanso.fin!! <= it.limite }
            .forEach { deuda ->
                if (deMas >= deuda.horas) {
                    deuda.compensadaCon = posible.descanso
                    deMas -= deuda.horas
                }
            }
    }

    return deudas.map { d ->
        val estado =
            when {
                d.compensadaCon != null -> EstadoDeuda.COMPENSADA

                d.limite > contexto.ahora -> EstadoDeuda.PENDIENTE

                // ¿Pudo compensarla en un hueco sin apuntar? Haría falta la deuda más un descanso de 9 h.
                contexto.huecoMasLargo(
                    d.reducido.descanso.fin!!,
                    d.limite,
                ) >= d.horas + MINIMO_UNIDO -> EstadoDeuda.NO_VERIFICADO

                else -> EstadoDeuda.VENCIDA
            }
        Deuda(d.reducido.descanso, d.semana, d.horas, d.limite, estado, d.compensadaCon)
    }
}

/**
 * R-DS-4 (art. 8.6 ter): la deuda de cada semanal reducido se devuelve antes del final de la 3ª semana siguiente
 * (T13, T14). El resultado va en la semana del reducido; mientras está pendiente, no hay resultado.
 */
internal object Compensacion : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val deudas = calcularDeudas(semana.contexto).filter { it.semana == semana.lunes }
        return deudas.mapNotNull { deuda ->
            val detalle = Detalle(valor = Intervalo.exacto(deuda.horas))
            when (deuda.estado) {
                EstadoDeuda.COMPENSADA -> {
                    Resultado.Cumple(ReglaId.COMPENSACION, detalle)
                }

                EstadoDeuda.VENCIDA -> {
                    Resultado.Excedido(ReglaId.COMPENSACION, detalle)
                }

                EstadoDeuda.NO_VERIFICADO -> {
                    Resultado.NoVerificado(ReglaId.COMPENSACION, MotivoNoVerificado.DESCANSO_NO_REGISTRADO, detalle)
                }

                EstadoDeuda.PENDIENTE -> {
                    null
                }
            }
        }
    }
}

/**
 * R-DS-5 (art. 8.6 ter): tras dos semanales reducidos seguidos, el siguiente semanal va precedido de la
 * compensación de los dos (vale la que va unida a ese mismo semanal) (T17e).
 *
 * El resultado va en la semana de ese siguiente semanal.
 */
internal object CompensacionAntesDelSemanal : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val contexto = semana.contexto
        val asignados = asignarSemanales(contexto)
        val siguiente = asignados[semana.lunes]?.firstOrNull() ?: return emptyList()
        val previos =
            listOf(1L, 2L).map { asignados[semana.lunes.minusWeeks(it)]?.lastOrNull() }
        if (previos.any { it == null || it.clase != ClaseDescanso.SEMANAL_REDUCIDO }) return emptyList()

        val deudas = calcularDeudas(contexto).filter { d -> previos.any { it!!.descanso.id == d.semanal.id } }
        val pagadasAntes =
            deudas.all { d ->
                val con = d.compensadaCon
                con != null && con.fin!! <= siguiente.descanso.fin!!
            }
        val detalle = Detalle(valor = Intervalo.exacto(deudas.fold(Duration.ZERO) { acc, d -> acc + d.horas }))
        val regla = ReglaId.COMPENSACION_ANTES_DEL_SEMANAL
        return listOf(if (pagadasAntes) Resultado.Cumple(regla, detalle) else Resultado.Excedido(regla, detalle))
    }
}
