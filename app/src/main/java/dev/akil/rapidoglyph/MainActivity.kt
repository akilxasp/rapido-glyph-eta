package dev.akil.rapidoglyph

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import java.util.Date

class MainActivity : Activity() {
    private lateinit var etaValue: TextView
    private lateinit var etaMeta: TextView
    private lateinit var notificationState: TextView
    private lateinit var essentialKeyState: TextView
    private lateinit var rawText: TextView
    private lateinit var developerPanel: LinearLayout
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
            setPadding(dp(24), dp(24), dp(24), dp(40))
            setOnApplyWindowInsetsListener { view, windowInsets ->
                val systemBars = windowInsets.getInsets(WindowInsets.Type.systemBars())
                view.setPadding(
                    dp(24),
                    systemBars.top + dp(24),
                    dp(24),
                    systemBars.bottom + dp(40),
                )
                windowInsets
            }
        }

        content.addView(label("NOTHING × RAPIDO", RED))
        content.addView(text("GLYPH ETA", 36f, Color.BLACK, Typeface.BOLD).apply {
            typeface = resources.getFont(R.font.doto)
            fontVariationSettings = "'ROND' 100, 'wght' 700"
            letterSpacing = 0.02f
            setPadding(0, dp(5), 0, 0)
        })
        content.addView(text(
            "Pickup time, at a glance—without unlocking your phone.",
            16f,
            MUTED,
        ).apply {
            setPadding(0, dp(8), 0, dp(24))
        })

        content.addView(etaCard())
        content.addView(sectionTitle("SETUP"))
        content.addView(setupCard())

        content.addView(primaryButton("SEND A 7 MIN TEST") {
            store.setTestEta(7)
            DiagnosticLog.record(this, "Manual 7-minute test requested")
            refresh()
        })

        val developerToggle = text("DEVELOPER TOOLS  ＋", 13f, Color.BLACK, Typeface.BOLD).apply {
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.08f
            setPadding(dp(4), dp(28), dp(4), dp(16))
            setOnClickListener {
                val opening = developerPanel.visibility != View.VISIBLE
                developerPanel.visibility = if (opening) View.VISIBLE else View.GONE
                text = if (opening) "DEVELOPER TOOLS  −" else "DEVELOPER TOOLS  ＋"
            }
        }
        content.addView(developerToggle)
        developerPanel = developerPanel()
        content.addView(developerPanel)

        return ScrollView(this).apply {
            setBackgroundColor(Color.WHITE)
            isFillViewport = true
            addView(content)
            content.requestApplyInsets()
        }
    }

    private fun etaCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(24), dp(22), dp(24), dp(22))
        background = rounded(BLACK, 28)

        addView(label("LIVE PICKUP", Color.WHITE))
        etaValue = text("—", 52f, Color.WHITE, Typeface.BOLD).apply {
            typeface = Typeface.MONOSPACE
            letterSpacing = -0.04f
            setPadding(0, dp(8), 0, dp(4))
        }
        addView(etaValue)
        etaMeta = text("Waiting for a Rapido ride", 14f, ON_DARK)
        addView(etaMeta)
    }.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(28) }
    }

    private fun setupCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, 0, 0, 0)
        background = rounded(SURFACE, 24)
        clipToOutline = true

        addView(setupRow(
            number = "01",
            title = "Notification access",
            description = "Read Rapido's live pickup ETA",
            onClick = { openNotificationAccessSettings() },
        ).also { notificationState = it })
        addView(divider())
        addView(setupRow(
            number = "02",
            title = "Select Glyph Toy",
            description = "Choose Rapido ETA in Nothing settings",
            onClick = { openGlyphToySettings() },
        ))
        addView(divider())
        addView(setupRow(
            number = "03",
            title = "Essential Key refresh",
            description = "Press the key to animate and refresh",
            onClick = { openEssentialKeySettings() },
        ).also { essentialKeyState = it })
    }.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(18) }
    }

    private fun setupRow(
        number: String,
        title: String,
        description: String,
        onClick: () -> Unit,
    ) = TextView(this).apply {
        tag = "$number|$title|$description"
        text = setupRowText(number, title, description, null)
        textSize = 15f
        setTextColor(Color.BLACK)
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        gravity = Gravity.CENTER_VERTICAL
        setLineSpacing(dp(3).toFloat(), 1f)
        setPadding(dp(18), dp(17), dp(18), dp(17))
        minHeight = dp(76)
        isClickable = true
        isFocusable = true
        background = setupRowBackground()
        setOnClickListener { onClick() }
    }

    private fun setupRowText(
        number: String,
        title: String,
        description: String,
        state: String?,
    ): String = buildString {
        append(number).append("    ").append(title)
        if (state != null) append("  ·  ").append(state)
        append("  ›")
        append("\n       ").append(description)
    }

    private fun developerPanel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(SURFACE, 20)

        addView(secondaryButton("COPY DEBUG DUMP") { copyDebugDump() })
        addView(secondaryButton("REFRESH DIAGNOSTICS") { refresh() })
        addView(label("LATEST RAPIDO PAYLOAD", MUTED).apply {
            setPadding(0, dp(22), 0, dp(8))
        })
        rawText = text("", 13f, MUTED).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        addView(rawText)
    }

    private fun sectionTitle(value: String) = label(value, Color.BLACK).apply {
        setPadding(dp(4), 0, 0, dp(12))
    }

    private fun primaryButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 14f
            letterSpacing = 0.08f
            setTextColor(Color.WHITE)
            typeface = Typeface.MONOSPACE
            background = rounded(BLACK, 18)
            minHeight = dp(58)
            stateListAnimator = null
            setOnClickListener { action() }
        }

    private fun secondaryButton(label: String, action: () -> Unit) =
        Button(this).apply {
            text = label
            textSize = 13f
            letterSpacing = 0.05f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            background = rounded(Color.WHITE, 14, STROKE)
            minHeight = dp(50)
            stateListAnimator = null
            setOnClickListener { action() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { topMargin = dp(10) }
        }

    private fun label(value: String, color: Int) =
        text(value, 12f, color, Typeface.BOLD).apply {
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.12f
        }

    private fun text(
        value: String,
        size: Float,
        color: Int,
        style: Int = Typeface.NORMAL,
    ) = TextView(this).apply {
        text = value
        textSize = size
        setTextColor(color)
        typeface = Typeface.create("sans-serif", style)
    }

    private fun divider() = View(this).apply {
        setBackgroundColor(STROKE)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1),
        )
    }

    private fun rounded(fill: Int, radiusDp: Int, stroke: Int? = null) =
        GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(fill)
            cornerRadius = dp(radiusDp).toFloat()
            stroke?.let { setStroke(dp(1), it) }
        }

    private fun setupRowBackground() = StateListDrawable().apply {
        addState(
            intArrayOf(android.R.attr.state_pressed),
            ColorDrawable(ROW_PRESSED),
        )
        addState(intArrayOf(), ColorDrawable(Color.TRANSPARENT))
    }

    private fun openNotificationAccessSettings() {
        val component = ComponentName(this, RapidoNotificationListener::class.java)
        openFirstAvailable(
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                component.flattenToString(),
            ),
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
        )
    }

    private fun openGlyphToySettings() {
        val packageLaunch = packageManager.getLaunchIntentForPackage(GLYPH_TOY_PACKAGE)
        openFirstAvailable(
            Intent().setComponent(GLYPH_TOY_MANAGER),
            packageLaunch,
            Intent(Settings.ACTION_SETTINGS),
        )
    }

    private fun openEssentialKeySettings() {
        val component = ComponentName(this, EssentialKeyAccessibilityService::class.java)
        openFirstAvailable(
            Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                component.flattenToString(),
            ),
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
        )
    }

    private fun openFirstAvailable(vararg intents: Intent?) {
        intents.filterNotNull().forEach { intent ->
            if (runCatching { startActivity(intent) }.isSuccess) return
        }
        Toast.makeText(this, "Could not open system settings", Toast.LENGTH_LONG).show()
    }

    private fun copyDebugDump() {
        val dump = DiagnosticLog.dump(this, store)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Rapido Glyph ETA debug dump", dump))
        DiagnosticLog.record(this, "Debug dump copied")
        Toast.makeText(this, "Debug dump copied — paste it into the chat", Toast.LENGTH_LONG)
            .show()
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

        val minutes = state.displayMinutes()
        etaValue.text = minutes?.let { "$it MIN" } ?: "—"
        val updated = state.updatedAtMillis.takeIf { it > 0 }?.let {
            DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))
        }
        etaMeta.text = when {
            minutes != null && updated != null -> "Rapido pickup  ·  Updated $updated"
            else -> "Waiting for a Rapido ride"
        }

        updateSetupState(
            notificationState,
            if (notificationAccess) "ON" else "OFF",
        )
        updateSetupState(
            essentialKeyState,
            if (accessibilityAccess) "ON" else "OFF",
        )

        rawText.text = state.rawNotification.ifBlank {
            "No Rapido notification captured yet."
        }
    }

    private fun updateSetupState(view: TextView, state: String) {
        val parts = (view.tag as String).split('|')
        view.text = setupRowText(parts[0], parts[1], parts[2], state)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private companion object {
        const val BLACK = 0xFF111111.toInt()
        const val MUTED = 0xFF696969.toInt()
        const val ON_DARK = 0xFFB7B7B7.toInt()
        const val SURFACE = 0xFFF2F2F2.toInt()
        const val STROKE = 0xFFD8D8D8.toInt()
        const val ROW_PRESSED = 0xFFE2E2E2.toInt()
        const val RED = 0xFFD71920.toInt()
        const val ACTION_ACCESSIBILITY_DETAILS_SETTINGS =
            "android.settings.ACCESSIBILITY_DETAILS_SETTINGS"

        const val GLYPH_TOY_PACKAGE = "com.nothing.thirdparty"

        val GLYPH_TOY_MANAGER = ComponentName(
            GLYPH_TOY_PACKAGE,
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
        )
    }
}
