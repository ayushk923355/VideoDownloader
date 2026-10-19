package com.videodownloader.app.playback

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.videodownloader.app.core.Item
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** The UI talks to the playback service through this MediaController. */
object PlayerHolder {
    var controller by mutableStateOf<Player?>(null)
        private set

    fun connect(c: Context) {
        if (controller != null) return
        val app = c.applicationContext
        val token = SessionToken(app, ComponentName(app, PlaybackService::class.java))
        val future = MediaController.Builder(app, token).buildAsync()
        future.addListener({
            controller = runCatching { future.get() }.getOrNull()
        }, ContextCompat.getMainExecutor(app))
    }

    /** Queues the whole list so next/previous work and playback continues track after track. */
    fun play(items: List<Item>, index: Int) {
        val p = controller ?: return
        if (items.isEmpty()) return
        val media = items.map {
            MediaItem.Builder()
                .setMediaId(it.id)
                .setUri(it.uri)
                .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).build())
                .build()
        }
        p.setMediaItems(media, index.coerceIn(0, media.lastIndex), 0L)
        p.prepare()
        p.play()
    }
}

class PB {
    var playing by mutableStateOf(false)
    var title by mutableStateOf("")
    var pos by mutableLongStateOf(0L)
    var dur by mutableLongStateOf(0L)
    var hasMedia by mutableStateOf(false)
}

@Composable
fun rememberPlayback(p: Player?): PB {
    val s = remember { PB() }
    LaunchedEffect(p) {
        while (isActive) {
            if (p != null) {
                s.playing = p.isPlaying
                s.hasMedia = p.mediaItemCount > 0
                s.title = p.mediaMetadata.title?.toString().orEmpty()
                s.pos = p.currentPosition
                s.dur = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            } else s.hasMedia = false
            delay(400)
        }
    }
    return s
}
