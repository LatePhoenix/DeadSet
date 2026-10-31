package com.example.data.seed

import androidx.compose.ui.graphics.Color
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
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
    val isVaultGenerated: Boolean = false
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
    val dailyCompletedDayOfYear: Int = -1,
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

    fun getCuratedQuestions(): List<TriviaQuestion> = listOf(
        TriviaQuestion(
            id = "q_cornell_77_venue",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.SPRING_77,
            question = "Which legendary university hall in Ithaca, NY hosted the iconic May 8, 1977 Grateful Dead concert?",
            options = listOf(
                "Barton Hall, Cornell University",
                "Pauley Pavilion, UCLA",
                "Cole Field House, Maryland",
                "Cameron Indoor Stadium, Duke"
            ),
            correctIndex = 0,
            linerNoteFact = "May 8, 1977 at Cornell's Barton Hall is so revered that Betty Cantor-Jackson's soundboard recording was inducted into the Library of Congress National Recording Registry in 2012.",
            relatedShowId = "1977-05-08"
        ),
        TriviaQuestion(
            id = "q_cornell_77_opener",
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.SPRING_77,
            question = "What song kicked off Set I of the legendary May 8, 1977 show at Barton Hall?",
            options = listOf(
                "New Minglewood Blues",
                "The Music Never Stopped",
                "Promised Land",
                "Bertha"
            ),
            correctIndex = 0,
            linerNoteFact = "Bobby Weir tore into 'New Minglewood Blues' to open Cornell '77, complete with the line 'Born in the desert, raised in a lion's den!'",
            relatedShowId = "1977-05-08"
        ),
        TriviaQuestion(
            id = "q_veneta_72_benefit",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.EARLY_70S,
            question = "The blazing August 27, 1972 Field Trip concert in Veneta, Oregon was played as a benefit for what local business?",
            options = listOf(
                "Springfield Creamery (Nancy's Yogurt)",
                "Oregon Country Fair Co-op",
                "Eugene Organic Bakery",
                "Willamette Valley Apple Orchard"
            ),
            correctIndex = 0,
            linerNoteFact = "Organized with Ken Kesey and the Merry Pranksters to save Chuck and Sue Kesey's Springfield Creamery, Veneta '72 (Sunshine Daydream) featured a 31-minute 'Dark Star' in 100°F heat.",
            relatedShowId = "1972-08-27"
        ),
        TriviaQuestion(
            id = "q_wall_of_sound_designer",
            category = TriviaCategory.GEAR_SOUND,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.WALL_OF_SOUND,
            question = "Which legendary audio pioneer and acid chemist masterminded the Grateful Dead's 1974 'Wall of Sound' PA system?",
            options = listOf(
                "Owsley 'Bear' Stanley",
                "Dan Healy",
                "Doug Irwin",
                "Rick Turner"
            ),
            correctIndex = 0,
            linerNoteFact = "Owsley 'Bear' Stanley's Wall of Sound debuted at the Cow Palace in March 1974, featuring 586 JBL speakers and 48 McIntosh amplifiers totaling 26,400 watts of crystal-clear power.",
            relatedShowId = "1974-05-19"
        ),
        TriviaQuestion(
            id = "q_wall_of_sound_mics",
            category = TriviaCategory.GEAR_SOUND,
            difficulty = TriviaDifficulty.ARCHIVIST_GURU,
            era = DeadEra.WALL_OF_SOUND,
            question = "How did the 1974 Wall of Sound prevent microphone feedback despite the massive speaker stacks standing directly behind the band?",
            options = listOf(
                "Dual phase-inverted microphones wired out of phase",
                "Plexiglass acoustic baffles around each singer",
                "In-ear radio frequency monitors",
                "Directional parabolic ceiling reflectors"
            ),
            correctIndex = 0,
            linerNoteFact = "Each vocalist sang into a matched pair of condensers spaced 60mm apart and wired out of phase—sound coming from the speakers behind hit both mics equally and canceled out!"
        ),
        TriviaQuestion(
            id = "q_scarlet_fire_debut",
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = TriviaDifficulty.ARCHIVIST_GURU,
            era = DeadEra.SPRING_77,
            question = "In what month and year did the Grateful Dead debut the live 'Scarlet Begonias ➔ Fire on the Mountain' segue?",
            options = listOf(
                "March 1977 (Winterland Arena)",
                "May 1977 (Boston Garden)",
                "June 1976 (Beacon Theatre)",
                "October 1974 (Winterland)"
            ),
            correctIndex = 0,
            linerNoteFact = "On March 18, 1977 at San Francisco's Winterland Arena, 'Scarlet Begonias' transitioned seamlessly into the live debut of 'Fire on the Mountain', birthing the band's most iconic pairing."
        ),
        TriviaQuestion(
            id = "q_china_rider_transition",
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.EARLY_70S,
            question = "During the 1973–1974 era, what named instrumental jam often bridged the transition between 'China Cat Sunflower' and 'I Know You Rider'?",
            options = listOf(
                "Feelin' Groovy Jam (Mind Left Body / Uncle John's theme)",
                "Spanish Jam",
                "Watkins Glen Soundcheck Jam",
                "Slipknot!"
            ),
            correctIndex = 0,
            linerNoteFact = "Often listed by tapers as the 'Feelin' Groovy Jam' (built on a descending four-chord progression), this sunlit thematic bridge peaked during 1973 and 1974 China ➔ Rider performances."
        ),
        TriviaQuestion(
            id = "q_help_slip_frank",
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.SPRING_77,
            question = "Complete the classic 1975 Blues for Allah suite triptych: 'Help on the Way ➔ Slipknot! ➔ ___'",
            options = listOf(
                "Franklin's Tower",
                "The Music Never Stopped",
                "Crazy Fingers",
                "Estimated Prophet"
            ),
            correctIndex = 0,
            linerNoteFact = "After navigating the intricate jazz-fusion labyrinth of 'Help on the Way ➔ Slipknot!', the band erupts into the joyous two-chord Mixolydian release of 'Franklin's Tower' ('Roll away the dew!').",
            relatedShowId = "1977-05-09"
        ),
        TriviaQuestion(
            id = "q_branford_marsalis_show",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.HORNSBY_VINCE_90S,
            question = "On March 29, 1990 at Nassau Coliseum, jazz saxophonist Branford Marsalis guested on an unforgettable rendition of which song?",
            options = listOf(
                "Eyes of the World (plus Bird Song & Dark Star)",
                "Terrapin Station",
                "Shakedown Street",
                "Morning Dew"
            ),
            correctIndex = 0,
            linerNoteFact = "Immortalized on the album 'Wake Up to Find Out', Branford Marsalis joined for 'Bird Song' in Set I and stayed for all of Set II, including a soaring 'Eyes of the World' and 'Dark Star'.",
            relatedShowId = "1990-03-29"
        ),
        TriviaQuestion(
            id = "q_jerry_guitars_luthier",
            category = TriviaCategory.GEAR_SOUND,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.BRENT_80S,
            question = "Which master luthier hand-built Jerry Garcia's custom guitars 'Wolf', 'Tiger', and 'Rosebud'?",
            options = listOf(
                "Doug Irwin",
                "Stephen Cripe",
                "Paul Reed Smith",
                "Travis Bean"
            ),
            correctIndex = 0,
            linerNoteFact = "Former Alembic luthier Doug Irwin crafted 'Eagle', 'Wolf' (debuted in 1973), 'Tiger' (Jerry's primary guitar from 1979–1989), and 'Rosebud' (1990–1995), featuring onboard effects loops."
        ),
        TriviaQuestion(
            id = "q_europe_72_alligator",
            category = TriviaCategory.GEAR_SOUND,
            difficulty = TriviaDifficulty.ARCHIVIST_GURU,
            era = DeadEra.EARLY_70S,
            question = "What iconic guitar—gifted to Jerry Garcia by Graham Nash—was Jerry's main instrument throughout the entire Europe '72 tour?",
            options = listOf(
                "'Alligator' (1957 Fender Stratocaster)",
                "'Wolf' (Doug Irwin Custom)",
                "Travis Bean TB500 (Aluminum Neck)",
                "Gibson Les Paul Custom SG"
            ),
            correctIndex = 0,
            linerNoteFact = "Graham Nash gifted Jerry a swamp-ash Fender Stratocaster after a poker game; adorned with a cartoon alligator sticker and Alembic Blaster preamp, 'Alligator' defined the 1971–1973 sound."
        ),
        TriviaQuestion(
            id = "q_lyricists_duo",
            category = TriviaCategory.LYRICS_SONGS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.ALL,
            question = "Who were the two principal non-performing lyricists for the Grateful Dead?",
            options = listOf(
                "Robert Hunter & John Perry Barlow",
                "Robert Hunter & Ken Kesey",
                "John Perry Barlow & Owsley Stanley",
                "Gerrit Graham & Robert Petersen"
            ),
            correctIndex = 0,
            linerNoteFact = "Robert Hunter primarily wrote with Jerry Garcia ('Dark Star', 'Ripple', 'Terrapin Station'), while John Perry Barlow primarily collaborated with Bob Weir ('Cassidy', 'Estimated Prophet')."
        ),
        TriviaQuestion(
            id = "q_ripple_lyrics",
            category = TriviaCategory.LYRICS_SONGS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.EARLY_70S,
            question = "Complete the beloved Robert Hunter couplet from 'Ripple': 'Reach out your hand, if your cup be empty / If your cup is full, ___'",
            options = listOf(
                "may it be again",
                "let the music play",
                "drink it to the brim",
                "share it with a friend"
            ),
            correctIndex = 0,
            linerNoteFact = "Robert Hunter wrote 'Ripple', 'Brokedown Palace', and 'To Lay Me Down' in a single afternoon in London in 1970 aided by half a bottle of Retsina wine."
        ),
        TriviaQuestion(
            id = "q_time_signatures_eleven",
            category = TriviaCategory.SEGUES_SETLISTS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.PRIMAL_60S,
            question = "Why is the Primal Dead powerhouse jam 'The Eleven' named 'The Eleven'?",
            options = listOf(
                "It is played in an 11/8 time signature (1-2-3, 1-2-3, 1-2-3, 1-2)",
                "It took 11 studio takes to record on Anthem of the Sun",
                "It has 11 verses written by Robert Hunter",
                "It was debuted on November 11, 1967"
            ),
            correctIndex = 0,
            linerNoteFact = "Phil Lesh and Mickey Hart locked into a propulsive 11/8 meter during band practices, and 'St. Stephen ➔ The Eleven' became the signature psychedelic peak of 1968–1969.",
            relatedShowId = "1969-02-27"
        ),
        TriviaQuestion(
            id = "q_estimated_prophet_meter",
            category = TriviaCategory.LYRICS_SONGS,
            difficulty = TriviaDifficulty.ARCHIVIST_GURU,
            era = DeadEra.SPRING_77,
            question = "What unusual time signature drives Bob Weir and John Perry Barlow's reggae-inflected 'Estimated Prophet'?",
            options = listOf(
                "7/4 time signature",
                "5/4 time signature",
                "9/8 time signature",
                "13/8 time signature"
            ),
            correctIndex = 0,
            linerNoteFact = "Propelled by a loping 7/4 groove and Jerry Garcia's Mu-Tron III envelope filter lead lines, 'Estimated Prophet' debuted in February 1977 and anchored second sets for 18 years."
        ),
        TriviaQuestion(
            id = "q_keyboardists_timeline",
            category = TriviaCategory.BAND_HISTORY,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.BRENT_80S,
            question = "Who was the Grateful Dead's longest-tenured keyboardist, playing from April 1979 until July 1990?",
            options = listOf(
                "Brent Mydland",
                "Keith Godchaux",
                "Ron 'Pigpen' McKernan",
                "Vince Welnick"
            ),
            correctIndex = 0,
            linerNoteFact = "Brent Mydland joined on April 22, 1979 at Spartan Stadium and brought roaring Hammond B3 organ textures, soulful high harmonies, and originals like 'Blow Away' for 11 years.",
            relatedShowId = "1989-07-04"
        ),
        TriviaQuestion(
            id = "q_warlocks_hampton_89",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.BRENT_80S,
            question = "In October 1989 at Hampton Coliseum, the band played two unannounced concerts billed under their original 1965 band name. What was it?",
            options = listOf(
                "Formerly the Warlocks",
                "Mother McCree's Uptown Jug Champions",
                "The Emergency Crew",
                "New Riders of the Golden Road"
            ),
            correctIndex = 0,
            linerNoteFact = "Billed as 'Formerly the Warlocks' at Hampton Coliseum (Oct 8–9, 1989), the band busted out 'Dark Star', 'Help ➔ Slip ➔ Franklin's', and 'Attics of My Life' to thunderous crowd roars.",
            relatedShowId = "1989-10-09"
        ),
        TriviaQuestion(
            id = "q_steal_your_face_designers",
            category = TriviaCategory.BAND_HISTORY,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.ALL,
            question = "Who originally designed the iconic red, white, and blue 'Steal Your Face' lightning skull logo in 1969?",
            options = listOf(
                "Owsley 'Bear' Stanley & Bob Thomas",
                "Stanley Mouse & Alton Kelley",
                "Rick Griffin & Victor Moscoso",
                "Bill Graham & Wes Wilson"
            ),
            correctIndex = 0,
            linerNoteFact = "Owsley Stanley and artist Bob Thomas designed the high-contrast red, white, and blue circle divided by a 13-point lightning bolt so the road crew could quickly stencil and spot the band's gear cases at festivals!"
        ),
        TriviaQuestion(
            id = "q_lightning_bolt_points",
            category = TriviaCategory.BAND_HISTORY,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.ALL,
            question = "How many points are on the classic lightning bolt inside the Grateful Dead's 'Steal Your Face' skull emblem?",
            options = listOf(
                "13 points",
                "11 points",
                "7 points",
                "16 points"
            ),
            correctIndex = 0,
            linerNoteFact = "The official Stealie lightning bolt has exactly 13 points—a hallmark of Deadhead iconography and the centerpiece of the 1976 live album 'Steal Your Face'."
        ),
        TriviaQuestion(
            id = "q_englishtown_77_crowd",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.SPRING_77,
            question = "Which September 3, 1977 outdoor concert in New Jersey saw the live return of 'Eyes of the World ➔ Estimated Prophet' before over 100,000 fans?",
            options = listOf(
                "Raceway Park, Englishtown, NJ",
                "Giants Stadium, East Rutherford, NJ",
                "Roosevelt Stadium, Jersey City, NJ",
                "Watkins Glen Grand Prix Circuit, NY"
            ),
            correctIndex = 0,
            linerNoteFact = "Released as Dick's Picks Vol. 15, the Raceway Park concert in Englishtown, NJ featured the first live performance of 'Terrapin Station' as an encore and a historic 'Half-Step' opener.",
            relatedShowId = "1977-09-03"
        ),
        TriviaQuestion(
            id = "q_watkins_glen_soundcheck",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.ARCHIVIST_GURU,
            era = DeadEra.WALL_OF_SOUND,
            question = "At Summer Jam at Watkins Glen in July 1973, the Dead played an legendary 90-minute improvisation the day before the festival bill. What is it called?",
            options = listOf(
                "The Watkins Glen Soundcheck Jam",
                "The Mind Left Body Suite",
                "Seastones Prelude",
                "Playin' in the Glen"
            ),
            correctIndex = 0,
            linerNoteFact = "Before 600,000 fans arrived for the Dead, Allman Brothers, and The Band, the Dead treated early arrivals on July 27, 1973 to an unannounced two-set show containing the mythic 'Soundcheck Jam'."
        ),
        TriviaQuestion(
            id = "q_single_drummer_years",
            category = TriviaCategory.BAND_HISTORY,
            difficulty = TriviaDifficulty.TAPER_SECTION,
            era = DeadEra.WALL_OF_SOUND,
            question = "Between February 1971 and October 1974, which drummer propelled the Grateful Dead solo as a single-drummer jazz-nimble unit?",
            options = listOf(
                "Bill Kreutzmann",
                "Mickey Hart",
                "Levon Helm",
                "Airto Moreira"
            ),
            correctIndex = 0,
            linerNoteFact = "During Mickey Hart's hiatus from Feb 1971 until the Oct 20, 1974 Winterland show, Billy Kreutzmann steered the band solo through Europe '72, Veneta '72, and the 1973–74 jazz-fusion peak."
        ),
        TriviaQuestion(
            id = "q_terrapin_flyer_hometown",
            category = TriviaCategory.BAND_HISTORY,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.ALL,
            question = "What city is home base for the premier Grateful Dead tribute band Terrapin Flyer, fronted by Josh Olken?",
            options = listOf(
                "Chicago, Illinois",
                "Boulder, Colorado",
                "San Rafael, California",
                "Burlington, Vermont"
            ),
            correctIndex = 0,
            linerNoteFact = "Founded in Chicago, Terrapin Flyer has frequently toured with Melvin Seals, Tom Constanten, and Mark Karan, keeping the live Grateful Dead songbook thriving across Midwest venues.",
            relatedShowId = "2025-05-16"
        ),
        TriviaQuestion(
            id = "q_soldier_field_95",
            category = TriviaCategory.LEGENDARY_SHOWS,
            difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
            era = DeadEra.HORNSBY_VINCE_90S,
            question = "Where did the Grateful Dead play their final concert with Jerry Garcia on July 9, 1995?",
            options = listOf(
                "Soldier Field, Chicago, IL",
                "Shoreline Amphitheatre, Mountain View, CA",
                "RFK Stadium, Washington, D.C.",
                "Madison Square Garden, New York, NY"
            ),
            correctIndex = 0,
            linerNoteFact = "The final show at Chicago's Soldier Field on July 9, 1995 concluded with an emotional double encore of 'Black Muddy River' and 'Box of Rain'.",
            relatedShowId = "1995-07-09"
        )
    )

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
        val generated = mutableListOf<TriviaQuestion>()

        for (show in historicalShows) {
            val era = DeadEra.fromId(show.eraId)
            val songs = parseSongs(show)
            val correctVenueLabel = "${show.venue} (${show.city})"

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

            // 2. Seamless Segue Question (`➔`) from this show's setlist
            for (i in 0 until (songs.size - 1)) {
                val songA = songs[i]
                val songB = songs[i + 1]
                if (songA.isSegue) {
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
                                question = "At ${show.venue} on ${show.date}, '${songA.name}' seamlessly transitioned (➔) into which song?",
                                options = options,
                                correctIndex = correctIdx,
                                linerNoteFact = "During ${songA.setType.label} on ${show.date} (${show.venue}, ${show.city}), the band executed a seamless '${songA.name} ➔ ${songB.name}' transition.",
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
        seed: Long = System.currentTimeMillis()
    ): List<TriviaQuestion> {
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

        return filtered.shuffled(Random(seed)).take(mode.questionCount.coerceAtLeast(5))
    }
}
