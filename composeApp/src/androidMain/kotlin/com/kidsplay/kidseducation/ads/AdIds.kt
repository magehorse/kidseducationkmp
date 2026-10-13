package com.kidsplay.kidseducation.ads

import android.content.Context

/** LIVE AdMob ids (same as the old app). Debug and release builds both use these. */
@Suppress("UNUSED_PARAMETER")
internal object AdIds {
    const val APP_ID = "ca-app-pub-9716860748494124~1730127719"
    private const val BANNER = "ca-app-pub-9716860748494124/9064493146"
    private const val INTERSTITIAL = "ca-app-pub-9716860748494124/4537222056"
    private const val APP_OPEN = "ca-app-pub-9716860748494124/6017824684"
    private const val REWARDED = "ca-app-pub-9716860748494124/3172685098"

    /** Same endpoint the old app used: the body is a plain number = max interstitials per day. */
    const val REMOTE_CAP_URL = "https://postkiyaapp.shivanshastrology.in/kidsplay/appversion.php"
    const val DEFAULT_MAX_INTERSTITIALS_PER_DAY = 5

    fun banner(c: Context) = BANNER
    fun interstitial(c: Context) = INTERSTITIAL
    fun appOpen(c: Context) = APP_OPEN
    fun rewarded(c: Context) = REWARDED
}
