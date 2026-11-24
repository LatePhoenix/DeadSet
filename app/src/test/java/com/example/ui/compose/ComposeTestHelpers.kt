package com.example.ui.compose

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode

/**
 * Infinite dancing-bear animations never idle under Robolectric. Pause the test clock
 * and advance it after each action (H4 / 6.7).
 */
fun ComposeContentTestRule.pauseInfiniteAnimations() {
    mainClock.autoAdvance = false
}

/** Advance the Compose clock so paused infinite animations / recompositions settle. */
fun ComposeContentTestRule.advanceUi(ms: Long = 100L) {
    mainClock.advanceTimeBy(ms)
}

/**
 * Scroll a tagged LazyColumn to a tagged child. Temporarily enables auto-advance
 * for the scroll gesture, then restores the paused clock.
 */
fun ComposeContentTestRule.scrollListToTag(listTag: String, nodeTag: String) {
    val wasAuto = mainClock.autoAdvance
    mainClock.autoAdvance = true
    onNodeWithTag(listTag).performScrollToNode(hasTestTag(nodeTag))
    mainClock.autoAdvance = wasAuto
    advanceUi()
}

fun ComposeContentTestRule.clickTag(tag: String) {
    onNodeWithTag(tag).performClick()
    advanceUi()
}
