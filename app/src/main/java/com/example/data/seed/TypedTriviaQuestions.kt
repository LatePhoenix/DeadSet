package com.example.data.seed

import com.example.data.model.DeadEra

/**
 * Step 5.3 question formats. Facts only — each item has a [TriviaQuestion.sourceUrl].
 * Lyric items use short paraphrases, never quoted lyrics.
 */
internal fun typedTriviaQuestions(): List<TriviaQuestion> = listOf(
    // --- True / False ---
    TriviaQuestion(
        id = "tf_cornell_registry",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.SPRING_77,
        question = "True or false: The Grateful Dead’s Barton Hall (May 8, 1977) recording is in the Library of Congress National Recording Registry.",
        options = listOf("True", "False"),
        correctIndex = 0,
        linerNoteFact = "Added to the National Recording Registry in 2011 (Library of Congress listing).",
        relatedShowId = "1977-05-08",
        sourceUrl = "https://www.loc.gov/programs/national-recording-preservation-board/recording-registry/complete-national-recording-registry-listing/",
        format = TriviaFormat.TRUE_FALSE
    ),
    TriviaQuestion(
        id = "tf_wall_of_sound_year",
        category = TriviaCategory.GEAR_SOUND,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.WALL_OF_SOUND,
        question = "True or false: The full Wall of Sound first toured with the Grateful Dead in 1974.",
        options = listOf("True", "False"),
        correctIndex = 0,
        linerNoteFact = "The completed Wall of Sound debuted March 23, 1974 at the Cow Palace.",
        sourceUrl = "https://en.wikipedia.org/wiki/Wall_of_Sound_(Grateful_Dead)",
        format = TriviaFormat.TRUE_FALSE
    ),
    TriviaQuestion(
        id = "tf_scarlet_fire_76",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "True or false: Scarlet Begonias → Fire on the Mountain debuted live in 1976.",
        options = listOf("True", "False"),
        correctIndex = 1,
        linerNoteFact = "Fire on the Mountain (and the Scarlet → Fire pairing) debuted March 18, 1977 at Winterland — not 1976.",
        sourceUrl = "https://www.dead.net/features/greatest-stories-ever-told/greatest-stories-ever-told-fire-mountain",
        format = TriviaFormat.TRUE_FALSE
    ),
    TriviaQuestion(
        id = "tf_final_show_soldier",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.HORNSBY_VINCE_90S,
        question = "True or false: The Grateful Dead’s final concert with Jerry Garcia was at Soldier Field on July 9, 1995.",
        options = listOf("True", "False"),
        correctIndex = 0,
        linerNoteFact = "July 9, 1995 at Soldier Field is the final Grateful Dead show with Jerry Garcia (dead.net).",
        relatedShowId = "1995-07-09",
        sourceUrl = "https://www.dead.net/show/july-9-1995",
        format = TriviaFormat.TRUE_FALSE
    ),

    // --- Order the dates (correct option = chronological; distractors are wrong permutations) ---
    TriviaQuestion(
        id = "ord_four_landmark_shows",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.ALL,
        question = "Put these shows in chronological order (earliest → latest).",
        options = listOf(
            "Veneta 8/27/72 → Cornell 5/8/77 → Englishtown 9/3/77 → Soldier Field 7/9/95",
            "Cornell 5/8/77 → Veneta 8/27/72 → Englishtown 9/3/77 → Soldier Field 7/9/95",
            "Veneta 8/27/72 → Englishtown 9/3/77 → Cornell 5/8/77 → Soldier Field 7/9/95",
            "Soldier Field 7/9/95 → Veneta 8/27/72 → Cornell 5/8/77 → Englishtown 9/3/77"
        ),
        correctIndex = 0,
        linerNoteFact = "Veneta (1972), Cornell and Englishtown (1977), then the final Soldier Field show (1995) — dates from dead.net / seed catalog.",
        sourceUrl = "https://www.dead.net/",
        format = TriviaFormat.ORDER_DATES
    ),
    TriviaQuestion(
        id = "ord_spring_77_three",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "Order these 1977 dates from earliest to latest.",
        options = listOf(
            "May 8 (Cornell) → May 9 (next night) → September 3 (Englishtown)",
            "May 9 → May 8 → September 3",
            "September 3 → May 8 → May 9",
            "May 8 → September 3 → May 9"
        ),
        correctIndex = 0,
        linerNoteFact = "Cornell is May 8, 1977; the following night is May 9; Raceway Park Englishtown is September 3, 1977.",
        relatedShowId = "1977-05-08",
        sourceUrl = "https://www.dead.net/show/may-8-1977",
        format = TriviaFormat.ORDER_DATES
    ),

    // --- Who sang it (lead vocal) ---
    TriviaQuestion(
        id = "sang_ripple",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Who usually sang lead on Ripple?",
        options = listOf("Jerry Garcia", "Bob Weir", "Phil Lesh", "Ron 'Pigpen' McKernan", "Brent Mydland"),
        correctIndex = 0,
        linerNoteFact = "Ripple is a Garcia/Hunter song; Jerry sang lead (American Beauty / dead.net song page).",
        sourceUrl = "https://www.dead.net/song/ripple",
        format = TriviaFormat.WHO_SANG
    ),
    TriviaQuestion(
        id = "sang_cassidy",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Who usually sang lead on Cassidy?",
        options = listOf("Jerry Garcia", "Bob Weir", "Phil Lesh", "Ron 'Pigpen' McKernan", "Brent Mydland"),
        correctIndex = 1,
        linerNoteFact = "Cassidy is a Weir/Barlow song; Bob Weir sang lead (dead.net).",
        sourceUrl = "https://www.dead.net/song/cassidy",
        format = TriviaFormat.WHO_SANG
    ),
    TriviaQuestion(
        id = "sang_box_of_rain",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Who usually sang lead on Box of Rain?",
        options = listOf("Jerry Garcia", "Bob Weir", "Phil Lesh", "Ron 'Pigpen' McKernan", "Brent Mydland"),
        correctIndex = 2,
        linerNoteFact = "Box of Rain is a Lesh/Hunter song; Phil Lesh sang lead (American Beauty / dead.net).",
        sourceUrl = "https://www.dead.net/song/box-rain",
        format = TriviaFormat.WHO_SANG
    ),
    TriviaQuestion(
        id = "sang_easy_wind",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.PRIMAL_60S,
        question = "Who usually sang lead on Easy Wind?",
        options = listOf("Jerry Garcia", "Bob Weir", "Phil Lesh", "Ron 'Pigpen' McKernan", "Brent Mydland"),
        correctIndex = 3,
        linerNoteFact = "Easy Wind is associated with Pigpen’s lead vocal (Workingman’s Dead era / dead.net).",
        sourceUrl = "https://www.dead.net/song/easy-wind",
        format = TriviaFormat.WHO_SANG
    ),
    TriviaQuestion(
        id = "sang_blow_away",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.BRENT_80S,
        question = "Who usually sang lead on Blow Away?",
        options = listOf("Jerry Garcia", "Bob Weir", "Phil Lesh", "Ron 'Pigpen' McKernan", "Brent Mydland"),
        correctIndex = 4,
        linerNoteFact = "Blow Away is a Brent Mydland original (Built to Last era); Brent sang lead.",
        sourceUrl = "https://www.dead.net/song/blow-away",
        format = TriviaFormat.WHO_SANG
    ),

    // --- Venue → city ---
    TriviaQuestion(
        id = "venue_barton_hall",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.SPRING_77,
        question = "In which city is Barton Hall (Cornell University)?",
        options = listOf("Ithaca, NY", "Buffalo, NY", "Boston, MA", "New Haven, CT"),
        correctIndex = 0,
        linerNoteFact = "Barton Hall is on the Cornell campus in Ithaca, New York.",
        relatedShowId = "1977-05-08",
        sourceUrl = "https://www.loc.gov/programs/national-recording-preservation-board/recording-registry/complete-national-recording-registry-listing/",
        format = TriviaFormat.VENUE_CITY
    ),
    TriviaQuestion(
        id = "venue_soldier_field",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.HORNSBY_VINCE_90S,
        question = "In which city is Soldier Field?",
        options = listOf("Chicago, IL", "Detroit, MI", "Milwaukee, WI", "St. Louis, MO"),
        correctIndex = 0,
        linerNoteFact = "Soldier Field is in Chicago; site of the final Grateful Dead show with Jerry Garcia.",
        relatedShowId = "1995-07-09",
        sourceUrl = "https://www.dead.net/show/july-9-1995",
        format = TriviaFormat.VENUE_CITY
    ),
    TriviaQuestion(
        id = "venue_winterland",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "In which city was Winterland Arena?",
        options = listOf("San Francisco, CA", "Los Angeles, CA", "Oakland, CA", "San Jose, CA"),
        correctIndex = 0,
        linerNoteFact = "Winterland was in San Francisco (Bill Graham); site of the Scarlet → Fire debut on March 18, 1977.",
        sourceUrl = "https://www.dead.net/show/march-18-1977",
        format = TriviaFormat.VENUE_CITY
    ),
    TriviaQuestion(
        id = "venue_hampton_coliseum",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.BRENT_80S,
        question = "In which city is Hampton Coliseum (Formerly the Warlocks shows, Oct 1989)?",
        options = listOf("Hampton, VA", "Richmond, VA", "Norfolk, VA", "Washington, DC"),
        correctIndex = 0,
        linerNoteFact = "Hampton Coliseum is in Hampton, Virginia (dead.net October 1989 pages).",
        relatedShowId = "1989-10-09",
        sourceUrl = "https://www.dead.net/show/october-9-1989",
        format = TriviaFormat.VENUE_CITY
    ),

    // --- Lyric paraphrase → song (no quoted lyrics) ---
    TriviaQuestion(
        id = "para_mountain_fire_weather",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.SPRING_77,
        question = "Which song’s title refers to a fire burning on a mountain?",
        options = listOf(
            "Fire on the Mountain",
            "Fire on the Bayou",
            "Mountains of the Moon",
            "Sunrise"
        ),
        correctIndex = 0,
        linerNoteFact = "Fire on the Mountain (Hart/Hunter) — title is the paraphrase; no lyric text quoted.",
        sourceUrl = "https://www.dead.net/song/fire-mountain",
        format = TriviaFormat.LYRIC_PARAPHRASE
    ),
    TriviaQuestion(
        id = "para_empty_cup",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Which song is about reaching out if your cup is empty, and wishing fullness again if it is full?",
        options = listOf("Ripple", "Brokedown Palace", "Attics of My Life", "Sugar Magnolia"),
        correctIndex = 0,
        linerNoteFact = "Paraphrase of Ripple’s theme (Hunter/Garcia; American Beauty). No lyric text quoted.",
        sourceUrl = "https://www.dead.net/song/ripple",
        format = TriviaFormat.LYRIC_PARAPHRASE
    ),
    TriviaQuestion(
        id = "para_estimated_prophet_meter",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "Which Weir/Barlow song is known for its unusual seven-beat groove and a narrator casting himself as a prophet?",
        options = listOf(
            "Estimated Prophet",
            "The Music Never Stopped",
            "Lazy Lightning",
            "Lost Sailor"
        ),
        correctIndex = 0,
        linerNoteFact = "Estimated Prophet (Weir/Barlow) is in 7/4; theme summarized without quoting lyrics.",
        sourceUrl = "https://www.dead.net/song/estimated-prophet",
        format = TriviaFormat.LYRIC_PARAPHRASE
    ),
    TriviaQuestion(
        id = "para_box_of_rain_gift",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Which Phil Lesh song is often described as offering a box of rain to ease someone’s pain?",
        options = listOf("Box of Rain", "Unbroken Chain", "Pride of Cucamonga", "Passenger"),
        correctIndex = 0,
        linerNoteFact = "Paraphrase of Box of Rain’s title/theme (Lesh/Hunter). No lyric text quoted.",
        sourceUrl = "https://www.dead.net/song/box-rain",
        format = TriviaFormat.LYRIC_PARAPHRASE
    )
).map { q ->
    val articleId = TYPED_ARTICLE_LINKS[q.id] ?: return@map q
    q.copy(relatedArticleId = articleId)
}

private val TYPED_ARTICLE_LINKS = mapOf(
    "tf_cornell_registry" to "cornell-1977-05-08",
    "tf_wall_of_sound_year" to "wall-of-sound",
    "tf_scarlet_fire_76" to "scarlet-fire",
    "venue_barton_hall" to "cornell-1977-05-08",
    "venue_winterland" to "winterland-1978-12-31",
    "para_mountain_fire_weather" to "scarlet-fire"
)
