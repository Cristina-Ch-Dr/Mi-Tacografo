package com.cristinachdr.mitacografo.domain.model

import java.time.Duration
import java.time.Instant

/** Art. 8.6: el semanal empieza como muy tarde 6 periodos de 24 h después de acabar el anterior. */
val SEIS_PERIODOS_DE_24_H: Duration = Duration.ofHours(144)

/**
 * Hasta cuándo puede empezar el siguiente descanso semanal (MT-51): el fin del último semanal terminado
 * (normal o reducido) más 144 h. Es lo que el conductor ve en Inicio como «empezar como muy tarde».
 *
 * @param clasificados los descansos, apuntados y calculados entre jornadas, ya clasificados.
 * @return `null` si aún no hay ningún semanal terminado.
 */
fun plazoSiguienteSemanal(clasificados: List<DescansoClasificado>): Instant? =
    clasificados
        .filter { it.clase == ClaseDescanso.SEMANAL_NORMAL || it.clase == ClaseDescanso.SEMANAL_REDUCIDO }
        .mapNotNull { it.descanso.fin }
        .maxOrNull()
        ?.plus(SEIS_PERIODOS_DE_24_H)
