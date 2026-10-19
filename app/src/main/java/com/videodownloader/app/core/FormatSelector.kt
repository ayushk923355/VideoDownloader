package com.videodownloader.app.core

/** Hard cap: a 2560 x 1440 box, rotated for portrait (long side <= 2560, short side <= 1440). */
const val MAX_WIDTH = 2560
const val MAX_HEIGHT = 1440

data class MediaFormat(
    val id: String,
    val width: Int?,
    val height: Int?,
    val hasVideo: Boolean,
    val hasAudio: Boolean,
    val bitrateKbps: Double? = null,
)

sealed interface Selection {
    data class Chosen(val video: MediaFormat, val audio: MediaFormat?) : Selection
    data object NoSupportedResolution : Selection
}

object FormatSelector {
    /** userMax is the user's preferred quality (short side, e.g. 1080). It can never raise the hard cap. */
    fun withinLimit(f: MediaFormat, userMax: Int = MAX_HEIGHT): Boolean {
        val w = f.width ?: return false
        val h = f.height ?: return false
        val long = maxOf(w, h)
        val short = minOf(w, h)
        return short in 1..minOf(userMax, MAX_HEIGHT) && long <= MAX_WIDTH
    }

    /** Best video at or below the limit (highest available if the wanted one is missing), plus best audio. */
    fun select(formats: List<MediaFormat>, userMax: Int = MAX_HEIGHT): Selection {
        val video = formats
            .filter { it.hasVideo && withinLimit(it, userMax) }
            .maxWithOrNull(
                compareBy<MediaFormat>({ it.width!!.toLong() * it.height!! }, { it.bitrateKbps ?: 0.0 })
            ) ?: return Selection.NoSupportedResolution

        if (video.hasAudio) return Selection.Chosen(video, null)

        val audio = formats
            .filter { it.hasAudio && !it.hasVideo }
            .maxByOrNull { it.bitrateKbps ?: 0.0 }
        return Selection.Chosen(video, audio)
    }
}
