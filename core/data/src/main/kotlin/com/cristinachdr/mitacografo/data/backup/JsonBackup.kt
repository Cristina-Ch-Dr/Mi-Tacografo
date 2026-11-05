package com.cristinachdr.mitacografo.data.backup

import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.model.Continente
import com.cristinachdr.mitacografo.domain.model.CopiaInvalidaException
import com.cristinachdr.mitacografo.domain.model.CopiaSeguridad
import com.cristinachdr.mitacografo.domain.model.ErrorCopia
import com.cristinachdr.mitacografo.domain.model.Jornada
import com.cristinachdr.mitacografo.domain.port.BackupPort
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

/**
 * Adaptador de salida: la copia de seguridad como JSON legible (con sangría).
 *
 * El archivo lleva `app` y `version` para reconocerlo y poder cambiar el formato en el futuro.
 * Las fechas van en ISO-8601 ("2026-11-02T05:00:00Z") y las zonas por su nombre ("Europe/Madrid"):
 * así el archivo se entiende abriéndolo con cualquier editor.
 */
internal class JsonBackup
    @Inject
    constructor() : BackupPort {
        private val json =
            Json {
                prettyPrint = true
                // Una copia de una versión más nueva puede traer campos que esta no conoce: se ignoran.
                ignoreUnknownKeys = true
                encodeDefaults = true
            }

        override fun codificar(copia: CopiaSeguridad): String = json.encodeToString(CopiaDto.serializer(), copia.aDto())

        override fun decodificar(texto: String): CopiaSeguridad {
            val dto =
                try {
                    json.decodeFromString(CopiaDto.serializer(), texto)
                } catch (e: SerializationException) {
                    throw CopiaInvalidaException(ErrorCopia.NO_ES_UNA_COPIA, e)
                } catch (e: IllegalArgumentException) {
                    throw CopiaInvalidaException(ErrorCopia.NO_ES_UNA_COPIA, e)
                }
            if (dto.app != APP) throw CopiaInvalidaException(ErrorCopia.NO_ES_UNA_COPIA)
            if (dto.version > CopiaSeguridad.VERSION_FORMATO) throw CopiaInvalidaException(ErrorCopia.VERSION_MAS_NUEVA)
            return try {
                dto.aDominio()
            } catch (e: RuntimeException) {
                // Fechas o zonas que no se entienden, km que no son números…
                throw CopiaInvalidaException(ErrorCopia.DATOS_INCORRECTOS, e)
            }
        }
    }

/** Marca que identifica nuestros archivos (para no restaurar el JSON de otra app). */
internal const val APP = "mi-tacografo"

private fun CopiaSeguridad.aDto() =
    CopiaDto(
        app = APP,
        version = CopiaSeguridad.VERSION_FORMATO,
        creada = creada.toString(),
        jornadas = jornadas.sortedBy { it.inicio }.map { it.aDto() },
        ajustes = ajustes.aDto(),
    )

private fun CopiaDto.aDominio() =
    CopiaSeguridad(
        creada = Instant.parse(creada),
        jornadas = jornadas.map { it.aDominio() },
        ajustes = ajustes.aDominio(),
    )

private fun Jornada.aDto() =
    JornadaDto(
        id = id,
        inicio = inicio.toString(),
        zonaInicio = zonaInicio.id,
        kmInicio = kmInicio,
        paisInicio = paisInicio,
        ciudadInicio = ciudadInicio,
        fin = fin?.toString(),
        zonaFin = zonaFin?.id,
        kmFin = kmFin,
        paisFin = paisFin,
        ciudadFin = ciudadFin,
        conduccionMinutos = conduccionTacografo?.toMinutes(),
        viajeId = viajeId,
        observaciones = observaciones,
    )

private fun JornadaDto.aDominio() =
    Jornada(
        id = id,
        inicio = Instant.parse(inicio),
        zonaInicio = ZoneId.of(zonaInicio),
        kmInicio = kmInicio,
        paisInicio = paisInicio,
        ciudadInicio = ciudadInicio,
        fin = fin?.let(Instant::parse),
        zonaFin = zonaFin?.let(ZoneId::of),
        kmFin = kmFin,
        paisFin = paisFin,
        ciudadFin = ciudadFin,
        conduccionTacografo = conduccionMinutos?.let(Duration::ofMinutes),
        viajeId = viajeId,
        observaciones = observaciones,
    )

private fun Ajustes.aDto() =
    AjustesDto(
        avisos = avisos.map { AvisoDto(it.desdeInicio.toMinutes(), it.activo) },
        paisEmpresa = paisEmpresa,
        paisResidencia = paisResidencia,
        nombreConductor = nombreConductor,
        zonaReferencia = zonaReferencia?.id,
        paisesHabituales = paisesHabituales.sorted(),
        continentes = continentes.map { it.name }.sorted(),
    )

private fun AjustesDto.aDominio() =
    Ajustes(
        avisos = avisos.map { AvisoConfig(Duration.ofMinutes(it.minutos), it.activo) },
        paisEmpresa = paisEmpresa,
        paisResidencia = paisResidencia,
        nombreConductor = nombreConductor,
        zonaReferencia = zonaReferencia?.let(ZoneId::of),
        paisesHabituales = paisesHabituales.toSet(),
        // Un continente que esta versión no conoce se ignora.
        continentes = continentes.mapNotNull { nombre -> Continente.entries.find { it.name == nombre } }.toSet(),
    )
