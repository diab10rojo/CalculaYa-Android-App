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
            Log.e(TAG, "❌ Archivo NO EXISTE: ${file.absolutePath}")
            Log.d(TAG, "📁 Archivos disponibles: ${context.filesDir.listFiles()?.joinToString()}")
            return false
        }

        return try {
            val jsonString = file.readText(Charset.forName("UTF-8"))
            Log.d(TAG, "✅ JSON leído: ${jsonString.length} caracteres")
            Log.d(TAG, "📄 Primeros 200 chars: ${jsonString.take(200)}")

            val gson = Gson()
            val jsonObject = JsonParser.parseString(jsonString).asJsonObject
            Log.d(TAG, "✅ JSON parseado: ${jsonObject.size()} entradas")

            val ciudades = mutableListOf<Ciudad>()

            for ((key, value) in jsonObject.entrySet()) {
                Log.d(TAG, "🔄 Procesando: $key")

                try {
                    val ciudad = gson.fromJson(value, Ciudad::class.java)

                    // 🔴 VALIDACIÓN CRÍTICA
                    if (ciudad.nombre.isNullOrEmpty()) {
                        Log.e(TAG, "❌ CIUDAD SIN NOMBRE! Key: $key, JSON: $value")
                        continue
                    }

                    Log.d(TAG, "   ✅ ${ciudad.nombre}: PR=${ciudad.puntoRetiroDia}, PE=${ciudad.puntoEntregaDia}")
                    ciudades.add(ciudad)

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error parseando $key", e)
                    Log.e(TAG, "   JSON problemático: $value")
                }
            }

            if (ciudades.isEmpty()) {
                Log.e(TAG, "❌ NO SE CARGÓ NINGUNA CIUDAD - Usando fallback")
                return false
            }

            listaCiudades = ciudades
            Log.d(TAG, "🎉 ÉXITO: ${listaCiudades.size} ciudades cargadas")
            Log.d(TAG, "📋 Ciudades: ${listaCiudades.joinToString { it.nombre }}")
            true

        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR CRÍTICO al deserializar", e)
            e.printStackTrace()
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
            // 🔹 Trelew
            Ciudad(
                nombre = "Trelew",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 215, tramo2Retiro = 375, tramo3Retiro = 585, tramo4Retiro = 655,
                tramo1Entrega = 215, tramo2Entrega = 375, tramo3Entrega = 585, tramo4Entrega = 655,
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
            // 🔹 SAN JUAN
            Ciudad(
                nombre = "San Juan",
                puntoRetiroDia = 550, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 640, puntoEntregaFindeDia = 400,
                puntoRetiroNoche = 805, puntoEntregaNoche = 480,
                puntoRetiroFindeNoche = 1295, puntoEntregaFindeNoche = 765,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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


            // 🔹 CABA
            Ciudad(
                nombre = "CABA",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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
            // 🔹 Zona Norte
            Ciudad(
                nombre = "Zona Norte",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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
            // 🔹 Córdoba
            Ciudad(
                nombre = "Córdoba",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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
            // 🔹 Mendoza
            Ciudad(
                nombre = "Mendoza",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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
            // 🔹 GBA Oeste
            Ciudad(
                nombre = "GBA Oeste",
                puntoRetiroDia = 560, puntoEntregaDia = 285,
                puntoRetiroFindeDia = 680, puntoEntregaFindeDia = 420,
                puntoRetiroNoche = 835, puntoEntregaNoche = 500,
                puntoRetiroFindeNoche = 1335, puntoEntregaFindeNoche = 775,
                tramo1Retiro = 190, tramo2Retiro = 345, tramo3Retiro = 570, tramo4Retiro = 640,
                tramo1Entrega = 190, tramo2Entrega = 345, tramo3Entrega = 570, tramo4Entrega = 640,
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
        )
}
}