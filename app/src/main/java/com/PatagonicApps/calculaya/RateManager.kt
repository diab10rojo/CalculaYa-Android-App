package com.PatagonicApps.calculaya

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.Charset

/**
 * Objeto Singleton que gestiona la carga de tarifas de cálculo.
 * Fuente central de datos de la aplicación (CalculaYa).
 */
object RateManager {

    private const val TAG = "RateManager"

    // Lista mutable con las tarifas actuales (inicializada con fallback)
    var listaCiudades: List<Ciudad> = getStaticFallbackRates()

    // Constantes para manejo de archivos
    const val RATES_FILE_NAME = "rates.json"
    const val RATES_FILE_OLD_NAME = "rates_old.json"

    /**
     * Carga la lista de tarifas desde archivo local.
     * Retorna TRUE si se cargó correctamente, FALSE si no existe o falló.
     */
    fun loadRates(context: Context): Boolean {
        val file = File(context.filesDir, RATES_FILE_NAME)

        if (file.exists()) {
            return try {
                val jsonString = file.readText(Charset.forName("UTF-8"))
                val gson = Gson()

                // Parseo manual con JsonObject
                val jsonObject = JsonParser.parseString(jsonString).asJsonObject
                val ciudades = mutableListOf<Ciudad>()
                for ((_, value) in jsonObject.entrySet()) {
                    val ciudad = gson.fromJson(value, Ciudad::class.java)
                    ciudades.add(ciudad)
                }

                listaCiudades = ciudades

                Log.d(TAG, "Tarifas cargadas con éxito. Total ciudades: ${listaCiudades.size}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error al deserializar tarifas: ${e.message}")
                false
            }
        }

        Log.d(TAG, "Archivo de tarifas no encontrado. Usando fallback.")
        return false
    }

    /**
     * Guarda el contenido JSON descargado en el archivo rates.json.
     */
    fun saveNewRates(context: Context, jsonString: String): Boolean {
        return try {
            val file = File(context.filesDir, RATES_FILE_NAME)
            file.writeText(jsonString, Charsets.UTF_8)
            val result = loadRates(context)
            Log.d(TAG, "Nuevas tarifas guardadas. Resultado carga: $result")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Fallo al guardar tarifas: ${e.message}")
            false
        }
    }

    /**
     * Descarga simulada de tarifas (ejemplo).
     * En producción se conecta a Firebase Storage.
     */
    suspend fun downloadAndSaveRatesAsync(context: Context): Boolean = withContext(Dispatchers.IO) {
        val downloadedJsonString: String? = try {
            """
            {
              "Trelew": {"nombre":"Trelew", "puntoRetiroDia": 750, "puntoEntregaDia": 750},
              "Zona Norte": {"nombre":"Zona Norte", "puntoRetiroDia": 850, "puntoEntregaDia": 850},
              "CABA": {"nombre":"CABA", "puntoRetiroDia": 950, "puntoEntregaDia": 950}
            }
            """.trimIndent()
        } catch (e: Exception) {
            Log.e(TAG, "Error de red al descargar tarifas: ${e.message}")
            null
        }

        if (!downloadedJsonString.isNullOrEmpty()) {
            val result = saveNewRates(context, downloadedJsonString)
            Log.d(TAG, "Descarga simulada completada. Guardado: $result")
            return@withContext result
        }

        Log.e(TAG, "Descarga simulada fallida: JSON vacío.")
        return@withContext false
    }

    /**
     * Fallback estático: asegura que siempre haya al menos 3 ciudades cargadas.
     */
    private fun getStaticFallbackRates(): List<Ciudad> {
        return listOf(
            Ciudad(
                nombre = "Trelew",
                // Puntos
                puntoRetiroDia = 560,
                puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680,
                puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835,
                puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335,
                puntoEntregaFindeNoche = 775,

                // Tramos (ajusta estos cuatro pares con tus valores reales)
                tramo1Retiro = 215,
                tramo1Entrega = 215,
                tramo2Retiro = 375,   // ← REEMPLAZAR
                tramo2Entrega = 375,  // ← REEMPLAZAR
                tramo3Retiro = 585,   // ← REEMPLAZAR
                tramo3Entrega = 585,  // ← REEMPLAZAR
                tramo4Retiro = 655,   // ← REEMPLAZAR
                tramo4Entrega = 655,  // ← REEMPLAZAR

                sEsD = false
            ))
    }
}
