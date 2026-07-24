package dev.akil.rapidoglyph

import android.content.Context

data class EtaState(
    val minutes: Int?,
    val etaAtMillis: Long,
    val rawNotification: String,
    val updatedAtMillis: Long,
) {
    fun displayMinutes(nowMillis: Long = System.currentTimeMillis()): Int? {
        if (minutes == null || etaAtMillis <= 0L) return null
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

data class SweepState(
    val enabled: Boolean,
    val minutes: Int,
)

class EtaStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): EtaState = EtaState(
        minutes = preferences.getInt(KEY_MINUTES, NO_ETA).takeUnless { it == NO_ETA },
        etaAtMillis = preferences.getLong(KEY_ETA_AT, 0L),
        rawNotification = preferences.getString(KEY_RAW, "").orEmpty(),
        updatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L),
    )

    fun save(eta: ParsedEta, rawNotification: String, nowMillis: Long = System.currentTimeMillis()) {
        preferences.edit()
            .putInt(KEY_MINUTES, eta.minutes)
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
        save(ParsedEta(minutes, "Manual test"), "Manual test ETA: $minutes min")
    }

    fun readSweep(): SweepState = SweepState(
        enabled = preferences.getBoolean(KEY_SWEEP_ENABLED, false),
        minutes = preferences.getInt(KEY_SWEEP_MINUTES, 1).coerceIn(1, 99),
    )

    fun startSweep() {
        preferences.edit()
            .putInt(KEY_SWEEP_MINUTES, 1)
            .putBoolean(KEY_SWEEP_ENABLED, true)
            .apply()
    }

    fun setSweepMinutes(minutes: Int) {
        preferences.edit().putInt(KEY_SWEEP_MINUTES, minutes.coerceIn(1, 99)).apply()
    }

    fun stopSweep() {
        preferences.edit().putBoolean(KEY_SWEEP_ENABLED, false).apply()
    }

    fun clear() {
        preferences.edit()
            .putInt(KEY_MINUTES, NO_ETA)
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
        const val KEY_MINUTES = "eta_minutes"
        const val KEY_ETA_AT = "eta_at"
        const val KEY_SWEEP_ENABLED = "sweep_enabled"
        const val KEY_SWEEP_MINUTES = "sweep_minutes"

        fun nextSweepMinute(minutes: Int): Int =
            if (minutes in 1 until 99) minutes + 1 else 1

        private const val KEY_RAW = "raw_notification"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val PREFERENCES_NAME = "rapido_eta"
        private const val NO_ETA = -1
        private const val MILLIS_PER_MINUTE = 60_000L
    }
}
