package dev.akil.rapidoglyph

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RapidoNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != RAPIDO_PACKAGE) return
        DiagnosticLog.record(this, "Rapido notification posted")
        capture(sbn.notification)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        DiagnosticLog.record(this, "Notification listener connected")
        activeNotifications
            ?.filter { it.packageName == RAPIDO_PACKAGE }
            ?.maxByOrNull { it.postTime }
            ?.notification
            ?.let(::capture)
    }

    private fun capture(notification: Notification) {
        val lines = extractText(notification)
        val nowMillis = System.currentTimeMillis()
        val eta = NotificationEtaResolver.resolve(
            lines = lines,
            shortCriticalText = notification.shortCriticalText?.toString(),
            whenMillis = notification.`when`,
            nowMillis = nowMillis,
        )
        val raw = lines.joinToString(separator = "\n")
        val store = EtaStore(this)
        if (eta != null) {
            store.save(eta, raw, nowMillis)
            DiagnosticLog.record(this, "ETA captured: minutes=${eta.minutes}")
        } else {
            // A secondary Rapido notification (for example, an OTP) must not erase
            // an ETA from the active ride. Keep it visible for diagnostics instead.
            store.saveDiagnostic(raw, nowMillis)
            DiagnosticLog.record(this, "Rapido notification captured without a parsed ETA")
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != RAPIDO_PACKAGE) return
        val rapidoStillActive = activeNotifications
            ?.any { it.packageName == RAPIDO_PACKAGE }
            ?: false
        DiagnosticLog.record(this, "Rapido notification removed: stillActive=$rapidoStillActive")
        if (!rapidoStillActive) EtaStore(this).clear()
    }

    private fun extractText(notification: Notification): List<String> {
        val extras = notification.extras
        val preferredKeys = listOf(
            Notification.EXTRA_TITLE,
            Notification.EXTRA_TITLE_BIG,
            Notification.EXTRA_TEXT,
            Notification.EXTRA_BIG_TEXT,
            Notification.EXTRA_SUB_TEXT,
            Notification.EXTRA_SUMMARY_TEXT,
            Notification.EXTRA_INFO_TEXT,
        )

        val values = buildList {
            notification.shortCriticalText
                ?.toString()
                ?.takeIf(String::isNotBlank)
                ?.let { add("shortCriticalText=$it") }
            preferredKeys.forEach { key -> extras.get(key)?.toString()?.let(::add) }
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                ?.map(CharSequence::toString)
                ?.let(::addAll)

            // Diagnostic fallback: useful if Rapido puts the status-bar ETA in a custom extra.
            extras.keySet().sorted().forEach { key ->
                val value = extras.get(key)
                if (value is CharSequence && key !in preferredKeys) {
                    add("$key=$value")
                }
            }
        }

        return values.distinct()
    }

    private companion object {
        const val RAPIDO_PACKAGE = "com.rapido.passenger"
    }
}
