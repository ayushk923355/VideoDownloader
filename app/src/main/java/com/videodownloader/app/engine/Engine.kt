package com.videodownloader.app.engine

import android.app.Application
import com.videodownloader.app.core.MediaFormat
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/** Starts yt-dlp + FFmpeg (bundled native libs) once, in the background. */
object Engine {
    val ready = CompletableDeferred<Boolean>()

    fun init(app: Application) {
        CoroutineScope(Dispatchers.IO).launch {
            val ok = runCatching {
                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)
            }.isSuccess
            ready.complete(ok)
        }
    }
}

data class VideoInfo(val title: String, val formats: List<MediaFormat>, val sizes: Map<String, Long>)

object Inspector {
    private fun JSONObject.str(k: String): String? = if (isNull(k)) null else optString(k)
    private fun JSONObject.int(k: String): Int? = if (isNull(k)) null else optInt(k)

    /** Asks yt-dlp for the real list of streams. Nothing is guessed. */
    fun inspect(url: String): VideoInfo {
        val req = YoutubeDLRequest(url).apply {
            addOption("--dump-single-json")
            addOption("--no-playlist")
            addOption("--no-warnings")
        }
        val out = YoutubeDL.getInstance().execute(req).out
        val o = JSONObject(out)
        val arr: JSONArray = o.optJSONArray("formats") ?: JSONArray()
        val formats = ArrayList<MediaFormat>()
        val sizes = HashMap<String, Long>()
        for (i in 0 until arr.length()) {
            val f = arr.getJSONObject(i)
            val id = f.str("format_id") ?: continue
            val vc = f.str("vcodec")
            val ac = f.str("acodec")
            val br = if (!f.isNull("tbr")) f.optDouble("tbr") else if (!f.isNull("abr")) f.optDouble("abr") else null
            formats += MediaFormat(
                id = id, width = f.int("width"), height = f.int("height"),
                hasVideo = vc != null && vc != "none",
                hasAudio = ac != null && ac != "none",
                bitrateKbps = br?.takeIf { !it.isNaN() && it > 0 },
            )
            val size = if (!f.isNull("filesize")) f.optLong("filesize")
            else if (!f.isNull("filesize_approx")) f.optLong("filesize_approx") else 0L
            if (size > 0) sizes[id] = size
        }
        return VideoInfo(o.str("title") ?: "Video", formats, sizes)
    }
}
