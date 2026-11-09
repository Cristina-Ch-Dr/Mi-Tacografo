package com.cristinachdr.mitacografo.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val LightColorScheme =
    lightColorScheme(
        primary = AzulLogo,
        onPrimary = SobreAzul,
        primaryContainer = AzulClaro,
        onPrimaryContainer = SobreAzulClaro,
        secondary = NaranjaTexto,
        onSecondary = SobreAzul,
        secondaryContainer = NaranjaContenedorClaro,
        onSecondaryContainer = SobreNaranjaContenedorClaro,
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
        primaryContainer = AzulLogo,
        onPrimaryContainer = SobreAzulLogoNoche,
        secondary = NaranjaNoche,
        onSecondary = SobreNaranjaNoche,
        secondaryContainer = NaranjaContenedorOscuro,
        onSecondaryContainer = SobreNaranjaContenedorOscuro,
        background = FondoOscuro,
        onBackground = TextoOscuro,
        surface = SuperficieOscura,
        onSurface = TextoOscuro,
        surfaceContainer = SuperficieContenedorOscura,
    )

/** Colores propios de la barra de pestañas, aparte de la paleta de Material para no tocar nada más. */
@Immutable
data class ColoresPestanas(
    val activa: Color,
    val iconoActiva: Color,
    val textoActiva: Color,
)

private val PestanasClaro = ColoresPestanas(PestanaActivaClara, TextoPestanaActivaClara, TextoPestanaActivaClara)
private val PestanasOscuro = ColoresPestanas(PestanaActivaOscura, IconoPestanaActivaOscura, TextoPestanaActivaOscura)

/** Los de la barra de pestañas según el tema que esté puesto (claro u oscuro). */
@Composable
@ReadOnlyComposable
fun coloresPestanas(): ColoresPestanas =
    if (MaterialTheme.colorScheme.background.luminance() < 0.5f) PestanasOscuro else PestanasClaro

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
