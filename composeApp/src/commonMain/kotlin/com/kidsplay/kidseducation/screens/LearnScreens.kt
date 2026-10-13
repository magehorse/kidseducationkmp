package com.kidsplay.kidseducation.screens

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
 * Swipeable full-screen flash cards (the old FullScreenActivity): big title on top, then the picture fills ALL the
 * remaining space (A for Apple -> a huge apple), with previous / next arrows on the sides.
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
            Box(Modifier.weight(1f).fillMaxWidth()) {
                HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                    Box(
                        Modifier.fillMaxSize().padding(4.dp)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                app.sayItem(items[page])
                            },
                        contentAlignment = Alignment.Center,
                    ) { GameImage(items[page].image, Modifier.fillMaxSize(), ContentScale.Fit) }
                }
                CircleButton("◀", enabled = pager.currentPage > 0, size = 48.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 6.dp)) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage - 1) }
                }
                CircleButton("▶", enabled = pager.currentPage < items.lastIndex, size = 48.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 6.dp)) {
                    scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("${pager.currentPage + 1} / ${items.size}", style = MaterialTheme.typography.titleMedium)
                CircleButton("🔊", size = 44.dp, modifier = Modifier.padding(start = 16.dp)) { app.sayItem(items[pager.currentPage]) }
            }
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
