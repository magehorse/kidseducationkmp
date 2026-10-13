package com.kidsplay.kidseducation

object AppConfig {
    const val APP_NAME = "Kids Game: Seekho JR"
    const val VERSION_NAME = "9"

    /**
     * Backend for the reel feed. Defaults to the legacy server the Android app already used.
     * To use the bundled Ktor server instead, change this to e.g. "http://localhost:8080/"
     * (Android emulator: "http://10.0.2.2:8080/").
     */
    const val API_BASE_URL = "https://postkiyaapp.shivanshastrology.in/"

    const val REEL_CATEGORY_ID = 37
    const val REEL_TYPE = "category"

    const val PRIVACY_POLICY_URL = "https://postkiyaapp.shivanshastrology.in/kidsplay/privacy-policy.html"
    const val STORE_URL = "https://play.google.com/store/apps/details?id=com.kidsplay.kidseducation"
}
