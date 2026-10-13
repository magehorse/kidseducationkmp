package com.kidsplay.kidseducation.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import kotlinx.coroutines.delay
import java.util.Locale

class AndroidSpeaker(context: Context) : Speaker {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context) { status -> ready = status == TextToSpeech.SUCCESS }

    override fun speak(text: String, languageTag: String) {
        if (!ready) return
        tts.language = Locale.forLanguageTag(languageTag)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "kidsgame")
    }

    override fun stop() { tts.stop() }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}

class AndroidLinkOpener(private val context: Context) : LinkOpener {
    override fun open(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onBack)
}

@OptIn(UnstableApi::class)
@Composable
actual fun ReelVideoPlayer(url: String, isActive: Boolean, modifier: Modifier, onProgress: (Float) -> Unit) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ONE
            prepare()
        }
    }
    val latestOnProgress by rememberUpdatedState(onProgress)
    LaunchedEffect(isActive, player) {
        player.playWhenReady = isActive
        if (isActive) {
            // Drives the thin progress bar at the bottom of each reel (like Instagram Reels).
            while (true) {
                val duration = player.duration
                if (duration > 0) latestOnProgress((player.currentPosition.toFloat() / duration).coerceIn(0f, 1f))
                delay(100)
            }
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        update = { it.player = player },
    )
}

@Composable
actual fun InAppYouTubePlayer(videoId: String, modifier: Modifier) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<YouTubePlayer?>(null) }
    val playerNow by rememberUpdatedState(player)
    val view = remember {
        YouTubePlayerView(context).apply {
            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    player = youTubePlayer
                }
            })
        }
    }
    // Loads the first video once the player is ready, and every time another video is picked.
    LaunchedEffect(videoId, player) { player?.loadVideo(videoId, 0f) }

    DisposableEffect(view) {
        val owner = context as? LifecycleOwner
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) playerNow?.pause()   // stop sound when the app is left
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose {
            owner?.lifecycle?.removeObserver(observer)
            view.release()
        }
    }
    AndroidView(factory = { view }, modifier = modifier)
}
