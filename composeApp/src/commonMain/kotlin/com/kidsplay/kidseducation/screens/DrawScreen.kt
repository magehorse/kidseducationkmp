package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame

private class PenStroke(val color: Color, val width: Float, val points: MutableList<Offset>)

private val palette = listOf(
    Color(0xFF212121), Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835),
    Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF6D4C41), Color.White,
)
private val widths = listOf(8f, 16f, 28f)

/**
 * Free-hand drawing pad. This replaces the old `kidsdrawing` module (a flood-fill colouring editor built on
 * Android Views / Bitmap APIs); the multiplatform version supports brush colours, sizes, undo and clear.
 */
@Composable
fun DrawScreen(onBack: () -> Unit) {
    val strokes = remember { mutableStateListOf<PenStroke>() }
    var color by remember { mutableStateOf(palette[1]) }
    var width by remember { mutableStateOf(widths[1]) }
    var tick by remember { mutableStateOf(0) }   // forces redraw while a stroke grows

    ScreenFrame("Draw", onBack) {
        Column(Modifier.fillMaxSize().padding(12.dp)) {
            Box(Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)) {
                Canvas(
                    Modifier.fillMaxSize().pointerInput(color, width) {
                        detectDragGestures(
                            onDragStart = { strokes.add(PenStroke(color, width, mutableListOf(it))); tick++ },
                        ) { change, _ ->
                            change.consume()
                            strokes.lastOrNull()?.points?.add(change.position)
                            tick++
                        }
                    },
                ) {
                    if (tick < 0) return@Canvas   // reads `tick` so the canvas redraws while a stroke grows
                    strokes.forEach { s ->
                        if (s.points.size == 1) {
                            drawCircle(s.color, s.width / 2f, s.points.first())
                        } else {
                            val path = Path().apply {
                                moveTo(s.points.first().x, s.points.first().y)
                                s.points.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(path, s.color, style = Stroke(s.width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                palette.forEach { c ->
                    Box(
                        Modifier.size(if (c == color) 38.dp else 30.dp).clip(CircleShape).background(c)
                            .border(2.dp, if (c == color) KidsColors.HeaderBottom else Color.LightGray, CircleShape)
                            .clickable { color = c },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                widths.forEach { w ->
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (w == width) KidsColors.Sun else Color.White)
                            .clickable { width = w },
                        contentAlignment = Alignment.Center,
                    ) { Box(Modifier.size((w / 2f + 6f).dp).clip(CircleShape).background(Color.DarkGray)) }
                }
                Box(Modifier.weight(1f))
                BigButton("Undo", { if (strokes.isNotEmpty()) { strokes.removeAt(strokes.lastIndex); tick++ } }, color = KidsColors.Sun)
                BigButton("Clear", { strokes.clear(); tick++ }, color = KidsColors.Bad)
            }
        }
    }
}
