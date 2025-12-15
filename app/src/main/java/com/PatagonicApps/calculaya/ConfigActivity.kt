package com.PatagonicApps.calculaya

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
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
 * Activity de configuración de la aplicación
 *
 * Funcionalidades principales:
 * 1. Permitir al usuario seleccionar su ciudad y grupo tarifario
 * 2. Gestionar la sincronización inteligente de tarifas desde Firebase
 * 3. Mostrar anuncios intersticiales según frecuencia configurada
 * 4. Mantener versiones actual e histórica de tarifas
 *
 * Sistema de versiones:
 * - version_actual: Tarifas vigentes que usa la app
 * - version_historica: Versión anterior, respaldo para transiciones
 *
 * Lógica de descarga inteligente:
 * - Solo descarga si hay cambios
 * - Gestiona 2 archivos JSON (actual + histórico)
 * - Promueve versión actual a histórica cuando corresponde
 */
class ConfigActivity : AppCompatActivity() {

    // ==================== CONSTANTES ====================

    // Preferencias compartidas
    private val PREFS_NAME = "AppConfig"

    // Claves para versiones de tarifas
    private val RATE_VERSION_KEY = "TarifaVersionLocal"
    private val RATE_VERSION_HISTORICAL_KEY = "TarifaVersionHistorical"

    // Configuración de anuncios intersticiales
    private val AD_COUNTER_KEY = "AdDisplayCounter"
    private val FREQUENCY_LIMIT = 2  // Mostrar anuncio cada 2 guardados
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6378961597427920/3082064109"
    private val KEY_ADS_LOCAL_ACTIVATED = "ads_local_activated"

    // Configuración de Firebase
    private val PREFS_NODE_KEY = "FirebaseReadNode"

    // Tolerancia para comparar versiones flotantes (evita problemas de precisión)
    private val VERSION_TOLERANCE = 0.001

    // ==================== VISTAS ====================

    private lateinit var spinnerGrupo: Spinner
    private lateinit var spinnerCiudad: Spinner
    private lateinit var btnGuardarConfig: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRateStatus: TextView

    // ==================== ESTADO ====================

    // Anuncio intersticial pre-cargado
    private var mInterstitialAd: InterstitialAd? = null

    // Nodo de Firebase a leer (produccion/desarrollo)
    private var readNode: String = "produccion"

    // Lista de ciudades disponibles (ordenada alfabéticamente)
    private val nombresCiudades: List<String>
        get() = RateManager.listaCiudades
            .map { it.nombre }
            .sorted()

    // Opciones de grupos tarifarios
    private val opcionesGrupos = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    // ==================== CICLO DE VIDA ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        // Inicialización de componentes
        initializeViews()

        // Cargar tarifas almacenadas localmente
        RateManager.loadRates(this)

        // Cargar configuración de nodo Firebase (producción/desarrollo)
        loadReadNodeConfig()

        // Configurar spinners con datos iniciales
        setupSpinners()

        // Verificar y descargar tarifas actualizadas de Firebase
        checkAndDownloadRates()

        // Pre-cargar anuncio intersticial para mostrarlo más tarde
        loadInterstitialAd()

