package com.videodownloader.app.ui

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.Prefs
import com.videodownloader.app.core.DownloadState
import com.videodownloader.app.core.Item
import com.videodownloader.app.core.Job
import com.videodownloader.app.engine.Downloads

@Composable
fun HomeScreen(onPlay: (Item) -> Unit) {
    val ctx = LocalContext.current
    val clip = LocalClipboardManager.current
    var url by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    val jobs by Downloads.jobs.collectAsState()
    val done by Downloads.completed.collectAsState()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            GlassCard {
                Text("Video link", color = TextLo, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it; err = null },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("https://example.com/video") }
                    )
                    GlassButton("Paste") { clip.getText()?.text?.let { url = it; err = null } }
                }
                err?.let { Text(it, color = Magenta, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp)) }
                Spacer(Modifier.height(12.dp))
                GradientButton("Download") {
                    val e = Downloads.start(ctx, url)
                    err = e
                    if (e == null) url = ""
                }
                Text(
                    "Your quality: up to ${Prefs.quality(ctx)}p (never above 1440p). Best audio is merged in.",
                    color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        done?.let { d -> item { CompleteCard(d, onPlay) } }
        if (jobs.isNotEmpty()) item { Text("Active downloads", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
        items(jobs, key = { it.id }) { JobCard(it) }
    }
}

@Composable
private fun CompleteCard(d: Item, onPlay: (Item) -> Unit) {
    val ctx = LocalContext.current
    GlassCard(glow = Cyan) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Download Complete", fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            Text(d.title, color = TextLo, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
            Row(Modifier.padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Chip(d.res); Chip(fmtBytes(d.size))
            }
            GlassProgress(1f)
            Text("100%  ·  Verified and saved to Download/Video Downloader/", color = TextLo, fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GradientButton("Play Video", Modifier.weight(1f)) { onPlay(d) }
                GlassButton("Open Folder", Modifier.weight(1f).heightIn(min = 52.dp)) {
                    runCatching {
                        ctx.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                }
            }
            GlassButton("Dismiss", Modifier.padding(top = 8.dp)) { Downloads.dismissCompleted() }
        }
    }
}

@Composable
private fun JobCard(j: Job) {
    val ctx = LocalContext.current
    val st = j.state
    GlassCard {
        Text(j.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
        Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (j.res.isNotEmpty()) Chip(j.res)
            Chip(when (st) {
                is DownloadState.Queued -> "Checking link"
                is DownloadState.Downloading -> "Downloading"
                is DownloadState.Merging -> "Merging"
                is DownloadState.Verifying -> "Verifying"
                is DownloadState.Failed -> "Failed"
                else -> "Stopped"
            })
        }
        when (st) {
            is DownloadState.Downloading -> {
                GlassProgress(st.fraction)
                Spacer(Modifier.height(8.dp))
                val pct = st.fraction?.let { "${(it * 100).toInt()}%" } ?: "Downloading…"
                val bytes = if (j.doneBytes != null && j.totalBytes != null) "  ·  ${fmtBytes(j.doneBytes)} of ${fmtBytes(j.totalBytes)}" else ""
                Text(pct + bytes, color = TextLo, fontSize = 12.sp)
                val sp = st.speedBps?.let { "${"%.1f".format(it / 1e6)} MB/s" }
                val eta = st.etaSec?.let { "${it}s left" }
                val tail = listOfNotNull(sp, eta).joinToString("  ·  ")
                if (tail.isNotEmpty()) Text(tail, color = TextLo, fontSize = 12.sp)
            }
            is DownloadState.Failed -> {
                Text(st.message, color = Magenta, fontSize = 13.sp)
                Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton("Retry", Modifier.weight(1f)) { Downloads.retry(ctx, j.id) }
                    GlassButton("Remove", Modifier.weight(1f)) { Downloads.dismiss(j.id) }
                }
            }
            else -> {
                GlassProgress(null)
                Text(
                    if (st is DownloadState.Merging) "Merging video and audio…" else "Working…",
                    color = TextLo, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        if (!st.isTerminal) {
            GlassButton("Cancel", Modifier.padding(top = 12.dp)) { Downloads.cancel(ctx, j.id) }
        }
    }
}
