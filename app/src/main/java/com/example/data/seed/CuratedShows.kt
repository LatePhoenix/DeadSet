package com.example.data.seed

import com.example.data.local.Converters
import com.example.data.local.ShowEntity
import com.example.data.model.DeadEra
import com.example.data.model.LegendarySegue
import com.example.data.model.SetType
import com.example.data.model.SongEntry

object CuratedShows {

    private val converters = Converters()

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
        isSegue = isSegue,
        durationMinutes = duration,
        notes = notes,
        isJamHighlight = isHighlight
    )

    fun getInitialShows(): List<ShowEntity> {
        return listOf(
            // 1. CORNELL 5/8/77
            ShowEntity(
                id = "1977-05-08-cornell",
                date = "1977-05-08",
                venue = "Barton Hall, Cornell University",
                city = "Ithaca",
                stateOrCountry = "NY",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring Tour 1977",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "The Barton Hall holy grail. The Scarlet -> Fire transition is sublime cosmic perfection, and Jerry's final crescendo on Morning Dew gave everyone in the room chills. Transcendent.",
                attendedWith = "College tape traders",
                favoriteSong = "Morning Dew",
                archiveIdentifier = "gd77-05-08.sbd.hicks.4982.sbeok.shnf",
                tapeQuality = "Betty Cantor-Jackson SBD Soundboard",
                tags = "All-Time Classic,Scarlet->Fire,Epic Dew,Spring 77",
                historicTrivia = "Selected for preservation in the National Recording Registry of the Library of Congress as one of the most culturally significant recordings in American history."
            ),

            // 2. VENETA 8/27/72 (Sunshine Daydream)
            ShowEntity(
                id = "1972-08-27-veneta",
                date = "1972-08-27",
                venue = "Old Renaissance Faire Grounds",
                city = "Veneta",
                stateOrCountry = "OR",
                eraId = DeadEra.EARLY_70S.id,
                tourName = "Springfield Creamery Benefit (Sunshine Daydream)",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "Sunshine Daydream! 100 degrees out in the Oregon dust. The 31-minute Dark Star is an absolute trip to outer space.",
                attendedWith = "",
                favoriteSong = "Dark Star",
                archiveIdentifier = "gd72-08-27.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "16-track Master Reel Soundboard",
                tags = "Sunshine Daydream,Dark Star,Bird Song,China->Rider",
                historicTrivia = "Organized as a benefit to rescue Ken Kesey's brother's yogurt creamery from bankruptcy. Performed outdoors in scorching 100-degree Oregon heat."
            ),

            // 3. FILLMORE WEST 2/27/69 (Live/Dead Core)
            ShowEntity(
                id = "1969-02-27-fillmore-west",
                date = "1969-02-27",
                venue = "Fillmore West",
                city = "San Francisco",
                stateOrCountry = "CA",
                eraId = DeadEra.PRIMAL_60S.id,
                tourName = "Fillmore West 1969 Run",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "The purest essence of primal Dead. The Dark Star -> Saint Stephen -> The Eleven -> Lovelight is the defining sequence of 1960s San Francisco music.",
                attendedWith = "",
                favoriteSong = "The Eleven",
                archiveIdentifier = "gd69-02-27.sbd.16trk.flac",
                tapeQuality = "16-Track Bear Recording",
                tags = "Live/Dead,Pigpen,Dark Star,The Eleven,Primal",
                historicTrivia = "Captured using a cutting-edge 16-track mobile studio, this show provided the definitive recordings for side 1 and 2 of 'Live/Dead'."
            ),

            // 4. HARPUR COLLEGE 5/2/70
            ShowEntity(
                id = "1970-05-02-harpur",
                date = "1970-05-02",
                venue = "Harpur College, SUNY",
                city = "Binghamton",
                stateOrCountry = "NY",
                eraId = DeadEra.EARLY_70S.id,
                tourName = "East Coast Spring 1970",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "Dick's Picks Vol. 8. The intimate acoustic first set followed by two soaring electric sets makes this one of the most complete shows in existence.",
                attendedWith = "",
                favoriteSong = "Uncle John's Band",
                archiveIdentifier = "gd70-05-02.sbd.miller.88500.sbeok.flac16",
                tapeQuality = "Soundboard Reel",
                tags = "Dick's Picks 8,Acoustic Set,Dancin->Other One",
                historicTrivia = "The campus gym had no air conditioning and was so packed with students that condensation ran down the walls during the 4-hour marathon."
            ),

            // 5. FREEDOM HALL 6/18/74 (Wall of Sound)
            ShowEntity(
                id = "1974-06-18-louisville",
                date = "1974-06-18",
                venue = "Freedom Hall",
                city = "Louisville",
                stateOrCountry = "KY",
                eraId = DeadEra.WALL_OF_SOUND.id,
                tourName = "Summer Tour 1974 (Wall of Sound)",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "Wall of Sound in full glory. The Weather Report Suite -> The Other One -> Stella Blue segment is peak jazz-fusion Dead.",
                attendedWith = "",
                favoriteSong = "Weather Report Suite",
                archiveIdentifier = "gd74-06-18.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Wall of Sound Multi-Mic SBD",
                tags = "Wall of Sound,Road Trips 2.3,Eyes->China Doll",
                historicTrivia = "Showcased the 604-speaker Wall of Sound. Phil Lesh's quadraphonic bass solos rattled Freedom Hall's concrete foundation."
            ),

            // 6. BUFFALO 5/9/77
            ShowEntity(
                id = "1977-05-09-buffalo",
                date = "1977-05-09",
                venue = "Buffalo Memorial Auditorium",
                city = "Buffalo",
                stateOrCountry = "NY",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring Tour 1977",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "The night after Cornell and many think it equals or beats it! Opening Help -> Slip -> Franklin's is blistering, and Comes a Time is pure magic.",
                attendedWith = "",
                favoriteSong = "Help on the Way",
                archiveIdentifier = "gd77-05-09.sbd.unknown.1234.sbeok.shnf",
                tapeQuality = "Betty Board SBD",
                tags = "Help->Slip->Frank,Spring 77,Comes a Time",
                historicTrivia = "Performed just 24 hours after Barton Hall. Many veteran tapers insist the second-set Comes a Time is Jerry's single greatest guitar solo ever."
            ),

            // 7. RACEWAY PARK ENGLISHTOWN 9/3/77
            ShowEntity(
                id = "1977-09-03-englishtown",
                date = "1977-09-03",
                venue = "Raceway Park",
                city = "Englishtown",
                stateOrCountry = "NJ",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Summer 1977 Raceway Festival",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "100,000 Deadheads on a hot racetrack in New Jersey. Dick's Picks Vol. 15. The Mississippi Half-Step has the most soaring Jerry solo of the entire summer, and the bust-out Truckin' drove the crowd insane.",
                attendedWith = "Jersey Shore Deadheads",
                favoriteSong = "Mississippi Half-Step",
                archiveIdentifier = "gd77-09-03.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard Master",
                tags = "Dick's Picks 15,100k Crowd,Truckin Bustout,Half-Step",
                historicTrivia = "Drew over 107,000 ticket buyers, making it the largest paid ticket concert on the East Coast at the time. Traffic backed up 20 miles on the NJ Turnpike."
            ),

            // 8. RED ROCKS 7/8/78
            ShowEntity(
                id = "1978-07-08-red-rocks",
                date = "1978-07-08",
                venue = "Red Rocks Amphitheatre",
                city = "Morrison",
                stateOrCountry = "CO",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Summer Tour 1978",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "Red Rocks under the stars. The Wharf Rat -> Franklin's Tower is unique and uplifting, and Jerry howling Werewolves of London was legendary.",
                attendedWith = "",
                favoriteSong = "Wharf Rat",
                archiveIdentifier = "gd78-07-08.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard Master",
                tags = "Red Rocks,Wharf->Franklin,Werewolves,Colorado",
                historicTrivia = "The Dead's debut headline at Colorado's iconic red sandstone monoliths. A full moon rose over the mountain as they encored with Werewolves of London."
            ),

            // 9. FOX THEATRE ATLANTA 11/30/80
            ShowEntity(
                id = "1980-11-30-fox-atlanta",
                date = "1980-11-30",
                venue = "Fox Theatre",
                city = "Atlanta",
                stateOrCountry = "GA",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Fall Tour 1980",
                setlistJson = converters.fromSongList(
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
                userRating = 4.5f,
                userReview = "Early Brent era at its peak in the Fox Theatre's pristine acoustics. The Scarlet -> Fire has incredible synthesizer and B3 textures.",
                attendedWith = "",
                favoriteSong = "Bird Song",
                archiveIdentifier = "gd80-11-30.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Soundboard SBD",
                tags = "Brent Era,Fox Theatre,Bird Song,Scarlet->Fire",
                historicTrivia = "The band personally fought to keep the grand historic Fox Theatre from being demolished, delivering one of their warmest Southern performances."
            ),

            // 10. RADIO CITY MUSIC HALL HALLOWEEN 10/31/80 (Matching Today in Dead History!)
            ShowEntity(
                id = "1980-10-31-radio-city",
                date = "1980-10-31",
                venue = "Radio City Music Hall",
                city = "New York",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Radio City 15th Anniversary Halloween Finale",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "The Halloween night finale of the 15th anniversary acoustic/electric run. Broadcast live via satellite into movie theatres nationwide with Al Franken and Tom Davis hosting! The acoustic Ripple and electric Scarlet->Fire were unforgettable.",
                attendedWith = "Costumed Deadhead crew",
                favoriteSong = "Ripple",
                archiveIdentifier = "gd80-10-31.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Dead Ahead 24-Track Video Soundtrack SBD",
                tags = "Halloween,Radio City,Dead Ahead,Acoustic Set,Oct 31",
                historicTrivia = "Broadcast live via closed-circuit satellite to movie theaters nationwide. Deadheads arrived in full Halloween costumes and danced in the ornate aisles of Radio City Music Hall."
            ),

            // 11. TRUCKIN UP TO BUFFALO 7/4/89
            ShowEntity(
                id = "1989-07-04-rich-stadium",
                date = "1989-07-04",
                venue = "Rich Stadium",
                city = "Orchard Park",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Summer Tour 1989 (Truckin' Up to Buffalo)",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "4th of July at Rich Stadium! The entire stadium was shaking during Deal and U.S. Blues. Jerry was grinning ear-to-ear during Touch of Grey, and the Watchtower -> Dew combo was pure thunder.",
                attendedWith = "Tour bus traveling crew",
                favoriteSong = "Morning Dew",
                archiveIdentifier = "gd89-07-04.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Multi-Track Soundboard / Official Release",
                tags = "Truckin Up to Buffalo,4th of July,Watchtower->Dew,Late 80s",
                historicTrivia = "Jerry wore an American flag tie on stage for Independence Day. Over 70,000 fans danced as fireworks erupted above the open-air football stadium."
            ),

            // 12. HAMPTON 10/9/89 (Formerly The Warlocks)
            ShowEntity(
                id = "1989-10-09-hampton",
                date = "1989-10-09",
                venue = "Hampton Coliseum",
                city = "Hampton",
                stateOrCountry = "VA",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Formerly The Warlocks Unannounced Shows",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "The Warlocks shows at Hampton. When Jerry struck the opening notes to Dark Star for the first time in 5 years, the mothership practically lifted off the ground. Attics of My Life encore brought grown Deadheads to tears.",
                attendedWith = "",
                favoriteSong = "Dark Star",
                archiveIdentifier = "gd89-10-09.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "UltraMatrix Soundboard",
                tags = "Warlocks,Dark Star Bustout,Attics,Hampton Mothership",
                historicTrivia = "Billed cryptically as 'Formerly The Warlocks'. When Jerry played the first note of Dark Star for the first time in 5 years, the collective roar drowned out the PA system."
            ),

            // 13. NASSAU 3/29/90 (with Branford Marsalis)
            ShowEntity(
                id = "1990-03-29-nassau",
                date = "1990-03-29",
                venue = "Nassau Veterans Memorial Coliseum",
                city = "Uniondale",
                stateOrCountry = "NY",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "Spring Tour 1990",
                setlistJson = converters.fromSongList(
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
                userRating = 5f,
                userReview = "Without Ever Having Seen the Grateful Dead, Branford Marsalis sat in and played the most transcendent jazz-rock set ever captured. Eyes of the World is peak musical telepathy.",
                attendedWith = "",
                favoriteSong = "Eyes of the World",
                archiveIdentifier = "gd90-03-29.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "24-Track Digital Master SBD",
                tags = "Branford Marsalis,Wake Up to Find Out,Eyes,Spring 90",
                historicTrivia = "Branford was only scheduled to sit in for Bird Song in Set 1, but was so blown away he stayed for the entire rest of the concert, creating a jazz-rock masterpiece."
            ),

            // 14. BOSTON GARDEN 9/25/91 (Bruce Hornsby Era)
            ShowEntity(
                id = "1991-09-25-boston",
                date = "1991-09-25",
                venue = "Boston Garden",
                city = "Boston",
                stateOrCountry = "MA",
                eraId = DeadEra.HORNSBY_VINCE_90S.id,
                tourName = "Fall Tour 1991",
                setlistJson = converters.fromSongList(
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
                userRating = 4.5f,
                userReview = "Bruce Hornsby's grand piano brought unbelievable energy to Boston Garden in September '91. The Help -> Slip -> Franklin's opener set the arena on fire.",
                attendedWith = "",
                favoriteSong = "Crazy Fingers",
                archiveIdentifier = "gd91-09-25.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Digital DAT SBD",
                tags = "Bruce Hornsby,Boston Garden,Crazy Fingers",
                historicTrivia = "Part of a historic 6-night residency at Boston Garden. Bruce Hornsby's presence pushed Jerry into rapid-fire jazz countermelodies all evening."
            ),

            // 15. SOLDIER FIELD 7/9/95 (The Final Show)
            ShowEntity(
                id = "1995-07-09-soldier-field",
                date = "1995-07-09",
                venue = "Soldier Field",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.HORNSBY_VINCE_90S.id,
                tourName = "Summer Tour 1995 (The Final Concert)",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "The final Grateful Dead show at Soldier Field. An emotional night that lives on forever in our hearts. When Jerry sang 'So Many Roads' with all of his heart and closed with Black Muddy River, there wasn't a dry eye in Chicago.",
                attendedWith = "Tour family",
                favoriteSong = "So Many Roads",
                archiveIdentifier = "gd95-07-09.sbd.miller.88600.sbeok.flac16",
                tapeQuality = "Digital Soundboard SBD",
                tags = "Final Show,So Many Roads,Black Muddy River,Chicago 95",
                historicTrivia = "The 2,318th and final Grateful Dead concert. Jerry Garcia's impassioned performance on 'So Many Roads' became his live musical epitaph."
            ),

            // 16. TERRAPIN FLYER - THALIA HALL CHICAGO (Upcoming Tour Date)
            ShowEntity(
                id = "2026-11-20-terrapin-flyer-thalia",
                date = "2026-11-20",
                venue = "Thalia Hall",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Fall 2026 Midwest Tour (feat. Melvin Seals)",
                setlistJson = converters.fromSongList(
                    listOf(
                        song("Feel Like a Stranger", SetType.SET_1, 1, false, 8, "Funky opening groove"),
                        song("Sugaree", SetType.SET_1, 2, false, 11, "Melvin Seals B3 organ flourishes", true),
                        song("Cats Under the Stars", SetType.SET_1, 3, false, 8, "JGB classic fan favorite"),
                        song("Me and My Uncle", SetType.SET_1, 4, true, 4),
                        song("Big River", SetType.SET_1, 5, false, 6),
                        song("They Love Each Other", SetType.SET_1, 6, false, 7),
                        song("Tangled Up in Blue", SetType.SET_1, 7, false, 9, "Bob Dylan cover with thunderous bass"),

                        song("Scarlet Begonias", SetType.SET_2, 1, true, 11, "Seamless transition into Fire", true),
                        song("Fire on the Mountain", SetType.SET_2, 2, false, 15, "Searing guitar and Hammond organ interplay", true),
                        song("Help on the Way", SetType.SET_2, 3, true, 4),
                        song("Slipknot!", SetType.SET_2, 4, true, 7),
                        song("Franklin's Tower", SetType.SET_2, 5, false, 12, "Joyous crowd dance party", true),
                        song("That's What Love Will Make You Do", SetType.SET_2, 6, false, 8),
                        song("The Wheel", SetType.SET_2, 7, true, 6),
                        song("Midnight Moonlight", SetType.SET_2, 8, false, 7, "Peter Rowan bluegrass rocker"),

                        song("Ripple", SetType.ENCORE, 1, false, 5, "Acoustic singalong finale")
                    )
                ),
                isAttended = false,
                userRating = 5f,
                userReview = "Upcoming show! Terrapin Flyer returning to Thalia Hall in Pilsen. Don't miss Melvin Seals on the B3 organ.",
                attendedWith = "",
                favoriteSong = "Cats Under the Stars",
                archiveIdentifier = "",
                tapeQuality = "Live Soundboard & Audience Matrix",
                tags = "Terrapin Flyer,Melvin Seals,Thalia Hall,Chicago,Upcoming",
                historicTrivia = "Chicago's beloved Grateful Dead tribute band, frequently collaborating with former Dead & JGB members. Known for delivering electrifying, soulful dual-lead guitar sets.",
                bandName = "Terrapin Flyer",
                isUpcoming = true,
                venueAddress = "1807 S Allport St, Chicago, IL 60608"
            ),

            // 17. TERRAPIN FLYER - THE VIC THEATRE CHICAGO (Upcoming NYE Show)
            ShowEntity(
                id = "2026-12-31-terrapin-flyer-vic",
                date = "2026-12-31",
                venue = "The Vic Theatre",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.BRENT_80S.id,
                tourName = "New Year's Eve Extravaganza 2026",
                setlistJson = converters.fromSongList(
                    listOf(
                        song("Bertha", SetType.SET_1, 1, false, 7),
                        song("China Cat Sunflower", SetType.SET_1, 2, true, 6, "Signature melodic guitar bridge", true),
                        song("I Know You Rider", SetType.SET_1, 3, false, 6),
                        song("How Sweet It Is (To Be Loved by You)", SetType.SET_1, 4, false, 7),
                        song("Jack Straw", SetType.SET_1, 5, false, 6),
                        song("Deal", SetType.SET_1, 6, false, 8, "Blistering set 1 closer", true),

                        song("Shakedown Street", SetType.SET_2, 1, true, 13, "Midnight countdown into Rubin & Cherise", true),
                        song("Rubin and Cherise", SetType.SET_2, 2, false, 9, "Jerry Garcia Band epic"),
                        song("Eyes of the World", SetType.SET_2, 3, true, 14, "Soaring upbeat tempo into Terrapin", true),
                        song("Terrapin Station", SetType.SET_2, 4, true, 15, "Complete Terrapin suite with full band crescendo", true),
                        song("Morning Dew", SetType.SET_2, 5, false, 12, "Emotional climax"),

                        song("Brokedown Palace", SetType.ENCORE, 1, false, 6),
                        song("One More Saturday Night", SetType.ENCORE, 2, false, 5)
                    )
                ),
                isAttended = false,
                userRating = 5f,
                userReview = "Annual New Year's Eve show in Lakeview! Midnight balloon drop and three sets of Dead and JGB.",
                attendedWith = "",
                favoriteSong = "Terrapin Station",
                archiveIdentifier = "",
                tapeQuality = "Live Multi-Track SBD",
                tags = "Terrapin Flyer,NYE 2026,The Vic,Chicago,Upcoming",
                historicTrivia = "The Vic Theatre's historic five-story vaudeville stage provides an acoustic paradise for Terrapin Flyer's annual New Year's Eve bash.",
                bandName = "Terrapin Flyer",
                isUpcoming = true,
                venueAddress = "3145 N Sheffield Ave, Chicago, IL 60657"
            ),

            // 18. TERRAPIN FLYER - MARTYRS' CHICAGO (Attended Local Classic)
            ShowEntity(
                id = "2024-05-18-terrapin-flyer-martyrs",
                date = "2024-05-18",
                venue = "Martyrs'",
                city = "Chicago",
                stateOrCountry = "IL",
                eraId = DeadEra.SPRING_77.id,
                tourName = "Spring 2024 Residency",
                setlistJson = converters.fromSongList(
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
                isAttended = true,
                userRating = 5f,
                userReview = "Terrapin Flyer tore the roof off Martyrs'! Melvin Seals was on the Hammond B3 and Jerry's spirit was alive and well in Chicago. The Mississippi Half-Step was unbelievable.",
                attendedWith = "Local Deadhead friends",
                favoriteSong = "Mississippi Half-Step",
                archiveIdentifier = "",
                tapeQuality = "Audience Matrix Recording",
                tags = "Terrapin Flyer,Martyrs,Chicago,Attended",
                historicTrivia = "Martyrs' on Lincoln Ave has been a sanctuary for live roots and jam music in Chicago for decades, and one of Terrapin Flyer's long-running home venues.",
                bandName = "Terrapin Flyer",
                isUpcoming = false,
                venueAddress = "3855 N Lincoln Ave, Chicago, IL 60613"
            )
        )
    }

    val legendaryTransitions = listOf(
        LegendarySegue(
            songA = "Scarlet Begonias",
            songB = "Fire on the Mountain",
            description = "The definitive two-song dance party segue born in 1977. Seamlessly bridges funk into cosmic reggae.",
            iconEmoji = "🔥"
        ),
        LegendarySegue(
            songA = "China Cat Sunflower",
            songB = "I Know You Rider",
            description = "The most played transition in Dead history. Melodic interlocking guitars carrying right into traditional folk bliss.",
            iconEmoji = "🌻"
        ),
        LegendarySegue(
            songA = "Help on the Way",
            songB = "Slipknot!",
            thirdSong = "Franklin's Tower",
            description = "The Holy Trinity suite from Blues for Allah. Jazz fusion complexities cascading into rolling exuberance.",
            iconEmoji = "⚡"
        ),
        LegendarySegue(
            songA = "Estimated Prophet",
            songB = "Eyes of the World",
            description = "7/4 California desert prophecy transforming into breezy 4/4 jazz-fusion light and swing.",
            iconEmoji = "👁️"
        ),
        LegendarySegue(
            songA = "Playing in the Band",
            songB = "Uncle John's Band",
            thirdSong = "Playing in the Band",
            description = "The iconic palindrome sandwich jam favored throughout the late 70s and 80s.",
            iconEmoji = "🥪"
        ),
        LegendarySegue(
            songA = "Dark Star",
            songB = "Saint Stephen",
            thirdSong = "The Eleven",
            description = "The psychedelic heart of Live/Dead (1969). Uncharted deep space into ecstatic counterculture anthem.",
            iconEmoji = "⭐"
        ),
        LegendarySegue(
            songA = "Drums",
            songB = "Space",
            thirdSong = "The Wheel",
            description = "The post-drums ambient realm giving birth to the optimistic spin of The Wheel.",
            iconEmoji = "🥁"
        ),
        LegendarySegue(
            songA = "Bertha",
            songB = "Good Lovin'",
            description = "High-energy rock 'n roll one-two punch to kick off summer stadium sets.",
            iconEmoji = "🌹"
        )
    )
}
