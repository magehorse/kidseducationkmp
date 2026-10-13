package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.rememberAsync
import com.kidsplay.kidseducation.shared.LifeSkillQuestionType
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.CenteredMessage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard

/** "About Me & Life Skills" level list. Level N+1 unlocks when level N is completed. */
@Composable
fun LifeLevelsScreen(nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    val levels by rememberAsync { app.content.lifeSkillLevels() }
    var hint by remember { mutableStateOf<String?>(null) }
    ScreenFrame("Life Skills", onBack, trailing = { Text("⭐ ${app.store.totalStars}", color = Color.White, style = MaterialTheme.typography.titleMedium) }) {
        val all = levels
        if (all == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        Column(Modifier.fillMaxSize()) {
            hint?.let { Text(it, Modifier.fillMaxWidth().background(KidsColors.Sun).padding(8.dp), textAlign = TextAlign.Center) }
            LazyColumn(Modifier.weight(1f), contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(all) { level ->
                    val unlocked = app.store.isLevelUnlocked(level, all)
                    val done = app.store.isLevelCompleted(level)
                    TileCard(
                        Modifier.fillMaxWidth(),
                        borderColor = if (done) KidsColors.Good else Color.White,
                        onClick = {
                            if (unlocked) nav.push(Screen.LifeQuestions(level.id)) else hint = "Finish the previous level first 🔒"
                        },
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (unlocked) level.emoji else "🔒", fontSize = 36.sp)
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(level.title, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                                Text(
                                    "${level.questions.size} questions" + if (done) " · ✅ done" else "",
                                    color = Color.Gray,
                                )
                            }
                            Text("⭐ ${app.store.starsForLevel(level)}", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LifeQuestionsScreen(levelId: Int, nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    val levels by rememberAsync { app.content.lifeSkillLevels() }
    val level = levels?.firstOrNull { it.id == levelId }

    ScreenFrame(level?.title ?: "", onBack) {
        if (level == null) { CenteredMessage("Loading…"); return@ScreenFrame }

        var index by remember { mutableIntStateOf(0) }
        var correct by remember { mutableIntStateOf(0) }
        var stars by remember { mutableIntStateOf(0) }
        var picked by remember(index) { mutableStateOf<Int?>(null) }
        var typed by remember(index) { mutableStateOf("") }
        var answered by remember(index) { mutableStateOf(false) }

        val q = level.questions[index]
        LaunchedEffect(index) { app.say(q.question) }

        fun finishQuestion(isCorrect: Boolean) {
            val earned = if (isCorrect) q.rewardStars else 0
            app.store.recordAnswer(level, isCorrect, earned)
            if (isCorrect) correct += 1
            stars += earned
            answered = true
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Question ${index + 1} of ${level.questions.size}   ⭐ ${app.store.totalStars}", color = Color.Gray)
            q.emoji?.let { Text(it, fontSize = 64.sp, modifier = Modifier.padding(top = 12.dp)) }
            Text(
                q.question,
                style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            if (!answered) q.hint?.let { Text("💡 $it", color = Color.Gray, textAlign = TextAlign.Center) }
            Spacer(Modifier.height(12.dp))

            if (q.questionType == LifeSkillQuestionType.TEXT_INPUT) {
                OutlinedTextField(
                    value = typed, onValueChange = { if (!answered) typed = it.take(40) },
                    singleLine = true, label = { Text("Type your answer") }, modifier = Modifier.fillMaxWidth(),
                )
                if (!answered) {
                    Spacer(Modifier.height(12.dp))
                    BigButton("Done ✓", { finishQuestion(true) }, enabled = typed.isNotBlank())
                }
            } else {
                q.options.forEachIndexed { i, option ->
                    val isRight = answered && q.hasAnswerKey && q.isCorrect(i)
                    val isWrongPick = answered && picked == i && q.hasAnswerKey && !q.isCorrect(i)
                    TileCard(
                        Modifier.fillMaxWidth().padding(vertical = 5.dp),
                        borderColor = when { isRight -> KidsColors.Good; isWrongPick -> KidsColors.Bad; picked == i -> KidsColors.HeaderTop; else -> Color.White },
                        onClick = if (answered) null else ({ picked = i; finishQuestion(q.isCorrect(i)) }),
                    ) {
                        Text(option, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.titleMedium, fontSize = 20.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            if (answered) {
                q.explanation?.let {
                    Text(it, Modifier.padding(top = 14.dp), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(14.dp))
                val last = index == level.questions.lastIndex
                BigButton(if (last) "Finish 🎉" else "Next ▶", {
                    if (last) {
                        app.store.markLevelCompleted(level)
                        nav.replace(Screen.LifeComplete(level.id, correct, level.questions.size, stars))
                    } else index += 1
                })
            }
        }
    }
}

@Composable
fun LifeCompleteScreen(result: Screen.LifeComplete, nav: Navigator) {
    val app = LocalAppState.current
    val levels by rememberAsync { app.content.lifeSkillLevels() }
    val level = levels?.firstOrNull { it.id == result.levelId }
    val next = level?.let { l -> levels?.firstOrNull { it.unlockOrder == l.unlockOrder + 1 } }
    LaunchedEffect(Unit) { app.say("Well done!") }

    ScreenFrame("Level Complete", onBack = { nav.pop() }) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("🎉", fontSize = 80.sp)
            Text(level?.title ?: "", style = MaterialTheme.typography.headlineLarge)
            Text("⭐ ${result.stars} stars earned", style = MaterialTheme.typography.titleMedium, fontSize = 22.sp, modifier = Modifier.padding(top = 8.dp))
            Text("${result.correct} of ${result.total} correct", color = Color.Gray)
            Spacer(Modifier.height(24.dp))
            if (next != null) {
                BigButton("Next level ▶", { nav.replace(Screen.LifeQuestions(next.id)) }, color = KidsColors.Good)
                Spacer(Modifier.height(10.dp))
            }
            BigButton("Back to levels", { nav.pop() }, color = Color.Gray)
        }
    }
}
