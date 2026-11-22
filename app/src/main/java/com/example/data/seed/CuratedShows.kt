package com.example.data.seed

import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.SetType
import com.example.data.model.SongEntry
import com.example.data.model.TransitionType

/** A built-in show and its setlist (setlists are stored separately, in `song_performances`). */
data class SeedShow(val show: ShowEntity, val songs: List<SongEntry>) {
    val id: String get() = show.id
}

object CuratedShows {

    @Suppress("LongParameterList")
    private fun seed(
        id: String,
        date: String,
        venue: String,
        city: String,
        stateOrCountry: String,
        eraId: String,
        tourName: String,
        songs: List<SongEntry>,
        isAttended: Boolean = false,
        userRating: Float = 0f,
        userReview: String = "",
        attendedWith: String = "",
        favoriteSong: String = "",
        archiveIdentifier: String = "",
        tapeQuality: String = "",
        tags: String = "",
        historicTrivia: String = "",
        bandName: String = "Grateful Dead",
        venueAddress: String = ""
    ) = SeedShow(
        show = ShowEntity(
            id = id, date = date, venue = venue, city = city, stateOrCountry = stateOrCountry,
            eraId = eraId, tourName = tourName, isAttended = isAttended, userRating = userRating,
            userReview = userReview, attendedWith = attendedWith, favoriteSong = favoriteSong,
            archiveIdentifier = archiveIdentifier, tapeQuality = tapeQuality, tags = tags,
            historicTrivia = historicTrivia, bandName = bandName, venueAddress = venueAddress
        ),
        songs = songs
    )

    private fun song(
        name: String,
        set: SetType,
        pos: Int,
        isSegue: Boolean = false,
        duration: Int? = null,
        notes: String? = null,
        isHighlight: Boolean = false
    ) = SongEntry(
        name = name,
        setType = set,
        position = pos,
        transitionOut = if (isSegue) TransitionType.SEGUE else TransitionType.NONE,
        durationMinutes = duration,
        notes = notes,
        isJamHighlight = isHighlight
    )

