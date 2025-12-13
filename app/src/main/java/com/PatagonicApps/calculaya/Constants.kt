package com.PatagonicApps.calculaya

/**
 * Constantes Globales de Configuración y Entorno de Pruebas.
 */

// ⭐️ CONTROL DE ENTORNO:
// true SOLO en desarrollo, false antes de subir a Google Play
const val IS_ADMIN_BUILD = true

// CLAVE DE PREFERENCIA: Usada en ConfigActivity para guardar y leer qué nodo de Firebase
const val PREFS_NODE_KEY = "FirebaseReadNode"

object Constants {
    const val PREFS_NAME = "AppConfig"
    const val KEY_ADS_FIREBASE_VALUE = "ads_enabled_production"
    const val KEY_ADS_LOCAL_ACTIVATED = "ads_local_activated"
    const val KEY_FORCE_UPDATE = "force_update_required"
    const val KEY_MIN_VERSION_CODE = "min_version_code"   // ⭐️ NUEVA CONSTANTE
}

