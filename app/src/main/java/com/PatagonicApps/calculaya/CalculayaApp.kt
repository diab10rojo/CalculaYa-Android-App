package com.PatagonicApps.calculaya

import android.app.Application
import com.google.firebase.FirebaseApp
import com.jakewharton.threetenabp.AndroidThreeTen

/**
 * Clase Application Personalizada.
 * Utilizada para inicializar servicios y SDKs globales (como Firebase y ThreeTenABP)
 * antes de que se lance cualquier Activity. Esto evita fallos de inicialización.
 */
class CalculayaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ⭐️ Inicialización Garantizada de Firebase: Ocurre antes de cualquier actividad.
        // La comprobación 'getApps(this).isEmpty()' evita la doble inicialización.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        // ⭐️ Inicialización de ThreeTenABP: Requerida para usar 'org.threeten.bp.LocalDateTime'
        // Esto carga los datos de zona horaria y evita la excepción: org.threeten.bp.zone.ZoneRulesException
        AndroidThreeTen.init(this)
    }
}