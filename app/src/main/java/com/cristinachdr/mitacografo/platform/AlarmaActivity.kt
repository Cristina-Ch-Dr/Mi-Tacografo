package com.cristinachdr.mitacografo.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.ui.FormatoHora
import com.cristinachdr.mitacografo.ui.components.BigButton
import com.cristinachdr.mitacografo.ui.theme.MiTacografoTheme
import java.time.LocalTime

/**
 * La pantalla del despertador sonando (MT-57): sale encima del bloqueo y enciende la pantalla, como la de un
 * reloj despertador. «Apagar» para el sonido y la cierra.
 */
class AlarmaActivity : ComponentActivity() {
    private val cerrar =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) = finish()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        ContextCompat.registerReceiver(this, cerrar, IntentFilter(ACCION_CERRAR), ContextCompat.RECEIVER_NOT_EXPORTED)

        val hora = LocalTime.now().format(FormatoHora)
        setContent {
            MiTacografoTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.primary) {
                    Column(
                        Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            hora,
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            stringResource(R.string.despertador_titulo),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            textAlign = TextAlign.Center,
                        )
                        BigButton(
                            text = stringResource(R.string.accion_apagar),
                            onClick = ::apagar,
                            icon = Icons.Filled.Close,
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        unregisterReceiver(cerrar)
        super.onDestroy()
    }

    private fun apagar() {
        runCatching { DespertadorService.apagarIntent(this).send() }
        finish()
    }

    companion object {
        const val ACCION_CERRAR = "com.cristinachdr.mitacografo.CERRAR_ALARMA"
    }
}
