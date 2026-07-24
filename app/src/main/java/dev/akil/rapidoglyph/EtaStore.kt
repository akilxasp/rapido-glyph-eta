package dev.akil.rapidoglyph

import android.content.Context

data class EtaState(
    val etaAtMillis: Long,
    val rawNotification: String,
    val updatedAtMillis: Long,
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

class EtaStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): EtaState = EtaState(
        etaAtMillis = preferences.getLong(KEY_ETA_AT, 0L),
        rawNotification = preferences.getString(KEY_RAW, "").orEmpty(),
        updatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L),
    )

    fun save(eta: ParsedEta, rawNotification: String, nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putLong(KEY_ETA_AT, nowMillis + eta.minutes * MILLIS_PER_MINUTE)
            .putString(KEY_RAW, rawNotification)
            .putLong(KEY_UPDATED_AT, nowMillis)
            .apply()
    }

    fun saveDiagnostic(rawNotification: String, nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putString(KEY_RAW, rawNotification)
            .putLong(KEY_UPDATED_AT, nowMillis)
            .apply()
    }

    fun setTestEta(minutes: Int) {
        save(ParsedEta(minutes), "Manual test ETA: $minutes min")
    }

    fun requestGlyphRefresh() {
        val nextToken = preferences.getLong(KEY_FORCE_REFRESH, 0L) + 1L
        preferences.edit().putLong(KEY_FORCE_REFRESH, nextToken).apply()
    }

    fun clear() {
        preferences.edit()
            .putLong(KEY_ETA_AT, 0L)
            .putString(KEY_RAW, "")
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
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

        private const val KEY_RAW = "raw_notification"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val PREFERENCES_NAME = "rapido_eta"
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
