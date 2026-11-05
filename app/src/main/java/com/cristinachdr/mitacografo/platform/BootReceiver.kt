package com.cristinachdr.mitacografo.platform

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cristinachdr.mitacografo.domain.usecase.GestorAvisos
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Vuelve a programar los avisos de la jornada abierta cuando Android los ha borrado:
 *
 * - al **reiniciar** el móvil (Android borra todas las alarmas al apagarse),
 * - al **actualizar** la app,
 * - al **conceder** el permiso de alarmas exactas (para pasar de aproximadas a exactas).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var gestorAvisos: GestorAvisos

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action !in ACCIONES) return
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                gestorAvisos.reprogramarJornadaAbierta()
            } finally {
                pendiente.finish()
            }
        }
    }

    private companion object {
        val ACCIONES =
            setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            )
    }
}
