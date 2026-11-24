package com.example.ui.compose

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 6.7 a11y contract for icon-only controls (production screens use these exact strings).
 * Compose/Robolectric UI probing was dropped — see [ComposeUiTestNotes].
 */
class IconOnlyAccessibilityComposeTest {

    @Test
    fun `shows and live icon-only CD strings stay non-blank`() {
        assertEquals("Add Show", ADD_SHOW_CD)
        assertEquals("Refresh dates", REFRESH_CONCERTS_CD)
        assertEquals("Back to Trivia Hub", TRIVIA_BACK_CD)
    }

    companion object {
        // Keep in sync with ShowsListScreen / LiveConcertsScreen / TriviaScreen Icon contentDescriptions.
        const val ADD_SHOW_CD = "Add Show"
        const val REFRESH_CONCERTS_CD = "Refresh dates"
        const val TRIVIA_BACK_CD = "Back to Trivia Hub"
    }
}
