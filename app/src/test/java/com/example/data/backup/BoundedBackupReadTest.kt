package com.example.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

/** H3: bounded backup read rejects input one byte over the limit. */
class BoundedBackupReadTest {

    @Test
    fun `bounded read accepts exact limit and rejects one byte over`() {
        val under = ByteArray(BACKUP_MAX_BYTES) { 'a'.code.toByte() }
        val ok = readBoundedUtf8(ByteArrayInputStream(under), BACKUP_MAX_BYTES)
        assertEquals(BACKUP_MAX_BYTES, ok!!.toByteArray(Charsets.UTF_8).size)

        val over = ByteArray(BACKUP_MAX_BYTES + 1) { 'b'.code.toByte() }
        assertNull(readBoundedUtf8(ByteArrayInputStream(over), BACKUP_MAX_BYTES))
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
        assertTrue(BACKUP_MAX_BYTES == 5 * 1024 * 1024)
    }
}
