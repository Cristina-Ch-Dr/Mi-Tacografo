package com.cristinachdr.mitacografo.platform

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * Qué necesita Android para que los avisos lleguen, y cómo pedírselo al usuario.
 *
 * Cada versión de Android añadió una barrera:
 * - **Android 12 (API 31)**: las alarmas exactas necesitan permiso (en 14+ viene denegado).
 * - **Android 13 (API 33)**: las notificaciones necesitan permiso (`POST_NOTIFICATIONS`).
 * - Fabricantes como Xiaomi, Samsung o Huawei cierran apps en segundo plano para ahorrar batería.
 */
object PermisosAvisos {
    /** `true` si hay que pedir el permiso de notificaciones en tiempo de ejecución. */
    val hayQuePedirNotificaciones: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    const val PERMISO_NOTIFICACIONES: String = Manifest.permission.POST_NOTIFICATIONS

    fun notificacionesPermitidas(context: Context): Boolean {
        val permiso =
            !hayQuePedirNotificaciones ||
                ContextCompat.checkSelfPermission(context, PERMISO_NOTIFICACIONES) == PackageManager.PERMISSION_GRANTED
        // Aunque haya permiso, el usuario puede haberlas apagado en los ajustes del sistema.
        return permiso && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun alarmasExactasPermitidas(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun bateriaSinRestricciones(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName)

    /** Pantalla del sistema donde se activa «Alarmas y recordatorios» para esta app (Android 12+). */
    fun intentAlarmasExactas(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, "package:${context.packageName}".toUri())
        } else {
            intentAjustesApp(context)
        }

    /** Lista de apps con optimización de batería, para quitarle las restricciones a esta. */
    fun intentBateria(): Intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    /** Ajustes de la app en el sistema (por si el permiso de notificaciones se denegó para siempre). */
    fun intentAjustesApp(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
}