    fun getInitialShows(): List<SeedShow> {
        return listOf(
            // 1. CORNELL 5/8/77
            seed(
                id = "1977-05-08-cornell",
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                city = "Ithaca",
                stateOrCountry = "NY",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring Tour 1977",
                songs = (
                    listOf(
                        song("New Minglewood Blues", SetType.SET_1, 1, false, 5),
                        song("Loser", SetType.SET_1, 2, false, 8),
                        song("El Paso", SetType.SET_1, 3, false, 5),
                        song("They Love Each Other", SetType.SET_1, 4, false, 7),
                        song("Jack Straw", SetType.SET_1, 5, false, 6),
                        song("Deal", SetType.SET_1, 6, false, 6),
                        song("Lazy Lightning", SetType.SET_1, 7, true, 3, "Seamless bolt transition into Supplication"),
                        song("Supplication", SetType.SET_1, 8, false, 5),
                        song("Brown-Eyed Women", SetType.SET_1, 9, false, 6),
                        song("Mama Tried", SetType.SET_1, 10, false, 4),
                        song("Row Jimmy", SetType.SET_1, 11, false, 11),
                        song("Dancin' in the Street", SetType.SET_1, 12, false, 16, "All-time funky disco Jerry", true),

                        song("Scarlet Begonias", SetType.SET_2, 1, true, 10, "The holy grail transition, ethereal bridge", true),
                        song("Fire on the Mountain", SetType.SET_2, 2, false, 15, "Peak Jerry solos with Mutron III", true),
                        song("Estimated Prophet", SetType.SET_2, 3, false, 9),
                        song("Saint Stephen", SetType.SET_2, 4, true, 5, "Furious tempo into NFA"),
                        song("Not Fade Away", SetType.SET_2, 5, true, 16, "Thunderous rhythm section segue back to Stephen"),
                        song("Saint Stephen", SetType.SET_2, 6, true, 2, "Chilling unresolved descent into Dew"),
                        song("Morning Dew", SetType.SET_2, 7, false, 14, "Regarded as the greatest Morning Dew ever played", true),

                        song("One More Saturday Night", SetType.ENCORE, 1, false, 5)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd77-05-08.sbd.hicks.4982.sbeok.shnf",
                tapeQuality = "Betty Cantor-Jackson SBD Soundboard",
                tags = "All-Time Classic,Scarlet->Fire,Epic Dew,Spring 77",
                historicTrivia = "Barton Hall 5/8/77 recording added to the Library of Congress National Recording Registry in 2011."
            ),

            // 2. VENETA 8/27/72 (Sunshine Daydream)
            seed(
                id = "1972-08-27-veneta",
                date = "1972-08-27",
                venue = "Old Renaissance Faire Grounds",
                city = "Veneta",
                stateOrCountry = "OR",
                eraId = DeadEra.EARLY_70S.id,
                tourName = "Springfield Creamery Benefit (Sunshine Daydream)",
                songs = (
                    listOf(
                        song("The Promised Land", SetType.SET_1, 1, false, 4),
                        song("Sugaree", SetType.SET_1, 2, false, 8),
                        song("Me and My Uncle", SetType.SET_1, 3, false, 4),
                        song("Deal", SetType.SET_1, 4, false, 5),
                        song("Black-Throated Wind", SetType.SET_1, 5, false, 7),
                        song("China Cat Sunflower", SetType.SET_1, 6, true, 6, "Signature melodic guitar bridge", true),
                        song("I Know You Rider", SetType.SET_1, 7, false, 6, "Thunderous 'Headlight on a northbound train'"),
                        song("Mexicali Blues", SetType.SET_1, 8, false, 4),
                        song("Bertha", SetType.SET_1, 9, false, 6),

                        song("Playing in the Band", SetType.SET_2, 1, false, 18, "Wild deep exploratory space exploration", true),
                        song("He's Gone", SetType.SET_2, 2, false, 9),
                        song("Jack Straw", SetType.SET_2, 3, false, 5),
                        song("Bird Song", SetType.SET_2, 4, false, 13, "Swirling, soaring guitar lines in the Oregon heat", true),
                        song("Greatest Story Ever Told", SetType.SET_2, 5, false, 6),

                        song("Dark Star", SetType.SET_3, 1, true, 31, "The pinnacle 30+ minute psychedelic exploration", true),
                        song("China Doll", SetType.SET_3, 2, false, 6),
                        song("El Paso", SetType.SET_3, 3, false, 5),
                        song("Sing Me Back Home", SetType.SET_3, 4, false, 11),
                        song("Sugar Magnolia", SetType.SET_3, 5, false, 9),

                        song("Casey Jones", SetType.ENCORE, 1, false, 7)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd72-08-27.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "16-track Master Reel Soundboard",
                tags = "Sunshine Daydream,Dark Star,Bird Song,China->Rider",
                historicTrivia = "Springfield Creamery benefit Field Trip (Sunshine Daydream); see dead.net show page for Veneta 8/27/72."
            ),

            // 3. FILLMORE WEST 2/27/69 (Live/Dead Core)
            seed(
                id = "1969-02-27-fillmore-west",
                date = "1969-02-27",
                venue = "Fillmore West",
                city = "San Francisco",
                stateOrCountry = "CA",
                eraId = DeadEra.PRIMAL_60S.id,
                tourName = "Fillmore West 1969 Run",
                songs = (
                    listOf(
                        song("Good Morning Little Schoolgirl", SetType.SET_1, 1, false, 12),
                        song("Doin' That Rag", SetType.SET_1, 2, false, 7),
                        song("That's It for the Other One", SetType.SET_1, 3, false, 15, "Primal Cryptical Envelopment suite", true),

                        song("Dupree's Diamond Blues", SetType.SET_2, 1, true, 5),
                        song("Mountains of the Moon", SetType.SET_2, 2, true, 5, "Ethereal Jerry acoustic into Dark Star"),
                        song("Dark Star", SetType.SET_2, 3, true, 22, "The legendary Live/Dead cut! Raw psychedelia", true),
                        song("Saint Stephen", SetType.SET_2, 4, true, 6, "William Tell bridge into The Eleven", true),
                        song("The Eleven", SetType.SET_2, 5, true, 14, "11/8 furious compound time polyrhythms", true),
                        song("Turn On Your Love Light", SetType.SET_2, 6, false, 20, "Pigpen commanding the Fillmore crowd", true),

                        song("Cosmic Charlie", SetType.ENCORE, 1, false, 7)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd69-02-27.sbd.16trk.flac",
                tapeQuality = "16-Track Bear Recording",
                tags = "Live/Dead,Pigpen,Dark Star,The Eleven,Primal",
                historicTrivia = "Fillmore West run recorded for the Live/Dead album (1969)."
            ),

            // 4. HARPUR COLLEGE 5/2/70
            seed(
                id = "1970-05-02-harpur",
                date = "1970-05-02",
                venue = "Harpur College, SUNY",
                city = "Binghamton",
                stateOrCountry = "NY",
                eraId = DeadEra.EARLY_70S.id,
                tourName = "East Coast Spring 1970",
                songs = (
                    listOf(
                        song("Don't Ease Me In", SetType.SET_1, 1, false, 4),
                        song("I Know You Rider", SetType.SET_1, 2, false, 5),
                        song("Friend of the Devil", SetType.SET_1, 3, false, 4),
                        song("Dire Wolf", SetType.SET_1, 4, false, 4),
                        song("Black Peter", SetType.SET_1, 5, false, 8),
                        song("Candyman", SetType.SET_1, 6, false, 6),
                        song("Cumberland Blues", SetType.SET_1, 7, false, 5),
                        song("Deep Elem Blues", SetType.SET_1, 8, false, 6),
                        song("Uncle John's Band", SetType.SET_1, 9, false, 6, "Gorgeous acoustic harmonies", true),

                        song("Saint Stephen", SetType.SET_2, 1, true, 5, "Explosive electric kickoff"),
                        song("That's It for the Other One", SetType.SET_2, 2, true, 16, "Cryptical > drums > Other One", true),
                        song("Cosmic Charlie", SetType.SET_2, 3, false, 8),
                        song("Casey Jones", SetType.SET_2, 4, false, 5),

                        song("China Cat Sunflower", SetType.SET_3, 1, true, 6),
                        song("I Know You Rider", SetType.SET_3, 2, false, 5),
                        song("Dancin' in the Street", SetType.SET_3, 3, true, 14, "Pivots seamlessly into The Other One jam", true),
                        song("The Other One", SetType.SET_3, 4, true, 12),
                        song("Morning Dew", SetType.SET_3, 5, false, 12, "Early electric Dew with raw emotion", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd70-05-02.sbd.miller.88500.sbeok.flac16",
                tapeQuality = "Soundboard Reel",
                tags = "Dick's Picks 8,Acoustic Set,Dancin->Other One",
                historicTrivia = ""
            ),

            // 5. FREEDOM HALL 6/18/74 (Wall of Sound)
            seed(
                id = "1974-06-18-louisville",
                date = "1974-06-18",
                venue = "Freedom Hall",
                city = "Louisville",
                stateOrCountry = "KY",
                eraId = DeadEra.WALL_OF_SOUND.id,
                tourName = "Summer Tour 1974 (Wall of Sound)",
                songs = (
                    listOf(
                        song("The Promised Land", SetType.SET_1, 1, false, 4),
                        song("It Must Have Been the Roses", SetType.SET_1, 2, false, 6),
                        song("Black-Throated Wind", SetType.SET_1, 3, false, 7),
                        song("Ramble On Rose", SetType.SET_1, 4, false, 6),
                        song("Beat It On Down the Line", SetType.SET_1, 5, false, 4),
                        song("Loser", SetType.SET_1, 6, false, 7),
                        song("Mexicali Blues", SetType.SET_1, 7, false, 4),
                        song("Eyes of the World", SetType.SET_1, 8, true, 14, "Wall of Sound jazz chords into China Doll", true),
                        song("China Doll", SetType.SET_1, 9, false, 6),
                        song("Around and Around", SetType.SET_1, 10, false, 6),

                        song("Loose Lucy", SetType.SET_2, 1, false, 5),
                        song("El Paso", SetType.SET_2, 2, false, 5),
                        song("Row Jimmy", SetType.SET_2, 3, false, 10),
                        song("Weather Report Suite", SetType.SET_2, 4, true, 16, "Majestic WRS into deep jam", true),
                        song("The Other One", SetType.SET_2, 5, true, 18, "Mind-bending Wall of Sound low-end from Phil", true),
                        song("It's a Sin Jam", SetType.SET_2, 6, true, 4, "Unique one-time improvisation", true),
                        song("Stella Blue", SetType.SET_2, 7, false, 9),
                        song("Mississippi Half-Step", SetType.SET_2, 8, false, 9),

                        song("Morning Dew", SetType.ENCORE, 1, false, 13, "Wall of Sound acoustic clarity", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd74-06-18.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Wall of Sound Multi-Mic SBD",
                tags = "Wall of Sound,Road Trips 2.3,Eyes->China Doll",
                historicTrivia = "Wall of Sound era show (Owsley Stanley system, 1974)."
            ),

            // 6. BUFFALO 5/9/77
            seed(
                id = "1977-05-09-buffalo",
                date = "1977-05-09",
                venue = "Buffalo Memorial Auditorium",
                city = "Buffalo",
                stateOrCountry = "NY",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring Tour 1977",
                songs = (
                    listOf(
                        song("Help on the Way", SetType.SET_1, 1, true, 4, "Classic tricky tempo changes into Slipknot", true),
                        song("Slipknot!", SetType.SET_1, 2, true, 6, "Dark and winding guitar labyrinth", true),
                        song("Franklin's Tower", SetType.SET_1, 3, false, 12, "Rolling joyous solos from Jerry", true),
                        song("Cassidy", SetType.SET_1, 4, false, 5),
                        song("Brown-Eyed Women", SetType.SET_1, 5, false, 5),
                        song("Mexicali Blues", SetType.SET_1, 6, false, 4),
                        song("Tennessee Jed", SetType.SET_1, 7, false, 8),
                        song("Big River", SetType.SET_1, 8, false, 6),
                        song("Peggy-O", SetType.SET_1, 9, false, 8),
                        song("Sunrise", SetType.SET_1, 10, false, 4),
                        song("The Music Never Stopped", SetType.SET_1, 11, false, 8),

                        song("Bertha", SetType.SET_2, 1, true, 7, "High energy opening segue"),
                        song("Good Lovin'", SetType.SET_2, 2, false, 6),
                        song("Ship of Fools", SetType.SET_2, 3, false, 7),
                        song("Estimated Prophet", SetType.SET_2, 4, true, 9, "Reggae bounce into The Other One"),
                        song("The Other One", SetType.SET_2, 5, true, 10, "Phil bomb drops"),
                        song("Drums", SetType.SET_2, 6, true, 4),
                        song("Not Fade Away", SetType.SET_2, 7, true, 11),
                        song("Comes a Time", SetType.SET_2, 8, true, 10, "Tender heartbreaking solo", true),
                        song("Sugar Magnolia", SetType.SET_2, 9, false, 8),

                        song("Uncle John's Band", SetType.ENCORE, 1, false, 7)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd77-05-09.sbd.unknown.1234.sbeok.shnf",
                tapeQuality = "Betty Board SBD",
                tags = "Help->Slip->Frank,Spring 77,Comes a Time",
                historicTrivia = "Played the night after Barton Hall (5/9/77)."
            ),

            // 7. RACEWAY PARK ENGLISHTOWN 9/3/77
            seed(
                id = "1977-09-03-englishtown",
                date = "1977-09-03",
                venue = "Raceway Park",
                city = "Englishtown",
                stateOrCountry = "NJ",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Summer 1977 Raceway Festival",
                songs = (
                    listOf(
                        song("The Promised Land", SetType.SET_1, 1, false, 4),
                        song("They Love Each Other", SetType.SET_1, 2, false, 7),
                        song("Me and My Uncle", SetType.SET_1, 3, false, 4),
                        song("Mississippi Half-Step", SetType.SET_1, 4, false, 11, "One of the greatest Half-Steps ever recorded", true),
                        song("Looks Like Rain", SetType.SET_1, 5, false, 8),
                        song("Peggy-O", SetType.SET_1, 6, false, 8),
                        song("New Minglewood Blues", SetType.SET_1, 7, false, 5),
                        song("Friend of the Devil", SetType.SET_1, 8, false, 7),
                        song("The Music Never Stopped", SetType.SET_1, 9, false, 8),

                        song("Bertha", SetType.SET_2, 1, true, 7),
                        song("Good Lovin'", SetType.SET_2, 2, false, 5),
                        song("Loser", SetType.SET_2, 3, false, 8),
                        song("Estimated Prophet", SetType.SET_2, 4, true, 9),
                        song("Eyes of the World", SetType.SET_2, 5, false, 13, "Fast, shimmering summer pace", true),
                        song("Samson and Delilah", SetType.SET_2, 6, false, 7),
                        song("He's Gone", SetType.SET_2, 7, true, 14, "Soulful vocal outro into NFA"),
                        song("Not Fade Away", SetType.SET_2, 8, true, 11),
                        song("Truckin'", SetType.SET_2, 9, false, 10, "First Truckin' in nearly 2 years!", true),

                        song("Terrapin Station", SetType.ENCORE, 1, false, 11, "Echoing across 100,000 fans", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd77-09-03.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard Master",
                tags = "Dick's Picks 15,100k Crowd,Truckin Bustout,Half-Step",
                historicTrivia = "Raceway Park, Englishtown — later issued as Dick's Picks Vol. 15."
            ),

            // 8. RED ROCKS 7/8/78
            seed(
                id = "1978-07-08-red-rocks",
                date = "1978-07-08",
                venue = "Red Rocks Amphitheatre",
                city = "Morrison",
                stateOrCountry = "CO",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Summer Tour 1978",
                songs = (
                    listOf(
                        song("Bertha", SetType.SET_1, 1, true, 7),
                        song("Good Lovin'", SetType.SET_1, 2, false, 6),
                        song("Dire Wolf", SetType.SET_1, 3, false, 4),
                        song("El Paso", SetType.SET_1, 4, false, 5),
                        song("It Must Have Been the Roses", SetType.SET_1, 5, false, 7),
                        song("New Minglewood Blues", SetType.SET_1, 6, false, 6),
                        song("Ramble On Rose", SetType.SET_1, 7, false, 7),
                        song("The Promised Land", SetType.SET_1, 8, false, 4),
                        song("Deal", SetType.SET_1, 9, false, 8, "Searing solo to close the first set", true),

                        song("Samson and Delilah", SetType.SET_2, 1, false, 7),
                        song("Ship of Fools", SetType.SET_2, 2, false, 7),
                        song("Estimated Prophet", SetType.SET_2, 3, true, 10),
                        song("The Other One", SetType.SET_2, 4, true, 8),
                        song("Eyes of the World", SetType.SET_2, 5, true, 11),
                        song("Drums", SetType.SET_2, 6, true, 6),
                        song("Space", SetType.SET_2, 7, true, 4),
                        song("Wharf Rat", SetType.SET_2, 8, true, 10, "Jerry crying out 'I'll get up and fly away'"),
                        song("Franklin's Tower", SetType.SET_2, 9, true, 11, "Surprise transition out of Wharf Rat!", true),
                        song("Sugar Magnolia", SetType.SET_2, 10, false, 9),

                        song("Terrapin Station", SetType.ENCORE, 1, true, 11),
                        song("One More Saturday Night", SetType.ENCORE, 2, false, 5),
                        song("Werewolves of London", SetType.ENCORE, 3, false, 6, "Warren Zevon cover under the Red Rocks moon!", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd78-07-08.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard Master",
                tags = "Red Rocks,Wharf->Franklin,Werewolves,Colorado",
                historicTrivia = ""
            ),

            // 9. FOX THEATRE ATLANTA 11/30/80
            seed(
                id = "1980-11-30-fox-atlanta",
                date = "1980-11-30",
                venue = "Fox Theatre",
                city = "Atlanta",
                stateOrCountry = "GA",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Fall Tour 1980",
                songs = (
                    listOf(
                        song("Feel Like a Stranger", SetType.SET_1, 1, false, 8, "Funky opening groove"),
                        song("Loser", SetType.SET_1, 2, false, 8),
                        song("Cassidy", SetType.SET_1, 3, false, 5),
                        song("Ramble On Rose", SetType.SET_1, 4, false, 7),
                        song("Little Red Rooster", SetType.SET_1, 5, false, 8, "Brent organ solos"),
                        song("Bird Song", SetType.SET_1, 6, false, 11, "Acoustic-tinged electric perfection", true),
                        song("Me and My Uncle", SetType.SET_1, 7, true, 4),
                        song("Big River", SetType.SET_1, 8, false, 6),
                        song("The Music Never Stopped", SetType.SET_1, 9, false, 8),

                        song("Scarlet Begonias", SetType.SET_2, 1, true, 11, "Brent's Hammond B3 organ roaring", true),
                        song("Fire on the Mountain", SetType.SET_2, 2, false, 14, "Explosive reggae grooves", true),
                        song("Samson and Delilah", SetType.SET_2, 3, false, 7),
                        song("Ship of Fools", SetType.SET_2, 4, false, 7),
                        song("Playing in the Band", SetType.SET_2, 5, true, 11),
                        song("Drums", SetType.SET_2, 6, true, 5),
                        song("Space", SetType.SET_2, 7, true, 6),
                        song("The Wheel", SetType.SET_2, 8, true, 6),
                        song("China Doll", SetType.SET_2, 9, true, 6),
                        song("Around and Around", SetType.SET_2, 10, true, 4),
                        song("Johnny B. Goode", SetType.SET_2, 11, false, 4),

                        song("Casey Jones", SetType.ENCORE, 1, false, 6)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd80-11-30.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard SBD",
                tags = "Brent Era,Fox Theatre,Bird Song,Scarlet->Fire",
                historicTrivia = ""
            ),

            // 10. RADIO CITY MUSIC HALL HALLOWEEN 10/31/80 (Matching Today in Dead History!)
            seed(
                id = "1980-10-31-radio-city",
                date = "1980-10-31",
                venue = "Radio City Music Hall",
                city = "New York",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Radio City 15th Anniversary Halloween Finale",
                songs = (
                    listOf(
                        song("Deep Elem Blues", SetType.SET_1, 1, false, 5, "Acoustic Set opener"),
                        song("The Monkey and the Engineer", SetType.SET_1, 2, false, 3),
                        song("I've Been All Around This World", SetType.SET_1, 3, false, 4),
                        song("Cassidy", SetType.SET_1, 4, false, 5),
                        song("Dire Wolf", SetType.SET_1, 5, false, 4),
                        song("To Lay Me Down", SetType.SET_1, 6, false, 8, "Sweet acoustic harmonies", true),
                        song("Bird Song", SetType.SET_1, 7, false, 11),
                        song("Ripple", SetType.SET_1, 8, false, 5, "Crowd singing along in unison", true),

                        song("China Cat Sunflower", SetType.SET_2, 1, true, 6, "Electric kickoff into Rider"),
                        song("I Know You Rider", SetType.SET_2, 2, false, 6),
                        song("Lost Sailor", SetType.SET_2, 3, true, 7),
                        song("Saint of Circumstance", SetType.SET_2, 4, false, 7),
                        song("Franklin's Tower", SetType.SET_2, 5, false, 12, "Electric dancing frenzy", true),

                        song("Scarlet Begonias", SetType.SET_3, 1, true, 11, "Halloween second set peak"),
                        song("Fire on the Mountain", SetType.SET_3, 2, false, 14, "Searing Brent B3 and Jerry soloing", true),
                        song("Drums", SetType.SET_3, 3, true, 6),
                        song("Space", SetType.SET_3, 4, true, 5),
                        song("Not Fade Away", SetType.SET_3, 5, true, 9),
                        song("Stella Blue", SetType.SET_3, 6, true, 9, "Stunning quiet beauty"),
                        song("Goin' Down the Road Feeling Bad", SetType.SET_3, 7, false, 7),

                        song("One More Saturday Night", SetType.ENCORE, 1, false, 6)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd80-10-31.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Dead Ahead 24-Track Video Soundtrack SBD",
                tags = "Halloween,Radio City,Dead Ahead,Acoustic Set,Oct 31",
                historicTrivia = ""
            ),

            // 11. TRUCKIN UP TO BUFFALO 7/4/89
            seed(
                id = "1989-07-04-rich-stadium",
                date = "1989-07-04",
                venue = "Rich Stadium",
                city = "Orchard Park",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Summer Tour 1989 (Truckin' Up to Buffalo)",
                songs = (
                    listOf(
                        song("Bertha", SetType.SET_1, 1, true, 7),
                        song("Greatest Story Ever Told", SetType.SET_1, 2, false, 5),
                        song("Cold Rain and Snow", SetType.SET_1, 3, false, 6),
                        song("Walkin' Blues", SetType.SET_1, 4, false, 6),
                        song("Row Jimmy", SetType.SET_1, 5, false, 10),
                        song("When I Paint My Masterpiece", SetType.SET_1, 6, false, 5),
                        song("Stagger Lee", SetType.SET_1, 7, false, 6),
                        song("Looks Like Rain", SetType.SET_1, 8, false, 8),
                        song("Deal", SetType.SET_1, 9, false, 7, "Blistering set 1 closer", true),

                        song("Touch of Grey", SetType.SET_2, 1, true, 6),
                        song("Man Smart Woman Smarter", SetType.SET_2, 2, false, 7),
                        song("Ship of Fools", SetType.SET_2, 3, false, 7),
                        song("Playing in the Band", SetType.SET_2, 4, true, 8),
                        song("Terrapin Station", SetType.SET_2, 5, true, 12, "Huge, soaring stadium rendition", true),
                        song("Drums", SetType.SET_2, 6, true, 7),
                        song("Space", SetType.SET_2, 7, true, 6),
                        song("I Will Take You Home", SetType.SET_2, 8, true, 4, "Brent's tender lullaby"),
                        song("All Along the Watchtower", SetType.SET_2, 9, true, 6, "Jerry unleashing ferocious Hendrix power chords", true),
                        song("Morning Dew", SetType.SET_2, 10, true, 11, "80s Dew with monstrous synth pads and Jerry solos", true),
                        song("Not Fade Away", SetType.SET_2, 11, false, 8),

                        song("U.S. Blues", SetType.ENCORE, 1, false, 5, "4th of July stadium fireworks celebration!")
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd89-07-04.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Multi-Track Soundboard / Official Release",
                tags = "Truckin Up to Buffalo,4th of July,Watchtower->Dew,Late 80s",
                historicTrivia = ""
            ),

            // 12. HAMPTON 10/9/89 (Formerly The Warlocks)
            seed(
                id = "1989-10-09-hampton",
                date = "1989-10-09",
                venue = "Hampton Coliseum",
                city = "Hampton",
                stateOrCountry = "VA",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Formerly The Warlocks Unannounced Shows",
                songs = (
                    listOf(
                        song("Feel Like a Stranger", SetType.SET_1, 1, false, 8),
                        song("Built to Last", SetType.SET_1, 2, false, 5),
                        song("Little Red Rooster", SetType.SET_1, 3, false, 8),
                        song("Ramble On Rose", SetType.SET_1, 4, false, 7),
                        song("We Can Run", SetType.SET_1, 5, false, 6),
                        song("Jack Straw", SetType.SET_1, 6, false, 6),
                        song("Row Jimmy", SetType.SET_1, 7, false, 10),
                        song("Memphis Blues Again", SetType.SET_1, 8, false, 8),
                        song("The Music Never Stopped", SetType.SET_1, 9, false, 8),

                        song("Playing in the Band", SetType.SET_2, 1, true, 7),
                        song("Uncle John's Band", SetType.SET_2, 2, true, 8, "Flawless segue sandwich"),
                        song("Playing in the Band", SetType.SET_2, 3, true, 3),
                        song("Dark Star", SetType.SET_2, 4, true, 19, "FIRST DARK STAR IN 5 YEARS! The coliseum exploded!", true),
                        song("Drums", SetType.SET_2, 5, true, 6),
                        song("Space", SetType.SET_2, 6, true, 7),
                        song("Death Don't Have No Mercy", SetType.SET_2, 7, true, 8, "First since 1970! Chilling Jerry vocals", true),
                        song("Dear Mr. Fantasy", SetType.SET_2, 8, true, 5),
                        song("Hey Jude", SetType.SET_2, 9, true, 4),
                        song("Good Lovin'", SetType.SET_2, 10, false, 6),

                        song("Attics of My Life", SetType.ENCORE, 1, false, 5, "First Attics since 1972! Total stunned silence then euphoria", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd89-10-09.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "UltraMatrix Soundboard",
                tags = "Warlocks,Dark Star Bustout,Attics,Hampton Mothership",
                historicTrivia = "Billed as Formerly the Warlocks (Hampton Coliseum, October 1989)."
            ),

            // 13. NASSAU 3/29/90 (with Branford Marsalis)
            seed(
                id = "1990-03-29-nassau",
                date = "1990-03-29",
                venue = "Nassau Veterans Memorial Coliseum",
                city = "Uniondale",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Spring Tour 1990",
                songs = (
                    listOf(
                        song("Jack Straw", SetType.SET_1, 1, false, 6),
                        song("Bertha", SetType.SET_1, 2, false, 6),
                        song("We Can Run", SetType.SET_1, 3, false, 6),
                        song("Ramble On Rose", SetType.SET_1, 4, false, 7),
                        song("When I Paint My Masterpiece", SetType.SET_1, 5, false, 5),
                        song("Bird Song", SetType.SET_1, 6, false, 13, "Branford joins on saxophone! Pure jazz telepathy", true),
                        song("The Promised Land", SetType.SET_1, 7, false, 4),

                        song("Eyes of the World", SetType.SET_2, 1, true, 16, "Branford's soprano sax weaving with Jerry", true),
                        song("Estimated Prophet", SetType.SET_2, 2, true, 14, "Soulful modal reggae jazz", true),
                        song("Dark Star", SetType.SET_2, 3, true, 18, "Otherworldly saxophone explorations", true),
                        song("Drums", SetType.SET_2, 4, true, 7),
                        song("Space", SetType.SET_2, 5, true, 6),
                        song("Dark Star Jam", SetType.SET_2, 6, true, 5),
                        song("The Wheel", SetType.SET_2, 7, true, 5),
                        song("Throwing Stones", SetType.SET_2, 8, true, 9),
                        song("Turn On Your Love Light", SetType.SET_2, 9, false, 7),

                        song("Knockin' on Heaven's Door", SetType.ENCORE, 1, false, 7, "Haunting sax fills", true)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd90-03-29.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "24-Track Digital Master SBD",
                tags = "Branford Marsalis,Wake Up to Find Out,Eyes,Spring 90",
                historicTrivia = "Branford Marsalis guest appearance; documented on Wake Up to Find Out."
            ),

            // 14. BOSTON GARDEN 9/25/91 (Bruce Hornsby Era)
            seed(
                id = "1991-09-25-boston",
                date = "1991-09-25",
                venue = "Boston Garden",
                city = "Boston",
                stateOrCountry = "MA",
                eraId = DeadEra.HORNSBY_VINCE_90S.id,
                tourName = "Fall Tour 1991",
                songs = (
                    listOf(
                        song("Help on the Way", SetType.SET_1, 1, true, 4),
                        song("Slipknot!", SetType.SET_1, 2, true, 7),
                        song("Franklin's Tower", SetType.SET_1, 3, false, 12, "Bruce Hornsby piano runs shining through", true),
                        song("Walkin' Blues", SetType.SET_1, 4, false, 6),
                        song("It Must Have Been the Roses", SetType.SET_1, 5, false, 6),
                        song("Dire Wolf", SetType.SET_1, 6, false, 4),
                        song("Queen Jane Approximately", SetType.SET_1, 7, false, 7),
                        song("Tennessee Jed", SetType.SET_1, 8, false, 8),
                        song("The Music Never Stopped", SetType.SET_1, 9, false, 9),

                        song("Victim or the Crime", SetType.SET_2, 1, true, 7),
                        song("Crazy Fingers", SetType.SET_2, 2, true, 8, "Lush piano and guitar interplay", true),
                        song("Playing in the Band", SetType.SET_2, 3, true, 9),
                        song("Terrapin Station", SetType.SET_2, 4, true, 12, "Deep resonant storytelling", true),
                        song("Drums", SetType.SET_2, 5, true, 8),
                        song("Space", SetType.SET_2, 6, true, 7),
                        song("That Would Be Something", SetType.SET_2, 7, true, 4, "Paul McCartney cover bustout"),
                        song("Playing in the Band", SetType.SET_2, 8, true, 4),
                        song("China Doll", SetType.SET_2, 9, true, 6),
                        song("Throwing Stones", SetType.SET_2, 10, true, 9),
                        song("Not Fade Away", SetType.SET_2, 11, false, 8),

                        song("The Mighty Quinn", SetType.ENCORE, 1, false, 5)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd91-09-25.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Digital DAT SBD",
                tags = "Bruce Hornsby,Boston Garden,Crazy Fingers",
                historicTrivia = ""
            ),

            // 15. SOLDIER FIELD 7/9/95 (The Final Show)
            seed(
                id = "1995-07-09-soldier-field",
                date = "1995-07-09",
                venue = "Soldier Field",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.HORNSBY_VINCE_90S.id,
                tourName = "Summer Tour 1995 (The Final Concert)",
                songs = (
                    listOf(
                        song("Touch of Grey", SetType.SET_1, 1, false, 6),
                        song("Little Red Rooster", SetType.SET_1, 2, false, 8),
                        song("Lazy River Road", SetType.SET_1, 3, false, 7),
                        song("When I Paint My Masterpiece", SetType.SET_1, 4, false, 5),
                        song("Childhood's End", SetType.SET_1, 5, false, 5),
                        song("Cumberland Blues", SetType.SET_1, 6, false, 5),
                        song("The Promised Land", SetType.SET_1, 7, false, 4),

                        song("Shakedown Street", SetType.SET_2, 1, false, 12, "Pumping funky crowd groove"),
                        song("Samson and Delilah", SetType.SET_2, 2, false, 7),
                        song("So Many Roads", SetType.SET_2, 3, false, 9, "Heart-wrenching Jerry vocal performance: 'So many roads to ease my soul'", true),
                        song("Samba in the Rain", SetType.SET_2, 4, false, 6),
                        song("Corrina", SetType.SET_2, 5, true, 8),
                        song("Drums", SetType.SET_2, 6, true, 11),
                        song("Space", SetType.SET_2, 7, true, 9),
                        song("Unbroken Chain", SetType.SET_2, 8, false, 6, "Phil Lesh masterpiece live"),
                        song("Sugar Magnolia", SetType.SET_2, 9, false, 8),

                        song("Black Muddy River", SetType.ENCORE, 1, false, 6, "Jerry's poignant farewell song", true),
                        song("Box of Rain", SetType.ENCORE, 2, false, 5, "Phil sings 'Such a long long time to be gone, and a short time to be there'")
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "gd95-07-09.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Digital Soundboard SBD",
                tags = "Final Show,So Many Roads,Black Muddy River,Chicago 95",
                historicTrivia = "Final Grateful Dead concert with Jerry Garcia (July 9, 1995)."
            ),

            // 18. TERRAPIN FLYER - MARTYRS' CHICAGO (Attended Local Classic)
            seed(
                id = "2024-05-18-terrapin-flyer-martyrs",
                date = "2024-05-18",
                venue = "Martyrs'",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring 2024 Residency",
                songs = (
                    listOf(
                        song("The Harder They Come", SetType.SET_1, 1, false, 7),
                        song("Althea", SetType.SET_1, 2, false, 8),
                        song("Ramble On Rose", SetType.SET_1, 3, false, 7),
                        song("Mississippi Half-Step", SetType.SET_1, 4, false, 10, "Greatest solo of the night", true),
                        song("Deal", SetType.SET_1, 5, false, 8),

                        song("Playing in the Band", SetType.SET_2, 1, true, 9),
                        song("Uncle John's Band", SetType.SET_2, 2, true, 8, "Flawless transition sandwich"),
                        song("Dark Star", SetType.SET_2, 3, true, 14, "Wild exploratory jam", true),
                        song("Wharf Rat", SetType.SET_2, 4, true, 9),
                        song("Sugar Magnolia", SetType.SET_2, 5, false, 8),

                        song("U.S. Blues", SetType.ENCORE, 1, false, 5)
                    )
                ),
                isAttended = false,
                userRating = 0f,
                userReview = "",
                attendedWith = "",
                favoriteSong = "",
                archiveIdentifier = "",
                tapeQuality = "Audience Matrix Recording",
                tags = "Terrapin Flyer,Martyrs,Chicago,Attended",
                historicTrivia = "Terrapin Flyer at Martyrs' (Chicago).",
                bandName = "Terrapin Flyer",
                venueAddress = "3855 N Lincoln Ave, Chicago, IL 60613"
            )
        )
    }

    /**
     * Seed shows that earlier builds shipped but were placeholder data (future-dated tribute
     * shows with invented setlists). Removed from existing installs unless the user touched them.
     */
    val retiredSeedShowIds = listOf(
        "2026-11-20-terrapin-flyer-thalia",
        "2026-12-31-terrapin-flyer-vic"
    )

    /**
     * Earlier builds shipped all 16 seed shows with an invented first-person review, rating and
     * favorite song; six were also pre-marked attended with invented companions. New installs no
     * longer do. On existing installs
     * [com.example.data.repository.ShowRepository.checkAndSeedDatabase] clears these, but only
     * where the rating, favorite song, companions and full review all still match exactly what
     * the app invented, so anything the user changed (including an appended note) is left alone.
     * Attendance is reset only for the six where it was invented, never for a show the user marked
     * attended themselves.
     */
    val legacySeedPersonalData = listOf(
        LegacySeedPersonalData("1977-05-08-cornell", 5f, "Morning Dew", "College tape traders",
            "The Barton Hall holy grail. The Scarlet -> Fire transition is sublime cosmic perfection, and Jerry's final crescendo on Morning Dew gave everyone in the room chills. Transcendent.",
            inventedAttendance = true),
        LegacySeedPersonalData("1972-08-27-veneta", 5f, "Dark Star", "",
            "Sunshine Daydream! 100 degrees out in the Oregon dust. The 31-minute Dark Star is an absolute trip to outer space.",
            inventedAttendance = false),
        LegacySeedPersonalData("1969-02-27-fillmore-west", 5f, "The Eleven", "",
            "The purest essence of primal Dead. The Dark Star -> Saint Stephen -> The Eleven -> Lovelight is the defining sequence of 1960s San Francisco music.",
            inventedAttendance = false),
        LegacySeedPersonalData("1970-05-02-harpur", 5f, "Uncle John's Band", "",
            "Dick's Picks Vol. 8. The intimate acoustic first set followed by two soaring electric sets makes this one of the most complete shows in existence.",
            inventedAttendance = false),
        LegacySeedPersonalData("1974-06-18-louisville", 5f, "Weather Report Suite", "",
            "Wall of Sound in full glory. The Weather Report Suite -> The Other One -> Stella Blue segment is peak jazz-fusion Dead.",
            inventedAttendance = false),
        LegacySeedPersonalData("1977-05-09-buffalo", 5f, "Help on the Way", "",
            "The night after Cornell and many think it equals or beats it! Opening Help -> Slip -> Franklin's is blistering, and Comes a Time is pure magic.",
            inventedAttendance = false),
        LegacySeedPersonalData("1977-09-03-englishtown", 5f, "Mississippi Half-Step", "Jersey Shore Deadheads",
            "100,000 Deadheads on a hot racetrack in New Jersey. Dick's Picks Vol. 15. The Mississippi Half-Step has the most soaring Jerry solo of the entire summer, and the bust-out Truckin' drove the crowd insane.",
            inventedAttendance = true),
        LegacySeedPersonalData("1978-07-08-red-rocks", 5f, "Wharf Rat", "",
            "Red Rocks under the stars. The Wharf Rat -> Franklin's Tower is unique and uplifting, and Jerry howling Werewolves of London was legendary.",
            inventedAttendance = false),
        LegacySeedPersonalData("1980-11-30-fox-atlanta", 4.5f, "Bird Song", "",
            "Early Brent era at its peak in the Fox Theatre's pristine acoustics. The Scarlet -> Fire has incredible synthesizer and B3 textures.",
            inventedAttendance = false),
        LegacySeedPersonalData("1980-10-31-radio-city", 5f, "Ripple", "Costumed Deadhead crew",
            "The Halloween night finale of the 15th anniversary acoustic/electric run. Broadcast live via satellite into movie theatres nationwide with Al Franken and Tom Davis hosting! The acoustic Ripple and electric Scarlet->Fire were unforgettable.",
            inventedAttendance = true),
        LegacySeedPersonalData("1989-07-04-rich-stadium", 5f, "Morning Dew", "Tour bus traveling crew",
            "4th of July at Rich Stadium! The entire stadium was shaking during Deal and U.S. Blues. Jerry was grinning ear-to-ear during Touch of Grey, and the Watchtower -> Dew combo was pure thunder.",
            inventedAttendance = true),
        LegacySeedPersonalData("1989-10-09-hampton", 5f, "Dark Star", "",
            "The Warlocks shows at Hampton. When Jerry struck the opening notes to Dark Star for the first time in 5 years, the mothership practically lifted off the ground. Attics of My Life encore brought grown Deadheads to tears.",
            inventedAttendance = false),
        LegacySeedPersonalData("1990-03-29-nassau", 5f, "Eyes of the World", "",
            "Without Ever Having Seen the Grateful Dead, Branford Marsalis sat in and played the most transcendent jazz-rock set ever captured. Eyes of the World is peak musical telepathy.",
            inventedAttendance = false),
        LegacySeedPersonalData("1991-09-25-boston", 4.5f, "Crazy Fingers", "",
            "Bruce Hornsby's grand piano brought unbelievable energy to Boston Garden in September '91. The Help -> Slip -> Franklin's opener set the arena on fire.",
            inventedAttendance = false),
        LegacySeedPersonalData("1995-07-09-soldier-field", 5f, "So Many Roads", "Tour family",
            "The final Grateful Dead show at Soldier Field. An emotional night that lives on forever in our hearts. When Jerry sang 'So Many Roads' with all of his heart and closed with Black Muddy River, there wasn't a dry eye in Chicago.",
            inventedAttendance = true),
        LegacySeedPersonalData("2024-05-18-terrapin-flyer-martyrs", 5f, "Mississippi Half-Step", "Local Deadhead friends",
            "Terrapin Flyer tore the roof off Martyrs'! Melvin Seals was on the Hammond B3 and Jerry's spirit was alive and well in Chicago. The Mississippi Half-Step was unbelievable.",
            inventedAttendance = true)
    )
}

/** Fingerprint of invented personal data a seed show used to ship with (see [CuratedShows.legacySeedPersonalData]). */
data class LegacySeedPersonalData(
    val showId: String,
    val rating: Float,
    val favoriteSong: String,
    val attendedWith: String,
    /** Full invented review text; must match exactly before cleanup clears the row. */
    val inventedReview: String,
    /** True only for the six shows that shipped pre-marked attended. */
    val inventedAttendance: Boolean
)
