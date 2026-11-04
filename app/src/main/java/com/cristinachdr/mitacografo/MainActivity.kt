package com.cristinachdr.mitacografo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cristinachdr.mitacografo.navigation.MiTacografoNavHost
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
