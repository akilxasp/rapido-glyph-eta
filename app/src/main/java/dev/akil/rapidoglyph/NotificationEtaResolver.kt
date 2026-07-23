package dev.akil.rapidoglyph

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationEtaResolver {
    fun resolve(
        lines: List<String>,
        shortCriticalText: String?,
        whenMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ParsedEta? {
        val candidates = buildList {
            shortCriticalText
                ?.takeIf(String::isNotBlank)
                ?.let { add("ETA: $it") }
            addAll(lines)
        }
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zoneId)
        EtaParser.parse(candidates, now)?.let { return it }

        val futureWhen = whenMillis - nowMillis
        if (futureWhen in 1..MAX_ETA_MILLIS) {
            val minutes = ((futureWhen + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE).toInt()
            return ParsedEta(minutes, "notification.when")
        }
        return null
    }

    private const val MILLIS_PER_MINUTE = 60_000L
    private const val MAX_ETA_MILLIS = 180 * MILLIS_PER_MINUTE
}
