package com.PatagonicApps.calculaya

/**
 * Clase de datos para almacenar el desglose de cada ítem de costo.
 */
data class CostDetail(
    val description: String,
    val amount: Float
)