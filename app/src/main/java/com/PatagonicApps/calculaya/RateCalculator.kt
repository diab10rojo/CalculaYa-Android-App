package com.PatagonicApps.calculaya

import org.threeten.bp.LocalDateTime
import org.threeten.bp.DayOfWeek
import org.threeten.bp.format.DateTimeFormatter
import kotlin.math.min

object RateCalculator {

    fun calcularMonto(
        ciudadSeleccionada: String,
        grupo: Int,
        distanciaLocal: Float,
        distanciaEntrega: Float
    ): CalculationResult {
        val detalles = mutableListOf<CostDetail>()

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

        if (horaActual < 7) diaDeCobro = diaDeCobro.minus(1)

        val esDomingo = (diaDeCobro == DayOfWeek.SUNDAY)
        val esSabado = (diaDeCobro == DayOfWeek.SATURDAY)
        val esHorarioNocturno = (horaActual < 7 || horaActual >= 20)
        val esTarifaMaxima = esHorarioNocturno && (diaDeCobro == DayOfWeek.FRIDAY || esSabado || esDomingo)

        // ==================== RECARGOS POR GRUPO ====================
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

        // ==================== TARIFAS BASE SEGÚN DÍA Y HORARIO ====================
        val (tarifaBaseRetiro, tarifaBaseEntrega) = when {
            esTarifaMaxima ->
                montosCiudad.puntoRetiroFindeNoche.toFloat() to montosCiudad.puntoEntregaFindeNoche.toFloat()
            esDomingo && !esHorarioNocturno ->
                montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()
            esSabado && !esHorarioNocturno -> {
                if (montosCiudad.sEsD)
                    montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()
                else
                    montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
            }
            esHorarioNocturno ->
                montosCiudad.puntoRetiroNoche.toFloat() to montosCiudad.puntoEntregaNoche.toFloat()
            else ->
                montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
        }

        // ==================== RETIRO ====================
        var totalRetiro = 0f

        if (tarifaBaseRetiro > 0f) {
            detalles.add(CostDetail("Punto de Retiro", tarifaBaseRetiro))
            totalRetiro += tarifaBaseRetiro
        }

        var distanciaRestanteLocal = distanciaLocal
        val tramosRetiro = listOf(
            montosCiudad.tramo1Retiro.toFloat(),
            montosCiudad.tramo2Retiro.toFloat(),
            montosCiudad.tramo3Retiro.toFloat(),
            montosCiudad.tramo4Retiro.toFloat()
        )
        val limitesRetiro = listOf(
            montosCiudad.distancia1Retiro,
            montosCiudad.distancia2Retiro,
            montosCiudad.distancia3Retiro,
            montosCiudad.distancia4Retiro
        )

        for (i in tramosRetiro.indices) {
            if (distanciaRestanteLocal <= 0f) break
            val limiteKm = limitesRetiro.getOrElse(i) { 2.0f }
            val montoKm = tramosRetiro[i]
            val tramoKm = min(distanciaRestanteLocal, limiteKm)
            val costoTramo = tramoKm * montoKm
            totalRetiro += costoTramo
            distanciaRestanteLocal -= tramoKm
            if (costoTramo > 0f) {
                detalles.add(CostDetail("Tramo Retiro ${i + 1} (${String.format("%.2f", tramoKm)} km)", costoTramo))
            }
        }

        // ==================== ENTREGA ====================
        var totalEntrega = 0f

        if (tarifaBaseEntrega > 0f) {
            detalles.add(CostDetail("Punto de Entrega", tarifaBaseEntrega))
            totalEntrega += tarifaBaseEntrega
        }

        var distanciaRestanteEntrega = distanciaEntrega
        val tramosEntrega = listOf(
            montosCiudad.tramo1Entrega.toFloat(),
            montosCiudad.tramo2Entrega.toFloat(),
            montosCiudad.tramo3Entrega.toFloat(),
            montosCiudad.tramo4Entrega.toFloat()
        )
        val limitesEntrega = listOf(
            montosCiudad.distancia1Entrega,
            montosCiudad.distancia2Entrega,
            montosCiudad.distancia3Entrega,
            montosCiudad.distancia4Entrega
        )

        for (i in tramosEntrega.indices) {
            if (distanciaRestanteEntrega <= 0f) break
            val limiteKm = limitesEntrega.getOrElse(i) { 2.0f }
            val montoBaseKm = tramosEntrega[i]
            val tramoKm = min(distanciaRestanteEntrega, limiteKm)
            val costoKmUnitario = montoBaseKm + extraKmEntrega
            val costoTramo = tramoKm * costoKmUnitario
            totalEntrega += costoTramo
            distanciaRestanteEntrega -= tramoKm
            if (costoTramo > 0f) {
                val extraLabel = if (extraKmEntrega > 0f) " (+ $extraKmEntrega/km)" else ""
                detalles.add(CostDetail("Tramo Entrega ${i + 1} (${String.format("%.2f", tramoKm)} km)$extraLabel", costoTramo))
            }
        }

        // ==================== EXTRA GRUPO - GARANTIZADO ====================
        // 🔴 ESTO SE AGREGA SIEMPRE QUE SEA > 0
        if (montoFijoExtraGrupo > 0f) {
            detalles.add(CostDetail("Extra Grupo Fijo", montoFijoExtraGrupo))
        }

        // ==================== TOTAL ====================
        val total = totalRetiro + totalEntrega + montoFijoExtraGrupo

        detalles.add(CostDetail("Hora de cálculo $horaFormateada", 0f))

        return CalculationResult(total, detalles)
    }

    // ... (función calcularMontoAgrupado sin cambios)

