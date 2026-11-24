package com.example.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class StatsMetricsTest {

    @Test
    fun `songFrequencyProgress clamps to floor and ceiling`() {
        assertEquals(0.05f, songFrequencyProgress(0, 100), 0.0001f)
        assertEquals(1f, songFrequencyProgress(100, 100), 0.0001f)
        assertEquals(0.5f, songFrequencyProgress(50, 100), 0.0001f)
        assertEquals(0.05f, songFrequencyProgress(1, 0), 0.0001f)
    }
}
