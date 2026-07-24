package dev.akil.rapidoglyph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EtaStateTest {
    @Test
    fun countsDownFromTheStoredArrivalTime() {
        val state = EtaState(
            etaAtMillis = 7 * 60_000L,
            rawNotification = "",
            updatedAtMillis = 0L,
        )

        assertEquals(7, state.displayMinutes(nowMillis = 1L))
        assertEquals(6, state.displayMinutes(nowMillis = 60_001L))
        assertEquals(0, state.displayMinutes(nowMillis = 7 * 60_000L))
    }

    @Test
    fun expiresFiveMinutesAfterArrival() {
        val etaAt = 7 * 60_000L
        val state = EtaState(
            etaAtMillis = etaAt,
            rawNotification = "",
            updatedAtMillis = 0L,
        )

        assertEquals(0, state.displayMinutes(nowMillis = etaAt + 5 * 60_000L))
        assertNull(state.displayMinutes(nowMillis = etaAt + 5 * 60_000L + 1L))
    }
}
