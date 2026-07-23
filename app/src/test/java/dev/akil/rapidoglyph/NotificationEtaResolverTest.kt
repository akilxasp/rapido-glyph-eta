package dev.akil.rapidoglyph

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationEtaResolverTest {
    @Test
    fun prefersAndroidLiveUpdateStatusText() {
        val eta = NotificationEtaResolver.resolve(
            lines = listOf("Your captain is on the way"),
            shortCriticalText = "5min",
            whenMillis = 0L,
            nowMillis = 1_000L,
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(5, eta?.minutes)
    }

    @Test
    fun derivesEtaFromFutureLiveUpdateTime() {
        val now = 1_000L
        val eta = NotificationEtaResolver.resolve(
            lines = emptyList(),
            shortCriticalText = null,
            whenMillis = now + 5 * 60_000L + 1L,
            nowMillis = now,
            zoneId = ZoneOffset.UTC,
        )

        assertEquals(6, eta?.minutes)
    }

    @Test
    fun ignoresPastOrImplausiblyDistantTimes() {
        val now = 1_000L
        assertNull(
            NotificationEtaResolver.resolve(
                emptyList(),
                null,
                now - 1L,
                now,
                ZoneOffset.UTC,
            ),
        )
        assertNull(
            NotificationEtaResolver.resolve(
                emptyList(),
                null,
                now + 181 * 60_000L,
                now,
                ZoneOffset.UTC,
            ),
        )
    }
}
