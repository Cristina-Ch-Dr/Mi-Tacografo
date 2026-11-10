package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.ClaseDescanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

// Reglamento (CE) 561/2006, art. 8.6 y 8.9, con el Reglamento (UE) 2020/1054 (MT-28). Texto revisado:
// - 8.6: en dos semanas seguidas, 2 semanales normales o 1 normal y 1 reducido (≥ 24 h). El semanal empieza
//   como muy tarde al acabar 6 periodos de 24 h desde el anterior. En transporte internacional de mercancías,
//   2 reducidos seguidos si se toman fuera del Estado de la empresa y del país de residencia, siempre que en
//   cualquier periodo de 4 semanas haya al menos 4 semanales, 2 de ellos normales.
// - 8.9: un semanal que cae en dos semanas cuenta en una de ellas, no en las dos.

private val SEIS_PERIODOS: Duration = Duration.ofHours(144)
private val MINIMO_SEMANAL: Duration = Duration.ofHours(24)
private val SEMANALES = setOf(ClaseDescanso.SEMANAL_NORMAL, ClaseDescanso.SEMANAL_REDUCIDO)

private fun ContextoReglas.semanales(): List<DescansoClasificado> =
    clasificados.filter { it.clase in SEMANALES && it.descanso.fin != null }

/** Zona en la que se cortan las semanas: la de referencia o, si no hay, la del primer registro. */
private fun ContextoReglas.zonaSemanas(): ZoneId =
    zonaReferencia ?: jornadas.firstOrNull()?.zonaInicio ?: descansos.firstOrNull()?.zonaInicio ?: ZoneOffset.UTC

private fun ContextoReglas.inicioSemana(lunes: LocalDate): Instant = lunes.atStartOfDay(zonaSemanas()).toInstant()

private fun ContextoReglas.terminada(lunes: LocalDate): Boolean = inicioSemana(lunes.plusWeeks(1)) <= ahora

/** La primera semana con algún dato (jornada o descanso): antes no se sabe nada. */
private fun ContextoReglas.primeraSemanaConDatos(): LocalDate? =
    (
        jornadas.map {
            lunesDe(
                it.inicio,
                zonaSemanas(),
            )
        } + descansos.map { lunesDe(it.inicio, zonaSemanas()) }
    ).minOrNull()

/**
 * D-2 (art. 8.9): cada semanal cuenta en la semana en la que empieza, salvo que esa semana ya tenga uno.
 * En ese caso, en la siguiente. ⚠️ Validar con la guía de la Comisión (espec. §16).
 */
internal fun asignarSemanales(contexto: ContextoReglas): Map<LocalDate, List<DescansoClasificado>> {
    val asignados = linkedMapOf<LocalDate, MutableList<DescansoClasificado>>()
    contexto.semanales().forEach { semanal ->
        var lunes = lunesDe(semanal.descanso.inicio, contexto.zonaReferencia ?: semanal.descanso.zonaInicio)
        if (asignados[lunes]?.isNotEmpty() == true) lunes = lunes.plusWeeks(1)
        asignados.getOrPut(lunes) { mutableListOf() } += semanal
    }
    return asignados
}

/** ¿Pudo haber un semanal sin apuntar en esta semana? Si queda un hueco de 24 h o más sin nada apuntado. */
private fun ContextoReglas.posibleSemanalSinApuntar(lunes: LocalDate): Boolean =
    huecoMasLargo(inicioSemana(lunes), minOf(inicioSemana(lunes.plusWeeks(1)), ahora)) >= MINIMO_SEMANAL

/** Dónde se tomaron dos reducidos seguidos (espec. §7.7): fuera de casa se permiten, en casa no. */
private sealed interface FueraDeCasa {
    data object Si : FueraDeCasa

    data object EnCasa : FueraDeCasa

    data class NoSeSabe(
        val motivo: MotivoNoVerificado,
    ) : FueraDeCasa
}

private fun ContextoReglas.fueraDeCasa(reducidos: List<DescansoClasificado>): FueraDeCasa {
    val empresa = paisEmpresa?.uppercase()
    val residencia = paisResidencia?.uppercase()
    if (empresa == null || residencia == null) return FueraDeCasa.NoSeSabe(MotivoNoVerificado.PAISES_NO_CONFIGURADOS)
    val paises = reducidos.map { it.descanso.pais?.uppercase() }
    if (paises.any { it == null }) return FueraDeCasa.NoSeSabe(MotivoNoVerificado.SIN_PAIS_DESCANSO)
    return if (paises.any { it == empresa || it == residencia }) FueraDeCasa.EnCasa else FueraDeCasa.Si
}

/**
 * R-DS-1 (art. 8.6): esta semana y la anterior, 2 semanales normales o 1 normal y 1 reducido.
 *
 * Dos reducidos seguidos solo valen fuera de casa (R-DS-3). Mientras la semana no ha acabado y aún falta
 * el semanal, no hay resultado.
 */
