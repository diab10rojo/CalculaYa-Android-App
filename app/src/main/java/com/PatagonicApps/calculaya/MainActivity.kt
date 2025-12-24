package com.PatagonicApps.calculaya

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.firebase.messaging.FirebaseMessaging
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // ==================== CONSTANTES ====================
    private val PREFS_NAME = "AppConfig"

    // ==================== VISTAS ====================
    private lateinit var distanciaLocalET: EditText
    private lateinit var distanciaEntregaET: EditText
    private lateinit var botonCalcular: Button
    private lateinit var btnSettings: ImageButton
    private lateinit var btnVerDetalles: Button
    private lateinit var textMonto: TextView
    private lateinit var tvCiudad: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var tvDetalleBase: TextView
    private lateinit var layoutDetalles: androidx.cardview.widget.CardView
    private lateinit var adView: AdView
    private lateinit var imgButtonPlus: ImageButton
    private lateinit var imgButtonMinus: ImageView
    private lateinit var layoutButtonsPlusMinus: LinearLayout

    // 🔹 Pedidos Agrupados
    private lateinit var layoutExtraDeliveries: LinearLayout
    private lateinit var btnAddDeliveryPoint: Button
    private lateinit var radioGroupMode: RadioGroup
    private lateinit var rbSimple: RadioButton
    private lateinit var rbGrouped: RadioButton

    // ==================== ESTADO ====================
    private var lastCalculationDetails: List<CostDetail> = emptyList()
    private var detallesVisibles = false
    private var isGroupMode = false
    private var lastFocusedEditText: EditText? = null

    // ==================== CICLO DE VIDA ====================
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Suscripción a topic de FCM
        FirebaseMessaging.getInstance().subscribeToTopic("general")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Suscripción al topic general exitosa")
                }
            }

        // ✅ Token FCM solo en DEBUG
        if (BuildConfig.DEBUG) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("FCM", "Token: ${task.result}")
                } else {
                    Log.e("FCM", "Error al obtener el token", task.exception)
                }
            }
        }

        RemoteConfigHelper.init()
        initializeViews()
        adView.loadAd(AdRequest.Builder().build())
        RateManager.loadRates(this)
        fetchRemoteConfig()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserConfig()
    }

    // ✅ AGREGADO: Limpieza de memoria
    override fun onDestroy() {
        super.onDestroy()
        lastFocusedEditText = null // Evitar memory leak
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
        imgButtonPlus = findViewById(R.id.imgButtonPlus)
        imgButtonMinus = findViewById(R.id.imgButtonMinus)
        layoutButtonsPlusMinus = imgButtonPlus.parent.parent as LinearLayout

        layoutExtraDeliveries = findViewById(R.id.layoutExtraDeliveries)
        btnAddDeliveryPoint = findViewById(R.id.btnAddDeliveryPoint)
        radioGroupMode = findViewById(R.id.radioGroupMode)
        rbSimple = findViewById(R.id.rbSimple)
        rbGrouped = findViewById(R.id.rbGrouped)

        lastFocusedEditText = distanciaEntregaET
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
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            if (hasFocus && v is EditText) {
                lastFocusedEditText = v
            }
        }
        distanciaLocalET.onFocusChangeListener = focusListener
        distanciaEntregaET.onFocusChangeListener = focusListener

        botonCalcular.setOnClickListener {
            if (isGroupMode) {
                performGroupedCalculation()
            } else {
                performSimpleCalculation()
            }
            hideKeyboard()
        }

        radioGroupMode.setOnCheckedChangeListener { _, checkedId ->
            isGroupMode = (checkedId == R.id.rbGrouped)
            toggleGroupModeUI()
        }

        btnAddDeliveryPoint.setOnClickListener {
            addExtraDeliveryField()
        }

        btnVerDetalles.setOnClickListener {
            toggleDetailsPanel()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }

        imgButtonPlus.setOnClickListener {
            incrementDistancia()
        }

        imgButtonMinus.setOnClickListener {
            decrementDistancia()
        }
    }

    // ==================== UI LOGIC ====================
    private fun toggleGroupModeUI() {
        if (isGroupMode) {
            layoutExtraDeliveries.visibility = View.VISIBLE
            btnAddDeliveryPoint.visibility = View.VISIBLE
            layoutButtonsPlusMinus.visibility = View.GONE

            if (layoutExtraDeliveries.childCount == 0) {
                addExtraDeliveryField()
            }
        } else {
            layoutExtraDeliveries.visibility = View.GONE
            btnAddDeliveryPoint.visibility = View.GONE
            layoutExtraDeliveries.removeAllViews()
            layoutButtonsPlusMinus.visibility = View.VISIBLE
            lastFocusedEditText = distanciaEntregaET
        }
    }

    private fun addExtraDeliveryField() {
        val count = layoutExtraDeliveries.childCount + 2

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                40.dpToPx()
            ).apply {
                topMargin = 10.dpToPx()
            }
        }

        val textView = TextView(this).apply {
            text = "Distancia al punto de entrega $count"
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                180.dpToPx(),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val editText = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 0)
            setIncludeFontPadding(false)
            setBackgroundColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                50.dpToPx(),
                25.dpToPx()
            )

            onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
                if (hasFocus && v is EditText) {
                    lastFocusedEditText = v
                }
            }
        }

        val unitTextView = TextView(this).apply {
            text = "km"
            setTextColor(Color.BLACK)
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        container.addView(textView)
        container.addView(editText)
        container.addView(unitTextView)
        layoutExtraDeliveries.addView(container)
    }

    private fun Int.dpToPx(): Int {
        val density = resources.displayMetrics.density
        return (this * density).toInt()
    }

    // ==================== OPERACIONES DE BOTONES +/- ====================
    private fun incrementDistancia() {
        val target = lastFocusedEditText ?: distanciaEntregaET
        val currentText = target.text.toString()
        val currentValue = currentText.toFloatOrNull() ?: 0f
        val newValue = currentValue + 1
        target.setText(String.format(Locale.US, "%.0f", newValue))
        target.setSelection(target.text.length)
    }

    private fun decrementDistancia() {
        val target = lastFocusedEditText ?: distanciaEntregaET
        val currentText = target.text.toString()
        val currentValue = currentText.toFloatOrNull() ?: 0f
        if (currentValue > 0) {
            val newValue = currentValue - 1
            target.setText(String.format(Locale.US, "%.0f", newValue))
            target.setSelection(target.text.length)
        }
    }

    // ==================== LÓGICA DE NEGOCIO ====================
    private fun performSimpleCalculation() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciudad = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupoStr = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
        val grupo = grupoStr.toIntOrNull() ?: 1

        val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f
        val distanciaEntrega = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f

        val result = RateCalculator.calcularMonto(ciudad, grupo, distanciaLocal, distanciaEntrega)
        lastCalculationDetails = result.detalles

        textMonto.text = "Monto Estimado: $${String.format(Locale.getDefault(), "%.2f", result.total)}"
        showResults()
    }

    private fun performGroupedCalculation() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ciudad = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
        val grupoStr = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
        val grupo = grupoStr.toIntOrNull() ?: 1

        val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f

        val distancias = mutableListOf<Float>()

        val distancia1 = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f
        if (distancia1 > 0) distancias.add(distancia1)

        for (i in 0 until layoutExtraDeliveries.childCount) {
            val container = layoutExtraDeliveries.getChildAt(i) as? LinearLayout
            val editText = container?.getChildAt(1) as? EditText
            val valor = editText?.text?.toString()?.toFloatOrNull()
            if (valor != null && valor > 0) {
                distancias.add(valor)
            }
        }

        if (distancias.isEmpty()) {
            Toast.makeText(this, "Ingrese al menos una distancia de entrega", Toast.LENGTH_SHORT).show()
            return
        }

        val result = RateCalculator.calcularMontoAgrupado(ciudad, grupo, distanciaLocal, distancias)
        lastCalculationDetails = result.detalles

        textMonto.text = "Monto Agrupado: $${String.format(Locale.getDefault(), "%.2f", result.total)}"
        showResults()
    }

    private fun showResults() {
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
    private fun hideKeyboard() {
        val view: View? = currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}