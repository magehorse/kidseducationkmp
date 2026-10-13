package com.kidsplay.kidseducation.data

import com.kidsplay.kidseducation.AppConfig
import com.kidsplay.kidseducation.shared.ApiRoutes
import com.kidsplay.kidseducation.shared.ReelItem
import com.kidsplay.kidseducation.shared.SharedJson
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/** Client for `GET {base}/123456/get-video?id=37&type=category` (the old Retrofit ReelApiService). */
class ReelApi(private val baseUrl: String = AppConfig.API_BASE_URL) {
    private val client = HttpClient()

    suspend fun reels(
        id: Int = AppConfig.REEL_CATEGORY_ID,
        type: String = AppConfig.REEL_TYPE,
    ): Result<List<ReelItem>> = runCatching {
        // Decode from text: the legacy server doesn't always send an application/json content type.
        val body = client.get(baseUrl.trimEnd('/') + ApiRoutes.REELS) {
            parameter("id", id)
            parameter("type", type)
        }.bodyAsText()
        SharedJson.decodeFromString<List<ReelItem>>(body)
    }
}
