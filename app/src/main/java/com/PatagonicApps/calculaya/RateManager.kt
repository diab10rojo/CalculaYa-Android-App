package com.PatagonicApps.calculaya

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
    const val RATES_FILE_NAME = "rates.json"          // Archivo de la versión ACTUAL
    const val RATES_FILE_OLD_NAME = "rates_old.json" // Archivo de la versión HISTÓRICA


    /**
     * Carga la lista de tarifas. Intenta cargar el archivo JSON guardado localmente.
     * Si falla o el archivo no existe, mantiene la lista de fallback.
     */
    fun loadRates(context: Context) {
        val file = File(context.filesDir, RATES_FILE_NAME) // Busca el archivo principal

        if (file.exists()) {
            try {
                val jsonString = file.readText(Charset.forName("UTF-8"))
                val gson = Gson()

                // Deserializa el JSON del mapa de ciudades (Map<String, Ciudad>) a una lista.
                val type = object : TypeToken<Map<String, Ciudad>>() {}.type
                val map: Map<String, Ciudad> = gson.fromJson(jsonString, type)

                listaCiudades = map.values.toList() // La lista que usa MainActivity
                Log.d(TAG, "Tarifas cargadas con éxito. Total: ${listaCiudades.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Error al deserializar o leer el archivo de tarifas: ${e.message}")
                // Mantiene el valor actual si la deserialización falla.
            }
        }
    }

    /**
     * Función que contiene la lista de tarifas estáticas (FALLBACK).
     * Esta lista se utiliza como datos iniciales si el usuario abre la app por primera vez.
     */
    private fun getStaticFallbackRates(): List<Ciudad> {
        // ⚠️ NOTA: Esta lista debe contener las 600 ciudades en producción
        // para que la generación de JSON funcione correctamente.
        return listOf(
            Ciudad(nombre = "Trelew", sEsD = false, extraPEGrupo1 = 270, tramo1Retiro = 215),
            Ciudad(nombre = "Gba Oeste", extraPEGrupo1 = 220),
            Ciudad(nombre = "CABA")
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
}