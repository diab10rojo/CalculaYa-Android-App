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
        val retiro = detalles.filter { it.description.contains("Retiro") || it.description.contains("Punto de Retiro") }
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
        var entregaActual = -1
        detalles.filter { it.description.contains("Entrega") }
            .forEach { d ->
                val match = Regex("Entrega (\\d+)").find(d.description)
                val nro = match?.groupValues?.get(1)?.toIntOrNull()
                if (nro != null && nro != entregaActual) {
                    if (entregaActual != -1) builder.append("----------------------------<br/>")
                    builder.append("<b>ENTREGA $nro:</b><br/>")
                    entregaActual = nro
                }
                val limpio = d.description.replace("Entrega $entregaActual - ", "")
                if (d.amount > MINIMUM_AMOUNT) {
                    builder.append(String.format(DETAIL_FORMAT, limpio, d.amount))
                    totalGeneral += d.amount
                }
            }

        // ==================== TOTAL ====================
        if (totalGeneral > 0f) {
            builder.append("----------------------------<br/>")
            builder.append("Total Estimado: <b>$%.2f</b>".format(totalGeneral))
        }

        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(builder.toString().trim(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(builder.toString().trim())
        }

        tvDetalleBase.text = htmlText
    }
}


/**
 * Ejemplo de salida generada:
 *
 * • Punto de Retiro: $150.00
 * • Punto de Entrega: $180.00
 * • Tramo Retiro 1 (2.00 km): $100.00
 * • Tramo Entrega 1 (1.50 km): $90.00
 * • Extra Grupo Fijo: $50.00
 *
 * ----------------------------
 * Total Estimado: $570.00
 */