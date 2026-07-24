package dev.akil.rapidoglyph

import android.content.Context
import android.content.ComponentName
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DiagnosticLog {
    private const val PREFERENCES_NAME = "rapido_glyph_diagnostics"
    private const val KEY_EVENTS = "events"
    private const val MAX_EVENTS = 80
    private val lock = Any()

    fun record(context: Context, event: String, error: Throwable? = null) {
        val timestamp = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS Z",
            Locale.US,
        ).format(Date())
        val detail = error?.let {
            " | ${it.javaClass.name}: ${it.message.orEmpty()}"
        }.orEmpty()
        val line = "$timestamp | $event$detail"

        synchronized(lock) {
            val preferences =
                context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            val events = preferences.getString(KEY_EVENTS, "")
                .orEmpty()
                .lineSequence()
                .filter(String::isNotBlank)
                .plus(line)
                .toList()
                .takeLast(MAX_EVENTS)
            preferences.edit().putString(KEY_EVENTS, events.joinToString("\n")).apply()
        }
    }

    fun dump(context: Context, etaStore: EtaStore): String {
        val state = etaStore.read()
        val notificationAccess = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ).orEmpty().contains(context.packageName)
        val accessibilityAccess = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().split(':').any {
            ComponentName.unflattenFromString(it)?.className ==
                EssentialKeyAccessibilityService::class.java.name
        }
        val version = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            "${info.versionName} (${info.longVersionCode})"
        }.getOrElse { "unknown (${it.javaClass.simpleName})" }
        val events = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(KEY_EVENTS, "")
            .orEmpty()
            .ifBlank { "(no diagnostic events recorded)" }

        return """
            Rapido Glyph ETA debug dump
            generated=${System.currentTimeMillis()}
            app=$version
            package=${context.packageName}
            device=${Build.MANUFACTURER} ${Build.MODEL}
            product=${Build.PRODUCT}
            android=${Build.VERSION.RELEASE} sdk=${Build.VERSION.SDK_INT}
            build=${Build.DISPLAY}
            notificationAccess=$notificationAccess
            essentialKeyAccessibility=$accessibilityAccess
            displayMinutes=${state.displayMinutes()}
            etaAtMillis=${state.etaAtMillis}
            updatedAtMillis=${state.updatedAtMillis}

            --- latest Rapido notification payload ---
            ${state.rawNotification.ifBlank { "(none)" }}

            --- event log (oldest first) ---
            $events
        """.trimIndent()
    }
}
