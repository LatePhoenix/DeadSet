package com.example.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.review.Leitner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReviewDaoTest {

    @Test
    fun `answers move a card between boxes and persist`() = runBlocking {
        val db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext<Context>())
        val dao = db.reviewDao()
        val id = "segue_chain_scarlet-fire_1"

        // Same path as DeadSetViewModel.recordCardAnswer.
        suspend fun answer(correct: Boolean, day: String) {
            val next = Leitner.afterAnswer(dao.get(id)?.toState(), correct, day)
            dao.upsert(ReviewCardEntity.from(id, next))
        }
        answer(correct = true, day = "2026-11-20")
        answer(correct = true, day = "2026-11-21")
        assertEquals(3, dao.get(id)!!.box)
        answer(correct = false, day = "2026-11-21")

        val stored = dao.observeAll().first().single()
        assertEquals(1, stored.box)
        assertEquals(3, stored.timesSeen)
        assertEquals(2, stored.timesCorrect)
        assertEquals("2026-11-21", stored.lastReviewedDate)
        db.close()
    }
}
