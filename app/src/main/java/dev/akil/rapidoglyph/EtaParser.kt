package dev.akil.rapidoglyph

import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

data class ParsedEta(
    val minutes: Int,
)

object EtaParser {
    private val relativePatterns = listOf(
        Regex("""(?i)\b(?:arriv(?:e|es|ing)|pickup|captain|driver)?\s*(?:in\s*)?(\d{1,3})\s*(?:min|mins|minute|minutes)\b"""),
        Regex("""(?i)\b(\d{1,3})\s*(?:min|mins|minute|minutes)\s*(?:away|to\s+arrive)\b"""),
        Regex("""(?i)\beta\s*[:\-]?\s*(\d{1,3})\s*(?:m|min|mins|minute|minutes)\b"""),
    )

    private val clockPattern =
        Regex("""(?i)\b(?:eta|arriv(?:e|es|ing)|pickup|by)\s*[:\-]?\s*(\d{1,2})[:.](\d{2})\s*(am|pm)?\b""")

    fun parse(lines: Iterable<String>, now: LocalDateTime = LocalDateTime.now()): ParsedEta? {
        val candidates = lines
            .map(String::trim)
            .filter(String::isNotEmpty)

        candidates.forEach { text ->
            relativePatterns.forEach { pattern ->
                val match = pattern.find(text) ?: return@forEach
                val minutes = match.groupValues[1].toIntOrNull() ?: return@forEach
                if (minutes in 0..180) return ParsedEta(minutes)
            }
        }

        candidates.forEach { text ->
            val match = clockPattern.find(text) ?: return@forEach
            val hour = match.groupValues[1].toIntOrNull() ?: return@forEach
            val minute = match.groupValues[2].toIntOrNull() ?: return@forEach
            val marker = match.groupValues[3].lowercase()
            val hour24 = when {
                minute !in 0..59 -> return@forEach
                marker == "am" && hour == 12 -> 0
                marker == "am" && hour in 1..11 -> hour
                marker == "pm" && hour == 12 -> 12
                marker == "pm" && hour in 1..11 -> hour + 12
                marker.isEmpty() && hour in 0..23 -> hour
                else -> return@forEach
            }
            var target = now.toLocalDate().atTime(LocalTime.of(hour24, minute))
            if (target.isBefore(now.minusMinutes(1))) target = target.plusDays(1)
            val minutesAway = Duration.between(now, target).toMinutes().toInt().coerceAtLeast(0)
            if (minutesAway <= 180) return ParsedEta(minutesAway)
        }

        return null
    }
}
