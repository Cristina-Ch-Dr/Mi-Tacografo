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
