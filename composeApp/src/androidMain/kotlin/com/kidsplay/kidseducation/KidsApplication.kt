package com.kidsplay.kidseducation

import android.app.Application
import com.kidsplay.kidseducation.ads.AdMob
import com.kidsplay.kidseducation.ads.AppOpenAdManager

class KidsApplication : Application() {
    lateinit var appOpenAds: AppOpenAdManager
        private set

    override fun onCreate() {
        super.onCreate()
        AdMob.init(this)                      // child-directed tag + SDK init + preload
        appOpenAds = AppOpenAdManager(this)   // "app switch" ad
    }
}
