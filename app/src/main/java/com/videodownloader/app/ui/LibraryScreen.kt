package com.videodownloader.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videodownloader.app.core.Item
import com.videodownloader.app.engine.History
import com.videodownloader.app.engine.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable
fun LibraryScreen(onPlay: (List<Item>, Int) -> Unit) {
    val ctx = LocalContext.current
    val items by History.items.collectAsState()
    var missing by remember { mutableStateOf(setOf<String>()) }
    var toDelete by remember { mutableStateOf<Item?>(null) }

    // Detect files deleted outside the app.
    LaunchedEffect(items) {
        missing = withContext(Dispatchers.IO) { items.filter { !Storage.exists(ctx, it.uri) }.map { it.id }.toSet() }
    }
    val playable = items.filter { it.id !in missing }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Library", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp) }
        if (items.isEmpty()) item {
            GlassCard {
                Text("Start your first download", fontWeight = FontWeight.Bold)
                Text("Finished videos show up here and play from here.", color = TextLo, fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
        items(items, key = { it.id }) { it ->
            GlassCard {
                Text(it.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2)
                val date = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it.dateMs))
                Text("${it.res}  ·  ${fmtBytes(it.size)}  ·  $date", color = TextLo, fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 4.dp))
                if (it.id in missing) {
                    Text("File not found. It was deleted outside the app.", color = Magenta, fontSize = 13.sp)
                    GlassButton("Remove entry", Modifier.padding(top = 10.dp)) { History.remove(ctx, it.id) }
                } else {
                    Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GlassButton("Play") { onPlay(playable, playable.indexOfFirst { p -> p.id == it.id }) }
                        GlassButton("Share") { share(ctx, it) }
                        GlassButton("Delete") { toDelete = it }
                    }
                }
            }
        }
    }

    toDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Delete this video?") },
            text = { Text("${d.title} will be removed from your phone. This can’t be undone.") },
            confirmButton = {
                TextButton(onClick = { Storage.delete(ctx, d.uri); History.remove(ctx, d.id); toDelete = null }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Keep") } }
        )
    }
}

private fun share(ctx: android.content.Context, item: Item) {
    runCatching {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, Uri.parse(item.uri))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Share video").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
