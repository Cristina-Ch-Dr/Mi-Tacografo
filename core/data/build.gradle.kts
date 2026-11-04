// Módulo de DATOS: adaptadores de salida (Room, DataStore, copia de seguridad, CSV, PDF).
// Implementa los puertos definidos en :core:domain.
plugins {
    alias(libs.plugins.android.library)
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
}

dependencies {
    implementation(project(":core:domain"))

    testImplementation(libs.junit4)
    androidTestImplementation(libs.androidx.junit)
}
