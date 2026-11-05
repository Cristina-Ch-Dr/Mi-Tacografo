package com.cristinachdr.mitacografo.domain.usecase

import com.cristinachdr.mitacografo.domain.model.ErrorJornada
import com.cristinachdr.mitacografo.domain.model.ResultadoJornada
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import javax.inject.Inject

/** Caso de uso: borrar una jornada (la pantalla pide confirmación antes). Si estaba abierta, quita sus avisos. */
class BorrarJornada
    @Inject
    constructor(
        private val repository: JornadaRepository,
        private val avisos: GestorAvisos,
    ) {
        suspend operator fun invoke(id: Long): ResultadoJornada<Unit> {
            repository.obtenerPorId(id) ?: return ResultadoJornada.Invalida(ErrorJornada.JORNADA_NO_EXISTE)
            repository.borrar(id)
            avisos.cancelar(id)
            return ResultadoJornada.Ok(Unit)
        }
    }
