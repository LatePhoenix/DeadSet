package com.example.data.seed

import com.example.data.model.DeadEra

/**
 * Hand-curated trivia (step 5.2). Every question has a [TriviaQuestion.sourceUrl].
 * Liner notes stick to what that source supports — no invented crowd sizes, wattages, or quotes.
 */
internal fun curatedTriviaQuestions(): List<TriviaQuestion> = listOf(
    TriviaQuestion(
        id = "q_cornell_77_venue",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.SPRING_77,
        question = "Which university hall in Ithaca, NY hosted the Grateful Dead on May 8, 1977?",
        options = listOf(
            "Barton Hall, Cornell University",
            "Pauley Pavilion, UCLA",
            "Cole Field House, Maryland",
            "Cameron Indoor Stadium, Duke"
        ),
        correctIndex = 0,
        linerNoteFact = "The Barton Hall recording was added to the Library of Congress National Recording Registry in 2011.",
        relatedShowId = "1977-05-08",
        sourceUrl = "https://www.loc.gov/programs/national-recording-preservation-board/recording-registry/complete-national-recording-registry-listing/"
    ),
    TriviaQuestion(
        id = "q_cornell_77_opener",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "What song opened Set I of the May 8, 1977 Barton Hall show?",
        options = listOf(
            "New Minglewood Blues",
            "The Music Never Stopped",
            "Promised Land",
            "Bertha"
        ),
        correctIndex = 0,
        linerNoteFact = "Set I opened with New Minglewood Blues (catalog / Archive setlist for 1977-05-08).",
        relatedShowId = "1977-05-08",
        sourceUrl = "https://archive.org/details/GratefulDead"
    ),
    TriviaQuestion(
        id = "q_veneta_72_benefit",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.EARLY_70S,
        question = "The August 27, 1972 Field Trip in Veneta, Oregon was a benefit for which local business?",
        options = listOf(
            "Springfield Creamery (Nancy's Yogurt)",
            "Oregon Country Fair Co-op",
            "Eugene Organic Bakery",
            "Willamette Valley Apple Orchard"
        ),
        correctIndex = 0,
        linerNoteFact = "The Veneta Field Trip (Sunshine Daydream) was a benefit for Springfield Creamery, tied to the Kesey family.",
        relatedShowId = "1972-08-27",
        sourceUrl = "https://www.dead.net/show/august-27-1972"
    ),
    TriviaQuestion(
        id = "q_wall_of_sound_designer",
        category = TriviaCategory.GEAR_SOUND,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.WALL_OF_SOUND,
        question = "Which audio pioneer led design of the Grateful Dead's 1974 Wall of Sound PA?",
        options = listOf(
            "Owsley 'Bear' Stanley",
            "Dan Healy",
            "Doug Irwin",
            "Rick Turner"
        ),
        correctIndex = 0,
        linerNoteFact = "Owsley \"Bear\" Stanley led the Wall of Sound design; the full system debuted at the Cow Palace on March 23, 1974.",
        relatedShowId = "1974-05-19",
        sourceUrl = "https://en.wikipedia.org/wiki/Wall_of_Sound_(Grateful_Dead)"
    ),
    TriviaQuestion(
        id = "q_wall_of_sound_mics",
        category = TriviaCategory.GEAR_SOUND,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.WALL_OF_SOUND,
        question = "How did the 1974 Wall of Sound reduce vocal microphone feedback with stacks behind the band?",
        options = listOf(
            "Dual phase-inverted microphones wired out of phase",
            "Plexiglass acoustic baffles around each singer",
            "In-ear radio frequency monitors",
            "Directional parabolic ceiling reflectors"
        ),
        correctIndex = 0,
        linerNoteFact = "Vocal channels used paired microphones wired so sound from the rear stacks largely canceled, letting the wall act as its own monitor system.",
        sourceUrl = "https://en.wikipedia.org/wiki/Wall_of_Sound_(Grateful_Dead)"
    ),
    TriviaQuestion(
        id = "q_scarlet_fire_debut",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.SPRING_77,
        question = "When did the Grateful Dead debut the live Scarlet Begonias → Fire on the Mountain pairing?",
        options = listOf(
            "March 1977 (Winterland Arena)",
            "May 1977 (Boston Garden)",
            "June 1976 (Beacon Theatre)",
            "October 1974 (Winterland)"
        ),
        correctIndex = 0,
        linerNoteFact = "Fire on the Mountain debuted March 18, 1977 at Winterland, immediately after Scarlet Begonias (dead.net / Archive).",
        sourceUrl = "https://www.dead.net/features/greatest-stories-ever-told/greatest-stories-ever-told-fire-mountain"
    ),
    TriviaQuestion(
        id = "q_china_rider_transition",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.EARLY_70S,
        question = "During 1973–1974, what named jam often bridged China Cat Sunflower and I Know You Rider?",
        options = listOf(
            "Feelin' Groovy Jam (Mind Left Body / Uncle John's theme)",
            "Spanish Jam",
            "Watkins Glen Soundcheck Jam",
            "Slipknot!"
        ),
        correctIndex = 0,
        linerNoteFact = "Tapers often label the China → Rider bridge in this era as the Feelin' Groovy / Mind Left Body jam.",
        sourceUrl = "https://www.deadlists.com/"
    ),
    TriviaQuestion(
        id = "q_help_slip_frank",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.SPRING_77,
        question = "Complete the Blues for Allah suite: Help on the Way → Slipknot! → ___",
        options = listOf(
            "Franklin's Tower",
            "The Music Never Stopped",
            "Crazy Fingers",
            "Estimated Prophet"
        ),
        correctIndex = 0,
        linerNoteFact = "Help on the Way → Slipknot! → Franklin's Tower is the Blues for Allah suite sequence (studio and many live setlists).",
        relatedShowId = "1977-05-09",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_branford_marsalis_show",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.HORNSBY_VINCE_90S,
        question = "On March 29, 1990 at Nassau Coliseum, Branford Marsalis sat in on which famous set?",
        options = listOf(
            "Eyes of the World (plus Bird Song & Dark Star)",
            "Terrapin Station",
            "Shakedown Street",
            "Morning Dew"
        ),
        correctIndex = 0,
        linerNoteFact = "The guest set is documented on the live album Wake Up to Find Out (Nassau Coliseum, March 29, 1990).",
        relatedShowId = "1990-03-29",
        sourceUrl = "https://www.dead.net/show/march-29-1990"
    ),
    TriviaQuestion(
        id = "q_jerry_guitars_luthier",
        category = TriviaCategory.GEAR_SOUND,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.BRENT_80S,
        question = "Which luthier built Jerry Garcia's custom guitars Wolf, Tiger, and Rosebud?",
        options = listOf(
            "Doug Irwin",
            "Stephen Cripe",
            "Paul Reed Smith",
            "Travis Bean"
        ),
        correctIndex = 0,
        linerNoteFact = "Doug Irwin built several of Jerry's custom instruments, including Wolf, Tiger, and Rosebud.",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_europe_72_alligator",
        category = TriviaCategory.GEAR_SOUND,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.EARLY_70S,
        question = "Which guitar was Jerry Garcia's main instrument on the Europe '72 tour?",
        options = listOf(
            "'Alligator' (1957 Fender Stratocaster)",
            "'Wolf' (Doug Irwin Custom)",
            "Travis Bean TB500 (Aluminum Neck)",
            "Gibson Les Paul Custom SG"
        ),
        correctIndex = 0,
        linerNoteFact = "Jerry's Europe '72 Stratocaster is widely known as Alligator (Graham Nash connection is often repeated; treat details beyond the nickname as uncertain).",
        sourceUrl = "https://www.dead.net/tour/europe-72"
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
        linerNoteFact = "Robert Hunter wrote mainly with Jerry Garcia; John Perry Barlow mainly with Bob Weir.",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_ripple_lyrics",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.EARLY_70S,
        question = "Which American Beauty song is a Robert Hunter lyric about offering your cup if it is empty?",
        options = listOf(
            "Ripple",
            "Brokedown Palace",
            "Attics of My Life",
            "Box of Rain"
        ),
        correctIndex = 0,
        linerNoteFact = "Ripple appears on American Beauty (1970); Hunter is the lyricist (Box of Rain / album credits).",
        sourceUrl = "https://www.dead.net/song/ripple"
    ),
    TriviaQuestion(
        id = "q_time_signatures_eleven",
        category = TriviaCategory.SEGUES_SETLISTS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.PRIMAL_60S,
        question = "Why is the Primal Dead jam 'The Eleven' named 'The Eleven'?",
        options = listOf(
            "It is played in an 11/8 time signature (1-2-3, 1-2-3, 1-2-3, 1-2)",
            "It took 11 studio takes to record on Anthem of the Sun",
            "It has 11 verses written by Robert Hunter",
            "It was debuted on November 11, 1967"
        ),
        correctIndex = 0,
        linerNoteFact = "The Eleven refers to its 11-beat meter; it often followed St. Stephen in 1968–69 setlists.",
        relatedShowId = "1969-02-27",
        sourceUrl = "https://www.dead.net/song/eleven"
    ),
    TriviaQuestion(
        id = "q_estimated_prophet_meter",
        category = TriviaCategory.LYRICS_SONGS,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.SPRING_77,
        question = "What unusual time signature drives Bob Weir and John Perry Barlow's Estimated Prophet?",
        options = listOf(
            "7/4 time signature",
            "5/4 time signature",
            "9/8 time signature",
            "13/8 time signature"
        ),
        correctIndex = 0,
        linerNoteFact = "Estimated Prophet is written in 7/4 (Weir/Barlow; Terrapin Station era repertoire).",
        sourceUrl = "https://www.dead.net/song/estimated-prophet"
    ),
    TriviaQuestion(
        id = "q_keyboardists_timeline",
        category = TriviaCategory.BAND_HISTORY,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.BRENT_80S,
        question = "Who was the Grateful Dead's longest-tenured keyboardist (1979–1990)?",
        options = listOf(
            "Brent Mydland",
            "Keith Godchaux",
            "Ron 'Pigpen' McKernan",
            "Vince Welnick"
        ),
        correctIndex = 0,
        linerNoteFact = "Brent Mydland joined in 1979 and remained until 1990 — the longest keyboard tenure in the band.",
        relatedShowId = "1989-07-04",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_warlocks_hampton_89",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.BRENT_80S,
        question = "In October 1989 at Hampton Coliseum, two shows were billed under which early band name?",
        options = listOf(
            "Formerly the Warlocks",
            "Mother McCree's Uptown Jug Champions",
            "The Emergency Crew",
            "New Riders of the Golden Road"
        ),
        correctIndex = 0,
        linerNoteFact = "October 8–9, 1989 at Hampton were billed as Formerly the Warlocks (dead.net show pages).",
        relatedShowId = "1989-10-09",
        sourceUrl = "https://www.dead.net/show/october-9-1989"
    ),
    TriviaQuestion(
        id = "q_steal_your_face_designers",
        category = TriviaCategory.BAND_HISTORY,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.ALL,
        question = "Who designed the red, white, and blue Steal Your Face lightning-skull logo?",
        options = listOf(
            "Owsley 'Bear' Stanley & Bob Thomas",
            "Stanley Mouse & Alton Kelley",
            "Rick Griffin & Victor Moscoso",
            "Bill Graham & Wes Wilson"
        ),
        correctIndex = 0,
        linerNoteFact = "The Stealie logo is credited to Owsley Stanley with artist Bob Thomas (gear-case stencil origin story).",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_lightning_bolt_points",
        category = TriviaCategory.BAND_HISTORY,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.ALL,
        question = "How many points are on the classic lightning bolt in the Steal Your Face emblem?",
        options = listOf(
            "13 points",
            "11 points",
            "7 points",
            "16 points"
        ),
        correctIndex = 0,
        linerNoteFact = "The standard Stealie bolt has 13 points; the emblem titles the 1976 live album Steal Your Face.",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_englishtown_77_crowd",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.SPRING_77,
        question = "Where did the Grateful Dead play the large outdoor show on September 3, 1977 later issued as Dick's Picks Vol. 15?",
        options = listOf(
            "Raceway Park, Englishtown, NJ",
            "Giants Stadium, East Rutherford, NJ",
            "Roosevelt Stadium, Jersey City, NJ",
            "Watkins Glen Grand Prix Circuit, NY"
        ),
        correctIndex = 0,
        linerNoteFact = "Dick's Picks Volume 15 documents the September 3, 1977 Raceway Park show in Englishtown, NJ.",
        relatedShowId = "1977-09-03",
        sourceUrl = "https://www.dead.net/show/september-3-1977"
    ),
    TriviaQuestion(
        id = "q_watkins_glen_soundcheck",
        category = TriviaCategory.LEGENDARY_SHOWS,
        difficulty = TriviaDifficulty.ARCHIVIST_GURU,
        era = DeadEra.WALL_OF_SOUND,
        question = "At Summer Jam at Watkins Glen in July 1973, what is the Dead's long pre-festival improvisation often called?",
        options = listOf(
            "The Watkins Glen Soundcheck Jam",
            "The Mind Left Body Suite",
            "Seastones Prelude",
            "Playin' in the Glen"
        ),
        correctIndex = 0,
        linerNoteFact = "On July 27, 1973 the Dead played an extended 'soundcheck' set the day before the festival bill with the Allman Brothers and The Band.",
        sourceUrl = "https://www.dead.net/show/july-27-1973"
    ),
    TriviaQuestion(
        id = "q_single_drummer_years",
        category = TriviaCategory.BAND_HISTORY,
        difficulty = TriviaDifficulty.TAPER_SECTION,
        era = DeadEra.WALL_OF_SOUND,
        question = "Between February 1971 and October 1974, which drummer played as the Dead's sole drummer?",
        options = listOf(
            "Bill Kreutzmann",
            "Mickey Hart",
            "Levon Helm",
            "Airto Moreira"
        ),
        correctIndex = 0,
        linerNoteFact = "Mickey Hart's hiatus left Bill Kreutzmann as the sole drummer from early 1971 until Hart's return in October 1974.",
        sourceUrl = "https://www.dead.net/"
    ),
    TriviaQuestion(
        id = "q_terrapin_flyer_hometown",
        category = TriviaCategory.BAND_HISTORY,
        difficulty = TriviaDifficulty.SHAKEDOWN_ROOKIE,
        era = DeadEra.ALL,
        question = "What city is home base for the Grateful Dead tribute band Terrapin Flyer?",
        options = listOf(
            "Chicago, Illinois",
            "Boulder, Colorado",
            "San Rafael, California",
            "Burlington, Vermont"
        ),
        correctIndex = 0,
        linerNoteFact = "Terrapin Flyer is a Chicago-based Grateful Dead tribute (owner-curated seed; confirm against the band's own listings when linking shows).",
        relatedShowId = "2025-05-16",
        sourceUrl = "https://www.bandsintown.com/"
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
        linerNoteFact = "July 9, 1995 at Soldier Field was the final Grateful Dead concert with Jerry Garcia (dead.net show page).",
        relatedShowId = "1995-07-09",
        sourceUrl = "https://www.dead.net/show/july-9-1995"
    )
)
