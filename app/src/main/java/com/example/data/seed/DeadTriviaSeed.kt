package com.example.data.seed

import androidx.compose.ui.graphics.Color
import com.example.data.local.ShowEntity
import com.example.data.local.isUpcoming
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType
import com.example.ui.theme.*
import java.util.Calendar
import kotlin.random.Random

enum class TriviaCategory(
    val id: String,
    val title: String,
    val shortLabel: String,
    val emoji: String,
    val accentColor: Color
) {
    ALL("all", "All Deadhead Lore", "All Lore", "⚡", StealieGold),
    LEGENDARY_SHOWS("shows", "Legendary Shows & Venues", "Shows & Venues", "🏟️", StealieCyan),
    SEGUES_SETLISTS("segues", "Seamless Segues & Setlists", "Segues ➔", "🎶", StealieCrimsonLight),
    BAND_HISTORY("history", "Band, Keyboardists & Era Lore", "Band History", "🌹", EraEurope72Color),
    LYRICS_SONGS("lyrics", "Hunter & Barlow Songbook", "Lyrics & Songs", "📜", EraSpring77Color),
    GEAR_SOUND("gear", "Wall of Sound & Guitars", "Guitars & Gear", "🎸", EraWallOfSoundColor),
    VAULT_CHALLENGE("vault", "Live Room Vault Challenge", "Vault Setlists", "🗄️", StealiePurple)
}

enum class TriviaDifficulty(
    val id: String,
    val label: String,
    val basePoints: Int,
    val badgeColor: Color
) {
    SHAKEDOWN_ROOKIE("rookie", "Shakedown Rookie", 100, EraEurope72Color),
    TAPER_SECTION("taper", "Taper Section", 200, StealieCyan),
    ARCHIVIST_GURU("archivist", "Vault Archivist", 300, StealieGold)
}

/** Presentation shape for a question (step 5.3). All formats still use [TriviaQuestion.options]. */
enum class TriviaFormat(val label: String) {
    MULTIPLE_CHOICE("Multiple choice"),
    TRUE_FALSE("True / False"),
    ORDER_DATES("Order the dates"),
    WHO_SANG("Who sang it"),
    VENUE_CITY("Venue → city"),
    LYRIC_PARAPHRASE("Paraphrase → song")
}

enum class TriviaGameMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val questionCount: Int,
    val timeLimitSeconds: Int?
) {
    CLASSIC_TOUR(
        id = "classic",
        title = "Classic Tour Quiz",
        subtitle = "10 questions • Liner note explanations • 50/50 Bear & Flip Tape lifelines",
        questionCount = 10,
        timeLimitSeconds = null
    ),
    TERRAPIN_BLITZ(
        id = "blitz",
        title = "Terrapin Blitz (60s)",
        subtitle = "Race a 60-second clock with streak multipliers & rapid setlist fire",
        questionCount = 18,
        timeLimitSeconds = 60
    ),
    DAILY_DEADHEAD(
        id = "daily",
        title = "Daily Deadhead Five",
        subtitle = "Today's curated 5-question vault challenge for your Daily Stealie Badge",
        questionCount = 5,
        timeLimitSeconds = null
    ),
    LORE_FLASHCARDS(
        id = "flashcards",
        title = "Archivist Lore Deck",
        subtitle = "Browse & flip all curated + Room database trivia cards with full liner notes",
        questionCount = 0,
        timeLimitSeconds = null
    ),
    SEGUE_SCHOOL(
        id = "segue_school",
        title = "Segue School",
        subtitle = "Learn which songs flow into which. Questions you miss come back sooner (spaced repetition).",
        questionCount = 10,
        timeLimitSeconds = null
    )
}

data class TriviaQuestion(
    val id: String,
    val category: TriviaCategory,
    val difficulty: TriviaDifficulty,
    val era: DeadEra = DeadEra.ALL,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val linerNoteFact: String,
    val relatedShowId: String? = null,
    val isVaultGenerated: Boolean = false,
    /** Primary citation for curated claims; null for catalog-generated vault questions. */
    val sourceUrl: String? = null,
    val format: TriviaFormat = TriviaFormat.MULTIPLE_CHOICE
)

