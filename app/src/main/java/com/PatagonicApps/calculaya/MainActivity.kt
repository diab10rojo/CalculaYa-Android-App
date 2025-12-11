package com.PatagonicApps.calculaya

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import android.util.Log
import com.google.firebase.remoteconfig.remoteConfig

class MainActivity : AppCompatActivity() {

    // Vistas principales
    private lateinit var distanciaLocalET: EditText
    private lateinit var distanciaEntregaET: EditText
    private lateinit var botonCalcular: Button
    private lateinit var botonMenos: ImageView
    private lateinit var botonMas: ImageButton
    private var activeEditText: EditText? = null
    private lateinit var textMonto: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var tvCiudad: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var btnAdminAccess: Button

    // Banner de AdMob
    private lateinit var adView: AdView

    // Firebase Remote Config
    private val KEY_ADS_FIREBASE_VALUE = "ads_enabled_production"
    private val KEY_ADS_LOCAL_ACTIVATED = "ads_local_activated"

    // Panel de detalles
    private lateinit var btnVerDetalles: Button
    private lateinit var layoutDetalles: View
    private lateinit var tvDetalleBase: TextView
    private lateinit var tvDetalleTramos: TextView
    private lateinit var tvDetalleExtras: TextView

    // Desglose de cálculo
    data class CostDetail(val description: String, val amount: Float)
    private var lastCalculationDetails: MutableList<CostDetail> = mutableListOf()

    private var currentRunnable: Runnable? = null
    private val PREFS_NAME = "AppConfig"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializaciones globales
        try { AndroidThreeTen.init(this) } catch (_: Exception) {}
        MobileAds.initialize(this) {}

        // Configuración de Remote Config
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings { minimumFetchIntervalInSeconds = 3600L }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(mapOf(KEY_ADS_FIREBASE_VALUE to false))

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Cargar tarifas (local o fallback)
        RateManager.loadRates(this)

        // Vinculación de vistas
        distanciaLocalET = findViewById(R.id.editTextDistanciaLocal)
        distanciaEntregaET = findViewById(R.id.editTextDistanciaEntrega)
        botonCalcular = findViewById(R.id.buttonCalcular)
        botonMas = findViewById(R.id.imgButtonPlus)
        botonMenos = findViewById(R.id.imgButtonMinus)
        textMonto = findViewById(R.id.tVMonto)
        btnSettings = findViewById(R.id.btnSettings)
        tvCiudad = findViewById(R.id.tVCiudad)
        tvGrupo = findViewById(R.id.tVGrupo)
        btnAdminAccess = findViewById(R.id.btnAdminAccess)
        btnVerDetalles = findViewById(R.id.btnVerDetalles)
        layoutDetalles = findViewById(R.id.layoutDetalles)
        tvDetalleBase = findViewById(R.id.tvDetalleBase)
        tvDetalleTramos = findViewById(R.id.tvDetalleTramos)
        tvDetalleExtras = findViewById(R.id.tvDetalleExtras)
        adView = findViewById(R.id.adViewBannerBottom)

        // Estado inicial UI
        distanciaLocalET.setText("0")
        distanciaEntregaET.setText("0")
        if (IS_ADMIN_BUILD) btnAdminAccess.visibility = View.VISIBLE

        // Banner y Remote Config
        adView.loadAd(AdRequest.Builder().build())
        applyFirebaseAdConfig()

        // Listeners
        setFocusListeners()
        setIncrementDecrementListeners()
        btnSettings.setOnClickListener { startActivity(Intent(this, ConfigActivity::class.java)) }
        btnAdminAccess.setOnClickListener { startActivity(Intent(this, AdminActivity::class.java)) }
        btnVerDetalles.setOnClickListener { toggleDetallesPanel() }
        botonCalcular.setOnClickListener { calcularMonto() }

        leerYMostrarConfiguracion()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // -------------------------------------------------------------
    // Firebase Ads
    // -------------------------------------------------------------
    private fun applyFirebaseAdConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isLocallyActivated = sharedPref.getBoolean(KEY_ADS_LOCAL_ACTIVATED, false)

        if (isLocallyActivated) {
            if (::adView.isInitialized) {
                adView.visibility = View.VISIBLE
                adView.resume()
            }
            return
        }

