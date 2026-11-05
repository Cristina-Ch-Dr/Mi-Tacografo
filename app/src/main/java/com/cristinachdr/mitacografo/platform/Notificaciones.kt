package com.cristinachdr.mitacografo.platform

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cristinachdr.mitacografo.MainActivity
import com.cristinachdr.mitacografo.R

/** Canal y notificaciones de los avisos de jornada. */
object Notificaciones {
    private const val CANAL_AVISOS = "avisos_jornada"

    /** Se llama al arrancar la app. Crear un canal que ya existe no hace nada. */
    fun crearCanal(context: Context) {
        val canal =
            NotificationChannel(
                CANAL_AVISOS,
                context.getString(R.string.canal_avisos_nombre),
                // Alta: suena, vibra y aparece encima de la pantalla.
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { description = context.getString(R.string.canal_avisos_descripcion) }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    /**
     * Muestra el aviso. Si no hay permiso de notificaciones, no hace nada
     * (la pantalla de Inicio ya avisa de que faltan permisos).
     */
    @SuppressLint("MissingPermission") // se comprueba con PermisosAvisos justo antes
    fun mostrarAviso(
        context: Context,
        id: Int,
        titulo: String,
        texto: String,
    ) {
        if (!PermisosAvisos.notificacionesPermitidas(context)) return

        // Tocar la notificación abre la app.
        val intentApp =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val abrirApp =
            PendingIntent.getActivity(
                context,
                0,
                intentApp,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val notificacion =
            NotificationCompat
                .Builder(context, CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_aviso)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(abrirApp)
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(context).notify(id, notificacion)
    }
}
