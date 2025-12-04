package com.PatagonicApps.calculaya

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.android.gms.tasks.Task
import java.io.File
import java.nio.charset.Charset
import kotlin.math.abs

// ⭐️ Importaciones de AdMob ⭐️
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

// Importaciones de Firebase para control remoto
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig

// Importaciones de Constantes
import com.PatagonicApps.calculaya.PREFS_NODE_KEY
import com.PatagonicApps.calculaya.RateManager.RATES_FILE_NAME
import com.PatagonicApps.calculaya.RateManager.RATES_FILE_OLD_NAME

/**
 * Activity para gestionar la configuración de usuario (Ciudad, Grupo)
 * y las actualizaciones de tarifas desde Firebase.
 */
class ConfigActivity : AppCompatActivity() {

    private lateinit var spinnerGrupo: Spinner
    private lateinit var spinnerCiudad: Spinner
    private lateinit var btnGuardarConfig: Button
    private lateinit var progressBar: ProgressBar

    // ⭐️ Variable del Anuncio Intersticial ⭐️
    private var mInterstitialAd: InterstitialAd? = null

    // ⭐️ Constantes para el Intersticial ⭐️
    private val AD_COUNTER_KEY = "AdDisplayCounter"
    private val FREQUENCY_LIMIT = 2 // Mostrará el anuncio cada 3 veces que se ejecute la acción.

    // ⚠️ ID DE PRODUCCIÓN: ASUMIENDO QUE ESTE ES TU ID REAL
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6378961597427920/3082064109"

    // ⭐️ CONSTANTES PARA EL CONTROL REMOTO (SharedPreferences de MainActivity) ⭐️
    private val KEY_ADS_LOCAL_ACTIVATED = "ads_local_activated" // Estado de monetización (true/false)

    // Nodo de lectura dinámico: 'produccion' para usuarios, 'prueba' para el admin
    private var readNode: String = "produccion"

    // La lista de nombres de ciudades se ordena alfabéticamente
    private val nombresCiudades: List<String>
        get() = RateManager.listaCiudades
            .map { it.nombre }
            .sorted()

    private val opcionesGrupos = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    // Constantes de persistencia (SharedPreferences)
    private val PREFS_NAME = "AppConfig"
    private val RATE_VERSION_KEY = "TarifaVersionLocal"
    private val RATE_VERSION_HISTORICAL_KEY = "TarifaVersionHistorical"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        // 1. Inicialización de Vistas
        spinnerCiudad = findViewById(R.id.spinnerCiudadConfig)
        spinnerGrupo = findViewById(R.id.spinnerGrupoConfig)
        btnGuardarConfig = findViewById(R.id.btnGuardarConfig)
        progressBar = findViewById(R.id.progressBarConfig)
        progressBar.visibility = View.GONE

        // 2. Cargar tarifas locales/fallback
        RateManager.loadRates(this)

        // 3. Cargar el nodo de lectura guardado desde AdminActivity
        loadReadNodeConfig()

        // 4. Setup de Spinners y Llamada a la verificación de tarifas
        setupSpinners()
        checkAndDownloadRates()

        // ⭐️ 5. PRE-CARGAR el anuncio intersticial (Solo si la monetización está activa) ⭐️
        loadInterstitialAd()