    fun calcularMontoAgrupado(
        ciudadSeleccionada: String,
        grupo: Int,
        distanciaLocal: Float,
        distancias: List<Float>
    ): CalculationResult {
        val detalles = mutableListOf<CostDetail>()

        val montosCiudad = RateManager.listaCiudades.find { it.nombre == ciudadSeleccionada }
            ?: return CalculationResult(
                total = 0f,
                detalles = listOf(CostDetail("Error: ciudad no encontrada ($ciudadSeleccionada)", 0f))
            )

        val ahora = LocalDateTime.now()
        val horaActual = ahora.hour
        var diaDeCobro = ahora.dayOfWeek
        val horaFormateada = ahora.format(DateTimeFormatter.ofPattern("HH:mm"))

        if (horaActual < 7) diaDeCobro = diaDeCobro.minus(1)

        val esDomingo = (diaDeCobro == DayOfWeek.SUNDAY)
        val esSabado = (diaDeCobro == DayOfWeek.SATURDAY)
        val esHorarioNocturno = (horaActual < 7 || horaActual >= 20)
        val esTarifaMaxima = esHorarioNocturno && (diaDeCobro == DayOfWeek.FRIDAY || esSabado || esDomingo)

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

        val (tarifaBaseRetiro, tarifaBaseEntrega) = when {
            esTarifaMaxima ->
                montosCiudad.puntoRetiroFindeNoche.toFloat() to montosCiudad.puntoEntregaFindeNoche.toFloat()
            esDomingo && !esHorarioNocturno ->
                montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()
            esSabado && !esHorarioNocturno -> {
                if (montosCiudad.sEsD)
                    montosCiudad.puntoRetiroFindeDia.toFloat() to montosCiudad.puntoEntregaFindeDia.toFloat()
                else
                    montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
            }
            esHorarioNocturno ->
                montosCiudad.puntoRetiroNoche.toFloat() to montosCiudad.puntoEntregaNoche.toFloat()
            else ->
                montosCiudad.puntoRetiroDia.toFloat() to montosCiudad.puntoEntregaDia.toFloat()
        }

        var total = 0f

        if (tarifaBaseRetiro > 0f) {
            detalles.add(CostDetail("Punto de Retiro", tarifaBaseRetiro))
            total += tarifaBaseRetiro
        }

        var distanciaRestanteLocal = distanciaLocal
        val tramosRetiro = listOf(
            montosCiudad.tramo1Retiro.toFloat(),
            montosCiudad.tramo2Retiro.toFloat(),
            montosCiudad.tramo3Retiro.toFloat(),
            montosCiudad.tramo4Retiro.toFloat()
        )
        val limitesRetiro = listOf(
            montosCiudad.distancia1Retiro,
            montosCiudad.distancia2Retiro,
            montosCiudad.distancia3Retiro,
            montosCiudad.distancia4Retiro
        )
        for (i in tramosRetiro.indices) {
            if (distanciaRestanteLocal <= 0f) break
            val limiteKm = limitesRetiro.getOrElse(i) { 2.0f }
            val montoKm = tramosRetiro[i]
            val tramoKm = min(distanciaRestanteLocal, limiteKm)
            val costoTramo = tramoKm * montoKm
            total += costoTramo
            distanciaRestanteLocal -= tramoKm
            if (costoTramo > 0f) {
                detalles.add(CostDetail("Tramo Retiro ${i + 1} (${String.format("%.2f", tramoKm)} km)", costoTramo))
            }
        }

        distancias.forEachIndexed { index, distancia ->
            if (distancia > 0f) {
                if (tarifaBaseEntrega > 0f) {
                    detalles.add(CostDetail("Punto de Entrega ${index + 1}", tarifaBaseEntrega))
                    total += tarifaBaseEntrega
                }

                var distanciaRestanteEntrega = distancia
                val tramosEntrega = listOf(
                    montosCiudad.tramo1Entrega.toFloat(),
                    montosCiudad.tramo2Entrega.toFloat(),
                    montosCiudad.tramo3Entrega.toFloat(),
                    montosCiudad.tramo4Entrega.toFloat()
                )
                val limitesEntrega = listOf(
                    montosCiudad.distancia1Entrega,
                    montosCiudad.distancia2Entrega,
                    montosCiudad.distancia3Entrega,
                    montosCiudad.distancia4Entrega
                )
                for (i in tramosEntrega.indices) {
                    if (distanciaRestanteEntrega <= 0f) break
                    val limiteKm = limitesEntrega.getOrElse(i) { 2.0f }
                    val montoBaseKm = tramosEntrega[i]
                    val tramoKm = min(distanciaRestanteEntrega, limiteKm)
                    val costoKmUnitario = montoBaseKm + extraKmEntrega
                    val costoTramo = tramoKm * costoKmUnitario
                    total += costoTramo
                    distanciaRestanteEntrega -= tramoKm
                    if (costoTramo > 0f) {
                        val extraLabel = if (extraKmEntrega > 0f) " (+ $extraKmEntrega/km)" else ""
                        detalles.add(
                            CostDetail(
                                "Entrega ${index + 1} - Tramo ${i + 1} (${String.format("%.2f", tramoKm)} km)$extraLabel",
                                costoTramo
                            )
                        )
                    }
                }

                if (montoFijoExtraGrupo > 0f) {
                    detalles.add(CostDetail("Entrega ${index + 1} - Extra Grupo Fijo", montoFijoExtraGrupo))
                    total += montoFijoExtraGrupo
                }
            }
        }

        detalles.add(CostDetail("Hora de cálculo $horaFormateada", 0f))
        return CalculationResult(total, detalles)
    }
}
