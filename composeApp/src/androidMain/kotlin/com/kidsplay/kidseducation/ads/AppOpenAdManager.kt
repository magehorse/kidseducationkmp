package com.kidsplay.kidseducation.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd

/**
 * App-open ad: shown when the whole app comes back to the foreground (user switched to another app and
 * returned). NOT shown on internal screen changes. Port of the old AppOpenManager.
 */
class AppOpenAdManager(private val app: Application) :
    Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private var ad: AppOpenAd? = null
    private var loadedAt = 0L
    private var loading = false
    private var current: Activity? = null

    init {
        app.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        load()
    }

    private fun load() {
        if (isAvailable() || loading) return
        loading = true
        AppOpenAd.load(
            app, AdIds.appOpen(app), AdRequest.Builder().build(),
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(loaded: AppOpenAd) {
                    ad = loaded
                    loadedAt = System.currentTimeMillis()
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "App-open failed to load: ${error.message}")
                    loading = false
                }
            },
        )
    }

    private fun isAvailable() = ad != null && System.currentTimeMillis() - loadedAt < FOUR_HOURS_MS

    private fun showIfAvailable(activity: Activity) {
        // Never stack on top of another full-screen ad, and never inside an ad's own activity.
        if (AdMob.fullScreenShowing || activity.javaClass.name.startsWith("com.google.android.gms.ads")) return
        val appOpen = ad
        if (appOpen == null || !isAvailable()) {
            ad = null
            load()
            return
        }
        appOpen.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                AdMob.fullScreenShowing = false
                load()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                ad = null
                AdMob.fullScreenShowing = false
                load()
            }
        }
        AdMob.fullScreenShowing = true
        try {
            appOpen.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "App-open show() threw", e)
            ad = null
            AdMob.fullScreenShowing = false
        }
    }

    // Fires only when the whole process goes background -> foreground, not on screen changes.
    override fun onStart(owner: LifecycleOwner) {
        current?.let { showIfAvailable(it) }
    }

    override fun onActivityStarted(activity: Activity) { current = activity }
    override fun onActivityResumed(activity: Activity) { current = activity }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (current === activity) current = null }

    private companion object {
        const val TAG = "AppOpenAd"
        const val FOUR_HOURS_MS = 4 * 60 * 60 * 1000L
    }
}
