package com.example.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TicketmasterAccessTest {

    @Test
    fun `proxy url wins over local key`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "https://proxy.example",
            apiKey = "secret-key",
            allowDirectKey = true
        )
        assertEquals(TicketmasterAccess.Mode.PROXY, access.mode)
        assertEquals("https://proxy.example/", access.baseUrl)
        assertEquals("", access.apiKey)
        assertTrue(access.isConfigured)
    }

    @Test
    fun `debug may use direct key when proxy unset`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "YOUR_TICKETMASTER_PROXY_URL",
            apiKey = "real-key",
            allowDirectKey = true
        )
        assertEquals(TicketmasterAccess.Mode.DIRECT, access.mode)
        assertEquals(TicketmasterAccess.DIRECT_BASE_URL, access.baseUrl)
        assertEquals("real-key", access.apiKey)
    }

    @Test
    fun `release without proxy ignores local key`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "",
            apiKey = "real-key",
            allowDirectKey = false
        )
        assertEquals(TicketmasterAccess.Mode.OFF, access.mode)
        assertFalse(access.isConfigured)
    }

    @Test
    fun `sentinels count as unset`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "YOUR_TICKETMASTER_PROXY_URL",
            apiKey = "YOUR_TICKETMASTER_API_KEY",
            allowDirectKey = true
        )
        assertEquals(TicketmasterAccess.Mode.OFF, access.mode)
    }

    @Test
    fun `proxy mode includes client header when token set`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "https://example.workers.dev",
            apiKey = "",
            clientToken = "shared-secret",
            allowDirectKey = false
        )
        assertEquals(TicketmasterAccess.Mode.PROXY, access.mode)
        assertEquals(
            mapOf(TicketmasterAccess.CLIENT_HEADER to "shared-secret"),
            access.requestHeaders()
        )
    }

    @Test
    fun `proxy mode omits header when token blank or sentinel`() {
        val blank = TicketmasterAccess.resolve(
            proxyUrl = "https://example.workers.dev",
            apiKey = "",
            clientToken = "",
            allowDirectKey = false
        )
        assertTrue(blank.requestHeaders().isEmpty())

        val sentinel = TicketmasterAccess.resolve(
            proxyUrl = "https://example.workers.dev",
            apiKey = "",
            clientToken = "YOUR_DEADSET_CLIENT_TOKEN",
            allowDirectKey = false
        )
        assertTrue(sentinel.requestHeaders().isEmpty())
    }

    @Test
    fun `direct mode never sends client header`() {
        val access = TicketmasterAccess.resolve(
            proxyUrl = "",
            apiKey = "debug-key",
            clientToken = "shared-secret",
            allowDirectKey = true
        )
        assertEquals(TicketmasterAccess.Mode.DIRECT, access.mode)
        assertTrue(access.requestHeaders().isEmpty())
    }
}
