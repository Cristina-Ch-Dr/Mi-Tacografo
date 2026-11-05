package com.cristinachdr.mitacografo.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.cristinachdr.mitacografo.R
import com.cristinachdr.mitacografo.domain.port.JornadaRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Duration
import javax.inject.Inject

/**
 * Recibe la alarma del sistema y muestra la notificación del aviso.
 *
 * Antes de avisar comprueba que la jornada siga abierta: si ya se finalizó
 * (por ejemplo, una alarma que no llegó a cancelarse), no molesta.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var repository: JornadaRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val jornadaId = intent.getLongExtra(EXTRA_JORNADA, -1)
        val desdeInicio = Duration.ofMinutes(intent.getLongExtra(EXTRA_MINUTOS, 0))

        // goAsync: el receptor tiene unos segundos para leer la base de datos sin bloquear.
        val pendiente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (jornadaId == JORNADA_PRUEBA) {
                    Notificaciones.mostrarAviso(
                        context,
                        id = ID_NOTIFICACION_PRUEBA,
                        titulo = context.getString(R.string.aviso_prueba_titulo),
                        texto = context.getString(R.string.aviso_prueba_texto),
                    )
                } else if (repository.obtenerPorId(jornadaId)?.abierta == true) {
                    val minutos = desdeInicio.toMinutes()
                    Notificaciones.mostrarAviso(
                        context,
                        id = minutos.toInt(),
                        titulo = context.getString(R.string.aviso_titulo, minutos / 60, minutos % 60),
                        texto = context.getString(R.string.aviso_texto),
                    )
                }
            } finally {
                pendiente.finish()
            }
        }
    }

    companion object {
        const val JORNADA_PRUEBA = -1L
        private const val ID_NOTIFICACION_PRUEBA = 1
        private const val EXTRA_JORNADA = "jornada_id"
        private const val EXTRA_MINUTOS = "minutos_desde_inicio"

        fun intent(
            context: Context,
            jornadaId: Long,
            desdeInicio: Duration,
        ): Intent =
            Intent(context, AlarmReceiver::class.java)
                .putExtra(EXTRA_JORNADA, jornadaId)
                .putExtra(EXTRA_MINUTOS, desdeInicio.toMinutes())
    }
}
