package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.GameImage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlinx.coroutines.delay
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/** One letter → picture pair. Artwork names follow "<letter>_<word>" (a_apple, b_ball, …). */
data class MatchPair(val id: Int, val letter: Char, val word: String, val image: String)

object LetterPictures {
    val ALL: List<MatchPair> = listOf(
        "apple", "ball", "cat", "dog", "elephant", "fish", "goat", "horse", "icecream", "joker", "kite", "line", "monkey",
        "nest", "orange", "parrot", "queen", "rabbit", "sun", "train", "umbrella", "violin", "watch", "xylophone", "yak", "zebra",
    ).mapIndexed { i, word ->
        val letter = 'A' + i
        MatchPair(
            id = i, letter = letter,
            word = when (word) { "icecream" -> "Ice Cream"; else -> word.replaceFirstChar { it.uppercase() } },
            image = "${letter.lowercaseChar()}_$word",
        )
    }

    fun randomRound(count: Int): List<MatchPair> = ALL.shuffled().take(count)
}

private class PatternLevel(val level: Int, val roundSize: Int, val image: String)

private val patternLevels = listOf(
    PatternLevel(1, 3, "a_apple"), PatternLevel(2, 4, "b_ball"), PatternLevel(3, 5, "c_cat"),
    PatternLevel(4, 6, "d_dog"), PatternLevel(5, 7, "e_elephant"),
)

@Composable
fun PatternLevelsScreen(nav: Navigator, onBack: () -> Unit) {
    ScreenFrame("Match Pattern", onBack) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(140.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(patternLevels) { level ->
                TileCard(onClick = { nav.push(Screen.MatchGame(level.roundSize)) }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        GameImage(level.image, Modifier.size(100.dp))
                        Text("Level ${level.level}", style = MaterialTheme.typography.labelLarge)
                        Text("${level.roundSize} pairs", color = Color.Gray)
                    }
                }
            }
        }
    }
}

/**
 * "Connect the Match": drag from a letter on the left to its picture on the right.
 * A wrong drop just snaps back — it's meant to be encouraging, not strict.
 */
@Composable
fun MatchGameScreen(roundSize: Int, onBack: () -> Unit) {
    val app = LocalAppState.current
    var round by remember { mutableIntStateOf(0) }
    val pairs = remember(round, roundSize) { LetterPictures.randomRound(roundSize) }
    val rightOrder = remember(round, roundSize) { pairs.shuffled() }
    val matched = remember(round, roundSize) { mutableStateListOf<Int>() }     // matched pair ids
    var misses by remember(round, roundSize) { mutableIntStateOf(0) }
    var dragFrom by remember(round, roundSize) { mutableStateOf<Int?>(null) }  // index in `pairs`
    var finger by remember(round, roundSize) { mutableStateOf(Offset.Zero) }
    val done = matched.size == pairs.size

    // No "Play again" button: a short celebration, then a fresh random round starts by itself.
    LaunchedEffect(round, done) {
        if (done) {
            app.log(ActivityKind.Game, "Match Pattern", "round completed")
            delay(1800)
            round += 1
        }
    }

    ScreenFrame("Connect the Match", onBack) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (done) "🎉 Great job! Next round…" else "Drag each letter to its picture",
                style = MaterialTheme.typography.titleMedium, fontSize = 18.sp,
            )
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                val density = LocalDensity.current
                val widthPx = with(density) { maxWidth.toPx() }
                val heightPx = with(density) { maxHeight.toPx() }
                val n = pairs.size
                val radius = min(widthPx * 0.16f, heightPx / n / 2f * 0.85f)
                fun leftCenter(i: Int) = Offset(widthPx * 0.2f, heightPx * (i + 0.5f) / n)
                fun rightCenter(i: Int) = Offset(widthPx * 0.8f, heightPx * (i + 0.5f) / n)
                fun dist(a: Offset, b: Offset) = hypot(a.x - b.x, a.y - b.y)

                Canvas(
                    Modifier.fillMaxSize().pointerInput(round, roundSize, widthPx, heightPx) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                finger = pos
                                dragFrom = pairs.indices.firstOrNull { i ->
                                    pairs[i].id !in matched && dist(leftCenter(i), pos) <= radius * 1.25f
                                }
                            },
                            onDragEnd = {
                                val from = dragFrom
                                if (from != null) {
                                    val target = rightOrder.indices.firstOrNull { j -> dist(rightCenter(j), finger) <= radius * 1.25f }
                                    if (target != null) {
                                        if (rightOrder[target].id == pairs[from].id) {
                                            matched.add(pairs[from].id)
                                            app.say("${pairs[from].letter} for ${pairs[from].word}")
                                        } else misses++
                                    }
                                }
                                dragFrom = null
                            },
                            onDragCancel = { dragFrom = null },
                        ) { change, _ ->
                            change.consume()
                            finger = change.position
                        }
                    },
                ) {
                    pairs.forEachIndexed { i, pair ->
                        if (pair.id in matched) {
                            val j = rightOrder.indexOfFirst { it.id == pair.id }
                            drawLine(KidsColors.Good, leftCenter(i), rightCenter(j), strokeWidth = 12f, cap = StrokeCap.Round)
                        }
                    }
                    dragFrom?.let { drawLine(KidsColors.HeaderTop, leftCenter(it), finger, strokeWidth = 12f, cap = StrokeCap.Round) }
                }

                // Letters (left) and pictures (right) sit above the canvas but don't consume touches.
                val diameterDp = with(density) { (radius * 2).toDp() }
                pairs.forEachIndexed { i, pair ->
                    val c = leftCenter(i)
                    val ok = pair.id in matched
                    Box(
                        Modifier.offset { IntOffset((c.x - radius).roundToInt(), (c.y - radius).roundToInt()) }
                            .size(diameterDp).clip(CircleShape)
                            .background(if (ok) KidsColors.Good else KidsColors.HeaderTop),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(pair.letter.toString(), color = Color.White, style = MaterialTheme.typography.headlineLarge,
                            fontSize = with(density) { (radius * 1.1f).toSp() })
                    }
                }
                rightOrder.forEachIndexed { j, pair ->
                    val c = rightCenter(j)
                    val ok = pair.id in matched
                    Box(
                        Modifier.offset { IntOffset((c.x - radius).roundToInt(), (c.y - radius).roundToInt()) }
                            .size(diameterDp).clip(CircleShape).background(Color.White)
                            .border(4.dp, if (ok) KidsColors.Good else Color.White, CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center,
                    ) { GameImage(pair.image, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}
