package com.kidsplay.kidseducation.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Port of the old CommonConstantAd + RewardedAdHelper. All calls must be made on the main thread.
 *  - interstitial: daily cap (default 5, remotely configurable), always preloaded
 *  - rewarded: plays directly, 3 minute cooldown, never blocks the child
 * Every "show" function guarantees its callback runs exactly once, whatever happens.
 */
object AdMob {
    private const val TAG = "AdMob"
    private const val PREFS = "kids_ads"
    private const val REWARDED_COOLDOWN_MS = 3 * 60 * 1000L

    private val main = Handler(Looper.getMainLooper())

    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private var lastRewardedAt = 0L
    private var fetchingCap = false

    /** True while any full-screen ad (interstitial / rewarded / app-open) is on screen. */
    @Volatile
    var fullScreenShowing = false

    // ------------------------------------------------------------------------------ init

    fun init(app: Application) {
        if (initialized) return
        initialized = true

        // Kids app: child-directed treatment BEFORE the first ad request (as in the old AppControl).
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE)
                .build(),
        )
        // Google recommends initialising off the main thread; loads are posted back to main.
        Thread {
            MobileAds.initialize(app) {
                main.post {
                    preloadInterstitial(app)
                    preloadRewarded(app)
                }
            }
        }.start()
        fetchRemoteCap(app)
    }

    fun preloadAll(context: Context) {
        preloadInterstitial(context.applicationContext)
        preloadRewarded(context.applicationContext)
    }

    // ------------------------------------------------------------------ interstitial

    fun preloadInterstitial(context: Context) {
        if (interstitial != null || interstitialLoading) return
        interstitialLoading = true
        val app = context.applicationContext
        InterstitialAd.load(
            app, AdIds.interstitial(app), AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Interstitial failed to load: ${error.message}")
                    interstitial = null
                    interstitialLoading = false
                }
            },
        )
    }

    /** Shows an interstitial if one is ready and today's cap isn't reached; then runs [onDone] once. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val done = once(onDone)
        if (activity.isFinishing || activity.isDestroyed || fullScreenShowing || capReached(activity)) {
            done(); return
        }
        val ad = interstitial
        if (ad == null) {
            done()
            preloadInterstitial(activity)
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                recordShown(activity.applicationContext)
            }

            override fun onAdDismissedFullScreenContent() {
                fullScreenShowing = false
                preloadInterstitial(activity.applicationContext)
                done()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Interstitial failed to show: ${error.message}")
                fullScreenShowing = false
                preloadInterstitial(activity.applicationContext)
                done()
            }
        }
        fullScreenShowing = true
        try {
            ad.show(activity)
        } catch (e: Exception) {
            Log.e(TAG, "Interstitial show() threw", e)
            fullScreenShowing = false
            done()
        }
    }

    // ---------------------------------------------------------------------- rewarded

    fun preloadRewarded(context: Context) {
        if (rewarded != null || rewardedLoading) return
        rewardedLoading = true
        val app = context.applicationContext
        RewardedAd.load(
            app, AdIds.rewarded(app), AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Rewarded failed to load: ${error.message}")
                    rewarded = null
                    rewardedLoading = false
                }
            },
        )
    }

    /** An ad is loaded and the 3 minute cooldown has passed. */
    fun shouldOfferRewarded(): Boolean =
        rewarded != null && !fullScreenShowing && System.currentTimeMillis() - lastRewardedAt >= REWARDED_COOLDOWN_MS

    /** Plays the rewarded video straight away (no confirm dialog), then runs [onDone]. */
    fun offerRewarded(activity: Activity, onDone: () -> Unit) {
        val done = once(onDone)
        if (activity.isFinishing || activity.isDestroyed || !shouldOfferRewarded()) {
            done(); return
        }
        showRewarded(activity, done)
    }

    private fun showRewarded(activity: Activity, done: () -> Unit) {
        val ad = rewarded
        if (ad == null || activity.isFinishing || activity.isDestroyed) {
            done(); return
        }
        rewarded = null
        lastRewardedAt = System.currentTimeMillis()
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                fullScreenShowing = false
                preloadRewarded(activity.applicationContext)
                done()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.e(TAG, "Rewarded failed to show: ${error.message}")
                fullScreenShowing = false
                preloadRewarded(activity.applicationContext)
                done()
            }
        }
        fullScreenShowing = true
        try {
            ad.show(activity) { Log.d(TAG, "Reward earned: ${it.amount}") }
        } catch (e: Exception) {
            Log.e(TAG, "Rewarded show() threw", e)
            fullScreenShowing = false
            done()
        }
    }

    // ------------------------------------------------- "first tap free, then alternate"

    /** Old MainActivity.runFromHomeWithInterstitial: free tap, then an interstitial, then free, ... */
    fun showEveryOtherTap(activity: Activity, onDone: () -> Unit) {
        val p = prefs(activity)
        if (p.getInt(KEY_TAP_TOGGLE, 1) == 1) {
            p.edit().putInt(KEY_TAP_TOGGLE, 2).apply()
            onDone()
        } else {
            p.edit().putInt(KEY_TAP_TOGGLE, 1).apply()
            showInterstitial(activity, onDone)
        }
    }

    // ------------------------------------------------------------------- daily cap

    private const val KEY_DATE = "interstitial_date"
    private const val KEY_COUNT = "interstitial_count"
    private const val KEY_CAP = "interstitial_cap"
    private const val KEY_TAP_TOGGLE = "home_tap_toggle"

    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun shownToday(c: Context): Int {
        val p = prefs(c)
        return if (p.getString(KEY_DATE, "") == today()) p.getInt(KEY_COUNT, 0) else 0
    }

    private fun recordShown(c: Context) {
        prefs(c).edit().putString(KEY_DATE, today()).putInt(KEY_COUNT, shownToday(c) + 1).apply()
    }

    private fun capReached(c: Context): Boolean =
        shownToday(c) >= prefs(c).getInt(KEY_CAP, AdIds.DEFAULT_MAX_INTERSTITIALS_PER_DAY)

    /** Fire-and-forget: asks the server for today's interstitial cap (plain number in the body). */
    private fun fetchRemoteCap(app: Application) {
        if (fetchingCap) return
        fetchingCap = true
        Thread {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(AdIds.REMOTE_CAP_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"; connectTimeout = 8000; readTimeout = 8000
                }
                if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                    val cap = conn.inputStream.bufferedReader().use { it.readText() }.trim().toInt()
                    if (cap > 0) prefs(app).edit().putInt(KEY_CAP, cap).apply()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Remote cap fetch failed: ${e.message}")
            } finally {
                conn?.disconnect()
                fetchingCap = false
            }
        }.start()
    }

    private fun once(block: () -> Unit): () -> Unit {
        var called = false
        return { if (!called) { called = true; block() } }
    }
}