data class TriviaBadge(
    val id: String,
    val title: String,
    val description: String,
    val iconText: String,
    val color: Color
)

data class TriviaUserProgress(
    val totalAnswered: Int = 0,
    val totalCorrect: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val totalPoints: Int = 0,
    val quizzesCompleted: Int = 0,
    val dailyCompletedDate: String = "", // ISO date (yyyy-MM-dd) of the last completed Daily Five
    val unlockedBadgeIds: Set<String> = emptySet()
) {
    val accuracyPercent: Int
        get() = if (totalAnswered > 0) ((totalCorrect.toFloat() / totalAnswered.toFloat()) * 100f).toInt() else 0

    val rankTitle: String
        get() = when {
            totalPoints >= 6000 -> "Grand Archivist of Lemieux ⚡"
            totalPoints >= 3500 -> "Owsley's Soundboard Guru 🎛️"
            totalPoints >= 1800 -> "Barton Hall Tape Scholar 📼"
            totalPoints >= 700 -> "Golden Road Historian 🌹"
            totalPoints >= 200 -> "Miracle Ticket Holder 🎟️"
            else -> "Shakedown Street Cruiser 🚌"
        }
}

object DeadTriviaSeed {

    val allBadges: List<TriviaBadge> = listOf(
        TriviaBadge(
            id = "first_miracle",
            title = "First Miracle Ticket",
            description = "Complete your first Grateful Dead Trivia round",
            iconText = "🎟️",
            color = StealieCyan
        ),
        TriviaBadge(
            id = "scarlet_fire_streak",
            title = "Scarlet ➔ Fire Streak",
            description = "Hit a 5-question consecutive correct answer streak",
            iconText = "🔥",
            color = StealieCrimsonLight
        ),
        TriviaBadge(
            id = "cornell_scholar",
            title = "Barton Hall '77 Scholar",
            description = "Score 80% or higher on a Classic Tour Quiz",
            iconText = "🏛️",
            color = StealieGold
        ),
        TriviaBadge(
            id = "owsley_soundman",
            title = "Wall of Sound Engineer",
            description = "Accumulate 1,500+ total Deadhead Trivia points",
            iconText = "🔊",
            color = EraWallOfSoundColor
        ),
        TriviaBadge(
            id = "daily_stealie",
            title = "Daily Stealie Champion",
            description = "Complete the Daily Deadhead Five challenge",
            iconText = "⚡",
            color = EraSpring77Color
        ),
        TriviaBadge(
            id = "terrapin_speedster",
            title = "Terrapin Blitz Master",
            description = "Answer 6+ questions correctly in a 60s Terrapin Blitz",
            iconText = "🐢",
            color = EraEurope72Color
        )
    )

    fun getCuratedQuestions(): List<TriviaQuestion> =
        curatedTriviaQuestions() + typedTriviaQuestions()


