package com.cristinachdr.mitacografo.domain.port

import com.cristinachdr.mitacografo.domain.model.Ajustes
import kotlinx.coroutines.flow.Flow

/** Puerto de salida para las preferencias. La implementación con DataStore vive en `core:data`. */
interface SettingsRepository {
    fun observar(): Flow<Ajustes>

    /** Cambia los ajustes de forma atómica a partir de los actuales. */
    suspend fun actualizar(cambio: (Ajustes) -> Ajustes)
}
