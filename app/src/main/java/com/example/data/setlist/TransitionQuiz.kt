package com.example.data.setlist

import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SongEntry
import com.example.data.seed.TriviaCategory
import com.example.data.seed.TriviaDifficulty
import com.example.data.seed.TriviaQuestion
import kotlin.random.Random

/**
 * "Segue School" questions (step 3.4), generated from the user's catalog plus [SegueFamilies].
 *
 * Every question has a stable [TriviaQuestion.id] that doubles as its spaced-repetition card id
 * (step 3.5), and every explanation cites the catalog evidence it was built from. Questions are
 * only generated when they have exactly one defensible answer; ambiguous cases are skipped.
 */
object TransitionQuiz {

    fun questions(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): List<TriviaQuestion> {
        val pool = shows.flatMap(songsFor).map { it.name }.distinctBy(SetlistAnalytics::canonicalKey)
        return completeTheChain(pool) +
            mostOftenNext(shows, songsFor, pool) +
            fillTheSandwich(shows, songsFor) +
            nameThatEra(shows, songsFor)
    }

    /** "Help on the Way -> ??? -> Franklin's Tower": fill the missing link of a known family. */
    fun completeTheChain(pool: List<String>): List<TriviaQuestion> =
        SegueFamilies.all.flatMap { family ->
            // Blank the middle of 3-song suites, the second song of pairs.
            val blanks = if (family.chain.size >= 3) 1 until family.chain.size - 1 else listOf(1)
            blanks.mapNotNull { blank ->
                val answer = family.chain[blank]
                val promptSong = family.chain[blank - 1]
                val promptKey = SetlistAnalytics.canonicalKey(promptSong)
                // Any family that declares a successor of the prompt song is also a defensible
                // answer; keep those out of the distractor pool so the question stays unique.
                val declaredSuccessors = SegueFamilies.all.flatMap { other ->
                    other.chain.zipWithNext()
                        .filter { (a, _) -> SetlistAnalytics.canonicalKey(a) == promptKey }
                        .map { it.second }
                }
                val prompt = family.chain.mapIndexed { i, s -> if (i == blank) "???" else s }.joinToString(" -> ")
                val id = "segue_chain_${family.id}_$blank"
                question(
                    id = id,
                    difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
                    text = "Complete the segue: $prompt",
                    answer = answer,
                    distractorPool = pool + SegueFamilies.all.flatMap { it.chain },
                    exclude = family.chain + declaredSuccessors,
                    note = "${family.nickname}: ${family.note}"
                )
            }
        }

    /** "Which song does Space most often flow into?" Only when one exit clearly leads. */
    fun mostOftenNext(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>,
        pool: List<String>
    ): List<TriviaQuestion> = pool.mapNotNull { song ->
        val flow = SetlistAnalytics.songFlow(song, shows, songsFor)
        val top = flow.outOf.firstOrNull() ?: return@mapNotNull null
        val runnerUp = flow.outOf.getOrNull(1)?.count ?: 0
        val total = flow.outOf.sumOf { it.count }
        if (total < 2 || top.count == runnerUp) return@mapNotNull null
        question(
            id = "segue_next_${SetlistAnalytics.canonicalKey(song)}",
            difficulty = TriviaDifficulty.TAPER_SECTION,
            text = "In your catalog, which song does $song most often flow into?",
            answer = top.songName,
            distractorPool = pool,
            exclude = flow.outOf.map { it.songName } + song,
            note = "In your catalog, $song flowed into ${top.songName} ${top.count} of $total times."
        )
    }

    /** A song the band left and came back to in the same set: what came right after the first one? */
    fun fillTheSandwich(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): List<TriviaQuestion> = shows.mapNotNull { show ->
        val songs = songsFor(show)
        for (i in songs.indices) {
            val key = SetlistAnalytics.canonicalKey(songs[i].name)
            val j = (i + 2 until songs.size).firstOrNull {
                songs[it].setType == songs[i].setType && SetlistAnalytics.canonicalKey(songs[it].name) == key
            } ?: continue
            if ((i until j).any { !songs[it].isConnected }) continue
            val filling = songs[i + 1].name
            return@mapNotNull question(
                id = "segue_sandwich_${show.id}_$i",
                difficulty = TriviaDifficulty.ARCHIVIST_GURU,
                text = "At ${show.venue} on ${show.date}, the band left ${songs[i].name} and returned to it " +
                    "later in the set. What came right after the first ${songs[i].name}?",
                answer = filling,
                distractorPool = songs.map { it.name } + songsFor(show).map { it.name },
                exclude = songs.subList(i, j + 1).map { it.name },
                note = "${show.date}: " + SetlistAnalytics.notation(songs.subList(i, j + 1)),
                relatedShowId = show.id
            )
        }
        null
    }

    /** "Scarlet Begonias -> Fire on the Mountain appears in which era?" Only for single-era pairs. */
    fun nameThatEra(
        shows: List<ShowEntity>,
        songsFor: (ShowEntity) -> List<SongEntry>
    ): List<TriviaQuestion> {
        val erasByPair = linkedMapOf<Pair<String, String>, MutableSet<DeadEra>>()
        val labels = mutableMapOf<Pair<String, String>, String>()
        for (show in shows.filter { it.bandName.equals("Grateful Dead", ignoreCase = true) }) {
            val era = DeadEra.fromId(show.eraId).takeIf { it != DeadEra.ALL } ?: continue
            val songs = songsFor(show)
            for (i in 0 until songs.size - 1) {
                if (!songs[i].isConnected) continue
                if (songs[i].setType != songs[i + 1].setType) continue
                val pair = SetlistAnalytics.canonicalKey(songs[i].name) to SetlistAnalytics.canonicalKey(songs[i + 1].name)
                erasByPair.getOrPut(pair) { mutableSetOf() } += era
                labels.putIfAbsent(pair, "${songs[i].name} ${songs[i].transitionOut.symbol} ${songs[i + 1].name}")
            }
        }
        val eraNames = DeadEra.entries.filter { it != DeadEra.ALL }
        return erasByPair.filterValues { it.size == 1 }.mapNotNull { (pair, eras) ->
            val era = eras.single()
            val label = labels.getValue(pair)
            question(
                id = "segue_era_${pair.first}_${pair.second}",
                difficulty = TriviaDifficulty.TAPER_SECTION,
                text = "In your catalog, \"$label\" appears in which era?",
                answer = era.title,
                distractorPool = eraNames.map { it.title },
                exclude = emptyList(),
                note = "Every catalog show with $label is from ${era.title} (${era.years})."
            )
        }
    }

    private fun question(
        id: String,
        difficulty: TriviaDifficulty,
        text: String,
        answer: String,
        distractorPool: List<String>,
        exclude: List<String>,
        note: String,
        relatedShowId: String? = null
    ): TriviaQuestion? {
        val excluded = (exclude + answer).map(SetlistAnalytics::canonicalKey).toSet()
        val distractors = distractorPool
            .distinctBy(SetlistAnalytics::canonicalKey)
            .filter { SetlistAnalytics.canonicalKey(it) !in excluded }
            .shuffled(Random(id.hashCode()))
            .take(3)
        if (distractors.size < 3) return null
        val options = (distractors + answer).shuffled(Random(id.hashCode() * 31 + 7))
        return TriviaQuestion(
            id = id,
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = difficulty,
            question = text,
            options = options,
            correctIndex = options.indexOf(answer),
            linerNoteFact = note,
            relatedShowId = relatedShowId,
            isVaultGenerated = true
        )
    }
}
