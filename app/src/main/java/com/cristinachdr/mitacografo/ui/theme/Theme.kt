package com.cristinachdr.mitacografo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme =
    lightColorScheme(
        primary = AzulCarretera,
        onPrimary = SobreAzul,
        primaryContainer = AzulContenedorClaro,
        onPrimaryContainer = SobreAzulContenedorClaro,
        secondary = Ambar,
        onSecondary = SobreAzul,
        background = FondoClaro,
        onBackground = TextoClaro,
        surface = SuperficieClara,
        onSurface = TextoClaro,
        surfaceContainer = SuperficieContenedorClara,
    )

private val DarkColorScheme =
    darkColorScheme(
        primary = AzulNoche,
        onPrimary = SobreAzulNoche,
        primaryContainer = AzulContenedorOscuro,
        onPrimaryContainer = SobreAzulContenedorOscuro,
        secondary = AmbarNoche,
        onSecondary = FondoOscuro,
        background = FondoOscuro,
        onBackground = TextoOscuro,
        surface = SuperficieOscura,
        onSurface = TextoOscuro,
        surfaceContainer = SuperficieContenedorOscura,
    )

/**
 * Tema de la app. No usa "dynamic color" (colores del fondo de pantalla)
 * para que el contraste sea siempre el mismo, en cualquier móvil.
 */
@Composable
fun MiTacografoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
