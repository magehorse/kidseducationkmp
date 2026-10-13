package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.rememberAsync
import com.kidsplay.kidseducation.shared.LearningItem
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.CenteredMessage
import com.kidsplay.kidseducation.ui.GameImage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard

enum class QuizMode { LookChoose, ListenGuess }

private class Question(val answer: LearningItem, val options: List<LearningItem>)

private fun makeQuestion(items: List<LearningItem>): Question {
    val answer = items.random()
    val others = items.filter { it !== answer }.shuffled().take(3)
    return Question(answer, (others + answer).shuffled())
}

/**
 * Look & Choose: see a picture, pick its name.
 * Listen & Guess: hear a word, pick its picture.
 */
@Composable
fun QuizScreen(categoryIndex: Int, mode: QuizMode, onBack: () -> Unit) {
    val app = LocalAppState.current
    val category by rememberAsync(categoryIndex) { app.content.learningCategory(categoryIndex) }
    val title = (if (mode == QuizMode.LookChoose) "Look & Choose" else "Listen & Guess") + (category?.let { " · ${it.title}" } ?: "")

    ScreenFrame(title, onBack) {
        val items = category?.items
        if (items == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        if (items.size < 2) { CenteredMessage("Not enough items in this category yet."); return@ScreenFrame }

        var round by remember { mutableIntStateOf(0) }
        var score by remember { mutableIntStateOf(0) }
        val question = remember(round) { makeQuestion(items) }
        var picked by remember(round) { mutableStateOf<LearningItem?>(null) }
        val solved = picked === question.answer

        if (mode == QuizMode.ListenGuess) {
            LaunchedEffect(round) { app.say(question.answer.show) }
        }

        Column(
            Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("⭐ $score", style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)

            // ---- question card
            TileCard(Modifier.fillMaxWidth().weight(1f).padding(vertical = 8.dp)) {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                    when (mode) {
                        QuizMode.LookChoose -> GameImage(question.answer.image, Modifier.fillMaxSize())
                        QuizMode.ListenGuess -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier.size(120.dp).clip(RoundedCornerShape(60.dp))
                                    .background(KidsColors.HeaderTop.copy(alpha = 0.15f))
                                    .clickable { app.say(question.answer.show) },
                                contentAlignment = Alignment.Center,
                            ) { GameImage("btn_sound", Modifier.size(72.dp)) }
                            Text(question.answer.show, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        }
                    }
                }
            }

            // ---- options (2 x 2)
            for (row in question.options.chunked(2)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (option in row) {
                        val state = when {
                            picked == null -> null
                            option === question.answer && solved -> true
                            option === picked -> false
                            else -> null
                        }
                        OptionTile(
                            option = option,
                            mode = mode,
                            state = state,
                            modifier = Modifier.weight(1f),
                            enabled = !solved,
                        ) {
                            picked = option
                            if (option === question.answer) {
                                score += 1
                                app.say(option.show)
                            }
                        }
                    }
                    if (row.size == 1) Box(Modifier.weight(1f))
                }
            }

            Row(Modifier.padding(top = 8.dp).height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                if (solved) {
                    Text("🎉 Great job!  ", style = MaterialTheme.typography.titleMedium)
                    BigButton("Next ▶", onClick = { round += 1 })
                } else if (picked != null) {
                    Text("Try again!", style = MaterialTheme.typography.titleMedium, color = KidsColors.Bad)
                } else {
                    BigButton("Skip ▶", onClick = { round += 1 }, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun OptionTile(
    option: LearningItem,
    mode: QuizMode,
    state: Boolean?,           // true = correct, false = wrong, null = neutral
    modifier: Modifier,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val border = when (state) { true -> KidsColors.Good; false -> KidsColors.Bad; null -> Color.White }
    TileCard(modifier.height(if (mode == QuizMode.ListenGuess) 140.dp else 72.dp), borderColor = border, onClick = if (enabled) onClick else null) {
        Box(Modifier.fillMaxSize().padding(8.dp), contentAlignment = Alignment.Center) {
            when (mode) {
                QuizMode.LookChoose -> Text(option.show, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp, textAlign = TextAlign.Center)
                QuizMode.ListenGuess -> GameImage(option.image, Modifier.fillMaxSize())
            }
        }
    }
}
