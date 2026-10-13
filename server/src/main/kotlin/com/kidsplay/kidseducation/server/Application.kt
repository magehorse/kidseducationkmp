package com.kidsplay.kidseducation.server

import com.kidsplay.kidseducation.shared.ApiRoutes
import com.kidsplay.kidseducation.shared.LearningContent
import com.kidsplay.kidseducation.shared.LifeSkillsData
import com.kidsplay.kidseducation.shared.ReelItem
import com.kidsplay.kidseducation.shared.SharedJson
import com.kidsplay.kidseducation.shared.VideoContent
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.singlePageApplication
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.compression.Compression
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.http.HttpMethod

/**
 * Kids Game backend.
 *
 *  - serves the same content the apps ship with, so content can be updated without a store release
 *  - re-implements the legacy reel / ad-config endpoints the Android app used to call on
 *    postkiyaapp.shivanshastrology.in, so [AppConfig.apiBaseUrl] can simply be pointed here
 *  - optionally hosts the compiled web app (set WEB_DIST to the wasm distribution folder)
 *
 * Env: PORT (default 8080), MAX_INTERSTITIALS_PER_DAY (default 5), WEB_DIST (optional path).
 */
fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

private fun resourceText(name: String): String =
    object {}.javaClass.classLoader.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)?.readText()
        ?: error("Missing server resource $name")

fun Application.module() {
    val learning = SharedJson.decodeFromString<LearningContent>(resourceText("learning.json"))
    val videos = SharedJson.decodeFromString<VideoContent>(resourceText("videos.json"))
    val lifeSkills = SharedJson.decodeFromString<LifeSkillsData>(resourceText("life_skills_questions.json"))
    val reels: List<ReelItem> = (System.getenv("REELS_FILE")?.let { java.io.File(it).readText() } ?: resourceText("reels.json"))
        .let { SharedJson.decodeFromString(it) }
    val maxInterstitials = System.getenv("MAX_INTERSTITIALS_PER_DAY")?.toIntOrNull() ?: 5
    val webDist = System.getenv("WEB_DIST")

    install(ContentNegotiation) { json(SharedJson) }
    install(Compression)
    install(CallLogging)
    install(CORS) {
        anyHost()                       // the web client may be hosted on another origin
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Options)
        allowHeader("Content-Type")
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "internal"))
        }
    }

    routing {
        get(ApiRoutes.HEALTH) { call.respondText("ok") }
        get(ApiRoutes.LEARNING) { call.respond(learning) }
        get(ApiRoutes.VIDEOS) { call.respond(videos) }
        get(ApiRoutes.LIFE_SKILLS) { call.respond(lifeSkills) }

        // Legacy reel API: GET /123456/get-video?id=37&type=category
        // Serves reels.json (list of ReelItem with direct .mp4 URLs in "image"). Ships empty:
        // drop your mp4 catalogue into server/src/main/resources/reels.json, or point REELS_FILE at one.
        get(ApiRoutes.REELS) {
            val id = call.request.queryParameters["id"]?.toIntOrNull()
            val type = call.request.queryParameters["type"]
            call.respond(reels.filter { (id == null || it.id == id) && (type == null || it.type == type) })
        }

        // Legacy ad-config endpoint: plain-text number = interstitials per user per day.
        get(ApiRoutes.APP_VERSION) {
            call.respondText(maxInterstitials.toString(), ContentType.Text.Plain)
        }

        if (webDist != null) singlePageApplication { filesPath = webDist }
    }
}
