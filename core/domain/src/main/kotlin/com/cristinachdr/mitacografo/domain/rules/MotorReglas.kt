package com.cristinachdr.mitacografo.domain.rules

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.DescansoClasificado
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.model.clasificarDescansos
import java.time.Duration
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
    /** País de la empresa y de residencia (Ajustes), para los semanales reducidos fuera de casa (R-DS-3). */
    val paisEmpresa: String? = null,
    val paisResidencia: String? = null,
    /**
     * Desde cuándo está todo apuntado. Lo de antes no se sabe: si la primera semana empieza a mitad
     * (el conductor empezó a usar la app un jueves), lo conducido antes cuenta como desconocido.
     * `null` = desde el primer registro (jornada o descanso).
     */
    val datosDesde: Instant? = null,
    /**
     * `true` si el tiempo entre jornadas cuenta como descanso (MT-46): [descansos] ya trae los calculados
     * y, sin jornada en curso, desde el fin de la última también se está descansando.
     */
    val descansoEntreJornadas: Boolean = false,
) {
    /** Los descansos con su clase (diario, fraccionado, semanal…), en orden. Se calculan una vez. */
    val clasificados: List<DescansoClasificado> by lazy { clasificarDescansos(descansos) }
}

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

/** El informe completo: las semanas con jornadas, de la más reciente a la más antigua, y las deudas. */
data class InformeReglas(
    val semanas: List<InformeSemana>,
    /** Las deudas de los semanales reducidos, de la más antigua a la más nueva. */
    val deudas: List<Deuda> = emptyList(),
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
    /** Todo el contexto, para las reglas que miran los descansos. */
    val contexto: ContextoReglas,
    /** Lo que pudo conducir antes de que empezaran los datos (solo en la primera semana). */
    val desconocido: Intervalo = Intervalo.CERO,
) {
    val conduccion: Intervalo get() = partes.fold(desconocido) { acc, p -> acc + p.conduccion }

    /**
     * El motivo de la incertidumbre, si la hay: el que suma más horas sin saber (lo que falta apuntar,
     * lo que cruza la semana o los días antes de empezar los datos).
     */
    val motivo: MotivoNoVerificado?
        get() {
            val horas = mutableMapOf<MotivoNoVerificado, Duration>()
            partes.forEach { p ->
                p.motivo?.let { horas.merge(it, p.conduccion.max - p.conduccion.min, Duration::plus) }
            }
            if (!desconocido.exacto) {
                horas.merge(
                    MotivoNoVerificado.DATOS_INSUFICIENTES_PERIODO,
                    desconocido.max - desconocido.min,
                    Duration::plus,
                )
            }
            return horas.maxByOrNull { it.value }?.key
        }
}

/**
 * El motor de reglas (espec. §8): reparte la conducción por semanas y pasa cada regla.
 *
 * Conducción (art. 6), pausas (art. 7), descanso diario (art. 8.2 y 8.4), semanal (art. 8.6 y 8.9)
 * y compensaciones (art. 8.6 ter y 8.7).
 */
class MotorReglas {
    private val reglasJornada: List<ReglaJornada> = listOf(ConduccionDiaria, Pausa, DescansoDiario)
    private val reglasSemana: List<ReglaSemana> =
        listOf(
            Ampliaciones,
            ConduccionSemanal,
            ConduccionBisemanal,
            ReducidosEntreSemanales,
            SemanalDosSemanas,
            SeisPeriodos,
            ReducidosFueraDeCasa,
            Compensacion,
            CompensacionAntesDelSemanal,
        )

    fun evaluar(contexto: ContextoReglas): InformeReglas {
        val jornadas = contexto.jornadas.sortedBy { it.inicio }
        if (jornadas.isEmpty()) return InformeReglas(emptyList())

        val partes = repartirPorSemanas(jornadas, contexto.zonaReferencia, contexto.ahora)
        val semanaDeInicio = jornadas.groupBy { lunesDe(it.inicio, contexto.zonaReferencia ?: it.zonaInicio) }
        val lunesConDatos = (partes.keys + semanaDeInicio.keys).sorted()

        // La primera semana con datos y lo que pudo conducir en ella antes del primer registro.
        val zona = contexto.zonaReferencia ?: jornadas.first().zonaInicio
        val inicioDatos =
            contexto.datosDesde
                ?: listOfNotNull(jornadas.first().inicio, contexto.descansos.minOfOrNull { it.inicio }).min()
        val primera = minOf(lunesDe(inicioDatos, zona), lunesConDatos.first())
        val antes = Duration.between(primera.atStartOfDay(zona).toInstant(), inicioDatos).coerceAtLeast(Duration.ZERO)

        val semanas =
            (lunesConDatos + primera).distinct().associateWith { lunes ->
                SemanaRepartida(
                    lunes = lunes,
                    partes = partes[lunes].orEmpty(),
                    jornadas = semanaDeInicio[lunes].orEmpty().filterNot { it.abierta },
                    ahora = contexto.ahora,
                    contexto = contexto,
                    desconocido = if (lunes == primera) Intervalo(Duration.ZERO, antes) else Intervalo.CERO,
                )
            }

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
        return InformeReglas(informes.sortedByDescending { it.lunes }, calcularDeudas(contexto))
    }
}
