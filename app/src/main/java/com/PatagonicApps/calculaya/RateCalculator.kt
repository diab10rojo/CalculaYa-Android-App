package com.PatagonicApps.calculaya

import org.threeten.bp.LocalDateTime
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import kotlin.math.min

/**
 * Motor de cálculo de tarifas de envío
 *
 * Calcula el monto total de un envío considerando:
 * - Ciudad seleccionada
 * - Grupo tarifario (1-8)
 * - Distancia de retiro (punto A)
 * - Distancia de entrega (punto B)
 * - Día de la semana y horario (tarifas diferenciadas)
 *
 * Sistema de tarifas por tramos:
 * - Cada tramo de distancia tiene un precio por km diferente
 * - Las tarifas varían según día (laborable/sábado/domingo) y horario (día/noche)
 * - Los grupos tienen recargos adicionales
 */
object RateCalculator {

    /**
     * Calcula el monto total de un envío con desglose detallado
     *
     * @param ciudadSeleccionada Nombre de la ciudad donde se realiza el envío
     * @param grupo Grupo tarifario del comercio (1-8)
     * @param distanciaLocal Distancia desde el local hasta el punto de retiro (en km)
     * @param distanciaEntrega Distancia desde el punto de retiro hasta la entrega final (en km)
     * @return CalculationResult con el total y detalles del cálculo
     */
    fun calcularMonto(
        ciudadSeleccionada: String,
        grupo: Int,
        distanciaLocal: Float,
        distanciaEntrega: Float
    ): CalculationResult {
        // Lista para almacenar cada concepto del cálculo
        val detalles = mutableListOf<CostDetail>()

        // Buscar la configuración de tarifas de la ciudad seleccionada
        val montosCiudad = RateManager.listaCiudades.find { it.nombre == ciudadSeleccionada }
            ?: return CalculationResult(
                total = 0f,
                detalles = listOf(CostDetail("Error: ciudad no encontrada ($ciudadSeleccionada)", 0f))
            )

        // ==================== DETERMINACIÓN DE FRANJA HORARIA ====================

        val ahora = LocalDateTime.now()
        val horaActual = ahora.hour
        var diaDeCobro = ahora.dayOfWeek
        val horaFormateada = ahora.format(DateTimeFormatter.ofPattern("HH:mm"))

        // Si es antes de las 7 AM, se considera del día anterior
        if (horaActual < 7) diaDeCobro = diaDeCobro.minus(1)

        // Determinar el tipo de día y horario
        val esDomingo = (diaDeCobro == DayOfWeek.SUNDAY)
        val esSabado = (diaDeCobro == DayOfWeek.SATURDAY)
        val esHorarioNocturno = (horaActual < 7 || horaActual >= 20)

        // Tarifa máxima: viernes/sábado/domingo en horario nocturno
        val esTarifaMaxima = esHorarioNocturno && (diaDeCobro == DayOfWeek.FRIDAY || esSabado || esDomingo)

        // ==================== RECARGOS POR GRUPO ====================

        // Monto fijo extra según el grupo del comercio
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

        // Extra por kilómetro en tramos de entrega según grupo
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

        // ==================== TARIFAS BASE SEGÚN DÍA Y HORARIO ====================

        // Determinar tarifas base de retiro y entrega según el momento del cálculo
        val (tarifaBaseRetiro, tarifaBaseEntrega) = when {
            // Tarifa máxima: Viernes/Sábado/Domingo noche
            esTarifaMaxima ->
                montosCiudad.puntoRetiroFindeNoche.toFloat() to montosCiudad.puntoEntregaFindeNoche.toFloat()

            // Domingo de día
            esDomingo && !esHorarioNocturno ->
                montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()

            // Sábado de día (puede tener tarifa especial o normal según configuración)
            esSabado && !esHorarioNocturno -> {
                if (montosCiudad.sEsD)
                    montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()
                else
                    montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
            }

            // Horario nocturno entre semana
            esHorarioNocturno ->
                montosCiudad.puntoRetiroNoche.toFloat() to montosCiudad.puntoEntregaNoche.toFloat()

            // Día laborable en horario diurno (tarifa normal)
            else ->
                montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
        }

        // Agregar tarifas base a los detalles
        if (tarifaBaseRetiro > 0f) detalles.add(CostDetail("Punto de Retiro", tarifaBaseRetiro))
        if (tarifaBaseEntrega > 0f) detalles.add(CostDetail("Punto de Entrega", tarifaBaseEntrega))

        // Inicializar totales con las tarifas base
        var totalRetiro = tarifaBaseRetiro
        var totalEntrega = tarifaBaseEntrega

        // ==================== CÁLCULO DE TRAMOS DE RETIRO ====================

        var distanciaRestanteLocal = distanciaLocal

        // Definir los costos por km de cada tramo de retiro
        val tramosRetiro = listOf(
            montosCiudad.tramo1Retiro.toFloat(),
            montosCiudad.tramo2Retiro.toFloat(),
            montosCiudad.tramo3Retiro.toFloat(),
            montosCiudad.tramo4Retiro.toFloat()
        )

        // Definir los límites de km para cada tramo
        val limitesRetiro = listOf(
            montosCiudad.distancia1Retiro, montosCiudad.distancia2Retiro,
            montosCiudad.distancia3Retiro, montosCiudad.distancia4Retiro
        )

        // Calcular costo por cada tramo de retiro
        for (i in tramosRetiro.indices) {
            if (distanciaRestanteLocal <= 0f) break

            val limiteKm = limitesRetiro.getOrElse(i) { 2.0f }
            val montoKm = tramosRetiro[i]

            // Tomar la menor distancia entre lo que queda y el límite del tramo
            val tramoKm = min(distanciaRestanteLocal, limiteKm)
            val costoTramo = tramoKm * montoKm

            totalRetiro += costoTramo
            distanciaRestanteLocal -= tramoKm

            if (costoTramo > 0f) {
                detalles.add(CostDetail(
                    "Tramo Retiro ${i + 1} (${String.format("%.2f", tramoKm)} km)",
                    costoTramo
                ))
            }
        }

        // ==================== CÁLCULO DE TRAMOS DE ENTREGA ====================

        var distanciaRestanteEntrega = distanciaEntrega

        // Definir los costos por km de cada tramo de entrega
        val tramosEntrega = listOf(
            montosCiudad.tramo1Entrega.toFloat(),
            montosCiudad.tramo2Entrega.toFloat(),
            montosCiudad.tramo3Entrega.toFloat(),
            montosCiudad.tramo4Entrega.toFloat()
        )

        // Definir los límites de km para cada tramo
        val limitesEntrega = listOf(
            montosCiudad.distancia1Entrega, montosCiudad.distancia2Entrega,
            montosCiudad.distancia3Entrega, montosCiudad.distancia4Entrega
        )

        // Calcular costo por cada tramo de entrega (incluye extra por grupo)
        for (i in tramosEntrega.indices) {
            if (distanciaRestanteEntrega <= 0f) break

            val limiteKm = limitesEntrega.getOrElse(i) { 2.0f }
            val montoBaseKm = tramosEntrega[i]

            // Tomar la menor distancia entre lo que queda y el límite del tramo
            val tramoKm = min(distanciaRestanteEntrega, limiteKm)

            // Sumar el extra por km del grupo al costo base
            val costoKmUnitario = montoBaseKm + extraKmEntrega
            val costoTramo = tramoKm * costoKmUnitario

            totalEntrega += costoTramo
            distanciaRestanteEntrega -= tramoKm

            if (costoTramo > 0f) {
                val extraLabel = if (extraKmEntrega > 0f) " (+ $extraKmEntrega/km)" else ""
                detalles.add(CostDetail(
                    "Tramo Entrega ${i + 1} (${String.format("%.2f", tramoKm)} km)$extraLabel",
                    costoTramo
                ))
            }
        }

        // ==================== RECARGO FIJO POR GRUPO ====================

        // Agregar el monto fijo extra del grupo si corresponde
        if (montoFijoExtraGrupo > 0f) {
            detalles.add(CostDetail("Extra Grupo Fijo", montoFijoExtraGrupo))
        }

        // ==================== TOTAL FINAL ====================

        // Sumar todos los componentes
        val total = totalRetiro + totalEntrega + montoFijoExtraGrupo

        // Agregar hora del cálculo como información adicional
        detalles.add(CostDetail("Hora de cálculo $horaFormateada", 0f))

        return CalculationResult(total, detalles)
    }
}