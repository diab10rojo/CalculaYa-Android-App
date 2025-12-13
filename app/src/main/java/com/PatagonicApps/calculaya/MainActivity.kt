package com.PatagonicApps.calculaya

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.PatagonicApps.calculaya.BuildConfig




class MainActivity : AppCompatActivity() {

    val v = BuildConfig.VERSION_CODE

    private lateinit var distanciaLocalET: EditText
    private lateinit var distanciaEntregaET: EditText
    private lateinit var botonCalcular: Button
    private lateinit var textMonto: TextView
    private lateinit var btnSettings: ImageButton
    private lateinit var tvCiudad: TextView
    private lateinit var tvGrupo: TextView
    private lateinit var btnVerDetalles: Button
    private lateinit var adView: AdView
    private lateinit var layoutDetalles: androidx.cardview.widget.CardView
    private lateinit var tvDetalleBase: TextView

    private var lastCalculationDetails: List<CostDetail> = emptyList()
    private var detallesVisibles = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializar Remote Config
        RemoteConfigHelper.init()

        // Vinculación de vistas
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

        adView.loadAd(AdRequest.Builder().build())

        // Fetch Remote Config
        RemoteConfigHelper.fetchAndActivate {
            AdsManager.showBanner(adView, RemoteConfigHelper.isAdsEnabled())

            val minVersion = RemoteConfigHelper.getMinVersionCode()
            val currentVersion = BuildConfig.VERSION_CODE

            if (currentVersion < minVersion || RemoteConfigHelper.isForceUpdateRequired()) {
                // Lanzar actualización forzada
                UpdateManager.checkForUpdate(this)
            }
        }

        botonCalcular.setOnClickListener {
            val ciudad = "Trelew" // leer de prefs
            val grupo = 1
            val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f
            val distanciaEntrega = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f

            val result = RateCalculator.calcularMonto(ciudad, grupo, distanciaLocal, distanciaEntrega)
            lastCalculationDetails = result.detalles
            textMonto.text = "Monto Estimado: $${String.format("%.2f", result.total)}"

            // Mostrar botón de detalles y resetear panel
            btnVerDetalles.visibility = View.VISIBLE
            btnVerDetalles.text = "Ver Detalles"
            layoutDetalles.visibility = View.GONE
            detallesVisibles = false
        }

        btnVerDetalles.setOnClickListener {
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

        btnSettings.setOnClickListener {
            startActivity(Intent(this, ConfigActivity::class.java))
        }
    }
}
