package com.kidsplay.kidseducation.nav

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kidsplay.kidseducation.shared.MathOp

enum class MainTab(val label: String, val emoji: String) {
    Home("Home", "🏠"),
    Look("Look", "👀"),
    Listen("Listen", "🎧"),
    Videos("Videos", "🎬"),
    Math("Maths", "➗"),
}

enum class GridMode { Learn, Look, Listen, Math }

sealed interface Screen {
    data object Main : Screen
    data class Categories(val mode: GridMode) : Screen
    data class LearnList(val category: Int) : Screen
    data class Flashcards(val category: Int, val start: Int) : Screen
    data class LookChoose(val category: Int) : Screen
    data class ListenGuess(val category: Int) : Screen
    data object NumberList : Screen
    data class TraceNumber(val number: Int) : Screen
    data object PatternLevels : Screen
    data class MatchGame(val roundSize: Int) : Screen
    data object LifeLevels : Screen
    data class LifeQuestions(val levelId: Int) : Screen
    data class LifeComplete(val levelId: Int, val correct: Int, val total: Int, val stars: Int) : Screen
    data class VideoList(val categoryId: Int) : Screen
    data class VideoPlay(val categoryId: Int, val videoId: String) : Screen
    data object Reels : Screen
    data object Draw : Screen
    data object Settings : Screen
    data object ChildActivity : Screen
    data object MathMenu : Screen
    data class MathGame(val op: MathOp) : Screen
    data class MathResult(val points: Int, val total: Int) : Screen
}

/** Minimal back-stack navigator (state-based, works identically on every target). */
class Navigator {
    private val stack = mutableStateListOf<Screen>(Screen.Main)

    var mainTab by mutableStateOf(MainTab.Home)

    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) { stack.add(screen) }

    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun popToRoot() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}
