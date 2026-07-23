package dev.akil.rapidoglyph

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RapidoNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != RAPIDO_PACKAGE) return

        val lines = extractText(sbn.notification.extras)
        EtaStore(this).save(
            eta = EtaParser.parse(lines),
            rawNotification = lines.joinToString(separator = "\n"),
        )
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.packageName != RAPIDO_PACKAGE) return
        val rapidoStillActive = activeNotifications
            ?.any { it.packageName == RAPIDO_PACKAGE }
            ?: false
        if (!rapidoStillActive) EtaStore(this).clear()
    }

    private fun extractText(extras: Bundle): List<String> {
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

