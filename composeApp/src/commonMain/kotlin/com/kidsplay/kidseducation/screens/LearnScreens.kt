package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidsplay.kidseducation.AppState
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.rememberAsync
import com.kidsplay.kidseducation.shared.LearningItem
import com.kidsplay.kidseducation.ui.CenteredMessage
import com.kidsplay.kidseducation.ui.GameImage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard
import kotlinx.coroutines.launch

/** Speaks a learning item in the language chosen in Settings (Hindi numbers if available). */
fun AppState.sayItem(item: LearningItem) {
    val hindi = isHindi && item.speakHi != null
    say(if (hindi) item.speakHi!! else item.speak, hindi)
}

/** Grid of everything in one category (the old SubActivity). */
@Composable
fun LearnListScreen(categoryIndex: Int, nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    val category by rememberAsync(categoryIndex) { app.content.learningCategory(categoryIndex) }
    ScreenFrame(category?.title ?: "", onBack) {
        val data = category
        if (data == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(data.items) { index, item ->
                TileCard(onClick = { nav.push(Screen.Flashcards(categoryIndex, index)) }) {
                    Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        GameImage(item.image, Modifier.size(120.dp))
                        Text(item.show, style = MaterialTheme.typography.labelLarge, maxLines = 1, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

/**
 * Swipeable full-screen flash cards (the old FullScreenActivity): colourful title, a big picture card that fills the
 * space, and a bottom control row — round arrow buttons for previous / next with a sound button and the page counter
 * between them. The ad banner stays at the very bottom (ScreenFrame).
 */
@Composable
fun FlashcardsScreen(categoryIndex: Int, start: Int, onBack: () -> Unit) {
    val app = LocalAppState.current
    val category by rememberAsync(categoryIndex) { app.content.learningCategory(categoryIndex) }
    ScreenFrame(category?.title ?: "", onBack) {
        val items = category?.items
        if (items.isNullOrEmpty()) { CenteredMessage("Loading…"); return@ScreenFrame }
        val pager = rememberPagerState(initialPage = start.coerceIn(0, items.lastIndex)) { items.size }
        val scope = rememberCoroutineScope()
        val current = items[pager.currentPage]

        LaunchedEffect(pager.currentPage, app.language) {
            app.sayItem(items[pager.currentPage])
            app.log(ActivityKind.Learn, category?.title.orEmpty(), items[pager.currentPage].show)
        }

        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                current.show,
                color = KidsColors.HeaderBottom, fontSize = 28.sp, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp),
            )
            if (app.isHindi && current.speakHi != null) {
                Text(current.speakHi!!, color = KidsColors.Ink, fontSize = 20.sp, style = MaterialTheme.typography.titleMedium)
            }
            // Picture card: full width, square, centred in the free space.
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) { page ->
                    val cardShape = RoundedCornerShape(30.dp)
                    val accent = KidsColors.Rainbow[page % KidsColors.Rainbow.size]
                    Box(
                        Modifier.fillMaxSize().padding(6.dp).shadow(8.dp, cardShape).clip(cardShape).background(Color.White)
                            .border(4.dp, accent, cardShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                app.sayItem(items[page])
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        // A little smaller than the card, so big pictures (A, 1, 2 …) are not stretched edge to edge.
                        GameImage(items[page].image, Modifier.fillMaxSize(0.52f), ContentScale.Fit)
                    }
                }
            }
            // [ ‹ ]   [ 🔊  3 / 26 ]   [ › ]
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArrowButton(forward = false, enabled = pager.currentPage > 0) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }
                Row(
                    Modifier.shadow(4.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(Color.White)
                        .border(2.dp, KidsColors.Sun, RoundedCornerShape(30.dp)).padding(start = 6.dp, end = 16.dp, top = 5.dp, bottom = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(KidsColors.HeaderBottom)
                            .clickable { app.sayItem(items[pager.currentPage]) },
                        contentAlignment = Alignment.Center,
                    ) { Text("🔊", fontSize = 20.sp) }
                    Text("${pager.currentPage + 1} / ${items.size}", style = MaterialTheme.typography.titleMedium, color = KidsColors.Ink)
                }
                ArrowButton(forward = true, enabled = pager.currentPage < items.lastIndex) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
        }
    }
}

/** Big round previous / next button: gradient disc, white ring, and a chevron drawn on a Canvas (perfectly centred). */
@Composable
private fun ArrowButton(forward: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val top = if (enabled) (if (forward) Color(0xFF66D96B) else Color(0xFFFFB74D)) else Color(0xFFD5D9E0)
    val bottom = if (enabled) (if (forward) Color(0xFF2EAD3A) else Color(0xFFFB8C00)) else Color(0xFFB8BDC7)
    Box(
        Modifier.size(64.dp)
            .then(if (enabled) Modifier.shadow(8.dp, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .border(3.dp, Color.White, CircleShape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(30.dp)) {
            val w = size.width
            val h = size.height
            // chevron, nudged so its optical centre sits on the disc centre
            val x0 = if (forward) w * 0.34f else w * 0.66f
            val x1 = if (forward) w * 0.68f else w * 0.32f
            val path = Path().apply {
                moveTo(x0, h * 0.18f); lineTo(x1, h * 0.5f); lineTo(x0, h * 0.82f)
            }
            drawPath(path, Color.White, style = Stroke(width = w * 0.20f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

@Composable
fun CircleButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, size: Dp = 56.dp, onClick: () -> Unit) {
    Box(
        modifier.size(size).clip(CircleShape)
            .background(if (enabled) KidsColors.HeaderTop.copy(alpha = 0.92f) else Color.LightGray.copy(alpha = 0.7f))
            .let { if (enabled) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(label, color = Color.White, fontSize = (size.value * 0.4f).sp) }
}
