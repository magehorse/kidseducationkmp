package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Canvas as GraphicsCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.resources.Res
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.roundToInt

/** The 101 Easy + 101 Hard colouring pictures from the old drawing module (small = list thumbnails, large = canvas). */
object ColoringCatalog {
    const val COUNT = 101

    fun path(easy: Boolean, index: Int, large: Boolean): String =
        "files/coloring/${if (easy) "easy" else "hard"}_${if (large) "large" else "small"}/${index + 1}.png"
}

/** Picture thumbnail (white card look) used by Home and the list. */
@Composable
fun ColoringThumb(easy: Boolean, index: Int, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, easy, index) {
        value = runCatching { Res.readBytes(ColoringCatalog.path(easy, index, large = false)).decodeToImageBitmap() }.getOrNull()
    }
    Box(modifier.background(Color.White), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize().padding(6.dp), contentScale = ContentScale.Fit) }
    }
}

// ------------------------------------------------------------------------------------ list (View All)

/** "View All": two tabs, Easy and Hard (like the old drawing module), 2-column grid of pictures. */
@Composable
fun ColoringListScreen(initialEasy: Boolean, nav: Navigator, onBack: () -> Unit) {
    var easy by remember { mutableStateOf(initialEasy) }
    ScreenFrame("Colouring", onBack) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LevelTab("⭐ Easy", easy, Modifier.weight(1f)) { easy = true }
                LevelTab("🔥 Hard", !easy, Modifier.weight(1f)) { easy = false }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ColoringCatalog.COUNT) { i ->
                    TileCard(
                        Modifier.fillMaxWidth().aspectRatio(1f),
                        borderColor = KidsColors.Rainbow[i % KidsColors.Rainbow.size],
                        onClick = { nav.push(Screen.Coloring(easy, i)) },
                    ) { ColoringThumb(easy, i, Modifier.fillMaxSize()) }
                }
            }
        }
    }
}

@Composable
private fun LevelTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.clip(shape).background(if (selected) KidsColors.HeaderBottom else Color.White)
            .border(2.dp, KidsColors.HeaderBottom, shape).clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label, fontSize = 15.sp, style = MaterialTheme.typography.titleMedium,
            color = if (selected) Color.White else KidsColors.HeaderBottom,
        )
    }
}

// ------------------------------------------------------------------------------------ editor (tap to fill)

private val palette = listOf(
    Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF43A047),
    Color(0xFF9CCC65), Color(0xFF26A69A), Color(0xFF29B6F6), Color(0xFF1E88E5),
    Color(0xFF8E24AA), Color(0xFFEC407A), Color(0xFF795548), Color(0xFFFFAB91),
    Color(0xFF212121), Color(0xFF9E9E9E), Color(0xFFF5CBA7), Color.White,   // white = eraser
)

/** One tap = one coloured region, stored as horizontal runs (y, x1, x2) so undo can repaint from scratch. */
private class FillOp(val color: Color, val runs: IntArray)

private class IntList {
    var data = IntArray(1024)
    var size = 0
    fun add(v: Int) {
        if (size == data.size) data = data.copyOf(size * 2)
        data[size++] = v
    }
    fun pop(): Int = data[--size]
    fun toArray(): IntArray = data.copyOf(size)
}

/** The outline picture, which pixels are outline ("walls" for the fill), and the colour layer under it. */
private class ColoringPage(val image: ImageBitmap, val isLine: BooleanArray) {
    val w = image.width
    val h = image.height
    val overlay = ImageBitmap(w, h)

    /** Paint bucket: returns the runs of the region around ([sx],[sy]), or null if the tap hit nothing fillable. */
    fun fill(sx: Int, sy: Int): IntArray? {
        var start = sy * w + sx
        if (isLine[start]) {
            // Child tapped on the outline itself: use the closest open pixel within a few pixels.
            var found = -1
            search@ for (r in 1..8) {
                for (dy in -r..r) for (dx in -r..r) {
                    val x = sx + dx
                    val y = sy + dy
                    if (x in 0 until w && y in 0 until h && !isLine[y * w + x]) { found = y * w + x; break@search }
                }
            }
            if (found < 0) return null
            start = found
        }
        val visited = BooleanArray(w * h)
        val runs = IntList()
        val stack = IntList()
        stack.add(start)
        while (stack.size > 0) {
            val pos = stack.pop()
            if (visited[pos] || isLine[pos]) continue
            val y = pos / w
            val row = y * w
            var x1 = pos - row
            var x2 = x1
            while (x1 > 0 && !isLine[row + x1 - 1] && !visited[row + x1 - 1]) x1--
            while (x2 < w - 1 && !isLine[row + x2 + 1] && !visited[row + x2 + 1]) x2++
            for (i in x1..x2) visited[row + i] = true
            runs.add(y); runs.add(x1); runs.add(x2)
            for (ny in intArrayOf(y - 1, y + 1)) {
                if (ny < 0 || ny >= h) continue
                val nrow = ny * w
                var prevOpen = false
                for (i in x1..x2) {
                    val open = !isLine[nrow + i] && !visited[nrow + i]
                    if (open && !prevOpen) stack.add(nrow + i)
                    prevOpen = open
                }
            }
        }
        return runs.toArray()
    }

