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
    var listaCiudadesHistoricas: List<Ciudad> = getStaticFallbackOldRates()

    const val RATES_FILE_NAME = "rates.json"
    const val RATES_FILE_OLD_NAME = "rates_old.json"

    // ==================== TARIFAS ACTUALES ====================
    fun loadRates(context: Context): Boolean {
        val file = File(context.filesDir, RATES_FILE_NAME)

        if (!file.exists()) {
            Log.e(TAG, "❌ Archivo NO EXISTE: ${file.absolutePath}")
            Log.d(TAG, "📁 Archivos disponibles: ${context.filesDir.listFiles()?.joinToString()}")
            return false
        }

        return try {
            val jsonString = file.readText(Charset.forName("UTF-8"))
            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject

            val ciudades = mutableListOf<Ciudad>()
            for ((key, value) in jsonObject.entrySet()) {
                try {
                    val ciudad = gson.fromJson(value, Ciudad::class.java)
                    if (!ciudad.nombre.isNullOrEmpty()) {
                        ciudades.add(ciudad)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando $key", e)
                }
            }

            if (ciudades.isEmpty()) {
                Log.e(TAG, "❌ NO SE CARGÓ NINGUNA CIUDAD - Usando fallback")
                return false
            }

            listaCiudades = ciudades
            true

        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR CRÍTICO al deserializar", e)
            false
        }
    }

    // ==================== TARIFAS HISTÓRICAS ====================
    fun loadOldRates(context: Context): List<Ciudad> {
        val file = File(context.filesDir, RATES_FILE_OLD_NAME)
        if (!file.exists()) {
            Log.e(TAG, "❌ Archivo histórico NO EXISTE: ${file.absolutePath}")
            return listaCiudadesHistoricas // fallback
        }
        return try {
            val jsonString = file.readText(Charsets.UTF_8)
            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            val ciudades = mutableListOf<Ciudad>()
            for ((_, value) in jsonObject.entrySet()) {
                val ciudad = gson.fromJson(value, Ciudad::class.java)
                if (!ciudad.nombre.isNullOrEmpty()) {
                    ciudades.add(ciudad)
                }
            }
            if (ciudades.isEmpty()) listaCiudadesHistoricas else ciudades
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando tarifas históricas", e)
            listaCiudadesHistoricas
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

    // ==================== FALLBACK ACTUAL ====================
    private fun getStaticFallbackRates(): List<Ciudad> {
        return listOf(
            Ciudad(
                nombre = "Trelew",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 240, tramo2Retiro = 420, tramo3Retiro = 650, tramo4Retiro = 730,
                tramo1Entrega = 240, tramo2Entrega = 420, tramo3Entrega = 650, tramo4Entrega = 730,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 270, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 0,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 0,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "San Juan",
                puntoRetiroDia = 575, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 725, puntoEntregaFindeDia = 455,
                puntoRetiroNoche = 920, puntoEntregaNoche = 550,
                puntoRetiroFindeNoche = 1610, puntoEntregaFindeNoche = 950,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 140, extraPEGrupo2 = 80, extraPEGrupo3 = 50, extraPEGrupo4 = 0,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 0,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "CABA",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Córdoba",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "GBA Oeste",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Zona Norte",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Mendoza",
                puntoRetiroDia = 585, puntoEntregaDia = 300,
                puntoRetiroFindeDia = 775, puntoEntregaFindeDia = 475,
                puntoRetiroNoche = 955, puntoEntregaNoche = 570,
                puntoRetiroFindeNoche = 1660, puntoEntregaFindeNoche = 965,
                tramo1Retiro = 210, tramo2Retiro = 385, tramo3Retiro = 635, tramo4Retiro = 715,
                tramo1Entrega = 210, tramo2Entrega = 385, tramo3Entrega = 635, tramo4Entrega = 715,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            )

            // 🔹 Podés agregar aquí el resto de ciudades actuales
        )
    }

    // ==================== FALLBACK HISTÓRICO ====================
    private fun getStaticFallbackOldRates(): List<Ciudad> {
        return listOf(
            // 🔹 Trelew
            Ciudad(
                nombre = "Trelew",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 230, tramo2Retiro = 400, tramo3Retiro = 620, tramo4Retiro = 695,
                tramo1Entrega = 230, tramo2Entrega = 400, tramo3Entrega = 620, tramo4Entrega = 695,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = false,
                extraPEGrupo1 = 270, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 0,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 0,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "San Juan",
                puntoRetiroDia = 570, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 685, puntoEntregaFindeDia = 430,
                puntoRetiroNoche = 875, puntoEntregaNoche = 525,
                puntoRetiroFindeNoche = 1465, puntoEntregaFindeNoche = 865,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = false,
                extraPEGrupo1 = 140, extraPEGrupo2 = 80, extraPEGrupo3 = 50, extraPEGrupo4 = 0,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 0,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "CABA",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Córdoba",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "GBA Oeste",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Zona Norte",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            ),
            Ciudad(
                nombre = "Mendoza",
                puntoRetiroDia = 580, puntoEntregaDia = 295,
                puntoRetiroFindeDia = 730, puntoEntregaFindeDia = 450,
                puntoRetiroNoche = 910, puntoEntregaNoche = 545,
                puntoRetiroFindeNoche = 1510, puntoEntregaFindeNoche = 875,
                tramo1Retiro = 200, tramo2Retiro = 365, tramo3Retiro = 605, tramo4Retiro = 680,
                tramo1Entrega = 200, tramo2Entrega = 365, tramo3Entrega = 605, tramo4Entrega = 680,
                distancia1Retiro = 2f, distancia2Retiro = 2f, distancia3Retiro = 2f, distancia4Retiro = 9999f,
                distancia1Entrega = 2f, distancia2Entrega = 2f, distancia3Entrega = 2f, distancia4Entrega = 9999f,
                sEsD = true,
                extraPEGrupo1 = 275, extraPEGrupo2 = 175, extraPEGrupo3 = 105, extraPEGrupo4 = 55,
                extraPEGrupo5 = 0, extraPEGrupo6 = 0, extraPEGrupo7 = 0, extraPEGrupo8 = 105,
                extraPRGrupo1 = 0, extraPRGrupo2 = 0, extraPRGrupo3 = 0, extraPRGrupo4 = 0,
                extraPRGrupo5 = 0, extraPRGrupo6 = 0, extraPRGrupo7 = 0, extraPRGrupo8 = 0,
                extraKmEGrupo1 = 0, extraKmEGrupo2 = 0, extraKmEGrupo3 = 0, extraKmEGrupo4 = 0,
                extraKmEGrupo5 = 0, extraKmEGrupo6 = 0, extraKmEGrupo7 = 0, extraKmEGrupo8 = 0,
                extraKmRGrupo1 = 0, extraKmRGrupo2 = 0, extraKmRGrupo3 = 0, extraKmRGrupo4 = 0,
                extraKmRGrupo5 = 0, extraKmRGrupo6 = 0, extraKmRGrupo7 = 0, extraKmRGrupo8 = 0
            )

            // 🔹 Podés agregar aquí el resto de ciudades actuales
        )
            // 🔹 Podés agregar aquí más ciudades históricas si querés

    }
}
