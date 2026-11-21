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
    fun `old JSON without transitionOut reads as NONE`() {
        val songs = Converters().toSongList("""[{"name":"Ripple","setType":"ENCORE"}]""")
        assertEquals(TransitionType.NONE, songs.single().transitionOut)
    }
}