    /**
     * Dynamically generates interactive trivia questions directly from the user's offline
     * Room concert database (ShowEntity & parsed setlists), testing knowledge of dates,
     * venues, cities, Set I openers, and seamless `➔` segues!
     */
    fun generateDynamicVaultQuestions(
        shows: List<ShowEntity>,
        parseSongs: (ShowEntity) -> List<SongEntry>
    ): List<TriviaQuestion> {
        if (shows.size < 3) return emptyList()

        val historicalShows = shows.filter { !it.isUpcoming && it.bandName.contains("Grateful Dead", ignoreCase = true) }
            .ifEmpty { shows }

        val allVenues = shows.map { "${it.venue} (${it.city})" }.distinct()
        val allCities = shows.map { "${it.city}, ${it.stateOrCountry}" }.distinct()
        val generated = mutableListOf<TriviaQuestion>()

        for (show in historicalShows) {
            val era = DeadEra.fromId(show.eraId)
            val songs = parseSongs(show)
            val correctVenueLabel = "${show.venue} (${show.city})"
            val correctCityLabel = "${show.city}, ${show.stateOrCountry}"

            // 1. Venue & City Question for this show date
            val distractorVenues = allVenues.filter { it != correctVenueLabel }.shuffled(Random(show.id.hashCode())).take(3)
            if (distractorVenues.size == 3) {
                val options = (distractorVenues + correctVenueLabel).shuffled(Random(show.date.hashCode()))
                val correctIdx = options.indexOf(correctVenueLabel)
                generated.add(
                    TriviaQuestion(
                        id = "vault_venue_${show.id}",
                        category = TriviaCategory.VAULT_CHALLENGE,
                        difficulty = TriviaDifficulty.TAPER_SECTION,
                        era = era,
                        question = "In your Offline Room Concert Vault, which venue hosted the ${show.date} (${show.tourName}) concert?",
                        options = options,
                        correctIndex = correctIdx,
                        linerNoteFact = "${show.tourName}: Performed on ${show.date} at ${show.venue} in ${show.city}, ${show.stateOrCountry}. ${show.historicTrivia.take(150)}",
                        relatedShowId = show.id,
                        isVaultGenerated = true
                    )
                )
            }

            // 1b. Venue → city (step 5.3) from the user's catalog
            val distractorCities = allCities.filter { it != correctCityLabel }
                .shuffled(Random(show.id.hashCode() + 19)).take(3)
            if (distractorCities.size == 3 && show.venue.isNotBlank()) {
                val cityOpts = (distractorCities + correctCityLabel).shuffled(Random(show.venue.hashCode()))
                generated.add(
                    TriviaQuestion(
                        id = "vault_venue_city_${show.id}",
                        category = TriviaCategory.VAULT_CHALLENGE,
                        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
                        era = era,
                        question = "In which city is ${show.venue}?",
                        options = cityOpts,
                        correctIndex = cityOpts.indexOf(correctCityLabel),
                        linerNoteFact = "${show.venue} is in ${show.city}, ${show.stateOrCountry} (from your catalog show on ${show.date}).",
                        relatedShowId = show.id,
                        isVaultGenerated = true,
                        format = TriviaFormat.VENUE_CITY
                    )
                )
            }

            // 2. Seamless Segue Question (`➔`) from this show's setlist
            for (i in 0 until (songs.size - 1)) {
                val songA = songs[i]
                val songB = songs[i + 1]
                if (songA.isConnected) {
                    val otherSongNames = songs.map { it.name }
                        .filter { it != songB.name && it != songA.name }
                        .distinct()
                        .shuffled(Random(songA.name.hashCode() + show.id.hashCode()))
                        .take(3)

                    if (otherSongNames.size == 3) {
                        val options = (otherSongNames + songB.name).shuffled(Random(songB.name.hashCode()))
                        val correctIdx = options.indexOf(songB.name)
                        generated.add(
                            TriviaQuestion(
                                id = "vault_segue_${show.id}_$i",
                                category = TriviaCategory.VAULT_CHALLENGE,
                                difficulty = TriviaDifficulty.ARCHIVIST_GURU,
                                era = era,
                                question = "At ${show.venue} on ${show.date}, '${songA.name}' ${transitionVerb(songA.transitionOut)} into which song?",
                                options = options,
                                correctIndex = correctIdx,
                                linerNoteFact = "During ${songA.setType.label} on ${show.date} (${show.venue}, ${show.city}), the band went '${songA.name} ${songA.transitionOut.symbol} ${songB.name}' (${songA.transitionOut.description}).",
                                relatedShowId = show.id,
                                isVaultGenerated = true
                            )
                        )
                        break // One segue question per show to keep balanced variety
                    }
                }
            }

            // 3. Set I Opener Question
            val opener = songs.firstOrNull { it.setType == SetType.SET_1 }
            if (opener != null) {
                val otherSongs = songs.drop(1).map { it.name }.distinct().take(3)
                if (otherSongs.size == 3) {
                    val options = (otherSongs + opener.name).shuffled(Random(show.id.hashCode() + 77))
                    val correctIdx = options.indexOf(opener.name)
                    generated.add(
                        TriviaQuestion(
                            id = "vault_opener_${show.id}",
                            category = TriviaCategory.VAULT_CHALLENGE,
                            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
                            era = era,
                            question = "Which song opened Set I at ${show.venue} (${show.city}) on ${show.date}?",
                            options = options,
                            correctIndex = correctIdx,
                            linerNoteFact = "'${opener.name}' kicked off Set I on ${show.date} at ${show.venue} during the ${show.tourName}.",
                            relatedShowId = show.id,
                            isVaultGenerated = true
                        )
                    )
                }
            }
        }

        return generated
    }

