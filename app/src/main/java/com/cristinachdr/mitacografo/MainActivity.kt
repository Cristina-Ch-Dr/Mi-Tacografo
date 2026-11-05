package com.cristinachdr.mitacografo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cristinachdr.mitacografo.navigation.MiTacografoNavHost
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Pantalla de carga con el icono y el nombre (tema Theme.MiTacografo.Arranque).
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Obligatorio en Android 15+; lo activamos desde el principio (espec. §3.1).
        enableEdgeToEdge()
        setContent {
            MiTacografoTheme {
                MiTacografoNavHost()
            }
        }
    }
}
