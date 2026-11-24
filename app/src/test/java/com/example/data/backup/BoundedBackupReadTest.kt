package com.example.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/** H3 / 6.7: bounded backup read rejects oversize input across buffer boundaries. */
class BoundedBackupReadTest {

    @Test
    fun `bounded read accepts exact limit and rejects one byte over`() {
        val limit = 64
        val under = ByteArray(limit) { 'a'.code.toByte() }
        val ok = readBoundedUtf8(ByteArrayInputStream(under), limit)
        assertEquals(limit, ok!!.toByteArray(Charsets.UTF_8).size)

        val over = ByteArray(limit + 1) { 'b'.code.toByte() }
        assertNull(readBoundedUtf8(ByteArrayInputStream(over), limit))
        assertTrue(BACKUP_MAX_BYTES == 5 * 1024 * 1024)
    }

    @Test
    fun `bounded read with tiny limit rejects early`() {
        val stream = ByteArrayInputStream("hello world".toByteArray(Charsets.UTF_8))
        assertNull(readBoundedUtf8(stream, maxBytes = 5))
        val ok = readBoundedUtf8(
            ByteArrayInputStream("hi".toByteArray(Charsets.UTF_8)),
            maxBytes = 5
        )
        assertEquals("hi", ok)
    }

    @Test
    fun `bounded read crosses 8192 buffer boundary`() {
        val limit = 8192
        val exact = ByteArray(limit) { 'x'.code.toByte() }
        assertEquals(limit, readBoundedUtf8(ByteArrayInputStream(exact), limit)!!.length)

        val over = ByteArray(limit + 1) { 'y'.code.toByte() }
        assertNull(readBoundedUtf8(ByteArrayInputStream(over), limit))
    }
}
