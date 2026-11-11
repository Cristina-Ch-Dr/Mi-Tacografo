package com.cristinachdr.mitacografo.navigation

import kotlinx.serialization.Serializable

/**
 * Rutas de navegación "type-safe": cada pantalla es un tipo, no un String.
 * Si una pantalla necesita parámetros (p. ej. el id de una jornada), se usa
 * una `data class` en lugar de un `object`.
 */
@Serializable data object RutaInicio

@Serializable data object RutaHistorial

@Serializable data object RutaViajes

@Serializable data object RutaResumen

@Serializable data object RutaAjustes

@Serializable data object RutaIniciarJornada

@Serializable data object RutaFinalizarJornada

/** Lista de pausas y descansos. */
@Serializable data object RutaDescansos

/** Un descanso para corregirlo o borrarlo; con [id] = 0, uno nuevo. */
@Serializable data class RutaDescanso(
    val id: Long = 0,
)

/** Informe de reglas de una semana. [lunes] en ISO («2026-11-09»): las rutas solo llevan tipos simples. */
@Serializable data class RutaInforme(
    val lunes: String,
)

/** Detalle de un viaje: sus jornadas y km, y corregirlo o borrarlo. */
@Serializable data class RutaViaje(
    val id: Long,
)

/** Detalle de una jornada del historial, para corregirla o borrarla. */
@Serializable data class RutaJornada(
    val id: Long,
)
