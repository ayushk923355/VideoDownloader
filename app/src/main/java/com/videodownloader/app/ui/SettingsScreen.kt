package com.videodownloader.app.ui

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.Prefs
import com.videodownloader.app.engine.History
import com.videodownloader.app.engine.Storage

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    var q by remember { mutableIntStateOf(Prefs.quality(ctx)) }
    val items by History.items.collectAsState()
    val stat = remember { runCatching { StatFs(Environment.getExternalStorageDirectory().path) }.getOrNull() }
    val free = stat?.availableBytes ?: 0L
    val total = stat?.totalBytes ?: 0L
    val used = items.sumOf { it.size }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
        GlassCard {
            Text("Default video quality", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                "If this quality isn’t available, the highest one below it is downloaded. Never above 1440p.",
                color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(vertical = 6.dp)
            )
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Prefs.qualities.forEach { v ->
                    val on = v == q
                    val shape = RoundedCornerShape(14.dp)
                    Box(
                        Modifier.heightIn(min = 44.dp).clip(shape)
                            .then(if (on) Modifier.background(Accent) else Modifier.background(Color.White.copy(.1f)).border(1.dp, Color.White.copy(.25f), shape))
                            .clickable { q = v; Prefs.setQuality(ctx, v) }.padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { Text("${v}p", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (on) Color.White else TextLo) }
                }
            }
        }
        GlassCard {
            Text("Audio", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Best available audio, merged into the same file.", color = TextLo, fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp))
        }
        GlassCard {
            Text("Storage", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(Storage.SHOWN_PATH, color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp))
            GlassProgress(if (total > 0) ((total - free).toFloat() / total) else 0f)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Videos here: ${fmtBytes(used)}", color = TextLo, fontSize = 12.sp)
                Text("Free: ${fmtBytes(free)}", color = TextLo, fontSize = 12.sp)
            }
        }
        GlassCard {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Video Downloader", fontWeight = FontWeight.Bold)
                Text("Version 0.2.0", color = TextLo, fontSize = 13.sp)
            }
        }
    }
}
