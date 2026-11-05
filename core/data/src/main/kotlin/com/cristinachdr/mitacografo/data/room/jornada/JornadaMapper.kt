package com.cristinachdr.mitacografo.data.room.jornada

import com.cristinachdr.mitacografo.domain.model.Jornada
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

// Conversión entre la fila de Room y el modelo de dominio.
// - Instant  ⇄ milisegundos desde 1970 (UTC)
// - ZoneId   ⇄ su nombre, por ejemplo "Europe/Madrid"
// - Duration ⇄ minutos (el tacógrafo marca horas y minutos)

internal fun JornadaEntity.aDominio(): Jornada =
    Jornada(
        id = id,
        inicio = Instant.ofEpochMilli(inicioEpochMs),
        zonaInicio = ZoneId.of(zonaInicio),
        kmInicio = kmInicio,
        paisInicio = paisInicio,
        ciudadInicio = ciudadInicio,
        fin = finEpochMs?.let(Instant::ofEpochMilli),
        zonaFin = zonaFin?.let(ZoneId::of),
        kmFin = kmFin,
        paisFin = paisFin,
        ciudadFin = ciudadFin,
        conduccionTacografo = conduccionMin?.let { Duration.ofMinutes(it.toLong()) },
        viajeId = viajeId,
        observaciones = observaciones,
    )

internal fun Jornada.aEntity(): JornadaEntity =
    JornadaEntity(
        id = id,
        inicioEpochMs = inicio.toEpochMilli(),
        zonaInicio = zonaInicio.id,
        kmInicio = kmInicio,
        paisInicio = paisInicio,
        ciudadInicio = ciudadInicio,
        finEpochMs = fin?.toEpochMilli(),
        zonaFin = zonaFin?.id,
        kmFin = kmFin,
        paisFin = paisFin,
        ciudadFin = ciudadFin,
        conduccionMin = conduccionTacografo?.toMinutes()?.toInt(),
        viajeId = viajeId,
        observaciones = observaciones,
    )