        // 6. Listener para guardar la configuración de usuario
        btnGuardarConfig.setOnClickListener {
            // Llama a la lógica de guardado que ahora maneja el intersticial
            saveConfigAndCheckAd()
        }
    }

    // -------------------------------------------------------------
    // ⭐️ GESTIÓN DE CICLO DE VIDA (Obligatorio para Intersticiales) ⭐️
    // -------------------------------------------------------------

    // El intersticial no necesita onResume/onPause/onDestroy, pero la Activity sí.
    // Como el anuncio solo está precargado en memoria (mInterstitialAd), no hay View que pausar.

    // -------------------------------------------------------------
    // ⭐️ LÓGICA DE INTERSTICIAL CONTROLADA POR MONETIZACIÓN ⭐️
    // -------------------------------------------------------------

    /**
     * Pre-carga el anuncio intersticial SOLO si la monetización está activa localmente.
     * Lee el estado permanente que guarda MainActivity tras verificar Firebase.
     */
    private fun loadInterstitialAd() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ⭐️ Verificación del estado de monetización guardado por MainActivity ⭐️
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

    /**
     * Muestra el anuncio si el contador lo permite Y si la monetización está activa.
     */
    private fun checkAndShowInterstitialOnSave() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ⭐️ NUEVA VERIFICACIÓN DE MONETIZACIÓN ⭐️
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        if (!isLocallyActivated) {
            // Si la monetización está apagada, saltamos el anuncio y terminamos el flujo.
            finishActivityFlow()
            return
        }

        // SI LA MONETIZACIÓN ESTÁ ACTIVA, CONTINUAMOS con el contador:
        var count = sharedPref.getInt(AD_COUNTER_KEY, 0)

        // 1. Incrementa el contador para la próxima vez
        count++

        if (count >= FREQUENCY_LIMIT && mInterstitialAd != null) {
            // 2. Si el contador llegó al límite Y el anuncio está cargado, lo mostramos

            // Define el Callback para saber cuándo termina el anuncio
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d("AdMob", "Anuncio cerrado por el usuario.")
                    mInterstitialAd = null
                    // ⭐️ CONTINÚA el flujo después de que el usuario cierra el anuncio ⭐️
                    finishActivityFlow()
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.d("AdMob", "Fallo al mostrar anuncio.")
                    mInterstitialAd = null
                    // ⭐️ CONTINÚA el flujo si falla al mostrarse ⭐️
                    finishActivityFlow()
                }
            }

            mInterstitialAd?.show(this)

            // 3. Reinicia el contador
            count = 0

            // 4. Pre-carga un nuevo anuncio para el próximo intento
            loadInterstitialAd()
        } else {
            // Si no se cumple la condición o el anuncio no está listo, saltamos el anuncio
            finishActivityFlow()
        }

        // 5. Guarda el nuevo valor del contador
        sharedPref.edit().putInt(AD_COUNTER_KEY, count).apply()
    }

    /**
     * Finaliza la actividad y regresa a MainActivity.
     */
    private fun finishActivityFlow() {
        Toast.makeText(this, "Configuración guardada.", Toast.LENGTH_SHORT).show()
        finish()
    }

    // -------------------------------------------------------------
    // ⭐️ FUNCIÓN DE GUARDADO (Ahora llama a la lógica del anuncio) ⭐️
    // -------------------------------------------------------------

    private fun saveConfigAndCheckAd() {
        val ciudadSeleccionada = spinnerCiudad.selectedItem.toString()
        val grupoSeleccionado = spinnerGrupo.selectedItem.toString()

        // Guarda las preferencias del usuario
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("CiudadSeleccionada", ciudadSeleccionada)
            putString("GrupoSeleccionado", grupoSeleccionado)
            apply()
        }

        // ⭐️ Llama al chequeo de AdMob. finish() se ejecuta dentro de esta función. ⭐️
        checkAndShowInterstitialOnSave()
    }

    // -------------------------------------------------------------
    // ... EL RESTO DEL CÓDIGO PERMANECE IGUAL ...
    // -------------------------------------------------------------

    private fun loadReadNodeConfig() {
        // Lee el nodo de Firebase ('produccion' o 'prueba') seleccionado por el administrador.
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        readNode = sharedPref.getString(PREFS_NODE_KEY, "produccion") ?: "produccion"
    }

    private fun setupSpinners() {
        // Configura el Spinner de Grupo (opciones fijas)
        spinnerGrupo.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opcionesGrupos).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Configura el Spinner de Ciudad (usa la lista ordenada dinámicamente)
        spinnerCiudad.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, nombresCiudades).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        loadSavedConfig() // Añadido para asegurar que la configuración guardada se cargue después de configurar los adapters
    }

    private fun loadSavedConfig() {
        // Carga la última ciudad y grupo seleccionados por el usuario
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCity = sharedPref.getString("CiudadSeleccionada", nombresCiudades.firstOrNull())
        val savedGroup = sharedPref.getString("GrupoSeleccionado", "1")

        // Selecciona la ciudad en el Spinner
        val cityIndex = nombresCiudades.indexOf(savedCity)
        if (cityIndex >= 0) spinnerCiudad.setSelection(cityIndex)

        // Selecciona el grupo en el Spinner
        val groupIndex = opcionesGrupos.indexOf(savedGroup)
        if (groupIndex >= 0) spinnerGrupo.setSelection(groupIndex)
    }


    // -------------------------------------------------------------
    // LÓGICA AVANZADA DE SINCRONIZACIÓN Y DESCARGA CONDICIONAL
    // -------------------------------------------------------------

    private fun checkAndDownloadRates() {
        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false // Deshabilita mientras verifica

        val dbRef = FirebaseDatabase.getInstance().reference.child(readNode)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Lectura de versiones locales de SharedPreferences
        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f).toDouble()
        val localHistoricalVersion = sharedPrefs.getFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f).toDouble()

        // Leer versiones remotas (RTDB)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteActualVersion = snapshot.child("version_actual").getValue(Double::class.java) ?: 0.0
                val remoteHistoricalVersion = snapshot.child("version_historica").getValue(Double::class.java) ?: 0.0

                // TOLERANCIA: Usamos 0.001 para evitar errores de precisión de punto flotante
                val tolerance = 0.001
                val areVersionsPracticallyEqual = abs(remoteActualVersion - localActualVersion) < tolerance

                if (areVersionsPracticallyEqual || remoteActualVersion <= localActualVersion) {
                    // Caso A: Ya estás al día o la versión local es más nueva.
                    onRatesCheckCompleted(true, "Tarifas al día.")
                    return
                }

                // 1. CHECK DE PROMOCIÓN EFICIENTE: ¿El V_H remoto coincide con el V_A local?
                val needsPromotion = abs(remoteHistoricalVersion - localActualVersion) < tolerance

                if (needsPromotion) {
                    // ➡️ CASO 1: V_H == L_A. Solo movemos el archivo y descargamos el nuevo.
                    promoteAndPerformSingleDownload(remoteActualVersion, remoteHistoricalVersion)
                    return
                }

                // 2. CHECK DE INTEGRIDAD DEL HISTORIAL: ¿Coincide el historial local con el remoto?
                val isLocalHistoricalCorrect = abs(remoteHistoricalVersion - localHistoricalVersion) < tolerance

                if (isLocalHistoricalCorrect) {
                    // ➡️ CASO 2: INTEGRIDAD OK. Solo descargamos V_A.
                    performSingleDownload(remoteActualVersion)
                    return
                }

                // 3. FALLBACK: SALTO DE VERSIÓN O INICIALIZACIÓN
                if (remoteHistoricalVersion > 0.0) {
                    // ➡️ CASO 3: L_H es incorrecto. Forzamos la descarga doble de V_H y V_A.
                    performConditionalDoubleDownload(remoteHistoricalVersion, localHistoricalVersion, remoteActualVersion)
                    return
                }

                // Default: Solo descarga el actual si V_H es 0.0
                performSingleDownload(remoteActualVersion)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ConfigActivity, "Error de Firebase: ${error.message}", Toast.LENGTH_LONG).show()
                onRatesCheckCompleted(false, "Error al verificar versión.")
            }
        })
    }

    // FUNCIÓN: Maneja la descarga doble si hay salto de versión o instalación limpia
    private fun performConditionalDoubleDownload(remoteHistoricalVersion: Double, localHistoricalVersion: Double, actualVersion: Double) {

        // La descarga es necesaria porque la versión local es anterior al historial remoto.
        val historicalDownloadNeeded = remoteHistoricalVersion > 0.0 && localHistoricalVersion < remoteHistoricalVersion

        if (historicalDownloadNeeded) {
            // Muestra el progreso de la descarga doble
            Toast.makeText(this, "Descargando histórico (v${String.format("%.1f", remoteHistoricalVersion)}) y actual...", Toast.LENGTH_LONG).show()

            val historicalVersionString = String.format("%.1f", remoteHistoricalVersion)
            val historicalFileName = "tarifas_v$historicalVersionString.json"

            // 1. Descargar el Histórico (V_H) y guardarlo como RATES_FILE_OLD_NAME
            downloadFile(historicalFileName, RATES_FILE_OLD_NAME, remoteHistoricalVersion)
                .addOnSuccessListener {
                    // 2. Si el Histórico se descargó, descargamos el Actual (V_A)
                    performSingleDownload(actualVersion)
                }
                .addOnFailureListener {
                    // Si falla el histórico, al menos descarga el actual
                    performSingleDownload(actualVersion)
                }
        } else {
            // Si V_H es 0.0 o la comprobación falló, descarga solo el actual.
            performSingleDownload(actualVersion)
        }
    }

    // FUNCIÓN: Promoción Local Eficiente (Copia local y descarga única)
    private fun promoteAndPerformSingleDownload(remoteActualVersion: Double, remoteHistoricalVersion: Double) {
        val currentFile = File(applicationContext.filesDir, RATES_FILE_NAME)
        val oldFile = File(applicationContext.filesDir, RATES_FILE_OLD_NAME)

        // 1. Copia V_A (V1.5) a V_H (V1.5) antes de la descarga
        if (currentFile.exists()) {
            currentFile.copyTo(oldFile, overwrite = true)
        }

        // 2. Actualizar el número de versión histórica en SharedPreferences
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putFloat(RATE_VERSION_HISTORICAL_KEY, remoteHistoricalVersion.toFloat())
            apply()
        }

        // 3. Proceder con la descarga única de la versión actual
        performSingleDownload(remoteActualVersion)
    }

    // FUNCIÓN: Descarga Única (Actualización Normal)
    private fun performSingleDownload(actualVersion: Double) {
        val actualVersionString = String.format("%.1f", actualVersion)
        val actualFileName = "tarifas_v$actualVersionString.json"

        downloadFile(actualFileName, RATES_FILE_NAME, actualVersion)
    }


    // FUNCIÓN DE DESCARGA GENÉRICA (Maneja la conexión a Storage)
    private fun downloadFile(remoteFileName: String, localFileName: String, newVersion: Double): Task<*> {
        val readNode = readNode
        val storageRef = FirebaseStorage.getInstance().reference
        val storagePath = "rates-$readNode"
        val fileRef = storageRef.child("$storagePath/$remoteFileName")

        val localFile = File(applicationContext.filesDir, localFileName)

        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false // Asegurar que el botón esté desactivado durante la descarga

        return fileRef.getFile(localFile)
            .addOnSuccessListener {
                // Si es la versión actual, actualiza SharedPreferences con el nuevo número (RATE_VERSION_KEY)
                if (localFileName == RATES_FILE_NAME) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putFloat(RATE_VERSION_KEY, newVersion.toFloat())
                        apply()
                    }
                    onRatesCheckCompleted(true, "¡Tarifas actualizadas a v${String.format("%.1f", newVersion)}!")
                }
                // Si es la versión histórica, actualiza SharedPreferences con el número histórico (RATE_VERSION_HISTORICAL_KEY)
                else if (localFileName == RATES_FILE_OLD_NAME) {
                    getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                        putFloat(RATE_VERSION_HISTORICAL_KEY, newVersion.toFloat())
                        apply()
                    }
                }
                // Recarga el RateManager para que los datos nuevos estén disponibles
                RateManager.loadRates(this)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fallo al descargar $remoteFileName.", Toast.LENGTH_LONG).show()
                if (localFileName == RATES_FILE_NAME) {
                    onRatesCheckCompleted(false, "Usando tarifas locales antiguas.")
                }
            }
    }

    // FUNCIÓN DE FINALIZACIÓN (Llamada al final de cualquier descarga de RATES_FILE_NAME)
    private fun onRatesCheckCompleted(success: Boolean, message: String) {
        progressBar.visibility = View.GONE
        btnGuardarConfig.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        loadSavedConfig()
    }
}