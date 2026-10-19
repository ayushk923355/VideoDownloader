package com.videodownloader.app.core

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class CoreTests {
    private fun v(id: String, w: Int?, h: Int?, audio: Boolean = false, br: Double = 1.0) =
        MediaFormat(id, w, h, true, audio, br)
    private fun a(id: String, br: Double) = MediaFormat(id, null, null, false, true, br)

    @Test fun neverPicksAbove1440p() {
        val r = FormatSelector.select(listOf(v("4k", 3840, 2160), v("2k", 2560, 1440), v("1080", 1920, 1080)))
        assertEquals("2k", (r as Selection.Chosen).video.id)
    }
    @Test fun fallsBackBelow1440() {
        val r = FormatSelector.select(listOf(v("4k", 3840, 2160), v("720", 1280, 720)))
        assertEquals("720", (r as Selection.Chosen).video.id)
    }
    @Test fun portraitRespectsBothDimensions() {
        val r = FormatSelector.select(listOf(v("p4k", 2160, 3840), v("p1440", 1440, 2560)))
        assertEquals("p1440", (r as Selection.Chosen).video.id)
    }
    @Test fun unknownDimensionsFailSafely() {
        assertEquals(Selection.NoSupportedResolution, FormatSelector.select(listOf(v("x", null, null))))
    }
    @Test fun picksBestAudio() {
        val r = FormatSelector.select(listOf(v("1080", 1920, 1080), a("lo", 64.0), a("hi", 160.0))) as Selection.Chosen
        assertEquals("hi", r.audio?.id)
    }
    @Test fun sanitizerBlocksTraversal() {
        val s = FilenameSanitizer.sanitize("../../etc/pa:ss*wd")
        assertFalse(s.contains("/")); assertFalse(s.contains(".."))
    }
    @Test fun uniqueNameAddsSuffix() {
        val taken = setOf("a.mp4", "a (1).mp4")
        assertEquals("a (2).mp4", FilenameSanitizer.uniqueName("a", "mp4") { it in taken })
    }
    @Test fun cannotCompleteWithoutVerifying() {
        assertFalse(StateMachine.canTransition(DownloadState.Downloading(0.5f), DownloadState.Completed("x")))
        assertTrue(StateMachine.canTransition(DownloadState.Verifying, DownloadState.Completed("x")))
        assertFalse(StateMachine.canTransition(DownloadState.Completed("x"), DownloadState.Downloading(0f)))
    }
    @Test fun verifierRejectsMissingAndEmpty() {
        assertFalse(CompletionVerifier.verify(File("/nonexistent/file.mp4")))
        val f = File.createTempFile("vdl", ".mp4")
        assertFalse(CompletionVerifier.verify(f))
        f.writeText("x"); assertTrue(CompletionVerifier.verify(f)); f.delete()
    }
}
