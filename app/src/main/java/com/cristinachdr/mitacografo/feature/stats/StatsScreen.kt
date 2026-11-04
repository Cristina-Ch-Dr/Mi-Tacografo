package com.cristinachdr.mitacografo.feature.stats

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.components.PantallaPendiente

@Composable
fun StatsScreen() {
    PantallaPendiente(titulo = stringResource(R.string.nav_resumen))
}
