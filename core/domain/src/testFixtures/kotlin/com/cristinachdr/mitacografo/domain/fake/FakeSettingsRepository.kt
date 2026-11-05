package com.cristinachdr.mitacografo.domain.fake

import com.cristinachdr.mitacografo.domain.model.Ajustes
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Ajustes en memoria para los tests. */
class FakeSettingsRepository(
    iniciales: Ajustes = Ajustes(),
) : SettingsRepository {
    private val ajustes = MutableStateFlow(iniciales)

    val actuales: Ajustes get() = ajustes.value

    override fun observar(): Flow<Ajustes> = ajustes

    override suspend fun actualizar(cambio: (Ajustes) -> Ajustes) = ajustes.update(cambio)
}
