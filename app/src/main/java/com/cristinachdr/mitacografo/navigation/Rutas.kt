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

/** Detalle de una jornada del historial, para corregirla o borrarla. */
@Serializable data class RutaJornada(
    val id: Long,
)
