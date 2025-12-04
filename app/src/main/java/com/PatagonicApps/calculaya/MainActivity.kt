package com.PatagonicApps.calculaya

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.jakewharton.threetenabp.AndroidThreeTen
import org.threeten.bp.LocalDateTime
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import kotlin.math.min

// ⭐️ NECESARIO PARA FORMATO BOLD Y SALTO DE LÍNEA HTML ⭐️
import android.text.Html
import android.os.Build

// La constante IS_ADMIN_BUILD debe estar definida en Constants.kt


/**
 * Activity Principal: Contiene la interfaz de la calculadora y el motor de tarifas.
 */
class MainActivity : AppCompatActivity() {

    // Declaraciones de Vistas principales
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

    // ⭐️ DECLARACIONES PARA EL PANEL DE DETALLES (Desglose) ⭐️
    private lateinit var btnVerDetalles: Button
    private lateinit var layoutDetalles: View
    private lateinit var tvDetalleBase: TextView // Contiene el desglose final
    private lateinit var tvDetalleTramos: TextView
    private lateinit var tvDetalleExtras: TextView

    // ⭐️ VARIABLE PARA ALMACENAR EL DESGLOSE DE CÁLCULO (lastCalculationDetails) ⭐️
    private var lastCalculationDetails: MutableList<CostDetail> = mutableListOf()


