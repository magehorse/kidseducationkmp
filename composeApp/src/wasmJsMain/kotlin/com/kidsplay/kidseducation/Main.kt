package com.kidsplay.kidseducation

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kidsplay.kidseducation.platform.PlatformDeps
import com.kidsplay.kidseducation.platform.WebLinkOpener
import com.kidsplay.kidseducation.platform.WebSpeaker
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        App(PlatformDeps(speaker = WebSpeaker(), settings = StorageSettings(), links = WebLinkOpener()))
    }
}
