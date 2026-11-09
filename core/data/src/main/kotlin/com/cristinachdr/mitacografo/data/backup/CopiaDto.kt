package com.cristinachdr.mitacografo.data.backup

import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import kotlinx.serialization.Serializable

// Forma del archivo JSON. Son clases aparte del dominio a propósito: si el dominio cambia,
// el archivo no tiene por qué cambiar (y las copias antiguas siguen valiendo).
// ¡Cuidado al renombrar un campo! Cambia el archivo: hay que subir `CopiaSeguridad.VERSION_FORMATO`.

@Serializable
internal data class CopiaDto(
    val app: String,
    val version: Int,
    val creada: String,
    val jornadas: List<JornadaDto> = emptyList(),
    val ajustes: AjustesDto = AjustesDto(),
    // Desde MT-20. Las copias anteriores no lo traen y se leen como lista vacía; las versiones
    // anteriores de la app lo ignoran (ignoreUnknownKeys). Por eso no hace falta subir la versión.
    val descansos: List<DescansoDto> = emptyList(),
    // Desde MT-18, igual que `descansos`: las copias antiguas se leen con la lista vacía.
    val viajes: List<ViajeDto> = emptyList(),
)

@Serializable
internal data class JornadaDto(
    val id: Long,
    val inicio: String,
    val zonaInicio: String,
    val kmInicio: Int,
    val paisInicio: String? = null,
    val ciudadInicio: String? = null,
    val fin: String? = null,
    val zonaFin: String? = null,
    val kmFin: Int? = null,
    val paisFin: String? = null,
    val ciudadFin: String? = null,
    val conduccionMinutos: Long? = null,
    val viajeId: Long? = null,
    val observaciones: String? = null,
)

@Serializable
internal data class DescansoDto(
    val id: Long,
    val inicio: String,
    val zonaInicio: String,
    val fin: String? = null,
    val zonaFin: String? = null,
    /** "PAUSA" o "DESCANSO". */
    val tipo: String,
    val pais: String? = null,
    val observaciones: String? = null,
)

@Serializable
internal data class ViajeDto(
    val id: Long,
    val nombre: String,
    val inicio: String,
    val zonaInicio: String,
    val fin: String? = null,
    val zonaFin: String? = null,
    val observaciones: String? = null,
)

@Serializable
internal data class AjustesDto(
    val avisos: List<AvisoDto> = AvisoConfig.POR_DEFECTO.map { AvisoDto(it.desdeInicio.toMinutes(), it.activo) },
    val paisEmpresa: String? = null,
    val paisResidencia: String? = null,
    val nombreConductor: String? = null,
    val zonaReferencia: String? = null,
    val paisesHabituales: List<String> = emptyList(),
    val continentes: List<String> = emptyList(),
)

@Serializable
internal data class AvisoDto(
    val minutos: Long,
    val activo: Boolean = true,
)
