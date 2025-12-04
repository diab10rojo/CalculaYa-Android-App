// build.gradle.kts (Nivel de Proyecto: CalculaYa)

plugins {
    // ⭐️ El ÚNICO bloque 'plugins' permitido ⭐️
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}