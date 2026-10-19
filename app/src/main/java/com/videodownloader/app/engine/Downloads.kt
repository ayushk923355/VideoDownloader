package com.videodownloader.app.engine

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.videodownloader.app.Prefs
import com.videodownloader.app.core.*
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Owns all active downloads. Progress comes from yt-dlp's real byte counts, never from a timer. */
object Downloads {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _jobs = MutableStateFlow<List<Job>>(emptyList())
    val jobs: StateFlow<List<Job>> = _jobs.asStateFlow()
    private val _completed = MutableStateFlow<Item?>(null)
    val completed: StateFlow<Item?> = _completed.asStateFlow()
    private val cancelled = ConcurrentHashMap.newKeySet<String>()

    fun dismissCompleted() { _completed.value = null }
    fun dismiss(id: String) { _jobs.update { l -> l.filter { it.id != id } } }

    private fun upd(id: String, f: (Job) -> Job) =
        _jobs.update { l -> l.map { if (it.id == id) f(it) else it } }

    private fun move(id: String, to: DownloadState, extra: (Job) -> Job = { it }) =
        _jobs.update { l ->
            l.map { if (it.id == id && StateMachine.canTransition(it.state, to)) extra(it).copy(state = to) else it }
        }

    /** Returns an error message for the UI, or null when the download started. */
    fun start(c: Context, raw: String): String? {
        val url = raw.trim()
        val uri = runCatching { Uri.parse(url) }.getOrNull()
        val scheme = uri?.scheme?.lowercase()
        if (uri == null || (scheme != "http" && scheme != "https") || uri.host.isNullOrBlank())
            return "Enter a valid link that starts with http:// or https://"
        if (_jobs.value.any { it.url == url && !it.state.isTerminal }) return "This link is already downloading."
        if (c.filesDir.usableSpace < 300L * 1024 * 1024) return "Not enough free storage. Free up some space and try again."
        val job = Job(id = UUID.randomUUID().toString().take(8), url = url)
        _jobs.update { it + job }
        val app = c.applicationContext
        DownloadService.start(app)
        scope.launch { runJob(app, job.id) }
        return null
    }

    fun retry(c: Context, id: String) {
        val app = c.applicationContext
        cancelled.remove(id)
        upd(id) { it.copy(state = DownloadState.Queued, title = "Checking link…", doneBytes = null) }
        DownloadService.start(app)
        scope.launch { runJob(app, id) }
    }

    fun cancel(c: Context, id: String) {
        cancelled.add(id)
        runCatching { YoutubeDL.getInstance().destroyProcessById(id) }
        dismiss(id)
        File(c.filesDir, "dl/$id").deleteRecursively()
    }

    private fun fail(id: String, msg: String) {
        move(id, DownloadState.Failed(msg))
    }

    private fun friendly(e: Exception): String {
        val m = (e.message ?: "").lowercase()
        return when {
            "unsupported url" in m -> "This link isn’t supported. Only sites that yt-dlp supports can be downloaded."
            "drm" in m -> "This video is DRM-protected and can’t be saved."
            "sign in" in m || "login" in m || "private" in m || "members-only" in m ->
                "YouTube requires sign-in for this video. Try a public, non-age-restricted video link; private/member-only videos aren't supported."
            "requested format is not available" in m -> "No compatible stream was found at this quality. Try a lower quality in Settings."
            "confirm you’re not a bot" in m || "po token" in m || "verify you’re human" in m ->
                "YouTube blocked this request for verification. Try again later or use a publicly accessible video. The app can't bypass YouTube verification."
            "resolve host" in m || "timed out" in m || "connection" in m || "network" in m ->
                "Connection problem. Check your internet and retry."
            else -> "Download failed. Check the link and try again."
        }
    }

