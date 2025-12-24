package com.PatagonicApps.calculaya

import android.os.Build
import android.text.Html
import android.widget.TextView

/**
 * Panel de visualización de detalles del cálculo de tarifas
 *
 * Genera una vista formateada en HTML con el desglose completo de:
 * - Cada componente del costo (retiro, entrega, tramos, extras)
 * - Monto individual de cada concepto
 * - Total general calculado
 *
 * Utiliza formato HTML para dar estilo visual (negrita, saltos de línea)
 * compatible con todas las versiones de Android.
 */
object DetailsPanel {

    private const val DETAIL_FORMAT = "• %s: <b>$%.2f</b><br/>"
    private const val MINIMUM_AMOUNT = 0.01f

    fun update(tvDetalleBase: TextView, detalles: List<CostDetail>) {
        val builder = StringBuilder()
        var totalGeneral = 0f

        // ==================== RETIRO ====================
        val retiro = detalles.filter {
            it.description.contains("Retiro") ||
                    it.description == "Punto de Retiro"
        }

        if (retiro.isNotEmpty()) {
            builder.append("<b>RETIRO:</b><br/>")
            retiro.forEach { d ->
                if (d.amount > MINIMUM_AMOUNT) {
                    builder.append(String.format(DETAIL_FORMAT, d.description, d.amount))
                    totalGeneral += d.amount
                }
            }
            builder.append("----------------------------<br/>")
        }

        // ==================== ENTREGAS ====================
        val entregas = detalles.filter {
            it.description.contains("Entrega") ||
                    it.description.contains("Punto de Entrega")
        }

        if (entregas.isNotEmpty()) {
            // Detectar modo agrupado: buscar "Entrega N -" o "Punto de Entrega N" (con número)
            val esAgrupado = entregas.any {
                it.description.matches(Regex(".*Entrega \\d+ -.*")) ||
                        it.description.matches(Regex("Punto de Entrega \\d+"))
            }

            if (esAgrupado) {
                // ==================== MODO AGRUPADO ====================
                var entregaActual = -1

                entregas.forEach { d ->
                    // Buscar número de entrega en cualquier patrón
                    val matchConGuion = Regex("Entrega (\\d+) -").find(d.description)
                    val matchPuntoEntrega = Regex("Punto de Entrega (\\d+)").find(d.description)

                    val nro = matchConGuion?.groupValues?.get(1)?.toIntOrNull()
                        ?: matchPuntoEntrega?.groupValues?.get(1)?.toIntOrNull()

                    // Cambiar de sección si es un nuevo número de entrega
                    if (nro != null && nro != entregaActual) {
                        if (entregaActual != -1) {
                            builder.append("----------------------------<br/>")
                        }
                        builder.append("<b>ENTREGA $nro:</b><br/>")
                        entregaActual = nro
                    }

                    // Limpiar el texto para mostrarlo
                    var limpio = d.description
                        .replace(Regex("Entrega \\d+ - "), "")  // Quitar "Entrega N - "
                        .replace(Regex("Punto de Entrega \\d+"), "Punto de Entrega")  // Normalizar

                    if (d.amount > MINIMUM_AMOUNT) {
                        builder.append(String.format(DETAIL_FORMAT, limpio, d.amount))
                        totalGeneral += d.amount
                    }
                }
                builder.append("----------------------------<br/>")
            } else {
                // ==================== MODO SIMPLE ====================
                // ✅ AGREGAR TÍTULO "ENTREGA:" EN NEGRITA
                builder.append("<b>ENTREGA:</b><br/>")

                entregas.forEach { d ->
                    if (d.amount > MINIMUM_AMOUNT) {
                        builder.append(String.format(DETAIL_FORMAT, d.description, d.amount))
                        totalGeneral += d.amount
                    }
                }
                builder.append("----------------------------<br/>")
            }
        }

        // ==================== EXTRAS (GRUPO, ETC.) ====================
        val extras = detalles.filter {
            it.description.contains("Extra Grupo") &&
                    !it.description.matches(Regex(".*Entrega \\d+ -.*"))
        }

        if (extras.isNotEmpty()) {
            extras.forEach { d ->
                if (d.amount > MINIMUM_AMOUNT) {
                    builder.append(String.format(DETAIL_FORMAT, d.description, d.amount))
                    totalGeneral += d.amount
                }
            }
            builder.append("----------------------------<br/>")
        }

        // ==================== TOTAL ====================
        if (totalGeneral > 0f) {
            builder.append("Total Estimado: <b>$%.2f</b>".format(totalGeneral))
        }

        // ==================== HORA DE CÁLCULO ====================
        val horaCalculo = detalles.find { it.description.startsWith("Hora de cálculo") }
        if (horaCalculo != null) {
            builder.append("<br/><br/><i>${horaCalculo.description}</i>")
        }

        // ==================== RENDERIZAR HTML ====================
        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(builder.toString().trim(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(builder.toString().trim())
        }

        tvDetalleBase.text = htmlText
    }
}