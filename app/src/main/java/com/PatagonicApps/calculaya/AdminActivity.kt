package com.PatagonicApps.calculaya

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import java.nio.charset.Charset
import java.io.File // Necesario para la manipulación de archivos locales

// ⭐️ IMPORTS CRÍTICOS DE OBJETOS GLOBALES ⭐️
// Estas constantes y clases deben ser accesibles para que el código compile.
import com.PatagonicApps.calculaya.PREFS_NODE_KEY
import com.PatagonicApps.calculaya.listaCiudades
import com.PatagonicApps.calculaya.RateManager.RATES_FILE_NAME
import com.PatagonicApps.calculaya.RateManager.RATES_FILE_OLD_NAME


class AdminActivity : AppCompatActivity() {

    // Declaraciones de Vistas de la Interfaz
    private lateinit var spinnerReadNode: Spinner
    private lateinit var btnSetReadNode: Button
    private lateinit var btnUploadRates: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var etHistoricalVersion: EditText
    private lateinit var btnFixHistorical: Button
    private lateinit var btnResetLocal: Button

    // Vistas para el Display de Debug de Versiones
    private lateinit var tvLocalActualVersion: TextView
    private lateinit var tvLocalHistoricalVersion: TextView


    // Mapeo de Nodos: Los nombres de la interfaz se mapean a las rutas de Firebase (live/test)
    private val ENVIRONMENTS = mapOf(
        "Producción (En Vivo)" to "produccion", // Nodo para usuarios finales
        "Prueba (Staging)" to "prueba"         // Nodo para pruebas del administrador
    )
    private val PREFS_NAME = "AppConfig"
    private val RATE_VERSION_KEY = "TarifaVersionLocal"
    private val RATE_VERSION_HISTORICAL_KEY = "TarifaVersionHistorical"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // 1. Inicialización de Vistas y Vinculación
        spinnerReadNode = findViewById(R.id.spinnerReadNode)
        btnSetReadNode = findViewById(R.id.btnSetReadNode)
        btnUploadRates = findViewById(R.id.btnUploadRates)
        progressBar = findViewById(R.id.progressBarAdmin)
        etHistoricalVersion = findViewById(R.id.etHistoricalVersion)
        btnFixHistorical = findViewById(R.id.btnFixHistorical)
        btnResetLocal = findViewById(R.id.btnResetLocal)

        // Vinculación de las Vistas de Debug
        tvLocalActualVersion = findViewById(R.id.tvLocalActualVersion)
        tvLocalHistoricalVersion = findViewById(R.id.tvLocalHistoricalVersion)


        progressBar.visibility = View.GONE

        displayLocalVersions() // Muestra el estado inicial de las versiones locales

        setupSpinner()

        // Listener para establecer el modo de lectura (Producción o Prueba)
        btnSetReadNode.setOnClickListener {
            saveReadNode()
        }

        // Listener para generar JSON, subir archivo y actualizar RTDB (solo version_actual)
        btnUploadRates.setOnClickListener {
            uploadNewRates()
        }

        // Listener para fijar manualmente la versión histórica
        btnFixHistorical.setOnClickListener {
            promoteHistoricalVersion()
        }

