package com.PatagonicApps.calculaya

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CostDetail(
    @SerializedName("description") val description: String,
    @SerializedName("amount") val amount: Float
)
