package com.cristinachdr.mitacografo.feature.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Estado de la pantalla de Inicio. Crecerá en MT-11 con la jornada en curso. */
data class HomeUiState(
    val hoy: LocalDate,
)

/**
 * `@HiltViewModel` + `@Inject constructor`: Hilt crea el ViewModel y le inyecta
 * el [Clock] (igual que un `@Service` con inyección por constructor en Spring).
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        clock: Clock,
    ) : ViewModel() {
        val uiState: HomeUiState = HomeUiState(hoy = LocalDate.now(clock))
    }
