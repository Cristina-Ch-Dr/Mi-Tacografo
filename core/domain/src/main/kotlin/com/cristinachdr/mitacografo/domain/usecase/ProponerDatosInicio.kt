package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import javax.inject.Inject

/** Valores con los que se rellena el formulario de inicio: donde terminó la jornada anterior. */
data class PropuestaInicio(
    val kmInicio: Int? = null,
    val pais: String? = null,
    val ciudad: String? = null,
)

/**
 * Caso de uso: proponer el km de inicio (el último km final), el país y la ciudad.
 *
 * Así, en el caso normal, iniciar la jornada son 2 toques.
 */
class ProponerDatosInicio
    @Inject
    constructor(
        private val repository: JornadaRepository,
    ) {
        suspend operator fun invoke(): PropuestaInicio {
            val anterior = repository.obtenerUltimaFinalizada() ?: return PropuestaInicio()
            return PropuestaInicio(
                kmInicio = anterior.kmFin,
                pais = anterior.paisFin,
                ciudad = anterior.ciudadFin,
            )
        }
    }
