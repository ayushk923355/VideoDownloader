package com.videodownloader.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.core.Item
import com.videodownloader.app.engine.History
import com.videodownloader.app.playback.PlayerHolder
import com.videodownloader.app.playback.rememberPlayback
import com.videodownloader.app.ui.*

class MainActivity : ComponentActivity() {
    private val perms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        History.load(this)
        PlayerHolder.connect(this)
        val ask = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= 33) ask += Manifest.permission.POST_NOTIFICATIONS
        if (Build.VERSION.SDK_INT <= 28) ask += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (ask.isNotEmpty()) perms.launch(ask.toTypedArray())
        setContent { VDTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showPlayer by remember { mutableStateOf(false) }
    val player = PlayerHolder.controller
    val pb = rememberPlayback(player)

    fun playList(list: List<Item>, index: Int) {
        PlayerHolder.play(list, index)
        showPlayer = true
    }

    Box(Modifier.fillMaxSize()) {
        Backdrop {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Text("Video Downloader", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp,
                    modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp))
                Box(Modifier.weight(1f)) {
                    when (tab) {
                        0 -> HomeScreen(onPlay = { item ->
                            val all = History.items.value
                            playList(all, all.indexOfFirst { it.id == item.id }.coerceAtLeast(0))
                        })
                        1 -> LibraryScreen(onPlay = { list, i -> playList(list, i) })
                        else -> SettingsScreen()
                    }
                }
                if (player != null && pb.hasMedia) MiniPlayer(player, pb) { showPlayer = true }
                NavBar(tab) { tab = it }
            }
        }
        if (showPlayer && player != null) {
            BackHandler { showPlayer = false }
            PlayerScreen(player, pb) { showPlayer = false }
        }
    }
}

@Composable
private fun NavBar(selected: Int, onSelect: (Int) -> Unit) {
    val names = listOf("Home", "Library", "Settings")
    GlassCard(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        BoxWithConstraints {
            val gap = 6.dp
            val itemWidth = (maxWidth - gap * 2) / 3
            val offset by animateDpAsState(targetValue = (itemWidth + gap) * selected, animationSpec = spring(dampingRatio = 0.82f, stiffness = 500f), label = "nav-slide")
            Box(Modifier.offset(x = offset).width(itemWidth).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Accent))
            Row(horizontalArrangement = Arrangement.spacedBy(gap), modifier = Modifier.fillMaxWidth().offset(y = (-46).dp)) {
                names.forEachIndexed { i, n ->
                    val shape = RoundedCornerShape(16.dp)
                    Box(Modifier.weight(1f).height(46.dp).clip(shape).clickable { onSelect(i) }, contentAlignment = Alignment.Center) {
                        Text(n, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (i == selected) Color.White else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}
