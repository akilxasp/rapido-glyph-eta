package dev.akil.rapidoglyph

import android.content.Context

data class EtaState(
    val minutes: Int?,
    val rawNotification: String,
    val updatedAtMillis: Long,
)

class EtaStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): EtaState = EtaState(
        minutes = preferences.getInt(KEY_MINUTES, NO_ETA).takeUnless { it == NO_ETA },
        rawNotification = preferences.getString(KEY_RAW, "").orEmpty(),
        updatedAtMillis = preferences.getLong(KEY_UPDATED_AT, 0L),
    )

    fun save(eta: ParsedEta?, rawNotification: String) {
        preferences.edit()
            .putInt(KEY_MINUTES, eta?.minutes ?: NO_ETA)
            .putString(KEY_RAW, rawNotification)
            .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            .apply()
    }

    fun setTestEta(minutes: Int) {
        save(ParsedEta(minutes, "Manual test"), "Manual test ETA: $minutes min")
    }

    fun clear() {
        save(null, "")
    }

    fun register(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregister(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener) {
        preferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    companion object {
        const val KEY_MINUTES = "eta_minutes"
        private const val KEY_RAW = "raw_notification"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val PREFERENCES_NAME = "rapido_eta"
        private const val NO_ETA = -1
    }
}

