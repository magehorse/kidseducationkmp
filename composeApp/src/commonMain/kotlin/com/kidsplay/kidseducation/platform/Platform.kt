package com.kidsplay.kidseducation.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.russhwolf.settings.Settings

/** Text-to-speech. Implemented per platform (Android TTS, AVSpeechSynthesizer, OS voice, Web Speech API). */
interface Speaker {
    fun speak(text: String, languageTag: String = "en-US")
    fun stop()
}

/** Opens a URL in the system browser / YouTube app. */
interface LinkOpener {
    fun open(url: String)
}

/**
 * Ad hook. The old Android app used AdMob; AdMob only exists on Android/iOS, so the shared UI just calls
 * these hooks and each platform entry point may supply a real implementation (Android: AndroidAdService).
 *
 * Every callback-taking function guarantees [onDone] runs exactly once (after the ad is closed, or
 * immediately when no ad is shown), so navigation can never get stuck behind an ad.
 */
interface AdService {
    /** Bottom banner slot — put at the bottom of every screen. */
    @Composable
    fun Banner(modifier: Modifier)

    /** Home's four big category tiles: rewarded offer, otherwise an interstitial every other tap. */
    fun onHomeCategoryTap(onDone: () -> Unit)

    /** Any "View All" button: interstitial (daily-capped) before opening the full list. */
    fun onViewAllTap(onDone: () -> Unit)
}

object NoAds : AdService {
    @Composable
    override fun Banner(modifier: Modifier) = Unit
    override fun onHomeCategoryTap(onDone: () -> Unit) = onDone()
    override fun onViewAllTap(onDone: () -> Unit) = onDone()
}

/** Everything the shared UI needs from the host platform. Built in each platform's entry point. */
class PlatformDeps(
    val speaker: Speaker,
    val settings: Settings,
    val links: LinkOpener,
    val ads: AdService = NoAds,
    /**
     * Opens the old drawing module (brushes, pencils, stickers) — Android only.
     * (easy, index) = open that colouring picture in the editor; (easy, null) = open the picture list.
     * When null (other platforms) the built-in tap-to-fill colouring screens are used instead.
     */
    val openDrawing: ((easy: Boolean, index: Int?) -> Unit)? = null,
    /** Opens the list of saved drawings ("My Drawings") — Android only, hidden elsewhere. */
    val openMyDrawings: (() -> Unit)? = null,
    /** Where the sound toggle lives. Android points this at the old app's `MyPref` file; elsewhere it is [settings]. */
    val soundSettings: Settings = settings,
    /** Where life-skills progress lives. Android points this at the old `life_skills_progress` file. */
    val lifeSkillsSettings: Settings = settings,
)

val LocalDeps = staticCompositionLocalOf<PlatformDeps> { error("PlatformDeps not provided") }

/** System back button (Android only; a no-op elsewhere — every screen also has an on-screen back button). */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

/**
 * Inline looping video for the reel feed: ExoPlayer on Android, AVPlayer on iOS.
 * Desktop and web fall back to [ExternalVideoFallback].
 * [onProgress] reports playback position as 0f..1f (Android only; other targets never call it).
 */
@Composable
expect fun ReelVideoPlayer(
    url: String,
    isActive: Boolean,
    modifier: Modifier = Modifier,
    onProgress: (Float) -> Unit = {},
)

/**
 * Plays a YouTube video INSIDE the app (Android: the same androidyoutubeplayer library the old app used).
 * Other platforms show an "Open video" button that leaves the app.
 */
@Composable
expect fun InAppYouTubePlayer(videoId: String, modifier: Modifier = Modifier)

@Composable
fun ExternalVideoFallback(url: String, modifier: Modifier = Modifier) {
    val opener = LocalDeps.current.links
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎬", color = Color.White, style = MaterialTheme.typography.displayMedium)
        Text("Inline playback isn't available on this platform.", color = Color.White)
        Button(onClick = { opener.open(url) }, modifier = Modifier.padding(top = 16.dp)) { Text("Open video") }
    }
}
