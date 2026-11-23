package com.example.data.seed

import com.example.TestSeeds
import com.example.data.local.ShowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CuratedSeedMergerTest {

    private val cornell = TestSeeds.seed("1977-05-08-cornell")
    private val terrapin = TestSeeds.seed("2024-05-18-terrapin-flyer-martyrs")

    @Test
    fun `venuesMatch accepts substring and token overlap`() {
        assertTrue(
            CuratedSeedMerger.venuesMatch(
                "Barton Hall, Cornell University",
                "Barton Hall"
            )
        )
        assertTrue(
            CuratedSeedMerger.venuesMatch(
                "Fillmore West",
                "Fillmore West, San Francisco"
            )
        )
        assertFalse(
            CuratedSeedMerger.venuesMatch("Barton Hall", "Soldier Field")
        )
    }

    @Test
    fun `empty database plans every seed as insert`() {
        val plan = CuratedSeedMerger.plan(
            seeds = listOf(cornell, terrapin),
            existing = emptyList(),
            options = CuratedSeedMerger.PlanOptions(emptyDatabase = true)
        )
        assertTrue(plan.all { it.insertNew && it.targetShowId == it.seed.id })
    }

    @Test
    fun `catalog UUID match merges onto catalog id and does not insert seed id`() {
        val catalogCornell = cornell.show.copy(
            id = "gdshowsdb-uuid-cornell",
            venue = "Barton Hall",
            archiveIdentifier = "",
            historicTrivia = "",
            tags = ""
        )
        val plan = CuratedSeedMerger.plan(
            seeds = listOf(cornell, terrapin),
            existing = listOf(catalogCornell),
            options = CuratedSeedMerger.PlanOptions(insertUnmatchedGd = false)
        )
        val gd = plan.first { it.seed.id == cornell.id }
        assertEquals("gdshowsdb-uuid-cornell", gd.targetShowId)
        assertFalse(gd.insertNew)
        assertTrue(gd.existed)

        val tribute = plan.first { it.seed.id == terrapin.id }
        assertTrue(tribute.insertNew)
        assertEquals(terrapin.id, tribute.targetShowId)

        val dupes = CuratedSeedMerger.duplicateSeedIdsToDrop(
            plan,
            listOf(catalogCornell, cornell.show)
        )
        assertEquals(listOf(cornell.id), dupes)
    }

    @Test
    fun `findCatalogMatch prefers catalog UUID over seed id regardless of list order`() {
        val catalog = cornell.show.copy(id = "gdshowsdb-uuid-cornell", venue = "Barton Hall")
        val seedFirst = CuratedSeedMerger.findCatalogMatch(
            cornell,
            listOf(cornell.show, catalog)
        )
        val catalogFirst = CuratedSeedMerger.findCatalogMatch(
            cornell,
            listOf(catalog, cornell.show)
        )
        assertEquals("gdshowsdb-uuid-cornell", seedFirst!!.id)
        assertEquals("gdshowsdb-uuid-cornell", catalogFirst!!.id)
    }

    @Test
    fun `findCatalogMatch falls back to seed row when it is the only match`() {
        val match = CuratedSeedMerger.findCatalogMatch(cornell, listOf(cornell.show))
        assertEquals(cornell.id, match!!.id)
    }

    @Test
    fun `startup does not resurrect unmatched GD seeds`() {
        val other = TestSeeds.seed("1995-07-09-soldier-field").show
        val plan = CuratedSeedMerger.plan(
            seeds = listOf(cornell),
            existing = listOf(other),
            options = CuratedSeedMerger.PlanOptions(insertUnmatchedGd = false)
        )
        assertFalse(plan.single().insertNew)
        assertFalse(plan.single().existed)
    }

    @Test
    fun `restore inserts unmatched GD seeds`() {
        val other = TestSeeds.seed("1995-07-09-soldier-field").show
        val plan = CuratedSeedMerger.plan(
            seeds = listOf(cornell),
            existing = listOf(other),
            options = CuratedSeedMerger.PlanOptions(insertUnmatchedGd = true)
        )
        assertTrue(plan.single().insertNew)
        assertEquals(cornell.id, plan.single().targetShowId)
    }

    @Test
    fun `mergeCatalogFields fills blanks from seed and keeps catalog id`() {
        val base = ShowEntity(
            id = "catalog-id",
            date = "1977-05-08",
            venue = "Barton Hall",
            city = "Ithaca",
            stateOrCountry = "NY",
            eraId = cornell.show.eraId,
            tourName = "",
            archiveIdentifier = "",
            historicTrivia = "",
            tags = "Spring 77",
            isAttended = true,
            userRating = 5f,
            userReview = "mine"
        )
        val merged = CuratedSeedMerger.mergeCatalogFields(base, cornell.show)
        assertEquals("catalog-id", merged.id)
        assertEquals(cornell.show.archiveIdentifier, merged.archiveIdentifier)
        assertEquals(cornell.show.historicTrivia, merged.historicTrivia)
        assertTrue(merged.tags.contains("All-Time Classic"))
        assertTrue(merged.tags.contains("Spring 77"))
        assertTrue(merged.isAttended)
        assertEquals(5f, merged.userRating)
        assertEquals("mine", merged.userReview)
    }

    @Test
    fun `mergeCatalogFields keeps non-blank base fields over seed`() {
        val base = cornell.show.copy(
            id = "catalog-id",
            tourName = "My edited tour",
            tapeQuality = "My tape note",
            archiveIdentifier = "keep-this-archive",
            historicTrivia = "keep-this-trivia"
        )
        val merged = CuratedSeedMerger.mergeCatalogFields(base, cornell.show)
        assertEquals("My edited tour", merged.tourName)
        assertEquals("My tape note", merged.tapeQuality)
        assertEquals("keep-this-archive", merged.archiveIdentifier)
        assertEquals("keep-this-trivia", merged.historicTrivia)
    }
}
