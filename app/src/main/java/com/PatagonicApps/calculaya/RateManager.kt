package com.PatagonicApps.calculaya

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.nio.charset.Charset

/**
 * Objeto Singleton que gestiona la carga de tarifas de cálculo.
 * Es la fuente de datos central de la aplicación (CalculaYa).
 */
object RateManager {

    private const val TAG = "RateManager"

    // ⭐️ La lista mutable que contiene las tarifas actuales.
    // Inicializada con el fallback estático para evitar fallos (crashes).
    var listaCiudades: List<Ciudad> = getStaticFallbackRates()

    // Constantes para el manejo de archivos en el Almacenamiento Interno
    const val RATES_FILE_NAME = "rates.json"
    const val RATES_FILE_OLD_NAME = "rates_old.json"


    /**
     * Carga la lista de tarifas. Intenta cargar el archivo JSON guardado localmente.
     * Retorna TRUE si se cargó desde el archivo local; FALSE si no existe o falló.
     */
    fun loadRates(context: Context): Boolean { // 👈 MODIFICADO: Ahora devuelve Boolean
        val file = File(context.filesDir, RATES_FILE_NAME)

        if (file.exists()) {
            try {
                val jsonString = file.readText(Charset.forName("UTF-8"))
                val gson = Gson()

                // Deserializa el JSON del mapa de ciudades (Map<String, Ciudad>) a una lista.
                val type = object : TypeToken<Map<String, Ciudad>>() {}.type
                val map: Map<String, Ciudad> = gson.fromJson(jsonString, type)

                listaCiudades = map.values.toList() // La lista que usa MainActivity
                Log.d(TAG, "Tarifas cargadas con éxito. Total: ${listaCiudades.size}")
                return true // 👈 Éxito en la carga desde el archivo

            } catch (e: Exception) {
                Log.e(TAG, "Error al deserializar o leer el archivo de tarifas: ${e.message}")
                // Mantiene el valor actual (fallback) si la deserialización falla.
                return false
            }
        }
        // Si el archivo NO existe, mantiene el fallback estático.
        Log.d(TAG, "Archivo de tarifas local no encontrado. Usando fallback.")
        return false // 👈 Falló la carga desde el archivo
    }

    /**
     * Función que contiene la lista de tarifas estáticas (FALLBACK).
     * ⚠️ NOTA CRÍTICA: Debe incluir las propiedades de tarifa con valores usables.
     */
    private fun getStaticFallbackRates(): List<Ciudad> {
        // Se asume que 'Ciudad' tiene propiedades como puntoRetiroDia y puntoEntregaDia,
        // y se les da un valor de emergencia para evitar cálculos a cero.
        return listOf(
            Ciudad(nombre = "Trelew", puntoRetiroDia = 1000, puntoEntregaDia = 1000 /*...otros parámetros...*/),
            Ciudad(nombre = "Zona Norte", puntoRetiroDia = 1100, puntoEntregaDia = 1100 /*...otros parámetros...*/),
            Ciudad(nombre = "CABA", puntoRetiroDia = 1200, puntoEntregaDia = 1200 /*...otros parámetros...*/)
        )
    }

    /**
     * Guarda el nuevo contenido JSON descargado en el archivo rates.json.
     * Llamada por ConfigActivity después de una descarga exitosa de Firebase Storage.
     */
    fun saveNewRates(context: Context, jsonString: String): Boolean {
        // NOTA: La promoción del archivo antiguo (rates.json -> rates_old.json)
        // se maneja en la ConfigActivity para simplificar el flujo.

        return try {
            val file = File(context.filesDir, RATES_FILE_NAME)
            file.writeText(jsonString, Charset.forName("UTF-8"))
            loadRates(context) // Recarga la lista inmediatamente con los nuevos datos
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al guardar las nuevas tarifas: ${e.message}")
            false
        }
    }

    // ----------------------------------------------------
    // ⭐️ FUNCIÓN ASÍNCRONA AÑADIDA PARA RESOLVER EL ERROR EN MainActivity ⭐️
    // ----------------------------------------------------

    /**
     * Intenta descargar las tarifas completas de la fuente remota.
     * Debe llamarse desde una Coroutine.
     */
    suspend fun downloadAndSaveRatesAsync(context: Context): Boolean = withContext(Dispatchers.IO) {

        val downloadedJsonString: String? = try {

            // ----------------------------------------------------
            // ➡️ IMPLEMENTACIÓN DE RED REAL AQUÍ ⬅️
            // (Simulación de descarga exitosa con un JSON de ejemplo)
            // ----------------------------------------------------

            """
            {
              "Trelew": {"nombre":"Trelew", "puntoRetiroDia": 750, "puntoEntregaDia": 750, "tramo1Retiro": 100, "distancia1Retiro": 2.0},
              "Zona Norte": {"nombre":"Zona Norte", "puntoRetiroDia": 850, "puntoEntregaDia": 850, "tramo1Retiro": 110, "distancia1Retiro": 2.5},
              "CABA": {"nombre":"CABA", "puntoRetiroDia": 950, "puntoEntregaDia": 950, "tramo1Retiro": 120, "distancia1Retiro": 3.0}
            }
            """.trimIndent()

        } catch (e: Exception) {
            Log.e(TAG, "Error de red al descargar tarifas: ${e.message}")
            null
        }

        if (!downloadedJsonString.isNullOrEmpty()) {
            return@withContext saveNewRates(context, downloadedJsonString)
        }

        return@withContext false
    }
}