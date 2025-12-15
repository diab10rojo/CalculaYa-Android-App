package com.PatagonicApps.calculaya

/**
 * Constantes Globales de Configuración y Entorno de Pruebas.
 */

// ⭐️ CONTROL DE ENTORNO:
// Debe ser 'true' SÓLO en tu versión de desarrollo para probar montos.
// Debe ser 'false' antes de subir la app a Google Play (para que use el nodo 'control').
const val IS_ADMIN_BUILD = false

// CLAVE DE PREFERENCIA: Usada en ConfigActivity para guardar y leer qué nodo de
// Firebase (control o production) debe leer la aplicación.
const val PREFS_NODE_KEY = "FirebaseReadNode"

object Constants {
    const val KEY_ADS_FIREBASE_VALUE = "ads_enabled"
    const val KEY_FORCE_UPDATE = "force_update_required"
    const val KEY_MIN_VERSION_CODE = "min_version_code"
}