    fun paint(op: FillOp) {
        val canvas = GraphicsCanvas(overlay)
        val paint = Paint().apply { color = op.color; isAntiAlias = false }
        var i = 0
        while (i < op.runs.size) {
            canvas.drawRect(op.runs[i + 1].toFloat(), op.runs[i].toFloat(), (op.runs[i + 2] + 1).toFloat(), (op.runs[i] + 1).toFloat(), paint)
            i += 3
        }
    }

    fun repaint(ops: List<FillOp>) {
        val canvas = GraphicsCanvas(overlay)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { blendMode = BlendMode.Clear })
        ops.forEach { paint(it) }
    }
}

private suspend fun loadPage(easy: Boolean, index: Int): ColoringPage = withContext(Dispatchers.Default) {
    val image = Res.readBytes(ColoringCatalog.path(easy, index, large = true)).decodeToImageBitmap()
    val pixels = IntArray(image.width * image.height)
    image.readPixels(pixels)
    // Outline = clearly visible dark pixel. Transparent / white / very light anti-aliased pixels are "paper".
    val line = BooleanArray(pixels.size) {
        val p = pixels[it]
        val alpha = p ushr 24
        val lum = (((p shr 16) and 255) * 30 + ((p shr 8) and 255) * 59 + (p and 255) * 11) / 100
        alpha >= 128 && lum < 150
    }
    ColoringPage(image, line)
}

/** Colouring page: tap a part of the picture to fill it with the chosen colour. */
@Composable
fun ColoringScreen(easy: Boolean, index: Int, onBack: () -> Unit) {
    val app = LocalAppState.current
    val scope = rememberCoroutineScope()
    val page by produceState<ColoringPage?>(null, easy, index) { value = runCatching { loadPage(easy, index) }.getOrNull() }
    var selected by remember { mutableStateOf(palette[0]) }
    val currentColor by rememberUpdatedState(selected)
    val ops = remember(easy, index) { mutableStateListOf<FillOp>() }
    var version by remember(easy, index) { mutableIntStateOf(0) }
    var busy by remember(easy, index) { mutableStateOf(false) }

    LaunchedEffect(easy, index) {
        app.log(ActivityKind.Draw, "Colouring", "${if (easy) "Easy" else "Hard"} picture ${index + 1}")
    }

    ScreenFrame(if (easy) "Easy colouring" else "Hard colouring", onBack) {
        Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White)
                    .border(3.dp, KidsColors.Sun, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val p = page
                if (p == null) {
                    Text("Loading…", color = Color.Gray)
                } else {
                    Canvas(
                        Modifier.fillMaxSize().pointerInput(p) {
                            detectTapGestures { pos ->
                                if (busy) return@detectTapGestures
                                val scale = minOf(size.width.toFloat() / p.w, size.height.toFloat() / p.h)
                                val x = ((pos.x - (size.width - p.w * scale) / 2f) / scale).toInt()
                                val y = ((pos.y - (size.height - p.h * scale) / 2f) / scale).toInt()
                                if (x !in 0 until p.w || y !in 0 until p.h) return@detectTapGestures
                                busy = true
                                val color = currentColor
                                scope.launch {
                                    val runs = withContext(Dispatchers.Default) { p.fill(x, y) }
                                    if (runs != null) {
                                        val op = FillOp(color, runs)
                                        ops.add(op)
                                        p.paint(op)
                                        version++
                                    }
                                    busy = false
                                }
                            }
                        },
                    ) {
                        @Suppress("UNUSED_VARIABLE") val redraw = version   // read so the canvas redraws after every fill
                        val scale = minOf(size.width / p.w, size.height / p.h)
                        val dstSize = IntSize((p.w * scale).roundToInt(), (p.h * scale).roundToInt())
                        val dstOffset = IntOffset(((size.width - p.w * scale) / 2f).roundToInt(), ((size.height - p.h * scale) / 2f).roundToInt())
                        drawRect(Color.White, Offset(dstOffset.x.toFloat(), dstOffset.y.toFloat()), Size(dstSize.width.toFloat(), dstSize.height.toFloat()))
                        drawImage(p.overlay, dstOffset = dstOffset, dstSize = dstSize, filterQuality = FilterQuality.Low)
                        // Multiply: white paper shows the colour, dark outline stays dark, transparent leaves it untouched.
                        drawImage(p.image, dstOffset = dstOffset, dstSize = dstSize, blendMode = BlendMode.Multiply, filterQuality = FilterQuality.Medium)
                    }
                }
            }

            palette.chunked(8).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    row.forEach { color ->
                        val isSelected = color == selected
                        Box(
                            Modifier.weight(1f).padding(3.dp).aspectRatio(1f).clip(CircleShape).background(color)
                                .border(if (isSelected) 4.dp else 1.dp, if (isSelected) KidsColors.Ink else Color.LightGray, CircleShape)
                                .clickable { selected = color },
                        )
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionPill("↩ Undo", ops.isNotEmpty(), Modifier.weight(1f)) {
                    page?.let { p ->
                        ops.removeAt(ops.lastIndex)
                        p.repaint(ops)
                        version++
                    }
                }
                ActionPill("🗑 Clear", ops.isNotEmpty(), Modifier.weight(1f)) {
                    page?.let { p ->
                        ops.clear()
                        p.repaint(ops)
                        version++
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionPill(label: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.clip(shape).background(if (enabled) KidsColors.HeaderBottom else Color(0xFFBFC8DA))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = 15.sp, style = MaterialTheme.typography.titleMedium) }
}
