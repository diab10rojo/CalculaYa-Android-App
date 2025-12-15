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

    // ==================== CONSTANTES DE FORMATO ====================

    // Formato HTML para cada línea de detalle: • Descripción: $Monto
    private const val DETAIL_FORMAT = "• %s: <b>$%.2f</b><br/>"

    // Umbral mínimo para mostrar un monto (evita mostrar $0.00)
    private const val MINIMUM_AMOUNT = 0.01f

    // ==================== MÉTODO PRINCIPAL ====================

    /**
     * Actualiza el TextView con el desglose detallado del cálculo
     *
     * Recorre todos los detalles del cálculo, filtra los montos relevantes,
     * los formatea en HTML y actualiza el TextView de forma compatible con
     * todas las versiones de Android.
     *
     * @param tvDetalleBase TextView donde se mostrará el desglose
     * @param detalles Lista de CostDetail con cada concepto y su monto
     */
    fun update(tvDetalleBase: TextView, detalles: List<CostDetail>) {
        val builder = StringBuilder()
        var totalGeneral = 0f

        // ==================== CONSTRUCCIÓN DEL DESGLOSE ====================

        // Recorrer cada detalle del cálculo
        for (detail in detalles) {
            // Solo mostrar montos significativos (mayores a $0.01)
            if (detail.amount > MINIMUM_AMOUNT) {
                // Agregar línea formateada: • Concepto: $Monto
                builder.append(String.format(DETAIL_FORMAT, detail.description, detail.amount))

                // Acumular el total
                totalGeneral += detail.amount
            }
        }

        // ==================== AGREGAR TOTAL ====================

        // Si hay montos para mostrar, agregar el total general
        if (totalGeneral != 0f) {
            builder.append("<br/>----------------------------<br/>")
            builder.append("Total Estimado: <b>$%.2f</b>".format(totalGeneral))
        }

        // ==================== CONVERSIÓN HTML ====================

        // Convertir el HTML a texto formateado según la versión de Android
        val htmlText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // Android 7.0 (API 24) o superior: usar método actualizado
            Html.fromHtml(builder.toString().trim(), Html.FROM_HTML_MODE_LEGACY)
        } else {
            // Android anterior a 7.0: usar método legacy
            @Suppress("DEPRECATION")
            Html.fromHtml(builder.toString().trim())
        }

        // ==================== ACTUALIZAR UI ====================

        // Aplicar el texto formateado al TextView
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