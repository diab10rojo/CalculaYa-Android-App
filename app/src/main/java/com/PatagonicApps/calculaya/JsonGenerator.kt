package com.PatagonicApps.calculaya

import com.google.gson.Gson
import java.io.File
import com.google.gson.reflect.TypeToken // Usado en la deserialización (carga en la app)

// NOTA: Asume que la variable 'listaCiudades' (con todas las tarifas)
// y la 'data class Ciudad' están definidas y son accesibles en este contexto.

/**
 * Función principal para generar el archivo JSON de tarifas a partir de la lista de Kotlin.
 * Este script debe ejecutarse en el entorno de desarrollo (Android Studio/PC).
 */
fun main() {
    // 1. Convertir la lista de ciudades a un mapa (Map<Nombre, Ciudad>).
    // Este formato es más eficiente para búsquedas y es la estructura ideal para Firebase.
    val mapaCiudades = listaCiudades.associateBy { it.nombre }

    // 2. Serializar el mapa completo utilizando Gson.
    // .setPrettyPrinting() mejora la legibilidad en la consola/archivo.
    val gson = Gson().newBuilder().setPrettyPrinting().create()
    val jsonCompleto = gson.toJson(mapaCiudades)

    // 3. Imprimir el JSON completo en la Consola.
    // Este texto se copia y se sube como tarifas_vX.X.json a Firebase Storage.
    println("--- JSON GENERADO (COPIAR EL TEXTO ABAJO) ---")
    println(jsonCompleto)

    // Opcional: Escribir en un archivo local si la salida de la consola es muy larga
    // File("ciudades_tarifas_firebase.json").writeText(jsonCompleto)
}