        val remoteConfig = Firebase.remoteConfig
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val isAdsEnabledByFirebase = remoteConfig.getBoolean(KEY_ADS_FIREBASE_VALUE)
                    if (::adView.isInitialized) {
                        if (isAdsEnabledByFirebase) {
                            sharedPref.edit().putBoolean(KEY_ADS_LOCAL_ACTIVATED, true).apply()
                            adView.visibility = View.VISIBLE
                            adView.resume()
                        } else {
                            adView.visibility = View.GONE
                            adView.pause()
                        }
                    }
                } else {
                    Log.e("FirebaseConfig", "Fetch fallido: ${task.exception?.message}")
                    if (::adView.isInitialized) {
                        adView.visibility = View.GONE
                        adView.pause()
                    }
                }
            }
    }

    // -------------------------------------------------------------
    // Listeners de foco en EditText
    // -------------------------------------------------------------
    private fun setFocusListeners() {
        distanciaLocalET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeEditText = distanciaLocalET
                if (distanciaLocalET.text.toString() == "0") distanciaLocalET.setText("")
            } else {
                if (distanciaLocalET.text.toString().isEmpty()) distanciaLocalET.setText("0")
            }
        }
        distanciaEntregaET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeEditText = distanciaEntregaET
                if (distanciaEntregaET.text.toString() == "0") distanciaEntregaET.setText("")
            } else {
                if (distanciaEntregaET.text.toString().isEmpty()) distanciaEntregaET.setText("0")
            }
        }
    }

    // -------------------------------------------------------------
    // Botones +/- (touch hold)
    // -------------------------------------------------------------
    private fun setIncrementDecrementListeners() {
        botonMas.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activeEditText?.let { et ->
                        val currentValue = et.text.toString().toFloatOrNull() ?: 0f
                        et.setText(String.format("%.2f", currentValue + 0.01f))

                        currentRunnable = object : Runnable {
                            override fun run() {
                                val value = et.text.toString().toFloatOrNull() ?: 0f
                                et.setText(String.format("%.2f", value + 0.01f))
                                view.postDelayed(this, 5)
                            }
                        }
                        view.postDelayed(currentRunnable, 500)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(currentRunnable)
                    true
                }
                else -> false
            }
        }

        botonMenos.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    activeEditText?.let { et ->
                        val currentValue = et.text.toString().toFloatOrNull() ?: 0f
                        et.setText(String.format("%.2f", currentValue - 0.01f))

                        currentRunnable = object : Runnable {
                            override fun run() {
                                val value = et.text.toString().toFloatOrNull() ?: 0f
                                et.setText(String.format("%.2f", value - 0.01f))
                                view.postDelayed(this, 100)
                            }
                        }
                        view.postDelayed(currentRunnable, 500)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    view.removeCallbacks(currentRunnable)
                    true
                }
                else -> false
            }
        }
    }

    // -------------------------------------------------------------
    // Cálculo principal
    // -------------------------------------------------------------
    private fun calcularMonto() {
        var total: Float = 0f

        // Reiniciar panel de detalles
        layoutDetalles.visibility = View.GONE
        lastCalculationDetails.clear()

        // Config guardada
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciudadSeleccionada = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupoString = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
        val grupo = grupoString.toInt()

        // Buscar montos
        val montosCiudad = RateManager.listaCiudades.find { it.nombre == ciudadSeleccionada }
        if (montosCiudad == null) {
            Toast.makeText(this, "Error: Datos de tarifas para $ciudadSeleccionada no encontrados. Intente actualizar en Configuración.", Toast.LENGTH_LONG).show()
            btnVerDetalles.visibility = View.GONE
            return
        }

        // Entradas
        val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f
        val distanciaEntrega = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f

        // Tiempo actual
        val ahora = LocalDateTime.now()
        val horaActual = ahora.hour
        var diaDeCobro = ahora.dayOfWeek
        val formatter = DateTimeFormatter.ofPattern("HH:mm")
        val horaFormateada = ahora.format(formatter)

        if (horaActual < 7) {
            diaDeCobro = diaDeCobro.minus(1)
        }

        val esDomingo = (diaDeCobro == DayOfWeek.SUNDAY)
        val esSabado = (diaDeCobro == DayOfWeek.SATURDAY)
        val esHorarioNocturno = (horaActual < 7 || horaActual >= 20)
        val esTarifaMaxima = esHorarioNocturno && (diaDeCobro == DayOfWeek.FRIDAY || esSabado || esDomingo)

        // Extras por grupo
        val montoFijoExtraGrupo = when (grupo) {
            1 -> montosCiudad.extraPEGrupo1
            2 -> montosCiudad.extraPEGrupo2
            3 -> montosCiudad.extraPEGrupo3
            4 -> montosCiudad.extraPEGrupo4
            5 -> montosCiudad.extraPEGrupo5
            6 -> montosCiudad.extraPEGrupo6
            7 -> montosCiudad.extraPEGrupo7
            8 -> montosCiudad.extraPEGrupo8
            else -> 0
        }.toFloat()

        val extraKmEntrega = when (grupo) {
            1 -> montosCiudad.extraKmEGrupo1
            2 -> montosCiudad.extraKmEGrupo2
            3 -> montosCiudad.extraKmEGrupo3
            4 -> montosCiudad.extraKmEGrupo4
            5 -> montosCiudad.extraKmEGrupo5
            6 -> montosCiudad.extraKmEGrupo6
            7 -> montosCiudad.extraKmEGrupo7
            8 -> montosCiudad.extraKmEGrupo8
            else -> 0
        }.toFloat()

        if (ciudadSeleccionada == montosCiudad.nombre) {
            // Base según día/hora
            val tarifaBaseRetiro: Float
            val tarifaBaseEntrega: Float
            val esSabadoDia = esSabado && !esHorarioNocturno

            when {
                esTarifaMaxima -> {
                    tarifaBaseRetiro = montosCiudad.puntoRetiroFindeNoche.toFloat()
                    tarifaBaseEntrega = montosCiudad.puntoEntregaFindeNoche.toFloat()
                }
                esDomingo && !esHorarioNocturno -> {
                    tarifaBaseRetiro = montosCiudad.puntoRetiroFindeDia.toFloat()
                    tarifaBaseEntrega = montosCiudad.puntoEntregaFindeDia.toFloat()
                }
                esSabadoDia -> {
                    if (montosCiudad.sEsD) {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroFindeDia.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaFindeDia.toFloat()
                    } else {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroDia.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaDia.toFloat()
                    }
                }
                esHorarioNocturno -> {
                    tarifaBaseRetiro = montosCiudad.puntoRetiroNoche.toFloat()
                    tarifaBaseEntrega = montosCiudad.puntoEntregaNoche.toFloat()
                }
                else -> {
                    tarifaBaseRetiro = montosCiudad.puntoRetiroDia.toFloat()
                    tarifaBaseEntrega = montosCiudad.puntoEntregaDia.toFloat()
                }
            }

            if (tarifaBaseRetiro > 0) lastCalculationDetails.add(CostDetail("Punto de Retiro (Base)", tarifaBaseRetiro))
            if (tarifaBaseEntrega > 0) lastCalculationDetails.add(CostDetail("Punto de Entrega (Base)", tarifaBaseEntrega))

            var totalRetiro: Float = tarifaBaseRetiro
            var totalEntrega: Float = tarifaBaseEntrega

            // Tramos Retiro
            var distanciaRestanteLocal = distanciaLocal
            val tramosRetiro = listOf(
                montosCiudad.tramo1Retiro,
                montosCiudad.tramo2Retiro,
                montosCiudad.tramo3Retiro,
                montosCiudad.tramo4Retiro
            )
            val limitesRetiro = listOf(
                montosCiudad.distancia1Retiro,
                montosCiudad.distancia2Retiro,
                montosCiudad.distancia3Retiro,
                montosCiudad.distancia4Retiro
            )

            for (i in 0 until 4) {
                if (distanciaRestanteLocal > 0) {
                    val limiteKm = limitesRetiro.getOrElse(i) { 2.0f }
                    val montoKm = tramosRetiro.getOrElse(i) { 640 }
                    val tramoKm = minOf(distanciaRestanteLocal, limiteKm)
                    val costoTramo = tramoKm * montoKm
                    totalRetiro += costoTramo
                    distanciaRestanteLocal -= tramoKm

                    if (costoTramo > 0) {
                        lastCalculationDetails.add(
                            CostDetail("Tramo Retiro ${i + 1} (${String.format("%.2f", tramoKm)} km)", costoTramo)
                        )
                    }
                }
            }

            // Tramos Entrega (+ extra por km del grupo)
            var distanciaRestanteEntrega = distanciaEntrega
            val tramosEntrega = listOf(
                montosCiudad.tramo1Entrega,
                montosCiudad.tramo2Entrega,
                montosCiudad.tramo3Entrega,
                montosCiudad.tramo4Entrega
            )
            val limitesEntrega = listOf(
                montosCiudad.distancia1Entrega,
                montosCiudad.distancia2Entrega,
                montosCiudad.distancia3Entrega,
                montosCiudad.distancia4Entrega
            )

            for (i in 0 until 4) {
                if (distanciaRestanteEntrega > 0) {
                    val limiteKm = limitesEntrega.getOrElse(i) { 2.0f }
                    val montoBaseKm = tramosEntrega.getOrElse(i) { 640 }
                    val tramoKm = minOf(distanciaRestanteEntrega, limiteKm)
                    val costoKmUnitario = montoBaseKm.toFloat() + extraKmEntrega
                    val costoTramo = tramoKm * costoKmUnitario

                    totalEntrega += costoTramo
                    distanciaRestanteEntrega -= tramoKm

                    if (costoTramo > 0) {
                        val extraLabel = if (extraKmEntrega > 0) " (+ $extraKmEntrega/km)" else ""
                        lastCalculationDetails.add(
                            CostDetail("Tramo Entrega ${i + 1} (${String.format("%.2f", tramoKm)} km)$extraLabel", costoTramo)
                        )
                    }
                }
            }

            if (montoFijoExtraGrupo > 0) {
                lastCalculationDetails.add(CostDetail("Extra Grupo Fijo", montoFijoExtraGrupo))
            }

            total = totalRetiro + totalEntrega + montoFijoExtraGrupo

        } else if (ciudadSeleccionada == "CABA") {
            // Placeholder específico por ciudad si fuera necesario
            total = 0f
        }

        Toast.makeText(this, "Hora de cálculo: $horaFormateada\nEl total es: $total", Toast.LENGTH_LONG).show()
        textMonto.text = "Monto Estimado: $${String.format("%.2f", total)}"
        btnVerDetalles.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------
    // Panel de detalles
    // -------------------------------------------------------------
    private fun toggleDetallesPanel() {
        if (layoutDetalles.visibility == View.GONE) {
            layoutDetalles.visibility = View.VISIBLE
            updateDetallesPanel()
        } else {
            layoutDetalles.visibility = View.GONE
        }
    }

    private fun updateDetallesPanel() {
        val builder = StringBuilder()
        var totalGeneral: Float = 0f
        val format = "• %s: <b>$%.2f</b><br/>"

        for (detail in lastCalculationDetails) {
            if (detail.amount > 0.01f || detail.amount < -0.01f) {
                val cleanedDescription = when {
                    detail.description.contains("Punto de Retiro (Base)") -> "Punto de Retiro"
                    detail.description.contains("Punto de Entrega (Base)") -> "Punto de Entrega"
                    detail.description.startsWith("Tramo Retiro") ->
                        detail.description.replace("Tramo Retiro ", "Tramo ") + " Retiro"
                    detail.description.startsWith("Tramo Entrega") ->
                        detail.description.replace("Tramo Entrega ", "Tramo ") + " Entrega"
                    else -> detail.description
                }

                builder.append(String.format(format, cleanedDescription, detail.amount))
                totalGeneral += detail.amount
            }
        }

        if (totalGeneral != 0f) {
            builder.append("<br/>----------------------------<br/>")
            builder.append(String.format("Total Estimado: <b>$%.2f</b>", totalGeneral))
        }

        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(builder.toString().trim(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(builder.toString().trim())
        }

        tvDetalleBase.text = htmlText
        tvDetalleTramos.visibility = View.GONE
        tvDetalleExtras.visibility = View.GONE
    }

    // -------------------------------------------------------------
    // Ciclo de vida
    // -------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        RateManager.loadRates(this)
        leerYMostrarConfiguracion()
        applyFirebaseAdConfig()
        if (::adView.isInitialized && adView.visibility == View.VISIBLE) adView.resume()
    }

    override fun onPause() {
        super.onPause()
        if (::adView.isInitialized) adView.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::adView.isInitialized) adView.destroy()
    }

    // -------------------------------------------------------------
    // Configuración guardada (Ciudad y Grupo)
    // -------------------------------------------------------------
    private fun leerYMostrarConfiguracion() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciudadSeleccionada = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupoSeleccionado = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
        tvCiudad.text = "Ciudad: $ciudadSeleccionada"
        tvGrupo.text = "Grupo: $grupoSeleccionado"
    }
}
