package dev.akil.rapidoglyph

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var statusText: TextView
    private lateinit var rawText: TextView
    private lateinit var store: EtaStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EtaStore(this)
        setContentView(buildContent())
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun buildContent(): View {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 56, 48, 56)
        }

        content.addView(TextView(this).apply {
            text = "Rapido Glyph ETA"
            textSize = 28f
        })
        content.addView(TextView(this).apply {
            text = "Reads Rapido notifications and renders the pickup ETA on the 13×13 Glyph Matrix."
            textSize = 16f
            setPadding(0, 16, 0, 24)
        })

        statusText = TextView(this).apply { textSize = 18f }
        content.addView(statusText)

        content.addView(button("1. Grant notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        content.addView(button("2. Select the Rapido ETA Glyph Toy") {
            runCatching {
                startActivity(Intent().setComponent(GLYPH_TOY_MANAGER))
            }.onFailure {
                statusText.text = "Could not open Glyph Toy settings: ${it.message}"
            }
        })
        content.addView(button("3. Enable Essential Key refresh") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        })
        content.addView(button("Test with 7 minutes") {
            store.stopSweep()
            store.setTestEta(7)
            DiagnosticLog.record(this, "Manual 7-minute test requested")
            refresh()
        })
        content.addView(button("Start 1–99 sweep (3 sec each)") {
            store.startSweep()
            DiagnosticLog.record(this, "1–99 matrix sweep requested")
            refresh()
        })
        content.addView(button("Stop number sweep") {
            store.stopSweep()
            DiagnosticLog.record(this, "Matrix sweep stop requested")
            refresh()
        })
        content.addView(button("Refresh diagnostics") { refresh() })
        content.addView(button("Copy debug dump") { copyDebugDump() })

        content.addView(TextView(this).apply {
            text = "Latest Rapido notification payload"
            textSize = 18f
            setPadding(0, 32, 0, 8)
        })
        rawText = TextView(this).apply {
            textSize = 14f
            setTextIsSelectable(true)
        }
        content.addView(rawText)

        return ScrollView(this).apply { addView(content) }
    }

    private fun copyDebugDump() {
        val dump = DiagnosticLog.dump(this, store)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Rapido Glyph ETA debug dump", dump))
        DiagnosticLog.record(this, "Debug dump copied")
        Toast.makeText(this, "Debug dump copied — paste it into the chat", Toast.LENGTH_LONG)
            .show()
    }

    private fun button(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            isAllCaps = false
            setOnClickListener { action() }
        }

    private fun refresh() {
        val state = store.read()
        val notificationAccess = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ).orEmpty().contains(packageName)
        val accessibilityAccess = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().split(':').any {
            ComponentName.unflattenFromString(it)?.className ==
                EssentialKeyAccessibilityService::class.java.name
        }

        val eta = state.displayMinutes()?.let { "$it min" } ?: "waiting for Rapido"
        val sweep = store.readSweep()
        val sweepStatus = if (sweep.enabled) {
            "running (${sweep.minutes}m, 3 sec each)"
        } else {
            "stopped"
        }
        val updated = state.updatedAtMillis.takeIf { it > 0 }?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        } ?: "never"
        statusText.text =
            "Notification access: ${if (notificationAccess) "on" else "off"}\n" +
                "Essential Key refresh: ${if (accessibilityAccess) "on" else "off"}\n" +
                "ETA: $eta\nSweep: $sweepStatus\nUpdated: $updated"
        rawText.text = state.rawNotification.ifBlank {
            "No Rapido notification captured yet. Keep this installed, grant notification access, then book a ride."
        }
    }

    private companion object {
        val GLYPH_TOY_MANAGER = ComponentName(
            "com.nothing.thirdparty",
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
        )
    }
}
