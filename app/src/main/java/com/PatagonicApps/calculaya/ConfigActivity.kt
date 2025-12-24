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

class ConfigActivity : AppCompatActivity() {

    // ==================== CONSTANTES ====================
    private val PREFS_NAME = "AppConfig"
    private val RATE_VERSION_KEY = "TarifaVersionLocal"
    private val RATE_VERSION_HISTORICAL_KEY = "TarifaVersionHistorical"
    private val AD_COUNTER_KEY = "AdDisplayCounter"
    private val FREQUENCY_LIMIT = 2
    private val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-6378961597427920/3082064109"
    private val PREFS_NODE_KEY = "FirebaseReadNode"
    private val VERSION_TOLERANCE = 0.001

    // ==================== VISTAS ====================
    private lateinit var spinnerGrupo: Spinner
    private lateinit var spinnerCiudad: Spinner
    private lateinit var btnGuardarConfig: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvRateStatus: TextView

    // ==================== ESTADO ====================
    private var mInterstitialAd: InterstitialAd? = null
    private var readNode: String = "produccion"

    private val nombresCiudades: List<String>
        get() = RateManager.listaCiudades.map { it.nombre }.sorted()

    private val opcionesGrupos = listOf("1", "2", "3", "4", "5", "6", "7", "8")

    // ==================== CICLO DE VIDA ====================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config)

        initializeViews()
        RateManager.loadRates(this)
        loadReadNodeConfig()
        setupSpinners()
        checkAndDownloadRates()
        loadInterstitialAd()

        btnGuardarConfig.setOnClickListener {
            saveConfigAndCheckAd()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mInterstitialAd = null // ✅ Evitar memory leak
    }

    // ==================== INICIALIZACIÓN ====================
    private fun initializeViews() {
        spinnerCiudad = findViewById(R.id.spinnerCiudadConfig)
        spinnerGrupo = findViewById(R.id.spinnerGrupoConfig)
        btnGuardarConfig = findViewById(R.id.btnGuardarConfig)
        progressBar = findViewById(R.id.progressBarConfig)
        tvRateStatus = findViewById(R.id.tvRateStatus)
        progressBar.visibility = View.GONE
    }

