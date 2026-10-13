package com.kidsplay.kidseducation

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.kidsplay.kidseducation.platform.DesktopLinkOpener
import com.kidsplay.kidseducation.platform.DesktopSpeaker
import com.kidsplay.kidseducation.platform.PlatformDeps
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

fun main() = application {
    val speaker = DesktopSpeaker()
    val deps = PlatformDeps(
        speaker = speaker,
        settings = PreferencesSettings(Preferences.userRoot().node("com/kidsplay/kidseducation")),
        links = DesktopLinkOpener(),
    )
    Window(
        onCloseRequest = {
            speaker.stop()
            exitApplication()
        },
        title = "Kids Game: Seekho JR",
        state = rememberWindowState(width = 520.dp, height = 880.dp),
    ) {
        App(deps)
    }
}
