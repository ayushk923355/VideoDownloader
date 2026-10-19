package com.videodownloader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import com.videodownloader.app.playback.PB

@Composable
fun PlayerScreen(player: Player, pb: PB, onClose: () -> Unit) {
    var drag by remember { mutableStateOf<Float?>(null) }
    val dur = pb.dur.coerceAtLeast(1L)
    Backdrop {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                GlassButton("Close") { onClose() }
                Spacer(Modifier.weight(1f))
                Text("Now playing", color = TextLo, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.width(72.dp))
            }
            Spacer(Modifier.height(16.dp))
            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(22.dp)).background(Color.Black),
                factory = { c -> PlayerView(c).apply { useController = false; this.player = player } },
                update = { it.player = player }
            )
            Text(pb.title, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 2,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
            Slider(
                value = drag ?: (pb.pos.toFloat() / dur),
                onValueChange = { drag = it },
                onValueChangeFinished = { drag?.let { player.seekTo((it * dur).toLong()) }; drag = null }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(fmtTime(pb.pos), color = TextLo, fontSize = 12.sp)
                Text(fmtTime(pb.dur), color = TextLo, fontSize = 12.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                GlassButton("Prev") { player.seekToPreviousMediaItem() }
                GlassButton("-10s") { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }
                Box(
                    Modifier.size(64.dp).clip(RoundedCornerShape(32.dp)).background(Accent)
                        .clickable { if (player.isPlaying) player.pause() else player.play() },
                    contentAlignment = Alignment.Center
                ) { Text(if (pb.playing) "Pause" else "Play", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
                GlassButton("+10s") { player.seekTo(player.currentPosition + 10_000) }
                GlassButton("Next") { player.seekToNextMediaItem() }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun MiniPlayer(player: Player, pb: PB, onOpen: () -> Unit) {
    GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).clickable { onOpen() }) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(pb.title, modifier = Modifier.weight(1f), maxLines = 1, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            GlassButton(if (pb.playing) "Pause" else "Play") { if (player.isPlaying) player.pause() else player.play() }
        }
    }
}
