package com.example.ui.compose

import androidx.compose.ui.test.junit4.ComposeContentTestRule

/**
 * Infinite dancing-bear animations never idle under Robolectric. Pause the test clock
 * so Compose UI tests can run (6.7).
 */
fun ComposeContentTestRule.pauseInfiniteAnimations() {
    mainClock.autoAdvance = false
}
