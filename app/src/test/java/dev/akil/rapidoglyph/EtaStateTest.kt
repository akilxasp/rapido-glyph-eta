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
            etaUpdatedAtMillis = 0L,
            payloadUpdatedAtMillis = 0L,
            glyphConfirmedAtMillis = 0L,
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
            etaUpdatedAtMillis = 0L,
            payloadUpdatedAtMillis = 0L,
            glyphConfirmedAtMillis = 0L,
        )

        assertEquals(0, state.displayMinutes(nowMillis = etaAt + 5 * 60_000L))
        assertNull(state.displayMinutes(nowMillis = etaAt + 5 * 60_000L + 1L))
    }

    @Test
    fun testEtaCountsDownAndExpiresAtZero() {
        val state = EtaState(
            etaAtMillis = 0L,
            rawNotification = "",
            etaUpdatedAtMillis = 0L,
            payloadUpdatedAtMillis = 0L,
            glyphConfirmedAtMillis = 0L,
            testEtaAtMillis = 7 * 60_000L,
        )

        assertEquals(DisplayEta(7, DisplayEtaSource.TEST), state.displayEta(nowMillis = 1L))
        assertEquals(DisplayEta(6, DisplayEtaSource.TEST), state.displayEta(nowMillis = 60_001L))
        assertEquals(DisplayEta(0, DisplayEtaSource.TEST), state.displayEta(nowMillis = 7 * 60_000L))
        assertNull(state.displayEta(nowMillis = 7 * 60_000L + 1L))
    }

    @Test
    fun liveRapidoEtaTakesPriorityOverTestEta() {
        val state = EtaState(
            etaAtMillis = 4 * 60_000L,
            rawNotification = "",
            etaUpdatedAtMillis = 0L,
            payloadUpdatedAtMillis = 0L,
            glyphConfirmedAtMillis = 0L,
            testEtaAtMillis = 7 * 60_000L,
        )

        assertEquals(DisplayEta(4, DisplayEtaSource.RAPIDO), state.displayEta(nowMillis = 1L))
    }
}
