package com.kidsplay.kidseducation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitViewController
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVSpeechBoundary
import platform.AVFoundation.AVSpeechSynthesisVoice
import platform.AVFoundation.AVSpeechSynthesizer
import platform.AVFoundation.AVSpeechUtterance
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVKit.AVPlayerViewController
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosSpeaker : Speaker {
    private val synthesizer = AVSpeechSynthesizer()

    init {
        // Let speech play even when the ring/silent switch is on.
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }
    }

    override fun speak(text: String, languageTag: String) {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
        utterance.rate = 0.45f
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

class IosLinkOpener : LinkOpener {
    override fun open(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl, options = emptyMap<Any?, Any>(), completionHandler = null)
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // iOS has no system back button; every screen has an on-screen back button.
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun ReelVideoPlayer(url: String, isActive: Boolean, modifier: Modifier, onProgress: (Float) -> Unit) {
    val player = remember(url) { AVPlayer(uRL = NSURL.URLWithString(url)!!) }
    val controller = remember(player) {
        AVPlayerViewController().apply {
            this.player = player
            showsPlaybackControls = false
            videoGravity = AVLayerVideoGravityResizeAspectFill
        }
    }
    DisposableEffect(player) { onDispose { player.pause() } }
    UIKitViewController(
        factory = { controller },
        modifier = modifier,
        update = { if (isActive) player.play() else player.pause() },
    )
}

@Composable
actual fun InAppYouTubePlayer(videoId: String, modifier: Modifier) {
    androidx.compose.foundation.layout.Box(modifier) {
        ExternalVideoFallback("https://www.youtube.com/watch?v=$videoId")
    }
}
