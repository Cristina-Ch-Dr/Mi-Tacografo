package com.cristinachdr.mitacografo.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.feature.editday.EditarJornadaScreen
import com.cristinachdr.mitacografo.feature.endday.FinalizarJornadaScreen
import com.cristinachdr.mitacografo.feature.history.HistoryScreen
import com.cristinachdr.mitacografo.feature.home.HomeScreen
import com.cristinachdr.mitacografo.feature.settings.SettingsScreen
import com.cristinachdr.mitacografo.feature.startday.IniciarJornadaScreen
import com.cristinachdr.mitacografo.feature.stats.StatsScreen
import com.cristinachdr.mitacografo.feature.trips.TripsScreen
import com.cristinachdr.mitacografo.ui.theme.AzulLogo
import com.cristinachdr.mitacografo.ui.theme.SobreAzul
import com.cristinachdr.mitacografo.ui.theme.coloresPestanas
import kotlin.reflect.KClass

/** Pestañas de la barra inferior. */
private enum class Pestana(
    val ruta: Any,
    val clase: KClass<*>,
    @StringRes val titulo: Int,
    val icono: ImageVector,
) {
    INICIO(RutaInicio, RutaInicio::class, R.string.nav_inicio, Icons.Filled.Home),
    HISTORIAL(RutaHistorial, RutaHistorial::class, R.string.nav_historial, Icons.AutoMirrored.Filled.List),
    VIAJES(RutaViajes, RutaViajes::class, R.string.nav_viajes, Icons.Filled.Place),
    RESUMEN(RutaResumen, RutaResumen::class, R.string.nav_resumen, Icons.Filled.DateRange),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiTacografoNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val destinoActual = entradaActual?.destination

    val pestanaActual =
        Pestana.entries.firstOrNull { pestana ->
            destinoActual?.hierarchy?.any { it.hasRoute(pestana.clase) } == true
        }
    // Las pantallas que no son pestañas (Ajustes, formularios…) tienen flecha atrás y no muestran la barra inferior.
    val pantallaSecundaria = destinoActual != null && pestanaActual == null
    val tituloSecundaria =
        when {
            destinoActual?.hasRoute(RutaIniciarJornada::class) == true -> R.string.accion_iniciar_jornada
            destinoActual?.hasRoute(RutaFinalizarJornada::class) == true -> R.string.accion_finalizar_jornada
            destinoActual?.hasRoute(RutaJornada::class) == true -> R.string.editar_titulo
            else -> R.string.nav_ajustes
        }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            pestanaActual?.titulo ?: if (pantallaSecundaria) tituloSecundaria else R.string.app_name,
                        ),
                    )
                },
                navigationIcon = {
                    if (pantallaSecundaria) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.accion_volver),
                            )
                        }
                    } else {
                        LogoApp(Modifier.padding(start = 12.dp, end = 4.dp))
                    }
                },
                actions = {
                    if (!pantallaSecundaria) {
                        IconButton(onClick = { navController.navigate(RutaAjustes) { launchSingleTop = true } }) {
                            Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.nav_ajustes))
                        }
                    }
                },
                // Azul del fondo del icono en los dos temas: el logo de la izquierda se funde con la barra.
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = AzulLogo,
                        titleContentColor = SobreAzul,
                        navigationIconContentColor = SobreAzul,
                        actionIconContentColor = SobreAzul,
                    ),
            )
        },
        bottomBar = {
            if (!pantallaSecundaria) {
                NavigationBar {
                    Pestana.entries.forEach { pestana ->
                        NavigationBarItem(
                            selected = pestana == pestanaActual,
                            onClick = {
                                navController.navigate(pestana.ruta) {
                                    // Comportamiento estándar de pestañas: no apilar pantallas
                                    // y conservar el estado de cada una.
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(pestana.icono, contentDescription = null) },
                            label = { Text(stringResource(pestana.titulo)) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = coloresPestanas().iconoActiva,
                                    selectedTextColor = coloresPestanas().textoActiva,
                                    indicatorColor = coloresPestanas().activa,
                                ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = RutaInicio,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<RutaInicio> {
                HomeScreen(
                    onIniciarJornada = { navController.navigate(RutaIniciarJornada) { launchSingleTop = true } },
                    onFinalizarJornada = { navController.navigate(RutaFinalizarJornada) { launchSingleTop = true } },
                )
            }
            composable<RutaHistorial> { HistoryScreen(onJornada = { navController.navigate(RutaJornada(it)) }) }
            composable<RutaViajes> { TripsScreen() }
            composable<RutaResumen> { StatsScreen() }
            composable<RutaAjustes> { SettingsScreen() }
            composable<RutaIniciarJornada> { IniciarJornadaScreen(onGuardado = { navController.popBackStack() }) }
            composable<RutaFinalizarJornada> { FinalizarJornadaScreen(onGuardado = { navController.popBackStack() }) }
            composable<RutaJornada> { EditarJornadaScreen(onTerminado = { navController.popBackStack() }) }
        }
    }
}

/**
 * El icono de la app, cuadrado con las esquinas un poco redondeadas, a la izquierda del título.
 *
 * Se usa la capa de primer plano del icono (el icono adaptable de `mipmap` no se puede pintar
 * en Compose). El dibujo ocupa el centro de esa capa, así que se amplía y se recorta.
 * Es decorativo: el título de al lado ya dice dónde se está.
 */
@Composable
private fun LogoApp(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_launcher_foreground),
        contentDescription = null,
        modifier =
            modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .graphicsLayer {
                    scaleX = 1.45f
                    scaleY = 1.45f
                },
    )
}
