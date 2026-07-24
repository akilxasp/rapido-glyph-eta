package dev.akil.rapidoglyph

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class EssentialKeyAccessibilityService : AccessibilityService() {
    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        DiagnosticLog.record(
            this,
            "Essential Key accessibility service connected; windowContent=false",
        )
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            DiagnosticLog.record(
                this,
                "Hardware key down: code=${event.keyCode} " +
                    "scanCode=${event.scanCode} deviceId=${event.deviceId}",
            )
        }

        if (EssentialKeyDetector.shouldRefresh(event.keyCode, event.action, event.repeatCount)) {
            EtaStore(this).requestGlyphRefresh()
            DiagnosticLog.record(this, "Essential Key requested Glyph refresh")
        }

        // Observe only. Nothing OS still receives the Essential Key event normally.
        return false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        DiagnosticLog.record(this, "Essential Key accessibility service interrupted")
    }

    override fun onDestroy() {
        DiagnosticLog.record(this, "Essential Key accessibility service destroyed")
        super.onDestroy()
    }
}
