package com.kidsplay.kidseducation

import androidx.compose.ui.window.ComposeUIViewController
import com.kidsplay.kidseducation.platform.IosLinkOpener
import com.kidsplay.kidseducation.platform.IosSpeaker
import com.kidsplay.kidseducation.platform.PlatformDeps
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

/** Called from iosApp/iosApp/ContentView.swift. */
fun MainViewController() = ComposeUIViewController {
    App(
        PlatformDeps(
            speaker = IosSpeaker(),
            settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults),
            links = IosLinkOpener(),
        ),
    )
}
