package com.example.data.local

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** H3: widget DAO returns catalog shows for a month-day and excludes custom shows. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogShowsOnMonthDayDaoTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = AppDatabase.createInMemoryDatabase(ApplicationProvider.getApplicationContext())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `catalog month-day query excludes custom shows and unmatched days`() = runBlocking {
        val dao = db.showDao()
        dao.insertShow(
            ShowEntity(
                id = "1977-05-08-cornell",
                date = "1977-05-08",
                venue = "Barton Hall",
                city = "Ithaca",
                stateOrCountry = "NY",
                eraId = "spring_77",
                tourName = "",
                isCustomShow = false
            )
        )
        dao.insertShow(
            ShowEntity(
                id = "1978-05-08-other",
                date = "1978-05-08",
                venue = "Field House",
                city = "X",
                stateOrCountry = "CA",
                eraId = "",
                tourName = "",
                isCustomShow = false
            )
        )
        dao.insertShow(
            ShowEntity(
                id = "custom-tribute",
                date = "2024-05-08",
                venue = "Martyrs'",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = "",
                tourName = "",
                isCustomShow = true,
                bandName = "Terrapin Flyer"
            )
        )
        dao.insertShow(
            ShowEntity(
                id = "1977-05-09-next",
                date = "1977-05-09",
                venue = "Somewhere",
                city = "Y",
                stateOrCountry = "CA",
                eraId = "",
                tourName = "",
                isCustomShow = false
            )
        )

        val matches = dao.getCatalogShowsOnMonthDay("05-08")
        assertEquals(
            listOf("1977-05-08-cornell", "1978-05-08-other"),
            matches.map { it.id }
        )
        assertTrue(matches.none { it.isCustomShow })
        assertTrue(dao.getCatalogShowsOnMonthDay("11-23").isEmpty())
    }
}