internal object SemanalDosSemanas : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val contexto = semana.contexto
        val anterior = semana.lunes.minusWeeks(1)
        val asignados = asignarSemanales(contexto)
        val enLasDos = asignados[anterior].orEmpty() + asignados[semana.lunes].orEmpty()
        val normales = enLasDos.count { it.clase == ClaseDescanso.SEMANAL_NORMAL }
        val detalle = Detalle(cuenta = enLasDos.size, maximo = 2)
        val regla = ReglaId.SEMANAL_DOS_SEMANAS

        if (enLasDos.size >= 2 && normales >= 1) return listOf(Resultado.Cumple(regla, detalle))
        if (enLasDos.size >= 2) {
            // Dos reducidos seguidos: depende de dónde se tomaron.
            return listOf(
                when (val fuera = contexto.fueraDeCasa(enLasDos)) {
                    FueraDeCasa.Si -> Resultado.Cumple(regla, detalle)
                    FueraDeCasa.EnCasa -> Resultado.Excedido(regla, detalle)
                    is FueraDeCasa.NoSeSabe -> Resultado.NoVerificado(regla, fuera.motivo, detalle)
                },
            )
        }
        if (!contexto.terminada(semana.lunes)) return emptyList()
        val primera = contexto.primeraSemanaConDatos() ?: return emptyList()
        val resultado =
            when {
                anterior < primera -> {
                    Resultado.NoVerificado(regla, MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO, detalle)
                }

                listOf(anterior, semana.lunes).any {
                    asignados[it].isNullOrEmpty() &&
                        contexto.posibleSemanalSinApuntar(it)
                } -> {
                    Resultado.NoVerificado(regla, MotivoNoVerificado.DESCANSO_NO_REGISTRADO, detalle)
                }

                else -> {
                    Resultado.Excedido(regla, detalle)
                }
            }
        return listOf(resultado)
    }
}

/**
 * R-DS-2 (art. 8.6): cada semanal empieza como muy tarde 6 × 24 h (144 h) después de acabar el anterior (T15).
 *
 * El resultado va en la semana en la que empieza el semanal (o, si aún no ha llegado, en la del plazo).
 * Si entre medias queda un hueco sin nada apuntado de 24 h o más, pudo haber otro semanal: no verificado.
 */
internal object SeisPeriodos : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val contexto = semana.contexto
        val zona = contexto.zonaSemanas()
        val semanales = contexto.semanales()
        val resultados = mutableListOf<Resultado>()
        semanales.forEachIndexed { i, previo ->
            val finPrevio = previo.descanso.fin!!
            val plazo = finPrevio.plus(SEIS_PERIODOS)
            val siguiente = semanales.getOrNull(i + 1)
            val empieza = siguiente?.descanso?.inicio
            val lunesResultado = lunesDe(empieza ?: plazo, zona)
            if (lunesResultado != semana.lunes) return@forEachIndexed
            if (empieza == null && plazo > contexto.ahora) return@forEachIndexed

            val espera = Duration.between(finPrevio, empieza ?: contexto.ahora)
            val detalle = Detalle(valor = Intervalo.exacto(espera), limite = SEIS_PERIODOS)
            resultados +=
                when {
                    empieza != null && empieza <= plazo -> {
                        Resultado.Cumple(ReglaId.SEIS_PERIODOS, detalle)
                    }

                    contexto.huecoMasLargo(finPrevio, empieza ?: contexto.ahora) >= MINIMO_SEMANAL -> {
                        Resultado.NoVerificado(
                            ReglaId.SEIS_PERIODOS,
                            MotivoNoVerificado.DESCANSO_NO_REGISTRADO,
                            detalle,
                        )
                    }

                    else -> {
                        Resultado.Excedido(ReglaId.SEIS_PERIODOS, detalle)
                    }
                }
        }
        return listOfNotNull(peor(resultados))
    }
}

/**
 * R-DS-3 (art. 8.6, transporte internacional): dos reducidos seguidos solo fuera del país de la empresa y del
 * de residencia, y en las 4 semanas desde el primero, al menos 4 semanales y 2 de ellos normales (T16–T17d).
 *
 * Solo hay resultado en la semana del segundo de dos reducidos seguidos.
 */
internal object ReducidosFueraDeCasa : ReglaSemana {
    override fun evaluar(
        semana: SemanaRepartida,
        todas: Map<LocalDate, SemanaRepartida>,
        primeraSemana: LocalDate,
    ): List<Resultado> {
        val contexto = semana.contexto
        val asignados = asignarSemanales(contexto)
        val anterior = semana.lunes.minusWeeks(1)
        val par = asignados[anterior].orEmpty().takeLast(1) + asignados[semana.lunes].orEmpty().take(1)
        if (par.size < 2 || par.any { it.clase != ClaseDescanso.SEMANAL_REDUCIDO }) return emptyList()
        val regla = ReglaId.REDUCIDOS_FUERA_DE_CASA

        when (val fuera = contexto.fueraDeCasa(par)) {
            FueraDeCasa.EnCasa -> return listOf(Resultado.Excedido(regla, Detalle()))
            is FueraDeCasa.NoSeSabe -> return listOf(Resultado.NoVerificado(regla, fuera.motivo))
            FueraDeCasa.Si -> Unit
        }

        // Las 4 semanas desde la del primer reducido.
        val cuatro = (0L..3L).map { anterior.plusWeeks(it) }
        if (!contexto.terminada(cuatro.last())) return emptyList()
        val enLasCuatro = cuatro.flatMap { asignados[it].orEmpty() }
        val normales = enLasCuatro.count { it.clase == ClaseDescanso.SEMANAL_NORMAL }
        val detalle = Detalle(cuenta = normales, maximo = 2)
        val resultado =
            when {
                enLasCuatro.size >= 4 && normales >= 2 -> {
                    Resultado.Cumple(regla, detalle)
                }

                cuatro.any { asignados[it].isNullOrEmpty() && contexto.posibleSemanalSinApuntar(it) } -> {
                    Resultado.NoVerificado(regla, MotivoNoVerificado.DESCANSO_NO_REGISTRADO, detalle)
                }

                else -> {
                    Resultado.Excedido(regla, detalle)
                }
            }
        return listOf(resultado)
    }
}

/** De varios resultados de una misma regla en una semana, el peor: excedido, luego no verificado. */
private fun peor(resultados: List<Resultado>): Resultado? =
    resultados.firstOrNull { it is Resultado.Excedido }
        ?: resultados.firstOrNull { it is Resultado.NoVerificado }
        ?: resultados.firstOrNull()
