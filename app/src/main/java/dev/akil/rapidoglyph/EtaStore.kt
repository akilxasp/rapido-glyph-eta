package dev.akil.rapidoglyph

import android.content.Context

data class EtaState(
    val etaAtMillis: Long,
    val rawNotification: String,
    val etaUpdatedAtMillis: Long,
    val payloadUpdatedAtMillis: Long,
    val glyphConfirmedAtMillis: Long,
) {
    fun displayMinutes(nowMillis: Long = System.currentTimeMillis()): Int? {
        if (etaAtMillis <= 0L) return null
        if (nowMillis > etaAtMillis + STALE_GRACE_MILLIS) return null
        return ((etaAtMillis - nowMillis).coerceAtLeast(0L) + MILLIS_PER_MINUTE - 1L)
            .div(MILLIS_PER_MINUTE)
            .toInt()
            .coerceAtMost(99)
    }

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
        const val STALE_GRACE_MILLIS = 5 * MILLIS_PER_MINUTE
    }
}

data class GlyphPreview(
    val token: Long,
    val minutes: Int,
)

internal fun isPreviewRequestFresh(requestedAtMillis: Long, nowMillis: Long): Boolean =
    requestedAtMillis > 0L &&
        nowMillis >= requestedAtMillis &&
        nowMillis - requestedAtMillis <= PREVIEW_REQUEST_TTL_MILLIS

class EtaStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): EtaState = EtaState(
        etaAtMillis = preferences.getLong(KEY_ETA_AT, 0L),
        rawNotification = preferences.getString(KEY_RAW, "").orEmpty(),
        etaUpdatedAtMillis = preferences.getLong(
            KEY_ETA_UPDATED_AT,
            preferences.getLong(LEGACY_KEY_UPDATED_AT, 0L),
        ),
        payloadUpdatedAtMillis = preferences.getLong(
            KEY_PAYLOAD_UPDATED_AT,
            preferences.getLong(LEGACY_KEY_UPDATED_AT, 0L),
        ),
        glyphConfirmedAtMillis = preferences.getLong(KEY_GLYPH_CONFIRMED_AT, 0L),
    )

    fun save(eta: ParsedEta, rawNotification: String, nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_ETA_AT, nowMillis + eta.minutes * MILLIS_PER_MINUTE)
            .putString(KEY_RAW, rawNotification)
            .putLong(KEY_ETA_UPDATED_AT, nowMillis)
            .putLong(KEY_PAYLOAD_UPDATED_AT, nowMillis)
            .apply()
    }

    fun saveDiagnostic(rawNotification: String, nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putString(KEY_RAW, rawNotification)
            .putLong(KEY_PAYLOAD_UPDATED_AT, nowMillis)
            .apply()
    }

    fun requestPreview(
        minutes: Int,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val nextToken = preferences.getLong(KEY_PREVIEW_REQUEST, 0L) + 1L
        preferences.edit()
            .putInt(KEY_PREVIEW_MINUTES, minutes.coerceIn(1, 99))
            .putLong(KEY_PREVIEW_REQUESTED_AT, nowMillis)
            .putLong(KEY_PREVIEW_REQUEST, nextToken)
            .apply()
    }

    fun takePendingPreview(
        nowMillis: Long = System.currentTimeMillis(),
    ): GlyphPreview? {
        val token = preferences.getLong(KEY_PREVIEW_REQUEST, 0L)
        if (token <= preferences.getLong(KEY_PREVIEW_HANDLED, 0L)) return null

        val requestedAt = preferences.getLong(KEY_PREVIEW_REQUESTED_AT, 0L)
        preferences.edit().putLong(KEY_PREVIEW_HANDLED, token).apply()
        if (!isPreviewRequestFresh(requestedAt, nowMillis)) {
            return null
        }

        return GlyphPreview(
            token = token,
            minutes = preferences.getInt(KEY_PREVIEW_MINUTES, 7).coerceIn(1, 99),
        )
    }

    fun requestGlyphRefresh() {
        val nextToken = preferences.getLong(KEY_FORCE_REFRESH, 0L) + 1L
        preferences.edit().putLong(KEY_FORCE_REFRESH, nextToken).apply()
    }

    fun markGlyphConfirmed(nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit().putLong(KEY_GLYPH_CONFIRMED_AT, nowMillis).apply()
    }

    fun clear() {
        preferences.edit()
            .putLong(KEY_ETA_AT, 0L)
            .putString(KEY_RAW, "")
            .putLong(KEY_ETA_UPDATED_AT, 0L)
            .putLong(KEY_PAYLOAD_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun register(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val KEY_ETA_AT = "eta_at"
        const val KEY_FORCE_REFRESH = "force_refresh"
        const val KEY_PREVIEW_REQUEST = "preview_request"

        private const val KEY_RAW = "raw_notification"
        private const val KEY_ETA_UPDATED_AT = "eta_updated_at"
        private const val KEY_PAYLOAD_UPDATED_AT = "payload_updated_at"
        private const val KEY_GLYPH_CONFIRMED_AT = "glyph_confirmed_at"
        private const val KEY_PREVIEW_MINUTES = "preview_minutes"
        private const val KEY_PREVIEW_REQUESTED_AT = "preview_requested_at"
        private const val KEY_PREVIEW_HANDLED = "preview_handled"
        private const val LEGACY_KEY_UPDATED_AT = "updated_at"
        private const val PREFERENCES_NAME = "rapido_eta"
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}

private const val PREVIEW_REQUEST_TTL_MILLIS = 2 * 60_000L
