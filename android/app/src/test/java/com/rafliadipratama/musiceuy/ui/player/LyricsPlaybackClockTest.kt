package com.rafliadipratama.musiceuy.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LyricsPlaybackClockTest {
    @Test
    fun `interpolation follows sample and stops at one player tick`() {
        assertEquals(1016L, lyricFramePosition(1000L, 500L, 516L, true))
        assertEquals(1060L, lyricFramePosition(1000L, 500L, 900L, true))
        assertEquals(1000L, lyricFramePosition(1000L, 500L, 490L, true))
    }

    @Test
    fun `pause freezes position and seek starts from new sample`() {
        assertEquals(1000L, lyricFramePosition(1000L, 500L, 900L, false))
        assertEquals(216L, lyricFramePosition(200L, 900L, 916L, true))
    }
}
