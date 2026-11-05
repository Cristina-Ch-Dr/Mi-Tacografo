package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.AvisoProgramado
import com.cristinachdr.mitacografo.domain.port.AlarmScheduler

/** Sistema de alarmas en memoria: guarda qué avisos hay "programados" para poder comprobarlo. */
class FakeAlarmScheduler : AlarmScheduler {
    private val avisos = mutableListOf<AvisoProgramado>()

    val programados: List<AvisoProgramado> get() = avisos.sortedBy { it.disparo }

    override suspend fun programar(avisos: List<AvisoProgramado>) {
        this.avisos += avisos
    }

    override suspend fun cancelar(jornadaId: Long) {
        avisos.removeAll { it.jornadaId == jornadaId }
    }
}
