package com.example.data.setlist

import com.example.data.local.AppDatabase
import com.example.data.local.AppDatabase.Companion.applyMigrationPolicy
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.data.repository.ShowRepository
import com.example.data.songs.SongCatalog
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import kotlin.system.measureTimeMillis

/**
 * Step 2.4: corpus-scale budgets for the Default-dispatcher snapshot work (stats, families,
 * song flow, transition graph). Synthetic data matches ~gdshowsdb size so CI can run without
 * the gitignored catalog.db asset; an optional Room load times the real asset when present.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CatalogPerfTest {

    @Test
    fun `corpus-scale analytics stay under soft budgets`() {
        val (shows, songsByShow) = syntheticCorpus(showCount = 2358, songsPerShow = 17)
        val songsFor = { show: ShowEntity -> songsByShow.getValue(show.id) }
        val db = AppDatabase.createInMemoryDatabase(RuntimeEnvironment.getApplication())
        try {
            val repo = ShowRepository(db.showDao(), db.songDao())

            val statsMs = measureTimeMillis { repo.computeOverallStats(shows, songsFor) }
            val familiesMs = measureTimeMillis { SetlistAnalytics.familyStats(shows, songsFor) }
            val flowMs = measureTimeMillis {
                SetlistAnalytics.songFlow("Scarlet Begonias", shows, songsFor)
            }
            val graphMs = measureTimeMillis {
                SetlistAnalytics.transitionGraph(shows, songsFor)
            }
            val structureMs = measureTimeMillis {
                shows.take(200).forEach { SetlistAnalytics.structureSegments(songsFor(it)) }
            }

            println(
                "CatalogPerf synthetic: stats=${statsMs}ms families=${familiesMs}ms " +
                    "flow=${flowMs}ms graph=${graphMs}ms structure200=${structureMs}ms " +
                    "(shows=${shows.size} songs=${songsByShow.values.sumOf { it.size }})"
            )

            // Soft budgets for a laptop/CI JVM — Default dispatcher work, not frame budget.
            assertTrue("stats $statsMs ms", statsMs < 2_500)
            assertTrue("families $familiesMs ms", familiesMs < 3_000)
            assertTrue("songFlow $flowMs ms", flowMs < 1_500)
            assertTrue("transitionGraph $graphMs ms", graphMs < 2_500)
            assertTrue("structureSegments $structureMs ms", structureMs < 500)
        } finally {
            db.close()
        }
    }

    @Test
    fun `Room load of catalog asset completes under budget when present`() = runBlocking {
        val asset = File("src/main/assets/catalog.db")
        assumeTrue("catalog.db asset required", asset.isFile && asset.length() > 1_000_000)

        val context = RuntimeEnvironment.getApplication()
        // Fresh file DB from asset (same path as production createFromAsset).
        context.getDatabasePath("perf_catalog.db").delete()
        val db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "perf_catalog.db")
            .createFromAsset("catalog.db")
            .applyMigrationPolicy()
            .allowMainThreadQueries()
            .build()
        try {
            val loadMs = measureTimeMillis {
                val rows = db.showDao().getAllShowsWithSongs().first()
                assertTrue("expected corpus-scale shows", rows.size in 2300..2400)
                assertTrue(
                    "expected corpus-scale performances",
                    rows.sumOf { it.songsPerformed.size } in 35_000..45_000
                )
            }
            println("CatalogPerf Room asset load: ${loadMs}ms")
            assertTrue("Room load $loadMs ms", loadMs < 15_000)
        } finally {
            db.close()
            context.getDatabasePath("perf_catalog.db").delete()
        }
    }

    private fun syntheticCorpus(
        showCount: Int,
        songsPerShow: Int
    ): Pair<List<ShowEntity>, Map<String, List<SongEntry>>> {
        val names = SongCatalog.songs.map { it.canonicalName }.ifEmpty {
            listOf(
                "Scarlet Begonias", "Fire on the Mountain", "Estimated Prophet",
                "Eyes of the World", "Drums", "Space", "The Wheel", "Not Fade Away",
                "Sugar Magnolia", "Truckin'", "China Cat Sunflower", "I Know You Rider"
            )
        }
        val eras = DeadEra.entries.filter { it != DeadEra.ALL }
        val shows = ArrayList<ShowEntity>(showCount)
        val songsByShow = HashMap<String, List<SongEntry>>(showCount)
        for (i in 0 until showCount) {
            val id = "perf-$i"
            val year = 1965 + (i % 31)
            val month = (i % 12) + 1
            val day = (i % 28) + 1
            val era = eras[i % eras.size]
            shows += ShowEntity(
                id = id,
                date = "%04d-%02d-%02d".format(year, month, day),
                venue = "Venue $i",
                city = "City ${i % 80}",
                stateOrCountry = "ST",
                eraId = era.id,
                tourName = "Tour ${year}",
                isAttended = i % 40 == 0
            )
            val setlist = ArrayList<SongEntry>(songsPerShow)
            for (t in 0 until songsPerShow) {
                val name = names[(i + t) % names.size]
                val setType = when {
                    t < songsPerShow / 2 -> SetType.SET_1
                    t < songsPerShow - 1 -> SetType.SET_2
                    else -> SetType.ENCORE
                }
                val transition = when {
                    name == "Drums" || name == "Scarlet Begonias" -> TransitionType.SEGUE
                    t % 5 == 0 -> TransitionType.TRANSITION
                    else -> TransitionType.NONE
                }
                setlist += SongEntry(
                    name = name,
                    setType = setType,
                    position = t,
                    transitionOut = if (t == songsPerShow - 1) TransitionType.NONE else transition
                )
            }
            songsByShow[id] = setlist
        }
        return shows to songsByShow
    }
}
