package com.kidsplay.kidseducation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.data.ActivityLog
import com.kidsplay.kidseducation.data.ContentRepository
import com.kidsplay.kidseducation.data.ProgressStore
import com.kidsplay.kidseducation.data.ReelApi
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.platform.PlatformDeps

/** App-wide observable state + services. One instance lives for the whole app session. */
class AppState(private val deps: PlatformDeps) {
    val store = ProgressStore(deps.settings, deps.soundSettings, deps.lifeSkillsSettings)
    val content = ContentRepository()
    /** Parent-facing diary of what the child learned / played (Settings -> Child Activity). */
    val activity = ActivityLog(deps.settings)
    val reelApi = ReelApi()

    var userName by mutableStateOf(store.userName)
        private set
    var soundEnabled by mutableStateOf(store.soundEnabled)
        private set
    /** English only: any previously saved Hindi choice is ignored. */
    var language by mutableStateOf("en")
        private set
    /** Don't ask for the child's name more than once per session. */
    var nameAsked by mutableStateOf(false)

    val isHindi: Boolean get() = language == "hi"

    fun updateName(value: String) {
        userName = value.trim()
        store.userName = userName
    }

    fun updateSound(value: Boolean) {
        soundEnabled = value
        store.soundEnabled = value
        if (!value) deps.speaker.stop()
    }

    fun updateLanguage(value: String) {
        // English only — nothing to change.
    }

    /** Speaks [text] if sound is on. [hindi] picks the Hindi voice. */
    fun say(text: String, hindi: Boolean = false) {
        if (soundEnabled) deps.speaker.speak(text, if (hindi) "hi-IN" else "en-US")
    }

    fun stopSpeaking() = deps.speaker.stop()

    fun openUrl(url: String) = deps.links.open(url)

    fun log(kind: ActivityKind, title: String, detail: String = "") = activity.record(kind, title, detail)

    /** Called whenever a screen opens: records games / lessons that don't log themselves. */
    suspend fun trackScreen(screen: Screen) {
        when (screen) {
            is Screen.LookChoose -> log(ActivityKind.Game, "Look & Choose", content.learningCategory(screen.category)?.title.orEmpty())
            is Screen.ListenGuess -> log(ActivityKind.Game, "Listen & Guess", content.learningCategory(screen.category)?.title.orEmpty())
            is Screen.TraceNumber -> log(ActivityKind.Learn, "Writing numbers", "Number ${screen.number}")
            is Screen.MatchGame -> log(ActivityKind.Game, "Match Pattern", "${screen.roundSize} pairs")
            is Screen.LifeQuestions -> log(
                ActivityKind.LifeSkills, "Life skills",
                runCatching { content.lifeSkillLevels().firstOrNull { it.id == screen.levelId }?.title }.getOrNull() ?: "Level ${screen.levelId}",
            )
            is Screen.LifeComplete -> log(ActivityKind.LifeSkills, "Life skills result", "${screen.correct} of ${screen.total} correct, ${screen.stars} stars")
            is Screen.MathGame -> log(ActivityKind.Game, "Maths", screen.op.title)
            is Screen.MathResult -> log(ActivityKind.Game, "Maths result", "${screen.points} of ${screen.total} points")
            else -> Unit
        }
    }
}

val LocalAppState = staticCompositionLocalOf<AppState> { error("AppState not provided") }

/** Loads suspend content once per [key]; null while loading. */
@Composable
fun <T> rememberAsync(key: Any? = Unit, block: suspend () -> T?): State<T?> =
    produceState<T?>(initialValue = null, key) { value = runCatching { block() }.getOrNull() }
