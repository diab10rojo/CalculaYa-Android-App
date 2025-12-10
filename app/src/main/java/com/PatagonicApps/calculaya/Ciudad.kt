package com.PatagonicApps.calculaya

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class Ciudad(
    val nombre: String,

    //SABADO DE DIA IGUAL A DOMINGO?
    val sEsD: Boolean = true,

    //PUNTO RETIRO
    val puntoRetiroDia: Int=560,
    val puntoRetiroNoche: Int=835,
    val puntoRetiroFindeNoche: Int=1335,
    val puntoRetiroFindeDia: Int=680,


    //PUNTO ENTREGA
    val puntoEntregaDia: Int=285,
    val puntoEntregaNoche: Int=500,
    val puntoEntregaFindeNoche: Int=775,
    val puntoEntregaFindeDia: Int=420,


    //EXTRA POR GRUPO RETIRO (CASI SIEMPRE  0)-----------------
    val extraPRGrupo1: Int=0,
    val extraPRGrupo2: Int=0,
    val extraPRGrupo3: Int=0,
    val extraPRGrupo4: Int=0,
    val extraPRGrupo5: Int=0,
    val extraPRGrupo6: Int=0,
    val extraPRGrupo7: Int=0,
    val extraPRGrupo8: Int=0,

    //EXTRA POR GRUPO ENTREGA
    val extraPEGrupo1: Int=275,
    val extraPEGrupo2: Int=175,
    val extraPEGrupo3: Int=105,
    val extraPEGrupo4: Int=55,
    val extraPEGrupo5: Int=0,
    val extraPEGrupo6: Int=0,
    val extraPEGrupo7: Int=0,
    val extraPEGrupo8: Int=105,

    //DISTANCIAS DE RETIRO
    val distancia1Retiro: Float=2f,
    val distancia2Retiro: Float=2f,
    val distancia3Retiro: Float=2f,
    val distancia4Retiro: Float=9999f,

    //DISTANCIAS DE ENTREGA
    val distancia1Entrega: Float=2.0f,
    val distancia2Entrega: Float=2.0f,
    val distancia3Entrega: Float=2.0f,
    val distancia4Entrega: Float=9999.0f,



    //MONTO POR KM AL PUNTO DE RETIRO
    val tramo1Retiro: Int=190,
    val tramo2Retiro: Int=375,
    val tramo3Retiro: Int=585,
    val tramo4Retiro: Int=655,

    //MONTO POR KM AL PUNTO DE ENTREGA
    val tramo1Entrega: Int=215,
    val tramo2Entrega: Int=375,
    val tramo3Entrega: Int=585,
    val tramo4Entrega: Int=655,


    //EXTRA DE GRUPO POR KM A RETIRO (CASI SIEMPRE 0)---------------
    val extraKmRGrupo1: Int=0,
    val extraKmRGrupo2: Int=0,
    val extraKmRGrupo3: Int=0,
    val extraKmRGrupo4: Int=0,
    val extraKmRGrupo5: Int=0,
    val extraKmRGrupo6: Int=0,
    val extraKmRGrupo7: Int=0,
    val extraKmRGrupo8: Int=0,

    //EXTRA DE GRUPO POR KM A ENTREGA
    val extraKmEGrupo1: Int=0,
    val extraKmEGrupo2: Int=0,
    val extraKmEGrupo3: Int=0,
    val extraKmEGrupo4: Int=0,
    val extraKmEGrupo5: Int=0,
    val extraKmEGrupo6: Int=0,
    val extraKmEGrupo7  : Int=0,
    val extraKmEGrupo8: Int=0,




)


