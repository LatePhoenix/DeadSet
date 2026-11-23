package com.example.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeadSetRoutesTest {

    @Test
    fun `round-trips show ids with reserved characters`() {
        val id = "1977-05-08-cornell"
        assertEquals(id, DeadSetRoutes.decode(DeadSetRoutes.showDetail(id).removePrefix("show/")))
    }

    @Test
    fun `five tab roots are distinct`() {
        assertEquals(5, DeadSetRoutes.tabRoutes.size)
        assertTrue(DeadSetRoutes.SHOWS in DeadSetRoutes.tabRoutes)
        assertTrue(DeadSetRoutes.ALMANAC in DeadSetRoutes.tabRoutes)
        assertTrue(DeadSetRoutes.ME in DeadSetRoutes.tabRoutes)
    }

    @Test
    fun `edit show without id uses empty query`() {
        assertEquals("edit_show?showId=", DeadSetRoutes.editShow(null))
    }
}
