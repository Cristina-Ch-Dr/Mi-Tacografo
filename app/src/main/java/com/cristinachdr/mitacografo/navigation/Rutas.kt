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

/** Resumen y PDF: desde MT-51 se abre desde el Historial, ya no es una pestaña. */
@Serializable data object RutaResumen

/** Salario del periodo, con sus viajes y gastos (MT-58). Se abre desde Viajes. */
@Serializable data object RutaSalario

@Serializable data object RutaAjustes

/** Lista de pausas y descansos: la pestaña Descanso (MT-51). */
@Serializable data object RutaDescansos

/** Un descanso para corregirlo o borrarlo; con [id] = 0, uno nuevo. */
@Serializable data class RutaDescanso(
    val id: Long = 0,
)

/** Informe de reglas de una semana. [lunes] en ISO («2026-11-09»): las rutas solo llevan tipos simples. */
@Serializable data class RutaInforme(
    val lunes: String,
)

/** Detalle de un viaje: sus jornadas y km, y corregirlo o borrarlo; con [id] = 0, añadir uno pasado (MT-52). */
@Serializable data class RutaViaje(
    val id: Long = 0,
)

/** Detalle de una jornada del historial, para corregirla o borrarla; con [id] = 0, añadir una pasada (MT-47). */
@Serializable data class RutaJornada(
    val id: Long = 0,
)
