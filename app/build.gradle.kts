plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.PatagonicApps.calculaya"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.PatagonicApps.calculaya"
        minSdk = 24
        targetSdk = 35
        versionCode = 4
        versionName = "1.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
    // Firebase BOM
    implementation(platform(libs.firebase.bom))

    // Firebase dependencias (controladas por BOM)
    implementation(libs.firebase.database.ktx)
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-config-ktx") // ✅ solo KTX

    // Gson
    implementation("com.google.code.gson:gson:2.13.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // Librerías AndroidX
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.9")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.preference)

    // AdMob
    implementation("com.google.android.gms:play-services-ads:24.9.0")

    // Play Core
    implementation("com.google.android.play:core-ktx:1.8.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// ✅ Forzar versiones más recientes para evitar demociones
configurations.all {
    resolutionStrategy {
        force(
            "com.google.android.gms:play-services-ads:24.9.0",
            "androidx.activity:activity:1.12.1",
            "androidx.core:core-ktx:1.17.0",
            "androidx.preference:preference:1.2.1",
            "com.google.android.material:material:1.13.0",
            "com.google.code.gson:gson:2.13.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2",
            "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
        )
    }
}