    private suspend fun runJob(app: Context, id: String) {
        val job = _jobs.value.firstOrNull { it.id == id } ?: return
        val tmp = File(app.filesDir, "dl/$id").also { it.mkdirs() }
        try {
            if (!Engine.ready.await()) return fail(id, "The download engine couldn’t start. Restart the app and try again.")
            val info = Inspector.inspect(job.url)
            if (id in cancelled) return
            val q = Prefs.quality(app)
            val sel = FormatSelector.select(info.formats, q)
            if (sel !is Selection.Chosen)
                return fail(id, "No stream at or under ${minOf(q, MAX_HEIGHT)}p was found, so nothing was downloaded.")

            val streams = listOfNotNull(sel.video, sel.audio)
            val weights = streams.map { info.sizes[it.id] ?: 0L }
            val total = if (weights.all { it > 0 }) weights.sum() else null
            val label = "${minOf(sel.video.width!!, sel.video.height!!)}p"
            upd(id) { it.copy(title = info.title, res = label, totalBytes = total) }
            move(id, DownloadState.Downloading(null))

            val req = YoutubeDLRequest(job.url).apply {
                addOption("-f", streams.joinToString("+") { it.id })
                addOption("--no-playlist")
                addOption("--no-mtime")
                addOption("--merge-output-format", "mp4")
                addOption("-o", tmp.absolutePath + "/out.%(ext)s")
            }

            var idx = 0
            var lastT = System.nanoTime()
            var lastB = 0L
            var speed = 0L
            YoutubeDL.getInstance().execute(req, id) { p, _, line ->
                val l = line ?: ""
                if (l.contains("[download] Destination:")) idx++
                if (l.contains("[Merger]")) {
                    move(id, DownloadState.Merging)
                } else if (p >= 0f && idx > 0) {
                    val i = idx.coerceIn(1, streams.size)
                    val frac = if (total != null)
                        (weights.take(i - 1).sum() + weights[i - 1] * (p / 100.0)) / total
                    else (i - 1 + p / 100.0) / streams.size
                    // Combined % is only trustworthy with known sizes, or a single stream.
                    val f: Float? = if (total == null && streams.size > 1) null
                    else frac.coerceIn(0.0, 0.99).toFloat()
                    val doneB = total?.let { (it * frac).toLong() }
                    val now = System.nanoTime()
                    if (doneB != null && now - lastT > 500_000_000L) {
                        speed = ((doneB - lastB) * 1e9 / (now - lastT)).toLong().coerceAtLeast(0)
                        lastT = now; lastB = doneB
                    }
                    val eta = if (total != null && doneB != null && speed > 0) (total - doneB) / speed else null
                    move(id, DownloadState.Downloading(f, if (speed > 0) speed else null, eta)) {
                        it.copy(doneBytes = doneB)
                    }
                }
            }
            if (id in cancelled) return
            move(id, DownloadState.Verifying)

            val out = tmp.listFiles()?.firstOrNull {
                Regex("out\\.[A-Za-z0-9]+").matches(it.name) && it.length() > 0
            }
            if (out == null || !CompletionVerifier.verify(out))
                return fail(id, "The finished file is missing or empty, so it wasn’t saved.")
            checkDims(out)?.let { return fail(id, it) }

            val size = out.length()
            val uri = Storage.save(app, out, info.title, out.extension.lowercase())
            tmp.deleteRecursively()
            val item = Item(id, info.title, uri.toString(), label, size, System.currentTimeMillis())
            History.add(app, item)
            dismiss(id)
            _completed.value = item
        } catch (e: Exception) {
            Log.e("VD", "job failed: " + e.javaClass.simpleName)
            if (id in cancelled) return
            fail(id, friendly(e))
        }
    }

    /** Re-checks the real file, so the 1440p cap holds even if a stream label was wrong. */
    private fun checkDims(f: File): String? = runCatching {
        val r = MediaMetadataRetriever()
        r.setDataSource(f.absolutePath)
        val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toInt()
        val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toInt()
        r.release()
        if (w != null && h != null && (maxOf(w, h) > MAX_WIDTH || minOf(w, h) > MAX_HEIGHT))
            "The finished video is above 1440p, so it was discarded." else null
    }.getOrNull()
}
