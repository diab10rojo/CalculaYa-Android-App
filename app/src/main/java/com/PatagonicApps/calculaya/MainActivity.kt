package com.PatagonicApps.calculaya

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import java.util.Locale

/**
 * Actividad principal de la aplicación CalculaYa
 *
 * Funcionalidades:
 * - Permite calcular montos de envío basados en distancias y configuración del usuario
 * - Muestra/oculta detalles del cálculo
 * - Integración con Firebase Remote Config para control de anuncios y actualizaciones
 * - Gestión de banners publicitarios (AdMob)
 */
class MainActivity : AppCompatActivity() {

    // ==================== CONSTANTES ====================

    private val PREFS_NAME = "AppConfig"

    // ==================== VISTAS ====================

    // Campos de entrada
    private lateinit var distanciaLocalET: EditText
    private lateinit var distanciaEntregaET: EditText

    // Botones
    private lateinit var botonCalcular: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var btnVerDetalles: Button

    // Textos informativos
    private lateinit var textMonto: TextView
    private lateinit var tvCiudad: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var tvDetalleBase: TextView

    // Contenedores y publicidad
    private lateinit var layoutDetalles: androidx.cardview.widget.CardView
    private lateinit var adView: AdView

    // ==================== ESTADO DE LA ACTIVIDAD ====================

    // Almacena los detalles del último cálculo realizado
    private var lastCalculationDetails: List<CostDetail> = emptyList()

    // Controla si el panel de detalles está visible u oculto
    private var detallesVisibles = false

    // ==================== CICLO DE VIDA ====================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar configuración remota de Firebase
        RemoteConfigHelper.init()

        // Vincular todas las vistas del layout con las variables
        initializeViews()

        // Cargar banner publicitario inicial
        adView.loadAd(AdRequest.Builder().build())

        // Cargar tarifas desde Firebase (necesario si es la primera vez que se abre la app)
        RateManager.loadRates(this)

        // Obtener configuración remota y aplicar reglas de anuncios/actualizaciones
        fetchRemoteConfig()

        // Configurar listeners de los botones
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        // Actualizar la UI con la configuración guardada del usuario cada vez que vuelve a la actividad
        loadUserConfig()
    }

    // ==================== INICIALIZACIÓN ====================

    private fun initializeViews() {
        distanciaLocalET = findViewById(R.id.editTextDistanciaLocal)
        distanciaEntregaET = findViewById(R.id.editTextDistanciaEntrega)
        botonCalcular = findViewById(R.id.buttonCalcular)
        textMonto = findViewById(R.id.tVMonto)
        btnSettings = findViewById(R.id.btnSettings)
        tvCiudad = findViewById(R.id.tVCiudad)
        tvGrupo = findViewById(R.id.tVGrupo)
        btnVerDetalles = findViewById(R.id.btnVerDetalles)
        adView = findViewById(R.id.adViewBannerBottom)
        layoutDetalles = findViewById(R.id.layoutDetalles)
        tvDetalleBase = findViewById(R.id.tvDetalleBase)
    }

    // ==================== CONFIG REMOTA ====================

    private fun fetchRemoteConfig() {
        RemoteConfigHelper.fetchAndActivate {
            AdsManager.showBanner(adView, RemoteConfigHelper.isAdsEnabled())

            val minVersion = RemoteConfigHelper.getMinVersionCode()
            val currentVersion = BuildConfig.VERSION_CODE

            if (currentVersion < minVersion || RemoteConfigHelper.isForceUpdateRequired()) {
                UpdateManager.checkForUpdate(this)
            }
        }
    }

    // ==================== LISTENERS ====================

    private fun setupListeners() {
        // Botón de cálculo: obtiene valores y calcula el monto estimado
        botonCalcular.setOnClickListener {
            performCalculation()
            hideKeyboard() // 🔹 cerrar teclado al calcular
        }

        // Botón de detalles: alterna entre mostrar/ocultar el desglose del cálculo
        btnVerDetalles.setOnClickListener {
            toggleDetailsPanel()
        }

        // Botón de configuración: abre la pantalla de ajustes
        btnSettings.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
    }

    // ==================== LÓGICA DE NEGOCIO ====================

    private fun performCalculation() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val ciudad = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupoStr = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
        val grupo = grupoStr.toIntOrNull() ?: 1

        val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f
        val distanciaEntrega = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f

        val result = RateCalculator.calcularMonto(ciudad, grupo, distanciaLocal, distanciaEntrega)

        lastCalculationDetails = result.detalles

        textMonto.text = "Monto Estimado: $${String.format(Locale.getDefault(), "%.2f", result.total)}"

        btnVerDetalles.visibility = View.VISIBLE
        btnVerDetalles.text = "Ver Detalles"
        layoutDetalles.visibility = View.GONE
        detallesVisibles = false
    }

    private fun toggleDetailsPanel() {
        if (!detallesVisibles) {
            layoutDetalles.visibility = View.VISIBLE
            DetailsPanel.update(tvDetalleBase, lastCalculationDetails)
            btnVerDetalles.text = "Ocultar Detalles"
            detallesVisibles = true
        } else {
            layoutDetalles.visibility = View.GONE
            btnVerDetalles.text = "Ver Detalles"
            detallesVisibles = false
        }
    }

    private fun loadUserConfig() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciudad = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupo = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"

        tvCiudad.text = "Ciudad: $ciudad"
        tvGrupo.text = "Grupo: $grupo"
    }

    // ==================== UTILIDADES ====================

    /**
     * Oculta el teclado si hay una vista con foco
     */
    private fun hideKeyboard() {
        val view: View? = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