    private fun loadReadNodeConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        readNode = sharedPref.getString(PREFS_NODE_KEY, "produccion") ?: "produccion"
    }

    // ==================== CONFIGURACIÓN DE SPINNERS ====================
    private fun setupSpinners() {
        spinnerGrupo.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            opcionesGrupos
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerCiudad.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresCiudades
        ).apply {
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

    // ==================== GESTIÓN DE ANUNCIOS ====================
    private fun loadInterstitialAd() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // ✅ Leer cache de Remote Config
        val isAdsEnabled = sharedPref.getBoolean("ads_enabled_cache", false)

        Log.d("AdMob", "🔧 Intentando cargar intersticial. Ads habilitados: $isAdsEnabled")

        if (isAdsEnabled) {
            val adRequest = AdRequest.Builder().build()

            InterstitialAd.load(
                this,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        Log.e("AdMob", "❌ Fallo al cargar intersticial: ${adError.message}")
                        mInterstitialAd = null
                    }

                    override fun onAdLoaded(interstitialAd: InterstitialAd) {
                        Log.d("AdMob", "✅ Intersticial cargado exitosamente")
                        mInterstitialAd = interstitialAd
                    }
                }
            )
        } else {
            Log.w("AdMob", "⚠️ Ads desactivados en Remote Config, no se carga intersticial")
        }
    }

    private fun checkAndShowInterstitialOnSave() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isAdsEnabled = sharedPref.getBoolean("ads_enabled_cache", false)

        if (!isAdsEnabled) {
            Log.w("AdMob", "⚠️ Ads desactivados, saltando intersticial")
            finishActivityFlow()
            return
        }

        var count = sharedPref.getInt(AD_COUNTER_KEY, 0)
        count++

        Log.d("AdMob", "📊 Contador de guardados: $count / $FREQUENCY_LIMIT")

        if (count >= FREQUENCY_LIMIT) {
            if (mInterstitialAd != null) {
                Log.d("AdMob", "🎬 Mostrando intersticial")

                mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("AdMob", "✅ Intersticial cerrado por el usuario")
                        mInterstitialAd = null
                        finishActivityFlow()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e("AdMob", "❌ Error al mostrar intersticial: ${adError.message}")
                        mInterstitialAd = null
                        finishActivityFlow()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d("AdMob", "✅ Intersticial mostrado correctamente")
                    }
                }

                mInterstitialAd?.show(this)
                count = 0
                loadInterstitialAd()
            } else {
                Log.w("AdMob", "⚠️ Intersticial NO cargado, saltando")
                finishActivityFlow()
            }
        } else {
            Log.d("AdMob", "⏳ No alcanza frecuencia todavía ($count/$FREQUENCY_LIMIT)")
            finishActivityFlow()
        }

        sharedPref.edit().putInt(AD_COUNTER_KEY, count).apply()
    }

    private fun finishActivityFlow() {
        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
        finish()
    }

    // ==================== GUARDADO DE CONFIGURACIÓN ====================
    private fun saveConfigAndCheckAd() {
        val ciudadSeleccionada = spinnerCiudad.selectedItem.toString()
        val grupoSeleccionado = spinnerGrupo.selectedItem.toString()

        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putString("CiudadSeleccionada", ciudadSeleccionada)
            putString("GrupoSeleccionado", grupoSeleccionado)
            apply()
        }

        checkAndShowInterstitialOnSave()
    }

    // ==================== SINCRONIZACIÓN DE TARIFAS ====================
    // ... (resto del código sin cambios)
    private fun checkAndDownloadRates() {
        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false

        val dbRef = FirebaseDatabase.getInstance().reference.child(readNode)
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f).toDouble()
        val localHistoricalVersion = sharedPrefs.getFloat(RATE_VERSION_HISTORICAL_KEY, 0.0f).toDouble()

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val remoteActualVersion = snapshot.child("version_actual").getValue(Double::class.java) ?: 0.0
                val remoteHistoricalVersion = snapshot.child("version_historica").getValue(Double::class.java) ?: 0.0

                val areVersionsPracticallyEqual = abs(remoteActualVersion - localActualVersion) < VERSION_TOLERANCE

                if (areVersionsPracticallyEqual || remoteActualVersion <= localActualVersion) {
                    onRatesCheckCompleted(true, "Tarifas al día")
                    return
                }

                val needsPromotion = abs(remoteHistoricalVersion - localActualVersion) < VERSION_TOLERANCE
                if (needsPromotion) {
                    promoteAndPerformSingleDownload(remoteActualVersion, remoteHistoricalVersion)
                    return
                }

                val isLocalHistoricalCorrect = abs(remoteHistoricalVersion - localHistoricalVersion) < VERSION_TOLERANCE
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
                onRatesCheckCompleted(false, "Error al verificar versión")
            }
        })
    }

    private fun performConditionalDoubleDownload(
        remoteHistoricalVersion: Double,
        localHistoricalVersion: Double,
        actualVersion: Double
    ) {
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

    private fun downloadFile(remoteFileName: String, localFileName: String, newVersion: Double): Task<*> {
        val storageRef = FirebaseStorage.getInstance().reference
        val storagePath = "rates-$readNode"
        val fileRef = storageRef.child("$storagePath/$remoteFileName")

        val localFile = File(applicationContext.filesDir, localFileName)

        progressBar.visibility = View.VISIBLE
        btnGuardarConfig.isEnabled = false

        return fileRef.getFile(localFile)
            .addOnSuccessListener {
                val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    prefs.putFloat(RATE_VERSION_KEY, newVersion.toFloat())
                } else if (localFileName == RateManager.RATES_FILE_OLD_NAME) {
                    prefs.putFloat(RATE_VERSION_HISTORICAL_KEY, newVersion.toFloat())
                }
                prefs.apply()

                RateManager.loadRates(this)

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    setupSpinners()
                }

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(true, "¡Tarifas actualizadas a v${String.format("%.1f", newVersion)}!")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Fallo al descargar $remoteFileName", Toast.LENGTH_LONG).show()

                if (localFileName == RateManager.RATES_FILE_NAME) {
                    onRatesCheckCompleted(false, "Usando tarifas locales antiguas")
                }
            }
    }

    private fun onRatesCheckCompleted(success: Boolean, message: String) {
        progressBar.visibility = View.GONE
        btnGuardarConfig.isEnabled = true
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val localActualVersion = sharedPrefs.getFloat(RATE_VERSION_KEY, 0.0f)
        val numCiudadesCargadas = RateManager.listaCiudades.size

        val statusMessage = StringBuilder()

        if (localActualVersion > 0.0f && numCiudadesCargadas > 3) {
            statusMessage.append("✅ Estado: Tarifas locales actualizadas\n")
            statusMessage.append("Versión de tarifas: ${String.format("%.1f", localActualVersion)}\n")
        } else {
            statusMessage.append("Versión: ${String.format("%.1f", localActualVersion)}\n")
        }

        statusMessage.append("Versión de aplicación: ${BuildConfig.VERSION_NAME}\n")
        statusMessage.append("Ciudades en memoria: $numCiudadesCargadas")

        tvRateStatus.text = statusMessage.toString()
    }
}