import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// --- Firma de la versión release ---
// Los datos del keystore NUNCA están en el repositorio. Se leen de:
//  - variables de entorno (en GitHub Actions, desde los "secrets"), o
//  - el archivo local `keystore.properties` (ignorado por git) en tu ordenador.
val keystoreProperties =
    Properties().apply {
        val archivo = rootProject.file("keystore.properties")
        if (archivo.exists()) archivo.inputStream().use(::load)
    }

fun secreto(nombre: String): String? = System.getenv(nombre) ?: keystoreProperties.getProperty(nombre)

// --- Versión ---
// En local: 0.0.0-dev. En la release, el workflow pasa -PversionName=0.1.0 -PversionCode=100.
val versionNameApp = providers.gradleProperty("versionName").getOrElse("0.0.0-dev")
val versionCodeApp = providers.gradleProperty("versionCode").getOrElse("1").toInt()

android {
    namespace = "com.cristinachdr.mitacografo"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.cristinachdr.mitacografo"
        minSdk = 26
        targetSdk = 37
        versionCode = versionCodeApp
        versionName = versionNameApp

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val rutaKeystore = secreto("MT_KEYSTORE_PATH")
            if (rutaKeystore != null) {
                storeFile = rootProject.file(rutaKeystore)
                storePassword = secreto("MT_KEYSTORE_PASSWORD")
                keyAlias = secreto("MT_KEY_ALIAS")
                keyPassword = secreto("MT_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Sin keystore configurado, el APK release se genera sin firmar (no instalable).
            signingConfig = signingConfigs.getByName("release").takeIf { it.storeFile != null }
            optimization {
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:data"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(testFixtures(project(":core:domain")))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
