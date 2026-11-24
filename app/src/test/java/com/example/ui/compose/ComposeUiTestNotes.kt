package com.example.ui.compose

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 6.7 Compose UI tests were attempted with Robolectric + `createComposeRule`, but every
 * `setContent` path hung on `AppNotIdleException` (Espresso never idles — including a bare
 * `Text`/`IconButton`). Full-suite runs then got harness-killed (exit 4294967295) after ~4–7 min.
 *
 * Production a11y fixes (headings, 48dp targets) and MockWebServer API tests still land in 6.7.
 * Real Compose flow coverage is deferred to instrumented `androidTest` / emulator before 6.3b.
 *
 * This file keeps a trivial unit assertion so the package stays compiled and documented.
 */
class ComposeUiTestNotes {
    @Test
    fun `compose ui tests deferred off robolectric idle hang`() {
        assertTrue(
            "Robolectric createComposeRule AppNotIdleException — use androidTest for 6.3b",
            true
        )
    }
}
