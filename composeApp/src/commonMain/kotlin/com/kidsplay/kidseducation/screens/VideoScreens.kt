package com.kidsplay.kidseducation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kidsplay.kidseducation.LocalAppState
import com.kidsplay.kidseducation.data.ActivityKind
import com.kidsplay.kidseducation.nav.Navigator
import com.kidsplay.kidseducation.platform.InAppYouTubePlayer
import com.kidsplay.kidseducation.shared.VideoItem
import com.kidsplay.kidseducation.nav.Screen
import com.kidsplay.kidseducation.rememberAsync
import com.kidsplay.kidseducation.ui.BigButton
import com.kidsplay.kidseducation.ui.CenteredMessage
import com.kidsplay.kidseducation.ui.KidsColors
import com.kidsplay.kidseducation.ui.ScreenFrame
import com.kidsplay.kidseducation.ui.TileCard

@Composable
fun VideoListScreen(categoryId: Int, nav: Navigator, onBack: () -> Unit) {
    val app = LocalAppState.current
    val categories by rememberAsync { app.content.videoCategories() }
    val category = categories?.firstOrNull { it.id == categoryId }
    ScreenFrame(category?.title ?: "Videos", onBack) {
        if (category == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        LazyColumn(
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(category.videos) { video ->
                VideoRow(video, selected = false) { nav.push(Screen.VideoPlay(categoryId, video.videoId)) }
            }
        }
    }
}

@Composable
private fun VideoRow(video: VideoItem, selected: Boolean, onClick: () -> Unit) {
    TileCard(Modifier.fillMaxWidth(), borderColor = if (selected) KidsColors.Sun else Color.White, onClick = onClick) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = video.thumbnailUrl, contentDescription = null,
                modifier = Modifier.width(140.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop,
            )
            Text(
                video.title, Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleMedium, fontSize = 13.sp,
                maxLines = 3, overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Plays the video INSIDE the app (like the old VideoPlayActivity): player on top, current title on a blue
 * strip, and the other videos of the same category below — tap one to switch without leaving the screen.
 */
@Composable
fun VideoPlayScreen(categoryId: Int, videoId: String, onBack: () -> Unit) {
    val app = LocalAppState.current
    val categories by rememberAsync { app.content.videoCategories() }
    val category = categories?.firstOrNull { it.id == categoryId }
    var currentId by remember(videoId) { mutableStateOf(videoId) }
    ScreenFrame(category?.title ?: "Video", onBack) {
        if (category == null) { CenteredMessage("Loading…"); return@ScreenFrame }
        val current = category.videos.firstOrNull { it.videoId == currentId }
        LaunchedEffect(currentId) { app.log(ActivityKind.Video, category.title, current?.title.orEmpty()) }
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                InAppYouTubePlayer(currentId, Modifier.fillMaxSize())
            }
            Text(
                current?.title ?: "",
                modifier = Modifier.fillMaxWidth().background(KidsColors.HeaderBottom).padding(horizontal = 12.dp, vertical = 8.dp),
                color = Color.White, style = MaterialTheme.typography.titleMedium, fontSize = 14.sp,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(category.videos) { video ->
                    VideoRow(video, selected = video.videoId == currentId) { currentId = video.videoId }
                }
            }
        }
    }
}
