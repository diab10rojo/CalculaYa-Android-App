package com.PatagonicApps.calculaya

import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings

/**
 * Helper para gestión de Firebase Remote Config
 *
 * Permite controlar características de la app de forma remota sin necesidad de actualizar:
 * - Activar/desactivar publicidad (ads)
 * - Forzar actualizaciones de la app
 * - Establecer versión mínima requerida
 *
 * Remote Config descarga configuraciones desde Firebase y las aplica en tiempo real.
 */
object RemoteConfigHelper {

    // ==================== INSTANCIA DE REMOTE CONFIG ====================

    // Instancia singleton de Firebase Remote Config
    private val remoteConfig = Firebase.remoteConfig

    // ==================== INICIALIZACIÓN ====================

    /**
     * Inicializa Firebase Remote Config con valores por defecto y configuración de fetch
     *
     * Comportamiento:
     * - DEBUG: Obtiene cambios inmediatamente (0 segundos de intervalo)
     * - RELEASE: Obtiene cambios cada 24 horas (86400 segundos)
     *
     * Debe llamarse al inicio de la aplicación (típicamente en Application.onCreate o MainActivity.onCreate)
     */
    fun init() {
        // Configurar intervalo de actualización según el modo de compilación
        val settings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
                0L      // ⚡ En debug: cambios inmediatos (útil para testing)
            } else {
                3600L  // ⏱ En release: cada 24 horas (evita consumo excesivo de red)
            }
        }
        remoteConfig.setConfigSettingsAsync(settings)

        // Establecer valores por defecto (se usan si falla la descarga de Firebase)
        remoteConfig.setDefaultsAsync(
            mapOf<String, Any>(
                Constants.KEY_ADS_FIREBASE_VALUE to false,  // Por defecto: sin anuncios
                Constants.KEY_FORCE_UPDATE to false,        // Por defecto: no forzar actualización
                Constants.KEY_MIN_VERSION_CODE to 1         // Versión mínima aceptable
            )
        )
    }

    // ==================== OBTENCIÓN DE CONFIGURACIÓN ====================

    /**
     * Descarga y activa la última configuración desde Firebase
     *
     * Este método debe llamarse después de init() para obtener los valores actualizados.
     * Respeta el intervalo mínimo configurado en init().
     *
     * @param onComplete Callback que recibe true si la descarga fue exitosa, false en caso contrario
     */
    fun fetchAndActivate(onComplete: (Boolean) -> Unit) {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                onComplete(task.isSuccessful)
            }
    }

    // ==================== GETTERS DE VALORES ====================

    /**
     * Verifica si los anuncios deben mostrarse en la aplicación
     *
     * @return true si los anuncios están habilitados, false en caso contrario
     */
    fun isAdsEnabled(): Boolean =
        remoteConfig.getBoolean(Constants.KEY_ADS_FIREBASE_VALUE)

    /**
     * Verifica si se debe forzar una actualización de la aplicación
     *
     * Útil para forzar a todos los usuarios a actualizar en caso de:
     * - Bugs críticos en versiones antiguas
     * - Cambios importantes en la API o backend
     * - Problemas de seguridad
     *
     * @return true si se debe forzar la actualización, false en caso contrario
     */
    fun isForceUpdateRequired(): Boolean =
        remoteConfig.getBoolean(Constants.KEY_FORCE_UPDATE)

    /**
     * Obtiene el código de versión mínima requerida de la aplicación
     *
     * Si la versión actual del usuario (BuildConfig.VERSION_CODE) es menor que este valor,
     * debe mostrarse un diálogo para actualizar.
     *
     * @return Código de versión mínima (versionCode) requerida
     */
    fun getMinVersionCode(): Int =
        remoteConfig.getLong(Constants.KEY_MIN_VERSION_CODE).toInt()
}