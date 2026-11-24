package com.example.data.backup

import com.example.data.local.ReviewCardEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewCardMergeTest {

    @Test
    fun `later lastReviewedDate wins`() {
        val local = ReviewCardEntity("c1", 2, "2026-11-01", 5, 3)
        val backup = ReviewCardEntity("c1", 1, "2026-11-10", 2, 1)
        assertEquals(backup, preferReviewCard(local, backup))
    }

    @Test
    fun `higher timesSeen breaks date tie`() {
        val local = ReviewCardEntity("c1", 2, "2026-11-10", 3, 2)
        val backup = ReviewCardEntity("c1", 4, "2026-11-10", 9, 8)
        assertEquals(backup, preferReviewCard(local, backup))
    }

    @Test
    fun `full tie keeps local`() {
        val local = ReviewCardEntity("c1", 2, "2026-11-10", 5, 3)
        val backup = ReviewCardEntity("c1", 9, "2026-11-10", 5, 9)
        assertEquals(local, preferReviewCard(local, backup))
    }

    @Test
    fun `missing local takes backup`() {
        val backup = ReviewCardEntity("c1", 1, "2026-11-10", 1, 0)
        assertEquals(backup, preferReviewCard(null, backup))
    }
}
