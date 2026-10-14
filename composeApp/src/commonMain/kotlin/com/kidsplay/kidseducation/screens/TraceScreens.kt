package com.kidsplay.kidseducation.screens

import kotlinx.coroutines.delay
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlin.math.hypot

private const val MAX_NUMBER = 20

/** 1–20 picker for the writing practice (the old NumberListActivity). */
@Composable
fun NumberListScreen(nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    ScreenFrame("Learn Writing", onBack) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(88.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items((1..MAX_NUMBER).toList()) { n ->
                val color = KidsColors.Rainbow[n % KidsColors.Rainbow.size]
                TileCard(Modifier.aspectRatio(1f), borderColor = color, onClick = {
                    app.say(n.toString())
                    nav.push(Screen.TraceNumber(n))
                }) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(n.toString(), color = color, style = MaterialTheme.typography.headlineLarge, fontSize = 34.sp)
                    }
                }
            }
        }
    }
}

/**
 * Simplified single-stroke numerals in a 0..100 box. Connecting the dots in order draws the digit.
 * Numbers 10–20 place two digits side by side. (Ported 1:1 from TraceNumberView.kt.)
 */
private val DIGIT_POINTS: Map<Char, List<Offset>> = mapOf(
    '0' to pts(50, 10, 30, 20, 20, 50, 30, 80, 50, 90, 70, 80, 80, 50, 70, 20, 50, 10),
    '1' to pts(35, 25, 50, 10, 50, 90, 30, 90, 70, 90),
    '2' to pts(20, 25, 35, 10, 55, 10, 70, 22, 68, 38, 45, 60, 20, 85, 80, 85),
    '3' to pts(20, 15, 45, 10, 65, 18, 68, 32, 50, 42, 68, 55, 68, 75, 50, 88, 22, 80),
    '4' to pts(65, 10, 20, 60, 80, 60, 65, 60, 65, 90),
    '5' to pts(68, 12, 30, 12, 25, 42, 45, 35, 65, 45, 68, 65, 55, 85, 30, 80),
    '6' to pts(62, 15, 38, 15, 25, 40, 20, 65, 30, 85, 55, 88, 68, 75, 65, 55, 45, 45, 28, 52),
    '7' to pts(20, 15, 78, 15, 42, 90),
    '8' to pts(50, 10, 32, 20, 32, 38, 50, 48, 68, 38, 68, 20, 50, 10, 32, 58, 28, 78, 42, 90, 58, 90, 72, 78, 68, 58, 50, 48),
    '9' to pts(55, 45, 35, 42, 25, 28, 32, 13, 52, 10, 68, 20, 72, 45, 65, 75, 48, 88, 30, 82),
)

private fun pts(vararg xy: Int): List<Offset> = xy.toList().chunked(2).map { (x, y) -> Offset(x.toFloat(), y.toFloat()) }

private class TraceDots(val dots: List<Offset>, val digitStarts: Set<Int>)

/** Dots in normalised 0..1 space for [number]. */
private fun buildDots(number: Int): TraceDots {
    val dots = ArrayList<Offset>()
    val starts = HashSet<Int>()
    val digits = number.toString()
    val slot = 1f / digits.length
    digits.forEachIndexed { digitIndex, ch ->
        val shape = DIGIT_POINTS[ch] ?: return@forEachIndexed
        starts.add(dots.size)
        shape.forEach { p ->
            val nx = (digitIndex + (p.x / 100f) * 0.8f + 0.1f) * slot
            val ny = (p.y / 100f) * 0.8f + 0.1f
            dots.add(Offset(nx, ny))
        }
    }
    return TraceDots(dots, starts)
}

