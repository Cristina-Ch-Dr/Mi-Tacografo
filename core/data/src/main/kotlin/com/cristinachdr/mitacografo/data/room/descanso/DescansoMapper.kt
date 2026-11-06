package com.cristinachdr.mitacografo.data.room.descanso

import com.cristinachdr.mitacografo.domain.model.Descanso
import com.cristinachdr.mitacografo.domain.model.TipoDescanso
import java.time.Instant
import java.time.ZoneId

// Conversión entre la fila de Room y el modelo de dominio (igual que en JornadaMapper).
// El tipo se guarda por su nombre: "PAUSA" o "DESCANSO".

internal fun DescansoEntity.aDominio(): Descanso =
    Descanso(
        id = id,
        inicio = Instant.ofEpochMilli(inicioEpochMs),
        zonaInicio = ZoneId.of(zonaInicio),
        fin = finEpochMs?.let(Instant::ofEpochMilli),
        zonaFin = zonaFin?.let(ZoneId::of),
        tipo = TipoDescanso.valueOf(tipo),
        pais = pais,
        observaciones = observaciones,
    )

internal fun Descanso.aEntity(): DescansoEntity =
    DescansoEntity(
        id = id,
        inicioEpochMs = inicio.toEpochMilli(),
        zonaInicio = zonaInicio.id,
        finEpochMs = fin?.toEpochMilli(),
        zonaFin = zonaFin?.id,
        tipo = tipo.name,
        pais = pais,
        observaciones = observaciones,
    )