        // Configurar listener del botón guardar
        btnGuardarConfig.setOnClickListener {
            saveConfigAndCheckAd()
        }
    }

    // ==================== INICIALIZACIÓN ====================

    /**
     * Vincula todas las vistas del layout con las variables de la clase
     */
    private fun initializeViews() {
        spinnerCiudad = findViewById(R.id.spinnerCiudadConfig)
        spinnerGrupo = findViewById(R.id.spinnerGrupoConfig)
        btnGuardarConfig = findViewById(R.id.btnGuardarConfig)
        progressBar = findViewById(R.id.progressBarConfig)
        tvRateStatus = findViewById(R.id.tvRateStatus)

        // Ocultar barra de progreso inicialmente
        progressBar.visibility = View.GONE
    }

    /**
     * Carga el nodo de lectura de Firebase desde SharedPreferences
     * Puede ser "produccion" o "desarrollo" para testing
     */
    private fun loadReadNodeConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        readNode = sharedPref.getString(PREFS_NODE_KEY, "produccion") ?: "produccion"
    }

    // ==================== CONFIGURACIÓN DE SPINNERS ====================

    /**
     * Configura los spinners de ciudad y grupo con adaptadores y datos
     * Restaura la selección previamente guardada del usuario
     */
    private fun setupSpinners() {
        // Spinner de grupos (1-8)
        spinnerGrupo.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesGrupos
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Spinner de ciudades (dinámico según tarifas cargadas)
        spinnerCiudad.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresCiudades
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        // Restaurar selección guardada
        loadSavedConfig()
    }

    /**
     * Carga la configuración previamente guardada y la aplica a los spinners
     */
    private fun loadSavedConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCity = sharedPref.getString("CiudadSeleccionada", nombresCiudades.firstOrNull())
        val savedGroup = sharedPref.getString("GrupoSeleccionado", "1")

        // Seleccionar ciudad guardada
        val cityIndex = nombresCiudades.indexOf(savedCity)
        if (cityIndex >= 0) spinnerCiudad.setSelection(cityIndex)

        // Seleccionar grupo guardado
        val groupIndex = opcionesGrupos.indexOf(savedGroup)
        if (groupIndex >= 0) spinnerGrupo.setSelection(groupIndex)
    }

    // ==================== GESTIÓN DE ANUNCIOS ====================

    /**
     * Pre-carga un anuncio intersticial si la monetización está activada
     * El anuncio se mostrará más tarde según la frecuencia configurada
     */
    private fun loadInterstitialAd() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        if (isLocallyActivated) {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.d("AdMob", "Fallo al cargar anuncio: ${adError.message}")
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdMob", "Anuncio cargado exitosamente")
                        mInterstitialAd = interstitialAd
                    }
                }
            )
        } else {
            Log.d("AdMob", "Monetización desactivada. No se carga intersticial")
        }
    }

    /**
     * Verifica si debe mostrar un anuncio intersticial según la frecuencia
     * Solo muestra el anuncio cada FREQUENCY_LIMIT guardados de configuración
     */
    private fun checkAndShowInterstitialOnSave() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        // Si los anuncios están desactivados, finalizar sin mostrar ad
        if (!isLocallyActivated) {
            finishActivityFlow()
            return
        }

        // Incrementar contador de guardados
        var count = sharedPref.getInt(AD_COUNTER_KEY, 0)
        count++

        // Mostrar anuncio si alcanzó la frecuencia y está cargado
        if (count >= FREQUENCY_LIMIT && mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
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
            count = 0  // Reiniciar contador
            loadInterstitialAd()  // Pre-cargar próximo anuncio
        } else {
            finishActivityFlow()
        }

        // Guardar contador actualizado
        sharedPref.edit().putInt(AD_COUNTER_KEY, count).apply()
    }

    /**
     * Finaliza el flujo de la activity mostrando mensaje de confirmación
     */
    private fun finishActivityFlow() {
        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ==================== GUARDADO DE CONFIGURACIÓN ====================

    /**
     * Guarda la configuración seleccionada por el usuario (ciudad y grupo)
     * y luego verifica si debe mostrar un anuncio intersticial
     */
    private fun saveConfigAndCheckAd() {
        val ciudadSeleccionada = spinnerCiudad.selectedItem.toString()
        val grupoSeleccionado = spinnerGrupo.selectedItem.toString()

        // Guardar en SharedPreferences
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("CiudadSeleccionada", ciudadSeleccionada)
            putString("GrupoSeleccionado", grupoSeleccionado)
            apply()
        }

        // Verificar si debe mostrar anuncio antes de cerrar
        checkAndShowInterstitialOnSave()
    }

    // ==================== SINCRONIZACIÓN DE TARIFAS ====================

    /**
     * Verifica las versiones de tarifas en Firebase y decide si descargar actualizaciones
     *
     * Lógica de versiones:
     * - version_actual: La que está usando la app ahora
     * - version_historica: Versión anterior, como respaldo
     *
     * Estrategias de descarga:
     * 1. Si versiones locales están al día → No hace nada
     * 2. Si local_actual == remoto_historico → Promociona actual a histórico y descarga nuevo actual
     * 3. Si histórico local está ok → Solo descarga actual
     * 4. Si histórico local desactualizado → Descarga ambos
     */
    private fun checkAndDownloadRates() {
        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false  // Deshabilitar durante verificación

        val dbRef = FirebaseDatabase.getInstance().reference.child(readNode)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Leer versiones locales almacenadas
        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f).toDouble()
        val localHistoricalVersion = sharedPrefs.getFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f).toDouble()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Leer versiones remotas desde Firebase
                val remoteActualVersion = snapshot.child("version_actual").getValue(Double::class.java) ?: 0.0
                val remoteHistoricalVersion = snapshot.child("version_historica").getValue(Double::class.java) ?: 0.0

                // Comparar con tolerancia para evitar problemas de precisión flotante
                val areVersionsPracticallyEqual = abs(remoteActualVersion - localActualVersion) < VERSION_TOLERANCE

                // CASO 1: Las tarifas locales están actualizadas
                if (areVersionsPracticallyEqual || remoteActualVersion <= localActualVersion) {
                    onRatesCheckCompleted(true, "Tarifas al día")
                    return
                }

                // CASO 2: La versión actual local es ahora la histórica remota
                // → Promover actual a histórico y descargar nueva actual
                val needsPromotion = abs(remoteHistoricalVersion - localActualVersion) < VERSION_TOLERANCE
                if (needsPromotion) {
                    promoteAndPerformSingleDownload(remoteActualVersion, remoteHistoricalVersion)
                    return
                }

                // CASO 3: Histórico local está correcto, solo descargar actual
                val isLocalHistoricalCorrect = abs(remoteHistoricalVersion - localHistoricalVersion) < VERSION_TOLERANCE
                if (isLocalHistoricalCorrect) {
                    performSingleDownload(remoteActualVersion)
                    return
                }

                // CASO 4: Histórico desactualizado, descargar ambos si es necesario
                if (remoteHistoricalVersion > 0.0) {
                    performConditionalDoubleDownload(remoteHistoricalVersion, localHistoricalVersion, remoteActualVersion)
                    return
                }

                // CASO 5: Fallback - solo descargar actual
                performSingleDownload(remoteActualVersion)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@ConfigActivity,
                    "Error de Firebase: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
                // Habilitar botón aunque falle (puede guardar config local)
                onRatesCheckCompleted(false, "Error al verificar versión")
            }
        })
    }

    /**
     * Descarga condicional de histórico + actual
     * Solo descarga histórico si la versión remota es mayor a la local
     */
    private fun performConditionalDoubleDownload(
        remoteHistoricalVersion: Double,
        localHistoricalVersion: Double,
        actualVersion: Double
    ) {
        val historicalDownloadNeeded = remoteHistoricalVersion > 0.0 &&
                localHistoricalVersion < remoteHistoricalVersion

        if (historicalDownloadNeeded) {
            Toast.makeText(
                this,
                "Descargando histórico (v${String.format("%.1f", remoteHistoricalVersion)}) y actual...",
                Toast.LENGTH_LONG
            ).show()

            val historicalVersionString = String.format("%.1f", remoteHistoricalVersion)
            val historicalFileName = "tarifas_v$historicalVersionString.json"

            // Descargar histórico primero, luego actual
            downloadFile(historicalFileName, RateManager.RATES_FILE_OLD_NAME, remoteHistoricalVersion)
                .addOnSuccessListener {
                    performSingleDownload(actualVersion)
                }
                .addOnFailureListener {
                    // Si falla histórico, intentar descargar actual de todas formas
                    performSingleDownload(actualVersion)
                }
        } else {
            performSingleDownload(actualVersion)
        }
    }

    /**
     * Promueve la versión actual a histórica y luego descarga la nueva versión actual
     * Esto ocurre cuando lo que era "actual" ahora es "histórico" en Firebase
     */
    private fun promoteAndPerformSingleDownload(remoteActualVersion: Double, remoteHistoricalVersion: Double) {
        val currentFile = File(applicationContext.filesDir, RateManager.RATES_FILE_NAME)
        val oldFile = File(applicationContext.filesDir, RateManager.RATES_FILE_OLD_NAME)

        // Copiar archivo actual → archivo histórico
        if (currentFile.exists()) {
            currentFile.copyTo(oldFile, overwrite = true)
        }

        // Actualizar versión histórica en preferencias
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putFloat(RATE_VERSION_HISTORICAL_KEY, remoteHistoricalVersion.toFloat())
            apply()
        }

        // Ahora descargar la nueva versión actual
        performSingleDownload(remoteActualVersion)
    }

    /**
     * Descarga solo la versión actual de tarifas
     */
    private fun performSingleDownload(actualVersion: Double) {
        val actualVersionString = String.format("%.1f", actualVersion)
        val actualFileName = "tarifas_v$actualVersionString.json"

        downloadFile(actualFileName, RateManager.RATES_FILE_NAME, actualVersion)
    }

    // ==================== DESCARGA DE ARCHIVOS ====================

    /**
     * Función genérica para descargar un archivo JSON de tarifas desde Firebase Storage
     *
     * @param remoteFileName Nombre del archivo en Storage (ej: "tarifas_v1.5.json")
     * @param localFileName Nombre local del archivo (RATES_FILE_NAME o RATES_FILE_OLD_NAME)
     * @param newVersion Número de versión a guardar en SharedPreferences
     * @return Task para encadenar operaciones
     */
    private fun downloadFile(remoteFileName: String, localFileName: String, newVersion: Double): Task<*> {
        val storageRef = FirebaseStorage.getInstance().reference
        val storagePath = "rates-$readNode"
        val fileRef = storageRef.child("$storagePath/$remoteFileName")

        val localFile = File(applicationContext.filesDir, localFileName)

        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false  // Deshabilitar durante descarga

        return fileRef.getFile(localFile)
            .addOnSuccessListener {
                // Actualizar versión en SharedPreferences según el tipo de archivo
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    // Actualizar versión actual
                    prefs.putFloat(RATE_VERSION_KEY, newVersion.toFloat())
                } else if (localFileName == RateManager.RATES_FILE_OLD_NAME) {
                    // Actualizar versión histórica
                    prefs.putFloat(RATE_VERSION_HISTORICAL_KEY, newVersion.toFloat())
                }
                prefs.apply()

                // Recargar tarifas en memoria desde el archivo descargado
                RateManager.loadRates(this)

                // Si descargamos la versión actual, refrescar spinners con nuevas ciudades
                if (localFileName == RateManager.RATES_FILE_NAME) {
                    setupSpinners()
                }

                // Notificar completado solo para descarga de versión actual
                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(
                        true,
                        "¡Tarifas actualizadas a v${String.format("%.1f", newVersion)}!"
                    )
                }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Fallo al descargar $remoteFileName",
                    Toast.LENGTH_LONG
                ).show()

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(false, "Usando tarifas locales antiguas")
                }
            }
    }

    /**
     * Callback ejecutado al completar la verificación/descarga de tarifas
     * Actualiza la UI con el estado final
     *
     * @param success Indica si la operación fue exitosa
     * @param message Mensaje a mostrar al usuario
     */
    private fun onRatesCheckCompleted(success: Boolean, message: String) {
        progressBar.visibility = View.GONE
        btnGuardarConfig.isEnabled = true  // Siempre habilitar al finalizar
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        // Generar y mostrar estado detallado de tarifas
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f)
        val numCiudadesCargadas = RateManager.listaCiudades.size

        val statusMessage = StringBuilder()

        // Determinar origen de datos
        if (localActualVersion > 0.0f && numCiudadesCargadas > 3) {
            // Tarifas completas de Firebase
            statusMessage.append("✅ Estado: Tarifas locales completas\n")
            statusMessage.append("Versión: ${String.format("%.1f", localActualVersion)}\n")
        } else {
            // Fallback o versión básica
            statusMessage.append("Versión: ${String.format("%.1f", localActualVersion)}\n")
        }

        // Información adicional
        statusMessage.append("Versión de aplicación: ${BuildConfig.VERSION_NAME}\n")
        statusMessage.append("Ciudades en memoria: $numCiudadesCargadas")

        tvRateStatus.text = statusMessage.toString()
    }
}