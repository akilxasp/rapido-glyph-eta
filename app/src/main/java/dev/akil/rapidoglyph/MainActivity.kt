package dev.akil.rapidoglyph

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
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
        content.addView(button("Test with 7 minutes") {
            store.setTestEta(7)
            refresh()
        })
        content.addView(button("Refresh diagnostics") { refresh() })

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

        val eta = state.minutes?.let { "$it min" } ?: "waiting for Rapido"
        val updated = state.updatedAtMillis.takeIf { it > 0 }?.let {
            DateFormat.getDateTimeInstance().format(Date(it))
        } ?: "never"
        statusText.text = "Notification access: ${if (notificationAccess) "on" else "off"}\nETA: $eta\nUpdated: $updated"
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

