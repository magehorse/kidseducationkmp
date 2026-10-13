package com.kidsplay.kidseducation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.awt.Desktop
import java.net.URI

/**
 * Desktop text-to-speech via the OS voice: `say` (macOS), PowerShell System.Speech (Windows),
 * `espeak` (Linux — install it with your package manager).
 */
class DesktopSpeaker : Speaker {
    private var process: Process? = null

    override fun speak(text: String, languageTag: String) {
        stop()
        val os = System.getProperty("os.name").lowercase()
        val hindi = languageTag.startsWith("hi")
        val command = when {
            "mac" in os -> if (hindi) listOf("say", "-v", "Lekha", text) else listOf("say", text)
            "win" in os -> listOf(
                "powershell", "-NoProfile", "-Command",
                "Add-Type -AssemblyName System.Speech; " +
                    "(New-Object System.Speech.Synthesis.SpeechSynthesizer).Speak('${text.replace("'", "''")}')",
            )
            else -> listOf("espeak", "-v", languageTag.substringBefore('-'), text)
        }
        process = runCatching { ProcessBuilder(command).redirectErrorStream(true).start() }.getOrNull()
    }

    override fun stop() {
        process?.destroy()
        process = null
    }
}

class DesktopLinkOpener : LinkOpener {
    override fun open(url: String) {
        runCatching {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            }
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // Desktop has no system back gesture; every screen has an on-screen back button.
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
