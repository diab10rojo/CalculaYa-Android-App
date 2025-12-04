package com.PatagonicApps.calculaya

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Clase Application Personalizada.
 * Utilizada para inicializar servicios y SDKs globales (como Firebase)
 * antes de que se lance cualquier Activity. Esto evita fallos de inicialización.
 */
class CalculayaApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // ⭐️ Inicialización Garantizada: Ocurre antes de cualquier actividad.
        // La comprobación 'getApps(this).isEmpty()' evita la doble inicialización.
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }
    }
}