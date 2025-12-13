package com.PatagonicApps.calculaya

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.File
import java.nio.charset.Charset

object RateManager {

    private const val TAG = "RateManager"

    var listaCiudades: List<Ciudad> = getStaticFallbackRates()

    const val RATES_FILE_NAME = "rates.json"
    const val RATES_FILE_OLD_NAME = "rates_old.json"

    fun loadRates(context: Context): Boolean {
        val file = File(context.filesDir, RATES_FILE_NAME)
        if (!file.exists()) {
            Log.d(TAG, "Archivo de tarifas no encontrado. Usando fallback.")
            return false
        }
        return try {
            val jsonString = file.readText(Charset.forName("UTF-8"))
            val gson = Gson()
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

    suspend fun downloadAndSaveRatesAsync(context: Context): Boolean = withContext(Dispatchers.IO) {
        val downloadedJsonString: String? = try {
            """{ "Trelew": {"nombre":"Trelew","puntoRetiroDia":560,"puntoEntregaDia":285} }"""
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
        false
    }

    private fun getStaticFallbackRates(): List<Ciudad> {
        return listOf(
            Ciudad(
                nombre = "Trelew",
                puntoRetiroDia = 560,
                puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680,
                puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835,
                puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335,
                puntoEntregaFindeNoche = 775,
                tramo1Retiro = 215, tramo2Retiro = 375, tramo3Retiro = 585, tramo4Retiro = 655,
                tramo1Entrega = 215, tramo2Entrega = 375, tramo3Entrega = 585, tramo4Entrega = 655,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 2f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 2f,
                sEsD = false,
                extraPEGrupo1 = 0, extraPEGrupo2 = 0, extraPEGrupo3 = 0, extraPEGrupo4 = 0,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0
            )
        )
    }
}
