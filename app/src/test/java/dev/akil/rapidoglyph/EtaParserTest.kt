package dev.akil.rapidoglyph

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtaParserTest {
    @Test
    fun parsesRelativeEta() {
        assertEquals(7, EtaParser.parse(listOf("Captain arriving in 7 mins"))?.minutes)
        assertEquals(3, EtaParser.parse(listOf("3 minutes away"))?.minutes)
        assertEquals(12, EtaParser.parse(listOf("ETA: 12 min"))?.minutes)
        assertEquals(5, EtaParser.parse(listOf("ETA: 5min"))?.minutes)
    }

    @Test
    fun parsesClockEta() {
        val now = LocalDateTime.of(2026, 7, 23, 22, 30)
        assertEquals(15, EtaParser.parse(listOf("Arriving by 10:45 PM"), now)?.minutes)
    }

    @Test
    fun ignoresUnrelatedNumbers() {
        assertNull(EtaParser.parse(listOf("Your OTP is 4832")))
        assertNull(EtaParser.parse(listOf("₹125 cash")))
    }
}
