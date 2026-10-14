package com.kidsplay.kidseducation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.platform.LocalDeps
import com.kidsplay.kidseducation.platform.PlatformBackHandler
import com.kidsplay.kidseducation.platform.PlatformDeps
import com.kidsplay.kidseducation.screens.CategoriesScreen
import com.kidsplay.kidseducation.screens.ChildActivityScreen
import com.kidsplay.kidseducation.screens.ColoringListScreen
import com.kidsplay.kidseducation.screens.ColoringScreen
import com.kidsplay.kidseducation.screens.FlashcardsScreen
import com.kidsplay.kidseducation.screens.LearnListScreen
import com.kidsplay.kidseducation.screens.LifeCompleteScreen
import com.kidsplay.kidseducation.screens.LifeLevelsScreen
import com.kidsplay.kidseducation.screens.LifeQuestionsScreen
import com.kidsplay.kidseducation.screens.MainScreen
import com.kidsplay.kidseducation.screens.MathGameScreen
import com.kidsplay.kidseducation.screens.MathMenuScreen
import com.kidsplay.kidseducation.screens.MathResultScreen
import com.kidsplay.kidseducation.screens.MatchGameScreen
import com.kidsplay.kidseducation.screens.NumberListScreen
import com.kidsplay.kidseducation.screens.PatternLevelsScreen
import com.kidsplay.kidseducation.screens.QuizMode
import com.kidsplay.kidseducation.screens.QuizScreen
import com.kidsplay.kidseducation.screens.ReelsScreen
import com.kidsplay.kidseducation.screens.SettingsScreen
import com.kidsplay.kidseducation.screens.TraceNumberScreen
import com.kidsplay.kidseducation.screens.VideoListScreen
import com.kidsplay.kidseducation.screens.VideoPlayScreen
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.KidsTheme

/** Shared entry point — every platform (Android, iOS, desktop, web) calls this with its [PlatformDeps]. */
@Composable
fun App(deps: PlatformDeps) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context).components { add(KtorNetworkFetcherFactory()) }.build()
    }
    val appState = remember { AppState(deps) }
    val nav = remember { Navigator() }

    DisposableEffect(Unit) { onDispose { appState.stopSpeaking() } }
    PlatformBackHandler(enabled = nav.canGoBack) {
        appState.stopSpeaking()
        nav.pop()
    }

    CompositionLocalProvider(LocalDeps provides deps, LocalAppState provides appState) {
        KidsTheme {
            Column(Modifier.fillMaxSize().background(KidsColors.PageTop)) {
                // Status-bar / notch area is painted header-blue so the top bar looks like one piece.
                Spacer(Modifier.fillMaxWidth().background(KidsColors.HeaderBottom).windowInsetsTopHeight(WindowInsets.safeDrawing))
                // Centre a phone-width column on wide screens (tablet / desktop / browser).
                Box(
                    Modifier.weight(1f).fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    Box(Modifier.widthIn(max = 840.dp).fillMaxHeight()) {
                        AppContent(nav, appState)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppContent(nav: Navigator, app: AppState) {
    val back: () -> Unit = {
        app.stopSpeaking()
        nav.pop()
    }
    val current = nav.current
    LaunchedEffect(current) { app.trackScreen(current) }
    when (val screen = current) {
        Screen.Main -> MainScreen(nav)
        is Screen.Categories -> CategoriesScreen(screen.mode, nav, onBack = back)
        is Screen.LearnList -> LearnListScreen(screen.category, nav, back)
        is Screen.Flashcards -> FlashcardsScreen(screen.category, screen.start, back)
        is Screen.LookChoose -> QuizScreen(screen.category, QuizMode.LookChoose, back)
        is Screen.ListenGuess -> QuizScreen(screen.category, QuizMode.ListenGuess, back)
        Screen.NumberList -> NumberListScreen(nav, back)
        is Screen.TraceNumber -> TraceNumberScreen(screen.number, nav, back)
        Screen.PatternLevels -> PatternLevelsScreen(nav, back)
        is Screen.MatchGame -> MatchGameScreen(screen.roundSize, back)
        Screen.LifeLevels -> LifeLevelsScreen(nav, back)
        is Screen.LifeQuestions -> LifeQuestionsScreen(screen.levelId, nav, back)
        is Screen.LifeComplete -> LifeCompleteScreen(screen, nav)
        is Screen.VideoList -> VideoListScreen(screen.categoryId, nav, back)
        is Screen.VideoPlay -> VideoPlayScreen(screen.categoryId, screen.videoId, back)
        Screen.Reels -> ReelsScreen(back)
        is Screen.ColoringList -> ColoringListScreen(screen.easy, nav, back)
        is Screen.Coloring -> ColoringScreen(screen.easy, screen.index, back)
        Screen.Settings -> SettingsScreen(nav, back)
        Screen.ChildActivity -> ChildActivityScreen(back)
        Screen.MathMenu -> MathMenuScreen(nav, back)
        is Screen.MathGame -> MathGameScreen(screen.op, nav, back)
        is Screen.MathResult -> MathResultScreen(screen.points, screen.total, nav, back)
    }
}