    private var currentRunnable: Runnable? = null
    private val PREFS_NAME = "AppConfig"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Inicialización de la librería de tiempo (Anti-Crash)
        try {
            AndroidThreeTen.init(this)
        } catch (e: Exception) {
            // Ignorar si ya está inicializado
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // ⭐️ CARGA CRÍTICA DE TARIFAS: Carga la versión local o el fallback estático ⭐️
        RateManager.loadRates(this)

        // 2. Vinculación de Vistas
        distanciaLocalET = findViewById(R.id.editTextDistanciaLocal)
        distanciaEntregaET = findViewById(R.id.editTextDistanciaEntrega)
        botonCalcular = findViewById(R.id.buttonCalcular)
        botonMas = findViewById(R.id.imgButtonPlus)
        botonMenos = findViewById(R.id.imgButtonMinus)
        textMonto = findViewById(R.id.tVMonto)
        btnSettings = findViewById(R.id.btnSettings)
        tvCiudad = findViewById(R.id.tVCiudad)
        tvGrupo = findViewById(R.id.tVGrupo)

        // Vinculación de botones y panel de detalles
        btnAdminAccess = findViewById(R.id.btnAdminAccess)
        btnVerDetalles = findViewById(R.id.btnVerDetalles)
        layoutDetalles = findViewById(R.id.layoutDetalles)
        tvDetalleBase = findViewById(R.id.tvDetalleBase)
        tvDetalleTramos = findViewById(R.id.tvDetalleTramos)
        tvDetalleExtras = findViewById(R.id.tvDetalleExtras)


        // Configuraciones de inicio y Lógica de visibilidad
        distanciaLocalET.setText("0")
        distanciaEntregaET.setText("0")

        if (IS_ADMIN_BUILD) {
            btnAdminAccess.visibility = View.VISIBLE
        }

        // 3. Listeners de Foco (Limpia el "0" al empezar a escribir para evitar "01")
        distanciaLocalET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeEditText = distanciaLocalET
                if (distanciaLocalET.text.toString() == "0") {
                    distanciaLocalET.setText("")
                }
            } else {
                if (distanciaLocalET.text.toString().isEmpty()) {
                    distanciaLocalET.setText("0")
                }
            }
        }
        distanciaEntregaET.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activeEditText = distanciaEntregaET
                if (distanciaEntregaET.text.toString() == "0") {
                    distanciaEntregaET.setText("")
                }
            } else {
                if (distanciaEntregaET.text.toString().isEmpty()) {
                    distanciaEntregaET.setText("0")
                }
            }
        }

        // 4. Configuración de Botones de Navegación
        btnSettings.setOnClickListener {
            val intent = Intent(this, ConfigActivity::class.java)
            startActivity(intent)
        }

        btnAdminAccess.setOnClickListener {
            val intent = Intent(this, AdminActivity::class.java)
            startActivity(intent)
        }

        // ⭐️ LISTENER DEL BOTÓN DE DETALLES (Expande/Contrae el desglose) ⭐️
        btnVerDetalles.setOnClickListener {
            toggleDetallesPanel()
        }


        // Lógica de los botones +/- (Touch Listeners) ... [Tu código existente]
        botonMas.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (activeEditText != null) {
                        val currentValue = activeEditText!!.text.toString().toFloatOrNull() ?: 0f
                        activeEditText!!.setText(String.format("%.2f", currentValue + 0.01f))

                        currentRunnable = object : Runnable {
                            override fun run() {
                                val value = activeEditText!!.text.toString().toFloatOrNull() ?: 0f
                                activeEditText!!.setText(String.format("%.2f", value + 0.01f))
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
                    if (activeEditText != null) {
                        val currentValue = activeEditText!!.text.toString().toFloatOrNull() ?: 0f
                        activeEditText!!.setText(String.format("%.2f", currentValue - 0.01f))

                        currentRunnable = object : Runnable {
                            override fun run() {
                                val value = activeEditText!!.text.toString().toFloatOrNull() ?: 0f
                                activeEditText!!.setText(String.format("%.2f", value - 0.01f))
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

        // Lógica del botón Calcular (Motor de la aplicación)
        botonCalcular.setOnClickListener {
            var total: Float = 0f

            // Limpiar y ocultar al recalcular
            layoutDetalles.visibility = View.GONE
            lastCalculationDetails.clear()

            // 💡 2. LEER CONFIGURACIÓN GUARDADA 💡
            val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val ciudadSeleccionada = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"
            val grupoString = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"
            val grupo = grupoString.toInt()

            // 🎯 3. BUSCAR MONTOS EN LA LISTA DEL MANAGER ⭐️
            val montosCiudad = RateManager.listaCiudades.find { it.nombre == ciudadSeleccionada }
            if (montosCiudad == null) {
                Toast.makeText(this, "Error: Datos de tarifas para $ciudadSeleccionada no encontrados. Intente actualizar en Configuración.", Toast.LENGTH_LONG).show()
                btnVerDetalles.visibility = View.GONE
                return@setOnClickListener
            }

            // 4. CONTINUAR CON TU LÓGICA DE CÁLCULO
            val distanciaLocal = distanciaLocalET.text.toString().toFloatOrNull() ?: 0f
            val distanciaEntrega = distanciaEntregaET.text.toString().toFloatOrNull() ?: 0f

            // ... (Lógica de determinación de Fecha y Hora) ...

            val ahora = LocalDateTime.now()
            val horaActual = ahora.hour
            var diaDeCobro = ahora.dayOfWeek
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val horaFormateada = ahora.format(formatter)

            if (horaActual < 7) {
                diaDeCobro = diaDeCobro.minus(1)
            }

            // 💡 LÓGICA DE TIEMPO Y DÍA 💡
            val esDomingo = (diaDeCobro == DayOfWeek.SUNDAY)
            val esSabado = (diaDeCobro == DayOfWeek.SATURDAY)
            val esHorarioNocturno = (horaActual < 7 || horaActual >= 20)

            val esTarifaMaxima = esHorarioNocturno && (diaDeCobro == DayOfWeek.FRIDAY || esSabado || esDomingo)


            // 🚨 CÁLCULO DEL MONTO FIJO EXTRA POR GRUPO (extraPEGrupoX)
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

            // 🚨 CÁLCULO DEL EXTRA POR KM DEL GRUPO (extraKmEGrupoX)
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

                // 🎯 PASO 1: DEFINICIÓN DE TARIFAS BASE SEGÚN DÍA EFECTIVO Y HORA
                val tarifaBaseRetiro: Float
                val tarifaBaseEntrega: Float

                val esSabadoDia = esSabado && !esHorarioNocturno

                when {
                    // Caso A: TARIFA MÁXIMA (VIERNES/SÁBADO/DOMINGO Noche)
                    esTarifaMaxima -> {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroFindeNoche.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaFindeNoche.toFloat()
                    }

                    // Caso B: DOMINGO DÍA (Tarifa Fin de Semana Día)
                    esDomingo && !esHorarioNocturno -> {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroFindeDia.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaFindeDia.toFloat()
                    }

                    // ⭐️ CASO C CORREGIDO: MANEJO DEL SÁBADO DE DÍA ⭐️
                    esSabadoDia -> {
                        if (montosCiudad.sEsD) {
                            tarifaBaseRetiro = montosCiudad.puntoRetiroFindeDia.toFloat()
                            tarifaBaseEntrega = montosCiudad.puntoEntregaFindeDia.toFloat()
                        } else {
                            tarifaBaseRetiro = montosCiudad.puntoRetiroDia.toFloat()
                            tarifaBaseEntrega = montosCiudad.puntoEntregaDia.toFloat()
                        }
                    }

                    // Caso D: LUNES a JUEVES Noche (20-07)
                    esHorarioNocturno -> {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroNoche.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaNoche.toFloat()
                    }

                    // Caso E (ELSE): Lunes a Viernes DÍA
                    else -> {
                        tarifaBaseRetiro = montosCiudad.puntoRetiroDia.toFloat()
                        tarifaBaseEntrega = montosCiudad.puntoEntregaDia.toFloat()
                    }
                }

                // ⭐️ REGISTRO DE BASES FIJAS ⭐️
                if (tarifaBaseRetiro > 0) {
                    lastCalculationDetails.add(CostDetail("Punto de Retiro (Base)", tarifaBaseRetiro))
                }
                if (tarifaBaseEntrega > 0) {
                    lastCalculationDetails.add(CostDetail("Punto de Entrega (Base)", tarifaBaseEntrega))
                }


                var totalRetiro: Float = tarifaBaseRetiro
                var totalEntrega: Float = tarifaBaseEntrega

                // Lógica para Tramos de Retiro
                var distanciaRestanteLocal = distanciaLocal
                val tramosRetiro = listOf(montosCiudad.tramo1Retiro, montosCiudad.tramo2Retiro, montosCiudad.tramo3Retiro, montosCiudad.tramo4Retiro)
                val limitesRetiro = listOf(montosCiudad.distancia1Retiro, montosCiudad.distancia2Retiro, montosCiudad.distancia3Retiro, montosCiudad.distancia4Retiro)

                for (i in 0 until 4) {
                    if (distanciaRestanteLocal > 0) {
                        val limiteKm = limitesRetiro.getOrElse(i) { 2.0f }
                        val montoKm = tramosRetiro.getOrElse(i) { 640 }

                        val tramoKm = minOf(distanciaRestanteLocal, limiteKm)
                        val costoTramo = tramoKm * montoKm
                        totalRetiro += costoTramo
                        distanciaRestanteLocal -= tramoKm

                        // ⭐️ REGISTRO DE TRAMO DE RETIRO ⭐️
                        if (costoTramo > 0) {
                            lastCalculationDetails.add(CostDetail("Tramo Retiro ${i+1} (${String.format("%.2f", tramoKm)} km)", costoTramo))
                        }
                    }
                }

                // Lógica de Tramos de Entrega (Aplica el extraKmEntrega)
                var distanciaRestanteEntrega = distanciaEntrega
                val tramosEntrega = listOf(montosCiudad.tramo1Entrega, montosCiudad.tramo2Entrega, montosCiudad.tramo3Entrega, montosCiudad.tramo4Entrega)
                val limitesEntrega = listOf(montosCiudad.distancia1Entrega, montosCiudad.distancia2Entrega, montosCiudad.distancia3Entrega, montosCiudad.distancia4Entrega)

                for (i in 0 until 4) {
                    if (distanciaRestanteEntrega > 0) {
                        val limiteKm = limitesEntrega.getOrElse(i) { 2.0f }
                        val montoBaseKm = tramosEntrega.getOrElse(i) { 640 }

                        val tramoKm = minOf(distanciaRestanteEntrega, limiteKm)
                        val costoKmUnitario = montoBaseKm.toFloat() + extraKmEntrega
                        val costoTramo = tramoKm * costoKmUnitario

                        // ⭐️ APLICACIÓN DEL EXTRA POR KM DEL GRUPO ⭐️
                        totalEntrega += costoTramo
                        distanciaRestanteEntrega -= tramoKm

                        // ⭐️ REGISTRO DE TRAMO DE ENTREGA ⭐️
                        if (costoTramo > 0) {
                            val extraLabel = if (extraKmEntrega > 0) " (+ $extraKmEntrega/km)" else ""
                            lastCalculationDetails.add(CostDetail("Tramo Entrega ${i+1} (${String.format("%.2f", tramoKm)} km)$extraLabel", costoTramo))
                        }
                    }
                }

                // ⭐️ REGISTRO DE EXTRA GRUPO FIJO ⭐️
                if (montoFijoExtraGrupo > 0) {
                    lastCalculationDetails.add(CostDetail("Extra Grupo Fijo", montoFijoExtraGrupo))
                }

                // 🎯 PASO 3: CÁLCULO FINAL
                total = totalRetiro + totalEntrega + montoFijoExtraGrupo

            } else if (ciudadSeleccionada == "CABA") {
                total = 0f // Placeholder para CABA
            }

            Toast.makeText(this, "Hora de cálculo: $horaFormateada\nEl total es: $total", Toast.LENGTH_LONG).show()
            textMonto.text = "Monto Estimado: $${String.format("%.2f",total)}"

            // ⭐️ HACER EL BOTÓN DE DETALLES VISIBLE ⭐️
            btnVerDetalles.visibility = View.VISIBLE
        }

        // 5. Lectura de Configuración en onResume para mostrar Ciudad y Grupo
        leerYMostrarConfiguracion()


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // -------------------------------------------------------------
    // ⭐️ LÓGICA DE PANEL EXPANDIBLE ⭐️
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
        // Combinamos la salida detallada en el TextView principal (tvDetalleBase)
        val builder = StringBuilder()
        var totalGeneral: Float = 0f

        // Usamos un formato limpio para el desglose: "• Descripción: $XX.XX"
        val format = "• %s: <b>$%.2f</b><br/>"

        // Itera sobre los detalles almacenados, aplicando el formato solicitado
        for (detail in lastCalculationDetails) {
            // Filtramos elementos con costo cero (o casi cero)
            if (detail.amount > 0.01f || detail.amount < -0.01f) {

                // ⭐️ LÓGICA DE LIMPIEZA Y REORDENAMIENTO DE ETIQUETAS ⭐️
                val cleanedDescription = when {
                    // 1. Quitar "(Base)"
                    detail.description.contains("Punto de Retiro (Base)") -> "Punto de Retiro"
                    detail.description.contains("Punto de Entrega (Base)") -> "Punto de Entrega"

                    // 2. Reordenar "Tramo Retiro 1" a "Tramo 1 Retiro"
                    detail.description.startsWith("Tramo Retiro") -> detail.description.replace("Tramo Retiro ", "Tramo ") + " Retiro"
                    detail.description.startsWith("Tramo Entrega") -> detail.description.replace("Tramo Entrega ", "Tramo ") + " Entrega"

                    // 3. Otros casos (Extra Grupo Fijo, etc.)
                    else -> detail.description
                }

                builder.append(String.format(format, cleanedDescription, detail.amount))
                totalGeneral += detail.amount
            }
        }

        // ⭐️ AÑADIR SEPARADOR Y TOTAL ⭐️
        if (totalGeneral != 0f) {
            builder.append("<br/>") // Salto antes del separador
            builder.append("----------------------------")
            builder.append("<br/>") // Salto después del separador

            // ⭐️ Usar <b> para el total ⭐️
            val totalFormat = "Total Estimado: <b>$%.2f</b>"
            builder.append(String.format(totalFormat, totalGeneral))
        }

        // ⭐️ USAR HTML.FROMHTML para interpretar los tags <b> y <br/> ⭐️
        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(builder.toString().trim(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(builder.toString().trim())
        }

        // Mostramos todo el desglose en el tvDetalleBase.
        tvDetalleBase.text = htmlText

        // Ocultamos los otros TextViews ya que todo el desglose está en tvDetalleBase
        tvDetalleTramos.visibility = View.GONE
        tvDetalleExtras.visibility = View.GONE
    }


    // -------------------------------------------------------------
    // FUNCIÓN PARA LEER Y MOSTRAR LA CONFIGURACIÓN GUARDADA (FIXED: Muestra el grupo correcto)
    // -------------------------------------------------------------
    override fun onResume() {
        super.onResume()
        // ⭐️ RECARGAMOS las tarifas por si ConfigActivity las actualizó
        RateManager.loadRates(this)
        leerYMostrarConfiguracion()
    }

    private fun leerYMostrarConfiguracion() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Lectura de la Ciudad (Clave: CiudadSeleccionada)
        val ciudadSeleccionada = sharedPref.getString("CiudadSeleccionada", "Trelew") ?: "Trelew"

        // Lectura del Grupo (Clave: GrupoSeleccionado)
        val grupoSeleccionado = sharedPref.getString("GrupoSeleccionado", "1") ?: "1"

        // 1. Muestra la Ciudad
        tvCiudad.text = "Ciudad: $ciudadSeleccionada"

        // 2. Muestra el Grupo (⭐️ CORRECCIÓN APLICADA: Muestra el número de grupo ⭐️)
        tvGrupo.text = "Grupo: $grupoSeleccionado"
    }
}