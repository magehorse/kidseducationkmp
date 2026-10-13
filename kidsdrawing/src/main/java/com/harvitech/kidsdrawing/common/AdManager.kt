package com.harvitech.kidsdrawing.common

import android.app.Activity
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

object AdManager {

    private const val TAG = "AdManager"

    // Live AdMob banner unit — same unit is reused on every screen.
    private const val LIVE_BANNER_AD_UNIT_ID = "ca-app-pub-9716860748494124/1443052483"

    val BANNER_AD_UNIT_ID: String
        get() = LIVE_BANNER_AD_UNIT_ID

    /**
     * Inflates an adaptive banner into [container] and loads it.
     * Call this once from an Activity's onCreate, after setContentView.
     */
    fun loadBanner(activity: Activity, container: FrameLayout) {
        if (activity.isFinishing || activity.isDestroyed) return

        applyBottomInsetPadding(container)
        container.removeAllViews()
        container.visibility = android.view.View.GONE // hide until it actually loads

        val adView = AdView(activity)
        adView.adUnitId = BANNER_AD_UNIT_ID
        adView.setAdSize(getAdaptiveBannerSize(activity))
        container.addView(adView)

        // Same leak as the main app module had: nothing was ever calling
        // adView.destroy() when a screen closed. Tying it to the Activity's
        // own lifecycle fixes every drawing-module screen at once.
        if (activity is LifecycleOwner) {
            activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onDestroy(owner: LifecycleOwner) {
                    adView.destroy()
                    owner.lifecycle.removeObserver(this)
                }
            })
        }

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Banner loaded.")
                container.visibility = android.view.View.VISIBLE
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                // Don't leave an empty container taking up space - and log
                // *why* so a "banner not showing" report is diagnosable.
                Log.e(TAG, "Banner failed to load: ${adError.message} (code ${adError.code})")
                container.visibility = android.view.View.GONE
            }
        }

        adView.loadAd(AdRequest.Builder().build())
    }

    /**
     * Pads a bottom-pinned view (the banner ad container) by the navigation
     * bar's height, so the ad is never drawn underneath a gesture nav bar.
     * Needed on every screen because this app targets SDK 35+, where
     * Android 15+ forces edge-to-edge regardless of whether enableEdgeToEdge()
     * was called. On an older/non-edge-to-edge device this is a harmless
     * no-op (the reported inset is 0, since the system already reserves
     * that space itself).
     */
    fun applyBottomInsetPadding(bottomView: View) {
        val originalLeft = bottomView.paddingLeft
        val originalRight = bottomView.paddingRight
        val originalTop = bottomView.paddingTop

        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(bottomView) { view, insets ->
            val navBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            view.setPadding(originalLeft, originalTop, originalRight, navBars.bottom)
            insets
        }
    }

    private fun getAdaptiveBannerSize(activity: Activity): AdSize {
        val display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val density = outMetrics.density
        var adWidthPixels = outMetrics.widthPixels.toFloat()
        if (adWidthPixels == 0f) {
            adWidthPixels = activity.resources.displayMetrics.widthPixels.toFloat()
        }

        val adWidth = (adWidthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }
}