    /** Question phrasing that matches the transition type, so only true segues are called seamless. */
    internal fun transitionVerb(type: TransitionType): String = when (type) {
        TransitionType.SEGUE -> "segued seamlessly (->)"
        TransitionType.TRANSITION -> "transitioned (>)"
        TransitionType.JAM -> "jammed (~>)"
        TransitionType.NONE -> "was followed"
    }

    private fun shuffleQuestionOptions(q: TriviaQuestion): TriviaQuestion {
        val correctAnswer = q.options.getOrElse(q.correctIndex) { q.options.first() }
        val shuffledOpts = q.options.shuffled(Random(q.id.hashCode()))
        val newCorrectIdx = shuffledOpts.indexOf(correctAnswer).coerceAtLeast(0)
        return q.copy(options = shuffledOpts, correctIndex = newCorrectIdx)
    }

    fun getAllAvailableQuestions(
        shows: List<ShowEntity>,
        parseSongs: (ShowEntity) -> List<SongEntry>
    ): List<TriviaQuestion> {
        val curated = getCuratedQuestions().map { shuffleQuestionOptions(it) }
        return curated + generateDynamicVaultQuestions(shows, parseSongs)
    }

    fun buildQuizDeck(
        mode: TriviaGameMode,
        category: TriviaCategory,
        difficultyFilter: TriviaDifficulty?,
        shows: List<ShowEntity>,
        parseSongs: (ShowEntity) -> List<SongEntry>,
        seed: Long = System.currentTimeMillis(),
        reviewStates: Map<String, com.example.data.review.ReviewState> = emptyMap(),
        today: String = com.example.data.todayIsoDate()
    ): List<TriviaQuestion> {
        if (mode == TriviaGameMode.SEGUE_SCHOOL) {
            // Spaced repetition: due cards first (weakest box first), then new cards.
            val byId = com.example.data.setlist.TransitionQuiz.questions(shows, parseSongs)
                .shuffled(Random(seed))
                .associateBy { it.id }
            return com.example.data.review.Leitner.order(byId.keys.toList(), reviewStates, today)
                .take(mode.questionCount)
                .map { byId.getValue(it) }
        }

        val allPool = getAllAvailableQuestions(shows, parseSongs)

        if (mode == TriviaGameMode.DAILY_DEADHEAD) {
            val cal = Calendar.getInstance()
            val daySeed = (cal.get(Calendar.YEAR) * 1000L) + cal.get(Calendar.DAY_OF_YEAR)
            return allPool.shuffled(Random(daySeed)).take(mode.questionCount)
        }

        val filtered = allPool.filter { q ->
            val catMatch = (category == TriviaCategory.ALL) || (q.category == category)
            val diffMatch = (difficultyFilter == null) || (q.difficulty == difficultyFilter)
            catMatch && diffMatch
        }.ifEmpty {
            allPool.filter { category == TriviaCategory.ALL || it.category == category }.ifEmpty { allPool }
        }

        // Per-question history (step 5.1): unseen and weak/due questions first, mastered ones that
        // are not due yet last, so a round rarely repeats what you already know. Shuffled first so
        // ties stay random.
        val byId = filtered.shuffled(Random(seed)).associateBy { it.id }
        val ordered = com.example.data.review.Leitner.order(byId.keys.toList(), reviewStates, today)
            .let { ids -> ids.filter { it !in reviewStates } + ids.filter { it in reviewStates } }
        return ordered.take(mode.questionCount.coerceAtLeast(5)).map { byId.getValue(it) }
    }
}
