package com.kidsplay.kidseducation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@JsFun(
    "(text, lang) => { try { const s = window.speechSynthesis; s.cancel(); " +
        "const u = new SpeechSynthesisUtterance(text); u.lang = lang; u.rate = 0.9; s.speak(u); } catch (e) {} }",
)
private external fun jsSpeak(text: String, lang: String)

@JsFun("() => { try { window.speechSynthesis.cancel(); } catch (e) {} }")
private external fun jsStopSpeaking()

@JsFun("(url) => { window.open(url, '_blank', 'noopener'); }")
private external fun jsOpen(url: String)

/** Browser text-to-speech (Web Speech API). Must be triggered from a user gesture on most browsers. */
class WebSpeaker : Speaker {
    override fun speak(text: String, languageTag: String) = jsSpeak(text, languageTag)
    override fun stop() = jsStopSpeaking()
}

class WebLinkOpener : LinkOpener {
    override fun open(url: String) = jsOpen(url)
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Browser history integration is not wired; every screen has an on-screen back button.
}

@Composable
actual fun ReelVideoPlayer(url: String, isActive: Boolean, modifier: Modifier, onProgress: (Float) -> Unit) {
    ExternalVideoFallback(url, modifier)
}

@Composable
actual fun InAppYouTubePlayer(videoId: String, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier) {
        ExternalVideoFallback("https://www.youtube.com/watch?v=$videoId")
    }
}
