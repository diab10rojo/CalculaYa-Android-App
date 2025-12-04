plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // ⭐️ APLICACIÓN DEL PLUGIN DE GOOGLE SERVICES ⭐️
    id("com.google.gms.google-services")
}

android {
    namespace = "com.PatagonicApps.calculaya"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.PatagonicApps.calculaya"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    // 1. Firebase BOM (Bill of Materials) - Fija las versiones de Firebase
    implementation(platform(libs.firebase.bom))

    // 2. Dependencias de Firebase (Versión controlada por el BOM)
    implementation(libs.firebase.database.ktx)     // Control de versión de tarifas (Realtime DB)
    implementation("com.google.firebase:firebase-storage-ktx") // Descarga del JSON (Storage)

    // 3. Librería de Serialización/Deserialización
    implementation("com.google.code.gson:gson:2.10.1")

    // 4. Coroutines (Para gestión de descargas asíncronas)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // 5. Librerías de la App (EXISTENTES)
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.9")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)

    //Para utilizar admob
    implementation("com.google.android.gms:play-services-ads:22.6.0")

    // Dependencias de testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}