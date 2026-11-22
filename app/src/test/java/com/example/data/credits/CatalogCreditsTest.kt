package com.example.data.credits

import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogCreditsTest {

    @Test
    fun `credits name required catalog sources and include MIT text`() {
        val names = CatalogCredits.sources.map { it.name }.toSet()
        assertTrue(names.contains("gdshowsdb"))
        assertTrue(names.contains("Deadlists"))
        assertTrue(names.contains("Archive.org"))
        assertTrue(CatalogCredits.GDSHOWSDB_MIT_LICENSE.contains("Copyright (c) 2013 Jeff Smith"))
        assertTrue(CatalogCredits.GDSHOWSDB_MIT_LICENSE.contains("MIT") ||
            CatalogCredits.GDSHOWSDB_MIT_LICENSE.contains("Permission is hereby granted"))
        val gd = CatalogCredits.sources.first { it.name == "gdshowsdb" }
        assertTrue(gd.url!!.contains("gdshowsdb"))
        assertTrue(gd.licenseNote!!.contains("MIT"))
    }
}
