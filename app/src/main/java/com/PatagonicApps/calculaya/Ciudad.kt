package com.PatagonicApps.calculaya

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class Ciudad(
    @SerializedName("nombre") val nombre: String,

    @SerializedName("sEsD") val sEsD: Boolean = true,

    // PUNTO RETIRO
    @SerializedName("puntoRetiroDia") val puntoRetiroDia: Int = 560,
    @SerializedName("puntoRetiroNoche") val puntoRetiroNoche: Int = 835,
    @SerializedName("puntoRetiroFindeNoche") val puntoRetiroFindeNoche: Int = 1335,
    @SerializedName("puntoRetiroFindeDia") val puntoRetiroFindeDia: Int = 680,

    // PUNTO ENTREGA
    @SerializedName("puntoEntregaDia") val puntoEntregaDia: Int = 285,
    @SerializedName("puntoEntregaNoche") val puntoEntregaNoche: Int = 500,
    @SerializedName("puntoEntregaFindeNoche") val puntoEntregaFindeNoche: Int = 775,
    @SerializedName("puntoEntregaFindeDia") val puntoEntregaFindeDia: Int = 420,

    // Extras por grupo retiro
    @SerializedName("extraPRGrupo1") val extraPRGrupo1: Int = 0,
    @SerializedName("extraPRGrupo2") val extraPRGrupo2: Int = 0,
    @SerializedName("extraPRGrupo3") val extraPRGrupo3: Int = 0,
    @SerializedName("extraPRGrupo4") val extraPRGrupo4: Int = 0,
    @SerializedName("extraPRGrupo5") val extraPRGrupo5: Int = 0,
    @SerializedName("extraPRGrupo6") val extraPRGrupo6: Int = 0,
    @SerializedName("extraPRGrupo7") val extraPRGrupo7: Int = 0,
    @SerializedName("extraPRGrupo8") val extraPRGrupo8: Int = 0,

    // Extras por grupo entrega
    @SerializedName("extraPEGrupo1") val extraPEGrupo1: Int = 275,
    @SerializedName("extraPEGrupo2") val extraPEGrupo2: Int = 175,
    @SerializedName("extraPEGrupo3") val extraPEGrupo3: Int = 105,
    @SerializedName("extraPEGrupo4") val extraPEGrupo4: Int = 55,
    @SerializedName("extraPEGrupo5") val extraPEGrupo5: Int = 0,
    @SerializedName("extraPEGrupo6") val extraPEGrupo6: Int = 0,
    @SerializedName("extraPEGrupo7") val extraPEGrupo7: Int = 0,
    @SerializedName("extraPEGrupo8") val extraPEGrupo8: Int = 105,

    // Distancias retiro
    @SerializedName("distancia1Retiro") val distancia1Retiro: Float = 2f,
    @SerializedName("distancia2Retiro") val distancia2Retiro: Float = 2f,
    @SerializedName("distancia3Retiro") val distancia3Retiro: Float = 2f,
    @SerializedName("distancia4Retiro") val distancia4Retiro: Float = 9999f,

    // Distancias entrega
    @SerializedName("distancia1Entrega") val distancia1Entrega: Float = 2f,
    @SerializedName("distancia2Entrega") val distancia2Entrega: Float = 2f,
    @SerializedName("distancia3Entrega") val distancia3Entrega: Float = 2f,
    @SerializedName("distancia4Entrega") val distancia4Entrega: Float = 9999f,

    // Tramos retiro
    @SerializedName("tramo1Retiro") val tramo1Retiro: Int = 190,
    @SerializedName("tramo2Retiro") val tramo2Retiro: Int = 345,
    @SerializedName("tramo3Retiro") val tramo3Retiro: Int = 570,
    @SerializedName("tramo4Retiro") val tramo4Retiro: Int = 640,

    // Tramos entrega
    @SerializedName("tramo1Entrega") val tramo1Entrega: Int = 190,
    @SerializedName("tramo2Entrega") val tramo2Entrega: Int = 345,
    @SerializedName("tramo3Entrega") val tramo3Entrega: Int = 570,
    @SerializedName("tramo4Entrega") val tramo4Entrega: Int = 640,

    // Extras por km retiro
    @SerializedName("extraKmRGrupo1") val extraKmRGrupo1: Int = 0,
    @SerializedName("extraKmRGrupo2") val extraKmRGrupo2: Int = 0,
    @SerializedName("extraKmRGrupo3") val extraKmRGrupo3: Int = 0,
    @SerializedName("extraKmRGrupo4") val extraKmRGrupo4: Int = 0,
    @SerializedName("extraKmRGrupo5") val extraKmRGrupo5: Int = 0,
    @SerializedName("extraKmRGrupo6") val extraKmRGrupo6: Int = 0,
    @SerializedName("extraKmRGrupo7") val extraKmRGrupo7: Int = 0,
    @SerializedName("extraKmRGrupo8") val extraKmRGrupo8: Int = 0,

    // Extras por km entrega
    @SerializedName("extraKmEGrupo1") val extraKmEGrupo1: Int = 0,
    @SerializedName("extraKmEGrupo2") val extraKmEGrupo2: Int = 0,
    @SerializedName("extraKmEGrupo3") val extraKmEGrupo3: Int = 0,
    @SerializedName("extraKmEGrupo4") val extraKmEGrupo4: Int = 0,
    @SerializedName("extraKmEGrupo5") val extraKmEGrupo5: Int = 0,
    @SerializedName("extraKmEGrupo6") val extraKmEGrupo6: Int = 0,
    @SerializedName("extraKmEGrupo7") val extraKmEGrupo7: Int = 0,
    @SerializedName("extraKmEGrupo8") val extraKmEGrupo8: Int = 0
)