        // Listener para resetear los números de versión y los archivos locales (Debug)
        btnResetLocal.setOnClickListener {
            resetLocalRates()
        }
    }

    // -------------------------------------------------------------
    // FUNCIÓN DE DEBUG: MUESTRA LAS VERSIONES LOCALES GUARDADAS
    // -------------------------------------------------------------
    private fun displayLocalVersions() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Lee los números de versión de SharedPreferences
        val actual = sharedPref.getFloat(RATE_VERSION_KEY, 0.0f)
        val historical = sharedPref.getFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f)

        // Actualiza los TextViews con formato de una decimal
        tvLocalActualVersion.text = String.format("Actual Local: v%.1f", actual)
        tvLocalHistoricalVersion.text = String.format("Histórica Local: v%.1f", historical)
    }
    // -------------------------------------------------------------

    private fun setupSpinner() {
        // Configura el Spinner para elegir el entorno de lectura (Producción/Prueba)
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            ENVIRONMENTS.keys.toList()
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerReadNode.adapter = adapter

        // Carga y establece la última selección del administrador
        val currentReadNode = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PREFS_NODE_KEY, "produccion")

        val currentKey = ENVIRONMENTS.entries.find { it.value == currentReadNode }?.key
        if (currentKey != null) {
            spinnerReadNode.setSelection(ENVIRONMENTS.keys.indexOf(currentKey))
        }
    }

    private fun saveReadNode() {
        // Guarda la preferencia de lectura para que ConfigActivity sepa qué nodo leer.
        val selectedKey = spinnerReadNode.selectedItem.toString()
        val selectedNode = ENVIRONMENTS[selectedKey] ?: "produccion"

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString(PREFS_NODE_KEY, selectedNode)
            apply()
        }
        Toast.makeText(this, "Modo de lectura establecido a: $selectedKey ($selectedNode).", Toast.LENGTH_LONG).show()
    }

    // -------------------------------------------------------------
    // FUNCIÓN PARA RESETEAR TARIFAS LOCALES (DEBUG)
    // -------------------------------------------------------------
    private fun resetLocalRates() {
        // Resetea las claves de versión a 0.0 para forzar la redescarga en ConfigActivity.
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        sharedPref.edit().apply {
            putFloat(RATE_VERSION_KEY, 0.0f)
            putFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f)
            apply()
        }

        // Borra los archivos JSON del almacenamiento interno.
        try {
            File(applicationContext.filesDir, RATES_FILE_NAME).delete()
            File(applicationContext.filesDir, RATES_FILE_OLD_NAME).delete()
        } catch (e: Exception) {
            // Ignorar si los archivos no existían
        }

        Toast.makeText(this, "Versión local RESTABLECIDA a 0.0. La próxima ConfigActivity forzará la descarga.", Toast.LENGTH_LONG).show()
        displayLocalVersions() // Actualiza el display de debug
    }
    // -------------------------------------------------------------

    // -------------------------------------------------------------
    // FUNCIÓN PARA FIJAR VERSIÓN HISTÓRICA (MANUAL Y SEGURO)
    // -------------------------------------------------------------
    private fun promoteHistoricalVersion() {
        // Fija la versión introducida manualmente en el campo etHistoricalVersion.
        val targetNode = ENVIRONMENTS[spinnerReadNode.selectedItem.toString()] ?: "produccion"

        val targetVersionString = etHistoricalVersion.text.toString()
        val targetVersion = targetVersionString.toDoubleOrNull()

        if (targetVersion == null || targetVersion <= 0.0) {
            Toast.makeText(this, "Debe ingresar un número de versión válido (Ej: 1.3).", Toast.LENGTH_LONG).show()
            return
        }

        val db = FirebaseDatabase.getInstance().reference

        // Escribe el valor exacto en el nodo version_historica (sin incrementar).
        db.child(targetNode).child("version_historica").setValue(targetVersion)
            .addOnSuccessListener {
                Toast.makeText(this, "Versión $targetVersion fijada como HISTÓRICA en $targetNode.", Toast.LENGTH_LONG).show()
                displayLocalVersions() // Actualiza el display de debug
            }
            .addOnFailureListener {
                // Falla si las reglas RTDB están en 'false' o no hay conexión.
                Toast.makeText(this, "Fallo al fijar Histórica. Revise reglas RTDB.", Toast.LENGTH_LONG).show()
            }
    }
    // -------------------------------------------------------------

    // -------------------------------------------------------------
    // FUNCIÓN PARA GENERAR JSON Y SUBIR ARCHIVO (SÓLO version_actual)
    // -------------------------------------------------------------
    private fun uploadNewRates() {
        // Prepara la UI para la subida
        progressBar.visibility = View.VISIBLE
        btnUploadRates.isEnabled = false

        // Genera el JSON a partir del código fuente de Kotlin (listaCiudades)
        val gson = Gson()
        val mapaCiudades = listaCiudades.associateBy { it.nombre }
        val jsonString = gson.toJson(mapaCiudades)

        val targetNode = ENVIRONMENTS[spinnerReadNode.selectedItem.toString()] ?: "produccion"
        val db = FirebaseDatabase.getInstance().reference

        // 1. Lee la versión actual para calcular la nueva (ej. 1.0 -> 1.1)
        db.child(targetNode).child("version_actual").addListenerForSingleValueEvent(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val currentVersion = snapshot.getValue(Double::class.java) ?: 1.0
                // Calcula la nueva versión, incrementando en 0.1
                val newVersion = (currentVersion * 10.0 + 1.0) / 10.0

                // 2. Procede a subir el archivo JSON
                uploadJsonToStorage(jsonString, newVersion, targetNode)
            }
            override fun onCancelled(error: DatabaseError) {
                progressBar.visibility = View.GONE
                btnUploadRates.isEnabled = true
                Toast.makeText(this@AdminActivity, "Error al leer la versión de $targetNode.", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun uploadJsonToStorage(jsonString: String, newVersion: Double, targetNode: String) {
        // Maneja la subida del archivo a Firebase Storage
        val storageRef = FirebaseStorage.getInstance().reference

        val versionString = String.format("%.1f", newVersion)
        val fileName = "tarifas_v$versionString.json"

        // Define la ruta de Storage (ej. rates-produccion/tarifas_v1.1.json)
        val storagePath = "rates-$targetNode"
        val ratesRef = storageRef.child("$storagePath/$fileName")

        val uploadTask = ratesRef.putBytes(jsonString.toByteArray(Charset.forName("UTF-8")))

        uploadTask.addOnSuccessListener {
            // Éxito en la subida del archivo: Ahora actualiza el número de versión en RTDB
            val db = FirebaseDatabase.getInstance().reference
            db.child(targetNode).child("version_actual").setValue(newVersion)
                .addOnSuccessListener {
                    progressBar.visibility = View.GONE
                    btnUploadRates.isEnabled = true
                    Toast.makeText(this@AdminActivity, "¡Tarifas V$versionString publicadas en $targetNode!", Toast.LENGTH_LONG).show()
                    displayLocalVersions() // Actualiza el display después de la subida
                }
                .addOnFailureListener {
                    // Si el archivo subió, pero RTDB falló (posiblemente reglas de escritura cerradas)
                    progressBar.visibility = View.GONE
                    btnUploadRates.isEnabled = true
                    Toast.makeText(this@AdminActivity, "Error: JSON subido, pero falló la actualización de versión RTDB.", Toast.LENGTH_LONG).show()
                }
        }.addOnFailureListener {
            // Falla en la subida (posiblemente reglas de Storage cerradas o no hay internet)
            progressBar.visibility = View.GONE
            btnUploadRates.isEnabled = true
            Toast.makeText(this@AdminActivity, "Fallo al subir el JSON a $storagePath. Revisa las reglas de Storage (permiso WRITE).", Toast.LENGTH_LONG).show()
        }
    }
}