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
    // Desde MT-58, igual.
    val gastos: List<GastoDto> = emptyList(),
)

@Serializable
internal data class GastoDto(
    val id: Long,
    /** "2026-11-22" */
    val fecha: String,
    val descripcion: String? = null,
    /** "12.50", como texto para no perder decimales. */
    val importe: String,
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
    // Desde MT-53. Opcionales: las copias antiguas se leen sin ellos.
    val conduccionApuntadaEn: String? = null,
    val conduccionAntesMedianocheMinutos: Long? = null,
    // Desde MT-55, opcionales como los anteriores.
    val regionInicio: String? = null,
    val regionFin: String? = null,
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
    // Desde MT-57. El tono y el despertador puesto son de cada móvil: no van en la copia.
    val antelacionDespertadorMinutos: Long? = null,
    // Desde MT-58: lo que cobra. Los importes, como texto ("0.045").
    val salarioDiaInicio: Int? = null,
    val salarioModo: String? = null,
    val salarioNomina: String? = null,
    val salarioPorKm: String? = null,
    val salarioPorDia: String? = null,
    // Desde MT-59: lo que ya llevaba antes de empezar a usar la app.
    val ajusteSemanas: AjusteSemanasDto? = null,
)

@Serializable
internal data class AjusteSemanasDto(
    /** "2026-11-02": el lunes de la semana. */
    val semana: String,
    val conduccionSemanaMinutos: Long = 0,
    val conduccionSemanaAnteriorMinutos: Long = 0,
    val ampliaciones: Int = 0,
    val reducidos: Int = 0,
)

@Serializable
internal data class AvisoDto(
    val minutos: Long,
    val activo: Boolean = true,
)
