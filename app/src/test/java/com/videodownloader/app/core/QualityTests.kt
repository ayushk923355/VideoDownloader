package com.videodownloader.app.core

import org.junit.Assert.*
import org.junit.Test

class QualityTests {
    private fun v(id: String, w: Int, h: Int, br: Double = 1.0) = MediaFormat(id, w, h, true, false, br)
    private fun a(id: String, br: Double) = MediaFormat(id, null, null, false, true, br)

    @Test fun userCap1080PicksHighestAtOrBelow() {
        val r = FormatSelector.select(listOf(v("1440", 2560, 1440), v("1080", 1920, 1080), v("720", 1280, 720)), 1080)
        assertEquals("1080", (r as Selection.Chosen).video.id)
    }
    @Test fun missingQualityFallsToHighestBelow() {
        val r = FormatSelector.select(listOf(v("720", 1280, 720), v("480", 854, 480)), 1080)
        assertEquals("720", (r as Selection.Chosen).video.id)
    }
    @Test fun userCapAbove1440IsClamped() {
        val r = FormatSelector.select(listOf(v("2160", 3840, 2160), v("1440", 2560, 1440)), 4000)
        assertEquals("1440", (r as Selection.Chosen).video.id)
    }
    @Test fun portrait1080pIsAllowedAt1080Cap() {
        val r = FormatSelector.select(listOf(v("p1080", 1080, 1920), v("p720", 720, 1280)), 1080)
        assertEquals("p1080", (r as Selection.Chosen).video.id)
    }
    @Test fun onlyHigherStreamsFailsSafely() {
        assertEquals(Selection.NoSupportedResolution, FormatSelector.select(listOf(v("1080", 1920, 1080)), 144))
    }
    @Test fun audioOnlyFormatIsNeverChosenAsVideo() {
        assertEquals(Selection.NoSupportedResolution, FormatSelector.select(listOf(a("m4a", 128.0))))
    }
}
