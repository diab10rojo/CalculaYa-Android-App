package com.PatagonicApps.calculaya

import android.content.Context
import com.google.android.gms.ads.AdView

object AdsManager {
    fun showBanner(adView: AdView, enabled: Boolean) {
        if (enabled) {
            adView.visibility = android.view.View.VISIBLE
            adView.resume()
        } else {
            adView.visibility = android.view.View.GONE
            adView.pause()
        }
    }
}
