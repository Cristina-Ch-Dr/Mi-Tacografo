// Módulo de DOMINIO: Kotlin puro, sin dependencias de Android.
// Aquí viven el modelo, el motor de reglas 561/2006, los casos de uso y los puertos.
plugins {
    alias(libs.plugins.kotlin.jvm)
    // "Test fixtures": dobles de prueba (repositorio en memoria…) que también usan los tests de :app.
    `java-test-fixtures`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Flow para los puertos que observan datos (Room los emitirá al cambiar).
    api(libs.kotlinx.coroutines.core)
    // @Inject en los casos de uso: Hilt los crea sin módulos extra.
    implementation(libs.javax.inject)

    testImplementation(platform(libs.junit5.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly(libs.junit5.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
