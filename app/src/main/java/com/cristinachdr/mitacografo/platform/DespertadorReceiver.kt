package com.cristinachdr.mitacografo.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.cristinachdr.mitacografo.domain.port.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Suena el despertador (MT-57): arranca el servicio que hace sonar el tono y apunta que ya no está puesto.
 *
 * Android deja arrancar un servicio en primer plano desde segundo plano justo cuando llega una alarma exacta.
 */
@AndroidEntryPoint
class DespertadorReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        ContextCompat.startForegroundService(context, DespertadorService.intentSonar(context))
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                settings.actualizar { it.copy(despertador = null) }
            } finally {
                pendiente.finish()
            }
        }
    }
}
