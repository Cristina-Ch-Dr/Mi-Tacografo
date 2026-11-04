// Módulo de DATOS: adaptadores de salida (Room, DataStore, copia de seguridad, CSV, PDF).
// Implementa los puertos definidos en :core:domain.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "com.cristinachdr.mitacografo.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Robolectric necesita los recursos de Android en los tests locales.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// El esquema de cada versión de la base de datos se guarda en git (core/data/schemas/).
// Sirve para revisar los cambios en los PR y para probar las migraciones.
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:domain"))

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Tests locales: Room en memoria sobre Robolectric (sin emulador).
    testImplementation(libs.junit4)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
}
