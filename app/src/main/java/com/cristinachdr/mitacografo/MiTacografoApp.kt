package com.cristinachdr.mitacografo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Punto de entrada de la aplicación. `@HiltAndroidApp` genera el contenedor
 * de dependencias (equivalente al ApplicationContext de Spring).
 */
@HiltAndroidApp
class MiTacografoApp : Application()
