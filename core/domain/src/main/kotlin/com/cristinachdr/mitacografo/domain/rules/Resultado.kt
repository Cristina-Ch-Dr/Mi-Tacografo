package com.cristinachdr.mitacografo.domain.rules

import java.time.Duration

/**
 * Cada regla del Reglamento (CE) 561/2006 que comprueba el motor (espec. §7).
 *
 * El dominio devuelve códigos, no textos: la app traduce cada uno con `strings.xml`.
 */
enum class ReglaId(
    /** Identificador de la especificación, para buscar la regla en §7. */
    val codigo: String,
) {
    /** Art. 6.1: conducción diaria ≤ 9 h (o ≤ 10 h si es ampliada). */
    CONDUCCION_DIARIA("R-COND-1"),

    /** Art. 6.1: como mucho 2 jornadas ampliadas (más de 9 h) por semana. */
    AMPLIACIONES_SEMANA("R-COND-2"),

    /** Art. 6.2: conducción semanal ≤ 56 h. */
    CONDUCCION_SEMANAL("R-COND-3"),

    /** Art. 6.3: conducción de dos semanas seguidas ≤ 90 h. */
    CONDUCCION_BISEMANAL("R-COND-4"),

    /** Art. 7: pausa de 45 min tras 4 h 30 de conducción. */
    PAUSA("R-PAUSA-1"),

    /** Art. 8.2: un descanso diario (≥ 9 h) dentro de las 24 h siguientes al anterior. */
    DESCANSO_DIARIO("R-DD-1"),

    /** Art. 8.4: como mucho 3 descansos diarios reducidos entre dos semanales. */
    REDUCIDOS_ENTRE_SEMANALES("R-DD-3"),

    /** Art. 8.6: en dos semanas seguidas, 2 semanales normales o 1 normal y 1 reducido. */
    SEMANAL_DOS_SEMANAS("R-DS-1"),

    /** Art. 8.6: el semanal empieza como muy tarde 6 × 24 h después del anterior. */
    SEIS_PERIODOS("R-DS-2"),

    /** Art. 8.6 (internacional): 2 reducidos seguidos solo fuera de casa y con 2 normales en 4 semanas. */
    REDUCIDOS_FUERA_DE_CASA("R-DS-3"),
}

/** Por qué una regla no se puede comprobar con lo que hay apuntado (espec. §8.1). */
enum class MotivoNoVerificado {
    /** Alguna jornada no tiene apuntada la conducción del tacógrafo. */
    SIN_CONDUCCION_TACOGRAFO,

    /** Pausas: la app solo sabe el total de conducción, no cuándo se condujo. */
    SIN_TRAMOS_CONDUCCION,

    /** Hay un hueco entre jornadas sin descanso apuntado (reglas de descanso, MT-27). */
    DESCANSO_NO_REGISTRADO,

    /** Una jornada cruza el domingo a las 24:00 y no se sabe cuánto se condujo a cada lado. */
    JORNADA_CRUZA_SEMANA,

    /** Faltan semanas anteriores (antes de empezar a usar la app). */
    DATOS_INSUFICIENTES_PERIODO,

    /** Un descanso reducido sin país apuntado (R-DS-3, MT-28). */
    SIN_PAIS_DESCANSO,

    /** Faltan el país de la empresa o el de residencia en Ajustes (R-DS-3, MT-28). */
    PAISES_NO_CONFIGURADOS,
}

/**
 * Lo que vale algo de lo que no siempre se sabe el valor exacto: entre [min] y [max] (espec. §8.2).
 *
 * Si con el peor caso se cumple, se cumple; si con el mejor ya se pasa, se pasa; si no, no se sabe.
 * Así el motor nunca se inventa un dato.
 */
data class Intervalo(
    val min: Duration,
    val max: Duration,
) {
    init {
        require(!min.isNegative && min <= max) { "Intervalo incorrecto: [$min, $max]" }
    }

    val exacto: Boolean get() = min == max

    operator fun plus(otro: Intervalo) = Intervalo(min + otro.min, max + otro.max)

    companion object {
        val CERO = exacto(Duration.ZERO)

        fun exacto(valor: Duration) = Intervalo(valor, valor)
    }
}

/** Los datos con los que se ha decidido el resultado, para enseñarlos en la pantalla. */
data class Detalle(
    /** El valor medido (conducción del día, de la semana…). */
    val valor: Intervalo? = null,
    val limite: Duration? = null,
    /** Para las reglas que cuentan algo (jornadas ampliadas…): cuántas hay seguro. */
    val cuenta: Int? = null,
    val maximo: Int? = null,
    /** Solo en la conducción diaria: si la jornada pasa de 9 h (cuenta como ampliada). */
    val ampliada: Boolean? = null,
)

/** El resultado de una regla: cumple, se excede o no se puede saber (y por qué). */
sealed interface Resultado {
    val regla: ReglaId
    val detalle: Detalle

    data class Cumple(
        override val regla: ReglaId,
        override val detalle: Detalle,
    ) : Resultado

    data class Excedido(
        override val regla: ReglaId,
        override val detalle: Detalle,
    ) : Resultado

    data class NoVerificado(
        override val regla: ReglaId,
        val motivo: MotivoNoVerificado,
        override val detalle: Detalle = Detalle(),
    ) : Resultado
}

/**
 * Compara un [valor] con un [limite] con la lógica de intervalos.
 *
 * @param motivo por qué no se sabe, si el intervalo no basta para decidir.
 */
internal fun comparar(
    regla: ReglaId,
    valor: Intervalo,
    limite: Duration,
    motivo: MotivoNoVerificado,
    detalle: Detalle = Detalle(valor = valor, limite = limite),
): Resultado =
    when {
        valor.max <= limite -> Resultado.Cumple(regla, detalle)
        valor.min > limite -> Resultado.Excedido(regla, detalle)
        else -> Resultado.NoVerificado(regla, motivo, detalle)
    }
