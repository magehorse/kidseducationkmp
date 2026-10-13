package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.platform.LocalDeps
import com.kidsplay.kidseducation.platform.ReelVideoPlayer
import com.kidsplay.kidseducation.shared.ReelItem
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenHeader

private sealed interface ReelState {
    data object Loading : ReelState
    data object Failed : ReelState
    data object Empty : ReelState
    data class Ready(val reels: List<ReelItem>) : ReelState
}

/**
 * Instagram-style vertical video feed (the old ReelActivity):
 * blue header like every other screen -> full-height reels with a thin progress bar -> bottom banner ad.
 */
@Composable
fun ReelsScreen(onBack: () -> Unit) {
    val app = LocalAppState.current
    var attempt by remember { mutableIntStateOf(0) }
    var state by remember { mutableStateOf<ReelState>(ReelState.Loading) }

    LaunchedEffect(attempt) {
        state = ReelState.Loading
        state = app.reelApi.reels().fold(
            onSuccess = { if (it.isEmpty()) ReelState.Empty else ReelState.Ready(it) },
            onFailure = { ReelState.Failed },
        )
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        ScreenHeader("Kids Reels", onBack)

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (val s = state) {
                ReelState.Loading -> Text("Loading…", color = Color.White, modifier = Modifier.align(Alignment.Center))
                ReelState.Empty -> Text("No videos yet.", color = Color.White, modifier = Modifier.align(Alignment.Center))
                ReelState.Failed -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Couldn't load videos. Check your connection.", color = Color.White)
                    BigButton("Try again", { attempt += 1 }, color = KidsColors.HeaderTop)
                }
                is ReelState.Ready -> {
                    val pager = rememberPagerState { s.reels.size }
                    LaunchedEffect(pager.currentPage) {
                        app.log(ActivityKind.Reels, "Kids Reels", "Watched reel ${pager.currentPage + 1}")
                    }
                    VerticalPager(pager, Modifier.fillMaxSize()) { page ->
                        val reel = s.reels[page]
                        var progress by remember(page) { mutableFloatStateOf(0f) }
                        var paused by remember(page) { mutableStateOf(false) }
                        Box(
                            Modifier.fillMaxSize().clickable(
                                interactionSource = remember { MutableInteractionSource() }, indication = null,
                            ) { if (reel.video) paused = !paused },   // tap anywhere on the video = play / pause
                        ) {
                            if (reel.video) {
                                ReelVideoPlayer(
                                    reel.image,
                                    isActive = pager.currentPage == page && !paused,
                                    modifier = Modifier.fillMaxSize(),
                                    onProgress = { progress = it },
                                )
                            } else {
                                AsyncImage(reel.image, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            Text(
                                reel.language.replaceFirstChar { it.uppercase() },
                                color = Color.White,
                                modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 26.dp),
                            )
                            if (reel.video) {
                                if (paused) {
                                    // Big play badge while paused
                                    Box(
                                        Modifier.align(Alignment.Center).size(76.dp).clip(CircleShape).background(Color(0x66000000)),
                                        contentAlignment = Alignment.Center,
                                    ) { PlayPauseIcon(playing = false, Modifier.size(40.dp)) }
                                }
                                // Always-visible play / pause button
                                Box(
                                    Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 16.dp).size(46.dp)
                                        .clip(CircleShape).background(Color(0x80000000))
                                        .clickable { paused = !paused },
                                    contentAlignment = Alignment.Center,
                                ) { PlayPauseIcon(playing = !paused, Modifier.size(24.dp)) }
                                ReelProgressBar(progress, Modifier.align(Alignment.BottomStart))
                            }
                        }
                    }
                }
            }
        }

        // Bottom banner ad (the old activity_reel had its own llAdView on a black background).
        LocalDeps.current.ads.Banner(Modifier.fillMaxWidth().background(Color.Black))
    }
}

/** Thin progress line on the very bottom edge of a reel, like Instagram Reels. */
@Composable
private fun ReelProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(3.dp).background(Color(0x40FFFFFF))) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(KidsColors.Accent))
    }
}

/** Pause bars / play triangle drawn on a Canvas so they are always perfectly centred. */
@Composable
private fun PlayPauseIcon(playing: Boolean, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        if (playing) {
            val bar = w * 0.2f
            drawRoundRect(Color.White, Offset(w * 0.26f, h * 0.2f), Size(bar, h * 0.6f), CornerRadius(bar / 3f))
            drawRoundRect(Color.White, Offset(w * 0.54f, h * 0.2f), Size(bar, h * 0.6f), CornerRadius(bar / 3f))
        } else {
            val triangle = Path().apply {
                moveTo(w * 0.34f, h * 0.2f); lineTo(w * 0.34f, h * 0.8f); lineTo(w * 0.8f, h * 0.5f); close()
            }
            drawPath(triangle, Color.White)
        }
    }
}
