package dev.akil.rapidoglyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticRedactorTest {
    @Test
    fun hidesNotificationContentsByDefault() {
        val payload = "Driver Akil\nCall 9876543210"

        val summary = DiagnosticRedactor.payloadSummary(payload)

        assertFalse(summary.contains("Akil"))
        assertFalse(summary.contains("9876543210"))
        assertTrue(summary.contains("2 lines"))
    }

    @Test
    fun keepsEmptyPayloadReadable() {
        assertEquals("(none)", DiagnosticRedactor.payloadSummary(""))
    }
}