/** "Connect the dashed dots" tracing: drag through dot 1, 2, 3… in order. No penalty for wandering. */
@Composable
fun TraceNumberScreen(number: Int, nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    val trace = remember(number) { buildDots(number) }
    var nextIndex by remember(number) { mutableIntStateOf(0) }
    var finger by remember(number) { mutableStateOf<Offset?>(null) }
    val complete = trace.dots.isNotEmpty() && nextIndex >= trace.dots.size
    val textMeasurer = rememberTextMeasurer()

    val pulse by rememberInfiniteTransition().animateFloat(
        initialValue = 0.85f, targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse),
    )

    // Finished the number -> say it, celebrate for a moment, then open the next number automatically.
    LaunchedEffect(complete, number) {
        if (complete) {
            app.say(number.toString())
            if (number < MAX_NUMBER) {
                delay(2200)
                nav.replace(Screen.TraceNumber(number + 1))
            }
        }
    }

    ScreenFrame("Write $number", onBack) {
        Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TileCard(Modifier.fillMaxWidth().weight(1f).padding(bottom = 8.dp)) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Canvas(
                        Modifier.fillMaxSize().aspectRatio(1f)
                            .pointerInput(number) {
                                detectDragGestures(
                                    onDragStart = { finger = it },
                                    onDragEnd = { finger = null },
                                    onDragCancel = { finger = null },
                                ) { change, _ ->
                                    change.consume()
                                    val pos = change.position
                                    finger = pos
                                    val size = this.size.width.coerceAtMost(this.size.height).toFloat()
                                    val pad = size * 0.06f
                                    val radius = size * 0.045f
                                    val originX = (this.size.width - size) / 2f
                                    val originY = (this.size.height - size) / 2f
                                    // Reaching dot N lights it up; moving fast can light several at once.
                                    while (nextIndex < trace.dots.size) {
                                        val d = trace.dots[nextIndex]
                                        val dx = originX + pad + d.x * (size - 2 * pad)
                                        val dy = originY + pad + d.y * (size - 2 * pad)
                                        if (hypot(pos.x - dx, pos.y - dy) <= radius * 2.2f) nextIndex++ else break
                                    }
                                }
                            },
                    ) {
                        val size = minOf(this.size.width, this.size.height)
                        val pad = size * 0.06f
                        val originX = (this.size.width - size) / 2f
                        val originY = (this.size.height - size) / 2f
                        fun toView(p: Offset) = Offset(originX + pad + p.x * (size - 2 * pad), originY + pad + p.y * (size - 2 * pad))
                        val radius = size * 0.045f

                        for (i in 1 until trace.dots.size) {
                            if (i in trace.digitStarts) continue          // no guide line between two digits
                            val a = toView(trace.dots[i - 1]); val b = toView(trace.dots[i])
                            if (nextIndex > i) {
                                drawLine(Color(0xFF4CAF50), a, b, strokeWidth = 14f, cap = StrokeCap.Round)
                            } else {
                                drawLine(
                                    Color(0xFFCBD5E1), a, b, strokeWidth = 10f, cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 16f), 0f),
                                )
                            }
                        }
                        trace.dots.forEachIndexed { i, dot ->
                            val c = toView(dot)
                            val (color, r) = when {
                                i < nextIndex -> Color(0xFF4CAF50) to radius
                                i == nextIndex -> Color(0xFFFF9800) to radius * pulse
                                else -> Color(0xFF94A3B8) to radius
                            }
                            drawCircle(color, r, c)
                            val label = textMeasurer.measure(
                                (i + 1).toString(),
                                TextStyle(color = Color.White, fontWeight = FontWeight.Bold, fontSize = (radius * 0.9f / density).sp),
                            )
                            drawText(label, topLeft = Offset(c.x - label.size.width / 2f, c.y - label.size.height / 2f))
                        }
                        finger?.let { drawCircle(Color(0xA04488FF), radius * 1.6f, it) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                BigButton("◀", { nav.replace(Screen.TraceNumber(number - 1)) }, enabled = number > 1, color = Color.Gray)
                if (complete) Text(if (number < MAX_NUMBER) "🎉 Great! Next…" else "🎉 Great!", style = MaterialTheme.typography.headlineMedium)
                else BigButton("Again", { nextIndex = 0 }, color = KidsColors.Sun)
                BigButton("▶", { nav.replace(Screen.TraceNumber(number + 1)) }, enabled = number < MAX_NUMBER)
            }
        }
    }
}
