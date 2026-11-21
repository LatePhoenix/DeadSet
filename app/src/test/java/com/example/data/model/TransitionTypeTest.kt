package com.example.data.model

import com.example.data.local.Converters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransitionTypeTest {

    @Test
    fun `tapping cycles stop, segue, transition, jam, then back to stop`() {
        val seen = generateSequence(TransitionType.NONE) { it.next() }.take(5).toList()
        assertEquals(
            listOf(
                TransitionType.NONE, TransitionType.SEGUE, TransitionType.TRANSITION,
                TransitionType.JAM, TransitionType.NONE
            ),
            seen
        )
    }

    @Test
    fun `only NONE is unconnected`() {
        assertFalse(TransitionType.NONE.connects)
        assertTrue(TransitionType.entries.filter { it != TransitionType.NONE }.all { it.connects })
    }

    @Test
    fun `setlist JSON round-trips transitionOut and omits the computed isConnected`() {
        val converters = Converters()
        val songs = listOf(
            SongEntry(name = "Scarlet Begonias", transitionOut = TransitionType.SEGUE),
            SongEntry(name = "Fire on the Mountain", transitionOut = TransitionType.JAM)
        )
        val json = converters.fromSongList(songs)
        assertTrue(json, json.contains("\"transitionOut\":\"SEGUE\""))
        assertFalse(json, json.contains("isConnected"))
        assertEquals(songs, converters.toSongList(json))
    }

    @Test
    fun `vault trivia only calls true segues seamless`() {
        val converters = Converters()
        fun questionFor(type: TransitionType): com.example.data.seed.TriviaQuestion {
            val setlist = listOf(
                SongEntry(name = "Estimated Prophet", setType = SetType.SET_2, transitionOut = type),
                SongEntry(name = "Eyes of the World", setType = SetType.SET_2),
                SongEntry(name = "Drums", setType = SetType.SET_2),
                SongEntry(name = "Space", setType = SetType.SET_2),
                SongEntry(name = "Morning Dew", setType = SetType.SET_2)
            )
            val base = com.example.data.seed.CuratedShows.getInitialShows()
                .filter { it.bandName == "Grateful Dead" }
            val shows = base.take(3).mapIndexed { i, s ->
                s.copy(id = "t$i", setlistJson = converters.fromSongList(setlist))
            }
            return com.example.data.seed.DeadTriviaSeed.generateDynamicVaultQuestions(shows) {
                converters.toSongList(it.setlistJson)
            }.first { it.id.startsWith("vault_segue_t0") }
        }

        val transition = questionFor(TransitionType.TRANSITION)
        assertFalse(transition.question, transition.question.contains("seamless", ignoreCase = true))
        assertFalse(transition.linerNoteFact, transition.linerNoteFact.contains("seamless", ignoreCase = true))
        assertTrue(transition.question.contains("(>)"))

        val segue = questionFor(TransitionType.SEGUE)
        assertTrue(segue.question.contains("seamlessly (->)"))
        assertTrue(segue.linerNoteFact.contains("seamless segue"))
    }

    @Test
    fun `old JSON without transitionOut reads as NONE`() {
        val songs = Converters().toSongList("""[{"name":"Ripple","setType":"ENCORE"}]""")
        assertEquals(TransitionType.NONE, songs.single().transitionOut)
    }
}
