package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.platform.PlatformBackHandler
import com.kidsplay.kidseducation.shared.MathOp
import com.kidsplay.kidseducation.shared.generateMathQuestion
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.GameImage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlinx.coroutines.delay

private const val GAME_SECONDS = 30

/** Drawable name of each operation's picture (addition, subtraction, multiplication, division). */
private val MathOp.image: String get() = title.lowercase()

/** Ports GameMenuActivity ("Math Quiz" -> choose an operation). */
@Composable
fun MathMenuScreen(nav: Navigator, onBack: () -> Unit) {
    ScreenFrame("Math Quiz", onBack) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Choose Quiz Type", style = MaterialTheme.typography.titleLarge)
            Text("Pick an operation to start practicing", style = MaterialTheme.typography.labelLarge)
            MathOp.entries.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { op ->
                        TileCard(Modifier.weight(1f), onClick = { nav.push(Screen.MathGame(op)) }) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                GameImage(op.image, Modifier.size(110.dp))
                                Text(op.title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatChip(text: String, color: Color) {
    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(color).padding(horizontal = 18.dp, vertical = 6.dp)) {
        Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 20.sp)
    }
}

/** Ports GameActivity: 30-second timer, "points/total" score, Correct/Wrong feedback, exit confirmation. */
@Composable
fun MathGameScreen(op: MathOp, nav: Navigator, onBack: () -> Unit) {
    var question by remember { mutableStateOf(generateMathQuestion(op)) }
    var points by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var secondsLeft by remember { mutableIntStateOf(GAME_SECONDS) }
    var alert by remember { mutableStateOf("Let's Go 😊") }
    var confirmExit by remember { mutableStateOf(false) }

    // Cancelled automatically when the screen leaves the composition (no leaked timer).
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
        nav.replace(Screen.MathResult(points, total))
    }

    // System back asks first (as the old onBackPressed did); the header arrow leaves directly (as onClickBack did).
    PlatformBackHandler(enabled = true) { confirmExit = true }

    fun answer(index: Int) {
        total++
        if (index == question.correctIndex) {
            points++
            alert = "Correct"
        } else {
            alert = "Wrong"
        }
        question = generateMathQuestion(op)
    }

    ScreenFrame(op.title, onBack) {
        Column(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip("${secondsLeft}s", Color(0xFFFF9800))
                StatChip("$points/$total", Color(0xFF4CAF50))
            }
            Text(question.text, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
            Text(
                alert,
                style = MaterialTheme.typography.titleMedium,
                color = when (alert) {
                    "Correct" -> KidsColors.Good
                    "Wrong" -> KidsColors.Bad
                    else -> KidsColors.Ink
                },
            )
            question.answers.chunked(2).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { col, value ->
                        val index = rowIndex * 2 + col
                        TileCard(Modifier.weight(1f), onClick = { answer(index) }) {
                            Box(Modifier.fillMaxWidth().padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                                Text(value.toString(), style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("Game in Progress") },
            text = { Text("Are you sure you want to exit the game?") },
            confirmButton = { TextButton(onClick = { confirmExit = false; onBack() }) { Text("Yes") } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("No") } },
        )
    }
}

/** Ports ResultsActivity. */
@Composable
fun MathResultScreen(points: Int, total: Int, nav: Navigator, onBack: () -> Unit) {
    ScreenFrame("Game Result", onBack) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉 Congratulations 🎉", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text("Your score: $points/$total", style = MaterialTheme.typography.titleLarge)
            BigButton("Play Again", onClick = { nav.replace(Screen.MathMenu) })
            BigButton("Exit", onClick = { nav.popToRoot() }, color = KidsColors.Sun)
        }
    }
}
