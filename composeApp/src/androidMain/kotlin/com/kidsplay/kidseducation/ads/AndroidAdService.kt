package com.kidsplay.kidseducation.ads

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.kidsplay.kidseducation.platform.AdService

/** Real AdMob implementation of the shared [AdService] hook (Android only). */
class AndroidAdService(private val activity: Activity) : AdService {

    init {
        AdMob.preloadAll(activity)
    }

    /** Bottom banner. Reserves the 50dp slot while loading; collapses if the ad can't load. */
    @Composable
    override fun Banner(modifier: Modifier) {
        val context = LocalContext.current
        var failed by remember { mutableStateOf(false) }
        val adView = remember {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdIds.banner(context)
                adListener = object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        failed = true
                    }
                }
                loadAd(AdRequest.Builder().build())
            }
        }
        // The old app destroyed every AdView with its screen; do the same so WebViews don't leak.
        DisposableEffect(adView) { onDispose { adView.destroy() } }

        Box(
            modifier.height(if (failed) 0.dp else AdSize.BANNER.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (!failed) AndroidView(factory = { adView })
        }
    }

    /** Home's four top tiles: rewarded offer when available, otherwise the "every other tap" interstitial. */
    override fun onHomeCategoryTap(onDone: () -> Unit) {
        if (AdMob.shouldOfferRewarded()) AdMob.offerRewarded(activity, onDone)
        else AdMob.showEveryOtherTap(activity, onDone)
    }

    /** Every "View All": interstitial (subject to the daily cap) before opening the list. */
    override fun onViewAllTap(onDone: () -> Unit) = AdMob.showInterstitial(activity, onDone)
}
