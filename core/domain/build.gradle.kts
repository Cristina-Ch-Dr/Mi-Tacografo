// Módulo de DOMINIO: Kotlin puro, sin dependencias de Android.
// Aquí viven el modelo, el motor de reglas 561/2006, los casos de uso y los puertos.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
