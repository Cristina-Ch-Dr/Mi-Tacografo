// Top-level build file where you can add configuration options common to all sub-projects/modules.
import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ktlint) apply false
}

// --- Calidad de código: la misma configuración para todos los módulos ---
// ktlint comprueba formato y estilo (+ reglas específicas de Compose).
// `./gradlew ktlintFormat` corrige casi todo automáticamente.
// El análisis estático lo hace Android Lint (`./gradlew lint`).
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<KtlintExtension> {
        version.set(rootProject.libs.versions.ktlint)
        android.set(true)
        filter {
            exclude { it.file.path.contains("${File.separator}build${File.separator}") }
        }
    }

    dependencies {
        add("ktlintRuleset", rootProject.libs.compose.rules.ktlint)
    }
}
