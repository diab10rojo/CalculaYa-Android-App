package com.PatagonicApps.calculaya

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import android.widget.ProgressBar
import android.widget.TextView // Importación necesaria para TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Task
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.io.File
import kotlin.math.abs

/**
 * Activity para gestionar la configuración de usuario (Ciudad, Grupo)
 * y las actualizaciones de tarifas desde Firebase.
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var spinnerGrupo: Spinner
    private lateinit var spinnerCiudad: Spinner
    private lateinit var btnGuardarConfig: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRateStatus: TextView // 👈 DECLARACIÓN DEL TEXTVIEW

    // ⭐️ Variable del Anuncio Intersticial ⭐️
    private var mInterstitialAd: InterstitialAd? = null

    // ⭐️ Constantes para el Intersticial ⭐️
    private val AD_COUNTER_KEY = "AdDisplayCounter"
    private val FREQUENCY_LIMIT = 2

    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6378961597427920/3082064109"

    private val KEY_ADS_LOCAL_ACTIVATED = "ads_local_activated"
    private val PREFS_NODE_KEY = "FirebaseReadNode"

    private var readNode: String = "produccion"

    private val nombresCiudades: List<String>
        get() = RateManager.listaCiudades
            .map { it.nombre }
            .sorted()

    private val opcionesGrupos = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    private val PREFS_NAME = "AppConfig"
    private val RATE_VERSION_KEY = "TarifaVersionLocal"
    private val RATE_VERSION_HISTORICAL_KEY = "TarifaVersionHistorical"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        // 1. Inicialización y Vinculación de Vistas
        spinnerCiudad = findViewById(R.id.spinnerCiudadConfig)
        spinnerGrupo = findViewById(R.id.spinnerGrupoConfig)
        btnGuardarConfig = findViewById(R.id.btnGuardarConfig)
        progressBar = findViewById(R.id.progressBarConfig)
        tvRateStatus = findViewById(R.id.tvRateStatus) // 👈 VINCULACIÓN DEL TEXTVIEW
        progressBar.visibility = View.GONE

        // 2. Cargar tarifas locales/fallback
        RateManager.loadRates(this)

        // 3. Cargar el nodo de lectura
        loadReadNodeConfig()

        // 4. Setup de Spinners
        setupSpinners()

        // 5. Llamada a la verificación de tarifas (Inicia el proceso asíncrono y deshabilita el botón)
        checkAndDownloadRates()

        // 6. PRE-CARGAR el anuncio
        loadInterstitialAd()

        // 7. Listener para guardar la configuración
        btnGuardarConfig.setOnClickListener {
            // El botón debe estar habilitado para llegar aquí,
            // y esta acción es LOCAL (no depende de la descarga).
            saveConfigAndCheckAd()
        }
    }

    // -------------------------------------------------------------
    // GESTIÓN DE ANUNCIOS
    // -------------------------------------------------------------

    private fun loadInterstitialAd() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        if (isLocallyActivated) {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(this, INTERSTITIAL_AD_UNIT_ID, adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("AdMob", "Fallo al cargar anuncio: ${adError.message}")
                        mInterstitialAd = null
                    }
                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdMob", "Anuncio cargado.")
                        mInterstitialAd = interstitialAd
                    }
                }
            )
        } else {
            Log.d("AdMob", "Monetización apagada. No se carga Intersticial.")
        }
    }

    private fun checkAndShowInterstitialOnSave() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        if (!isLocallyActivated) {
            finishActivityFlow()
            return
        }

        var count = sharedPref.getInt(AD_COUNTER_KEY, 0)
        count++

        if (count >= FREQUENCY_LIMIT && mInterstitialAd != null) {

            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    mInterstitialAd = null
                    finishActivityFlow()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    mInterstitialAd = null
                    finishActivityFlow()
                }
            }

            mInterstitialAd?.show(this)
            count = 0
            loadInterstitialAd()
        } else {
            finishActivityFlow()
        }

        sharedPref.edit().putInt(AD_COUNTER_KEY, count).apply()
    }

    private fun finishActivityFlow() {
        Toast.makeText(this, "Configuración guardada.", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun saveConfigAndCheckAd() {
        val ciudadSeleccionada = spinnerCiudad.selectedItem.toString()
        val grupoSeleccionado = spinnerGrupo.selectedItem.toString()

        // Guarda las preferencias del usuario
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("CiudadSeleccionada", ciudadSeleccionada)
            putString("GrupoSeleccionado", grupoSeleccionado)
            apply()
        }

        checkAndShowInterstitialOnSave()
    }

    // -------------------------------------------------------------
    // GESTIÓN DE CIUDADES Y CONFIGURACIÓN
    // -------------------------------------------------------------

    private fun loadReadNodeConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        readNode = sharedPref.getString(PREFS_NODE_KEY, "produccion") ?: "produccion"
    }

    private fun setupSpinners() {
        spinnerGrupo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesGrupos).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        spinnerCiudad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCiudades).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        loadSavedConfig()
    }

    private fun loadSavedConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCity = sharedPref.getString("CiudadSeleccionada", nombresCiudades.firstOrNull())
        val savedGroup = sharedPref.getString("GrupoSeleccionado", "1")

        val cityIndex = nombresCiudades.indexOf(savedCity)
        if (cityIndex >= 0) spinnerCiudad.setSelection(cityIndex)

        val groupIndex = opcionesGrupos.indexOf(savedGroup)
        if (groupIndex >= 0) spinnerGrupo.setSelection(groupIndex)
    }


    // -------------------------------------------------------------
    // LÓGICA AVANZADA DE SINCRONIZACIÓN Y DESCARGA CONDICIONAL
    // -------------------------------------------------------------

    private fun checkAndDownloadRates() {
        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false // 👈 Deshabilitar al inicio de la verificación

        val dbRef = FirebaseDatabase.getInstance().reference.child(readNode)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f).toDouble()
        val localHistoricalVersion = sharedPrefs.getFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f).toDouble()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteActualVersion = snapshot.child("version_actual").getValue(Double::class.java) ?: 0.0
                val remoteHistoricalVersion = snapshot.child("version_historica").getValue(Double::class.java) ?: 0.0

                val tolerance = 0.001
                val areVersionsPracticallyEqual = abs(remoteActualVersion - localActualVersion) < tolerance

                if (areVersionsPracticallyEqual || remoteActualVersion <= localActualVersion) {
                    onRatesCheckCompleted(true, "Tarifas al día.")
                    return
                }

                val needsPromotion = abs(remoteHistoricalVersion - localActualVersion) < tolerance

                if (needsPromotion) {
                    promoteAndPerformSingleDownload(remoteActualVersion, remoteHistoricalVersion)
                    return
                }

                val isLocalHistoricalCorrect = abs(remoteHistoricalVersion - localHistoricalVersion) < tolerance

                if (isLocalHistoricalCorrect) {
                    performSingleDownload(remoteActualVersion)
                    return
                }

                if (remoteHistoricalVersion > 0.0) {
                    performConditionalDoubleDownload(remoteHistoricalVersion, localHistoricalVersion, remoteActualVersion)
                    return
                }

                performSingleDownload(remoteActualVersion)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ConfigActivity, "Error de Firebase: ${error.message}", Toast.LENGTH_LONG).show()
                // ⚠️ Habilitar el botón aunque falle la conexión, ya que la preferencia local puede guardarse.
                onRatesCheckCompleted(false, "Error al verificar versión.")
            }
        })
    }

    private fun performConditionalDoubleDownload(remoteHistoricalVersion: Double, localHistoricalVersion: Double, actualVersion: Double) {

        val historicalDownloadNeeded = remoteHistoricalVersion > 0.0 && localHistoricalVersion < remoteHistoricalVersion

        if (historicalDownloadNeeded) {
            Toast.makeText(this, "Descargando histórico (v${String.format("%.1f", remoteHistoricalVersion)}) y actual...", Toast.LENGTH_LONG).show()

            val historicalVersionString = String.format("%.1f", remoteHistoricalVersion)
            val historicalFileName = "tarifas_v$historicalVersionString.json"

            downloadFile(historicalFileName, RateManager.RATES_FILE_OLD_NAME, remoteHistoricalVersion)
                .addOnSuccessListener {
                    performSingleDownload(actualVersion)
                }
                .addOnFailureListener {
                    performSingleDownload(actualVersion)
                }
        } else {
            performSingleDownload(actualVersion)
        }
    }

    private fun promoteAndPerformSingleDownload(remoteActualVersion: Double, remoteHistoricalVersion: Double) {
        val currentFile = File(applicationContext.filesDir, RateManager.RATES_FILE_NAME)
        val oldFile = File(applicationContext.filesDir, RateManager.RATES_FILE_OLD_NAME)

        if (currentFile.exists()) {
            currentFile.copyTo(oldFile, overwrite = true)
        }

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putFloat(RATE_VERSION_HISTORICAL_KEY, remoteHistoricalVersion.toFloat())
            apply()
        }

        performSingleDownload(remoteActualVersion)
    }

    private fun performSingleDownload(actualVersion: Double) {
        val actualVersionString = String.format("%.1f", actualVersion)
        val actualFileName = "tarifas_v$actualVersionString.json"

        downloadFile(actualFileName, RateManager.RATES_FILE_NAME, actualVersion)
    }


    // FUNCIÓN DE DESCARGA GENÉRICA (Maneja la conexión a Storage)
    private fun downloadFile(remoteFileName: String, localFileName: String, newVersion: Double): Task<*> {
        val storageRef = FirebaseStorage.getInstance().reference
        val storagePath = "rates-$readNode"
        val fileRef = storageRef.child("$storagePath/$remoteFileName")

        val localFile = File(applicationContext.filesDir, localFileName)

        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false // 👈 Deshabilitar mientras se descarga (¡Obligatorio para evitar corrupción!)

        return fileRef.getFile(localFile)
            .addOnSuccessListener {
                // Si es la versión actual, actualiza SharedPreferences con el nuevo número
                if (localFileName == RateManager.RATES_FILE_NAME) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putFloat(RATE_VERSION_KEY, newVersion.toFloat())
                        apply()
                    }
                }
                // Si es la versión histórica, actualiza SharedPreferences
                else if (localFileName == RateManager.RATES_FILE_OLD_NAME) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putFloat(RATE_VERSION_HISTORICAL_KEY, newVersion.toFloat())
                        apply()
                    }
                }

                // ⭐️ ACCIÓN CLAVE ⭐️
                RateManager.loadRates(this) // Recarga RateManager con el nuevo archivo JSON
                if (localFileName == RateManager.RATES_FILE_NAME) {
                    setupSpinners() // 👈 SOLUCIÓN: Refresca el Spinner con la lista completa.
                }

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(true, "¡Tarifas actualizadas a v${String.format("%.1f", newVersion)}!")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fallo al descargar $remoteFileName.", Toast.LENGTH_LONG).show()
                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(false, "Usando tarifas locales antiguas.")
                }
            }
    }

    private fun onRatesCheckCompleted(success: Boolean, message: String) {
        progressBar.visibility = View.GONE
        btnGuardarConfig.isEnabled = true // 👈 HABILITAR SIEMPRE
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // ⭐️ CÓDIGO PARA GENERAR Y MOSTRAR EL ESTADO EN EL TEXTVIEW ⭐️
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f)
        val numCiudadesCargadas = RateManager.listaCiudades.size

        val statusMessage = StringBuilder()

        // 1. Determinar la fuente de datos
        if (localActualVersion > 0.0f && numCiudadesCargadas > 3) { // Asumimos >3 es la versión completa
            statusMessage.append("✅ **Estado:** Tarifas locales completas.\n")
            statusMessage.append("Versión: ${String.format("%.1f", localActualVersion)}\n")
        } else {
            // Este caso aplica si v0.0 o si solo se cargó el fallback de 3 ciudades
            statusMessage.append("⚠️ **Estado:** Usando tarifas de emergencia.\n")
            statusMessage.append("Versión: ${String.format("%.1f", localActualVersion)}\n")
            statusMessage.append("Origen: Lista de fallback estática.\n")
        }

        // 2. Mostrar conteo
        statusMessage.append("Ciudades en memoria: **$numCiudadesCargadas**.")

        tvRateStatus.text = statusMessage.toString()
        // ---------------------------------------------------------------------
    }
}