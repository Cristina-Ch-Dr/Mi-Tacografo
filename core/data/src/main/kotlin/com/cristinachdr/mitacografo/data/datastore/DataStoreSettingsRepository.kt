package com.cristinachdr.mitacografo.data.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.model.AvisoConfig
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.ZoneId
import javax.inject.Inject

/**
 * Adaptador de salida: guarda los [Ajustes] con DataStore (un archivo clave-valor).
 *
 * Si falta un valor o está estropeado, se usa el de por defecto: los ajustes nunca
 * deben impedir que la app arranque.
 */
internal class DataStoreSettingsRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) : SettingsRepository {
        override fun observar(): Flow<Ajustes> = dataStore.data.map { it.aAjustes() }

        override suspend fun actualizar(cambio: (Ajustes) -> Ajustes) {
            // `edit` es atómico: lee, aplica el cambio y escribe sin que se cuele otra escritura.
            dataStore.edit { preferencias ->
                val nuevos = cambio(preferencias.aAjustes())
                preferencias[Claves.AVISOS] = codificarAvisos(nuevos.avisos)
                preferencias.guardar(Claves.PAIS_EMPRESA, nuevos.paisEmpresa)
                preferencias.guardar(Claves.PAIS_RESIDENCIA, nuevos.paisResidencia)
                preferencias.guardar(Claves.NOMBRE_CONDUCTOR, nuevos.nombreConductor)
                preferencias.guardar(Claves.ZONA_REFERENCIA, nuevos.zonaReferencia?.id)
            }
        }

        private object Claves {
            val AVISOS = stringPreferencesKey("avisos")
            val PAIS_EMPRESA = stringPreferencesKey("pais_empresa")
            val PAIS_RESIDENCIA = stringPreferencesKey("pais_residencia")
            val NOMBRE_CONDUCTOR = stringPreferencesKey("nombre_conductor")
            val ZONA_REFERENCIA = stringPreferencesKey("zona_referencia")
        }

        private fun Preferences.aAjustes(): Ajustes =
            Ajustes(
                avisos = this[Claves.AVISOS]?.let(::decodificarAvisos) ?: AvisoConfig.POR_DEFECTO,
                paisEmpresa = this[Claves.PAIS_EMPRESA],
                paisResidencia = this[Claves.PAIS_RESIDENCIA],
                nombreConductor = this[Claves.NOMBRE_CONDUCTOR],
                zonaReferencia = this[Claves.ZONA_REFERENCIA]?.let { runCatching { ZoneId.of(it) }.getOrNull() },
            )

        private fun androidx.datastore.preferences.core.MutablePreferences.guardar(
            clave: Preferences.Key<String>,
            valor: String?,
        ) {
            if (valor.isNullOrBlank()) remove(clave) else set(clave, valor.trim())
        }
    }

/** Avisos como texto: "750:1,780:1,870:0,900:1" (minutos desde el inicio : activo). */
internal fun codificarAvisos(avisos: List<AvisoConfig>): String =
    avisos.joinToString(",") { "${it.desdeInicio.toMinutes()}:${if (it.activo) 1 else 0}" }

/** Lo contrario de [codificarAvisos]. Si el texto está estropeado devuelve `null` (→ por defecto). */
internal fun decodificarAvisos(texto: String): List<AvisoConfig>? =
    runCatching {
        texto.split(",").filter { it.isNotBlank() }.map { parte ->
            val (minutos, activo) = parte.split(":")
            AvisoConfig(Duration.ofMinutes(minutos.trim().toLong()), activo.trim() == "1")
        }
    }.getOrNull()
