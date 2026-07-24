package dev.akil.rapidoglyph

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.math.max

class MainActivity : Activity() {
    private lateinit var etaEyebrow: TextView
    private lateinit var etaValue: TextView
    private lateinit var etaMeta: TextView
    private lateinit var notificationRow: SetupRow
    private lateinit var glyphRow: SetupRow
    private lateinit var essentialKeyRow: SetupRow
    private lateinit var setupSummary: Button
    private lateinit var setupRows: LinearLayout
    private lateinit var rawText: TextView
    private lateinit var developerPanel: LinearLayout
    private lateinit var developerToggle: Button
    private lateinit var previewButton: Button
    private lateinit var store: EtaStore

    private val mainHandler = Handler(Looper.getMainLooper())
    private var setupExpanded = true
    private var previousSetupComplete: Boolean? = null

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            mainHandler.post { refresh() }
        }

    private val foregroundTicker = object : Runnable {
        override fun run() {
            refresh()
            mainHandler.postDelayed(this, FOREGROUND_REFRESH_MILLIS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = EtaStore(this)
        setContentView(buildContent())
    }

    override fun onStart() {
        super.onStart()
        store.register(preferenceListener)
        mainHandler.post(foregroundTicker)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onStop() {
        mainHandler.removeCallbacks(foregroundTicker)
        store.unregister(preferenceListener)
        super.onStop()
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

        content.addView(label(getString(R.string.brand_label), RED))
        content.addView(text(getString(R.string.title), 36f, Color.BLACK, Typeface.BOLD).apply {
            typeface = resources.getFont(R.font.doto)
            fontVariationSettings = "'ROND' 100, 'wght' 700"
            letterSpacing = 0.02f
            setPadding(0, dp(5), 0, 0)
            isAccessibilityHeading = true
        })
        content.addView(text(getString(R.string.tagline), 16f, MUTED).apply {
            setPadding(0, dp(8), 0, dp(24))
        })

        content.addView(etaCard())
        content.addView(sectionTitle(getString(R.string.setup_heading)))
        content.addView(setupCard())

        developerToggle = sectionToggle(getString(R.string.developer_tools_collapsed)).apply {
            setOnClickListener {
                val opening = developerPanel.visibility != View.VISIBLE
                developerPanel.visibility = if (opening) View.VISIBLE else View.GONE
                text = getString(
                    if (opening) {
                        R.string.developer_tools_expanded
                    } else {
                        R.string.developer_tools_collapsed
                    },
                )
                stateDescription = getString(
                    if (opening) R.string.expanded else R.string.collapsed,
                )
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

        etaEyebrow = label(getString(R.string.ready_heading), Color.WHITE).apply {
            isAccessibilityHeading = true
        }
        addView(etaEyebrow)
        etaValue = text("—", 52f, Color.WHITE, Typeface.BOLD).apply {
            typeface = Typeface.MONOSPACE
            letterSpacing = -0.04f
            setPadding(0, dp(8), 0, dp(4))
        }
        addView(etaValue)
        etaMeta = text(getString(R.string.waiting_for_rapido), 14f, ON_DARK)
        addView(etaMeta)
    }.apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(28) }
    }

    private fun setupCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        background = rounded(SURFACE, 24)
        clipToOutline = true

        setupSummary = Button(this@MainActivity).apply {
            text = getString(R.string.setup_complete_review)
            textSize = 13f
            setTextColor(Color.BLACK)
            typeface = Typeface.MONOSPACE
            letterSpacing = 0.05f
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(8), dp(18), dp(8))
            minHeight = dp(58)
            background = setupRowBackground()
            stateListAnimator = null
            visibility = View.GONE
            setOnClickListener {
                setupExpanded = !setupExpanded
                updateSetupVisibility(setupComplete = true)
            }
        }
        addView(setupSummary)

        setupRows = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.VERTICAL
        }
        notificationRow = setupRow(
            number = "01",
            title = getString(R.string.notification_access_title),
            description = getString(R.string.notification_access_description),
            onClick = ::showNotificationAccessExplanation,
        )
        setupRows.addView(notificationRow.container)
        setupRows.addView(divider())

        glyphRow = setupRow(
            number = "02",
            title = getString(R.string.glyph_toy_title),
            description = getString(R.string.glyph_toy_description),
            onClick = ::showGlyphToyInstructions,
        )
        setupRows.addView(glyphRow.container)
        setupRows.addView(divider())

        essentialKeyRow = setupRow(
            number = "03",
            title = getString(R.string.essential_key_title),
            description = getString(R.string.essential_key_description),
            onClick = ::showEssentialKeyExplanation,
        )
        setupRows.addView(essentialKeyRow.container)
        addView(setupRows)
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
    ): SetupRow {
        val stateView = label(getString(R.string.status_not_set), MUTED).apply {
            gravity = Gravity.END
        }
        val titleView = text(title, 15f, Color.BLACK).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(titleView)
                addView(stateView)
                addView(text("›", 20f, Color.BLACK).apply {
                    setPadding(dp(10), 0, 0, 0)
                })
            })
            addView(text(description, 14f, MUTED).apply {
                setPadding(0, dp(3), 0, 0)
            })
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(18), dp(17), dp(18), dp(17))
            minimumHeight = dp(76)
            isClickable = true
            isFocusable = true
            background = setupRowBackground()
            addView(label(number, MUTED).apply {
                gravity = Gravity.START
                layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(body, LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f,
            ))
            setOnClickListener { onClick() }
            accessibilityDelegate = object : View.AccessibilityDelegate() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfo,
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = Button::class.java.name
                }
            }
        }
        return SetupRow(container, stateView, title, description)
    }

    private fun developerPanel() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        visibility = View.GONE
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(SURFACE, 20)

        previewButton = secondaryButton(getString(R.string.preview_seven_minutes)) {
            store.requestPreview(7)
            DiagnosticLog.record(this@MainActivity, "7-minute Glyph preview requested")
            Toast.makeText(
                this@MainActivity,
                getString(R.string.preview_requested),
                Toast.LENGTH_LONG,
            ).show()
        }
        addView(previewButton)
        addView(secondaryButton(getString(R.string.copy_redacted_dump)) {
            copyDebugDump(includeRawPayload = false)
        })
        addView(secondaryButton(getString(R.string.copy_raw_dump)) {
            confirmRawDebugDump()
        })
        addView(secondaryButton(getString(R.string.refresh_diagnostics)) { refresh() })
        addView(label(getString(R.string.latest_payload_heading), MUTED).apply {
            setPadding(0, dp(22), 0, dp(8))
            isAccessibilityHeading = true
        })
        rawText = text("", 13f, MUTED).apply {
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        addView(rawText)
    }

    private fun sectionTitle(value: String) = label(value, Color.BLACK).apply {
        setPadding(dp(4), 0, 0, dp(12))
        isAccessibilityHeading = true
    }

    private fun sectionToggle(value: String) = Button(this).apply {
        text = value
        textSize = 13f
        letterSpacing = 0.08f
        setTextColor(Color.BLACK)
        typeface = Typeface.MONOSPACE
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        setPadding(dp(4), dp(20), dp(4), dp(12))
        minHeight = dp(58)
        background = ColorDrawable(Color.TRANSPARENT)
        stateListAnimator = null
        stateDescription = getString(R.string.collapsed)
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

    private fun showNotificationAccessExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.notification_dialog_title)
            .setMessage(R.string.notification_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                openNotificationAccessSettings()
            }
            .show()
    }

    private fun showGlyphToyInstructions() {
        AlertDialog.Builder(this)
            .setTitle(R.string.glyph_dialog_title)
            .setMessage(R.string.glyph_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.open_glyph_toys) { _, _ ->
                openGlyphToySettings()
            }
            .show()
    }

    private fun showEssentialKeyExplanation() {
        AlertDialog.Builder(this)
            .setTitle(R.string.essential_key_dialog_title)
            .setMessage(R.string.essential_key_dialog_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.continue_label) { _, _ ->
                openEssentialKeySettings()
            }
            .show()
    }

    private fun openNotificationAccessSettings() {
        val component = ComponentName(this, RapidoNotificationListener::class.java)
        if (!openFirstAvailable(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).putExtra(
                    Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                    component.flattenToString(),
                ),
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
            )
        ) {
            showSettingsError()
        }
    }

    private fun openGlyphToySettings() {
        val opened = openFirstAvailable(
            Intent().setComponent(GLYPH_TOY_MANAGER),
            packageManager.getLaunchIntentForPackage(GLYPH_TOY_PACKAGE),
        )
        if (!opened) {
            AlertDialog.Builder(this)
                .setTitle(R.string.glyph_settings_unavailable_title)
                .setMessage(R.string.glyph_settings_unavailable_message)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun openEssentialKeySettings() {
        val component = ComponentName(this, EssentialKeyAccessibilityService::class.java)
        if (!openFirstAvailable(
                Intent(ACTION_ACCESSIBILITY_DETAILS_SETTINGS).putExtra(
                    Intent.EXTRA_COMPONENT_NAME,
                    component.flattenToString(),
                ),
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),
            )
        ) {
            showSettingsError()
        }
    }

    private fun openFirstAvailable(vararg intents: Intent?): Boolean {
        intents.filterNotNull().forEach { intent ->
            if (runCatching { startActivity(intent) }.isSuccess) return true
        }
        return false
    }

    private fun showSettingsError() {
        Toast.makeText(this, R.string.settings_open_error, Toast.LENGTH_LONG).show()
    }

    private fun confirmRawDebugDump() {
        AlertDialog.Builder(this)
            .setTitle(R.string.raw_dump_warning_title)
            .setMessage(R.string.raw_dump_warning_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.copy_raw_dump) { _, _ ->
                copyDebugDump(includeRawPayload = true)
            }
            .show()
    }

    private fun copyDebugDump(includeRawPayload: Boolean) {
        val dump = DiagnosticLog.dump(this, store, includeRawPayload)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.debug_dump_label), dump))
        DiagnosticLog.record(
            this,
            if (includeRawPayload) "Raw debug dump copied" else "Redacted debug dump copied",
        )
        Toast.makeText(
            this,
            getString(
                if (includeRawPayload) R.string.raw_dump_copied else R.string.redacted_dump_copied,
            ),
            Toast.LENGTH_LONG,
        ).show()
    }

    private fun refresh() {
        val nowMillis = System.currentTimeMillis()
        val state = store.read()
        val notificationAccess = isNotificationAccessEnabled()
        val accessibilityAccess = isEssentialKeyAccessEnabled()
        val glyphConfirmed = state.glyphConfirmedAtMillis > 0L
        val setupComplete = notificationAccess && glyphConfirmed
        val minutes = state.displayMinutes(nowMillis)

        when {
            minutes != null -> {
                etaEyebrow.text = getString(R.string.live_pickup_heading)
                etaValue.text = getString(R.string.minutes_value, minutes)
                etaMeta.text = etaFreshness(state.etaUpdatedAtMillis, nowMillis)
            }
            !notificationAccess -> {
                etaEyebrow.text = getString(R.string.setup_required_heading)
                etaValue.text = "—"
                etaMeta.text = getString(R.string.enable_notification_next)
            }
            !glyphConfirmed -> {
                etaEyebrow.text = getString(R.string.setup_required_heading)
                etaValue.text = "—"
                etaMeta.text = getString(R.string.select_glyph_next)
            }
            else -> {
                etaEyebrow.text = getString(R.string.ready_heading)
                etaValue.text = "—"
                etaMeta.text = getString(R.string.waiting_for_rapido)
            }
        }

        updateSetupRow(
            notificationRow,
            if (notificationAccess) {
                getString(R.string.status_enabled)
            } else {
                getString(R.string.status_required)
            },
        )
        updateSetupRow(
            glyphRow,
            if (glyphConfirmed) {
                getString(R.string.status_confirmed)
            } else {
                getString(R.string.status_required)
            },
        )
        updateSetupRow(
            essentialKeyRow,
            if (accessibilityAccess) {
                getString(R.string.status_enabled)
            } else {
                getString(R.string.status_optional)
            },
        )

        val previous = previousSetupComplete
        if (previous == null || (!previous && setupComplete)) {
            setupExpanded = !setupComplete
        }
        previousSetupComplete = setupComplete
        updateSetupVisibility(setupComplete)

        previewButton.isEnabled = minutes == null
        previewButton.alpha = if (minutes == null) 1f else 0.45f
        previewButton.contentDescription = if (minutes == null) {
            getString(R.string.preview_seven_minutes)
        } else {
            getString(R.string.preview_disabled_during_ride)
        }

        rawText.text = state.rawNotification.ifBlank {
            getString(R.string.no_payload)
        }
    }

    private fun updateSetupVisibility(setupComplete: Boolean) {
        setupSummary.visibility = if (setupComplete) View.VISIBLE else View.GONE
        setupRows.visibility = if (!setupComplete || setupExpanded) View.VISIBLE else View.GONE
        if (setupComplete) {
            setupSummary.text = getString(
                if (setupExpanded) R.string.setup_complete_hide else R.string.setup_complete_review,
            )
            setupSummary.stateDescription = getString(
                if (setupExpanded) R.string.expanded else R.string.collapsed,
            )
        }
    }

    private fun updateSetupRow(row: SetupRow, state: String) {
        row.state.text = state
        row.container.stateDescription = state
        row.container.contentDescription = getString(
            R.string.setup_row_accessibility,
            row.title,
            state,
            row.description,
        )
    }

    private fun etaFreshness(etaUpdatedAtMillis: Long, nowMillis: Long): String {
        if (etaUpdatedAtMillis <= 0L) return getString(R.string.rapido_pickup)
        val ageMinutes = max(0L, (nowMillis - etaUpdatedAtMillis) / MILLIS_PER_MINUTE)
        return when {
            ageMinutes == 0L -> getString(R.string.updated_just_now)
            ageMinutes >= STALE_UPDATE_MINUTES ->
                getString(R.string.last_rapido_update_minutes, ageMinutes)
            else -> getString(R.string.updated_minutes_ago, ageMinutes)
        }
    }

    private fun isNotificationAccessEnabled(): Boolean =
        Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        ).orEmpty().split(':').any {
            ComponentName.unflattenFromString(it)?.packageName == packageName
        }

    private fun isEssentialKeyAccessEnabled(): Boolean =
        Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty().split(':').any {
            ComponentName.unflattenFromString(it)?.className ==
                EssentialKeyAccessibilityService::class.java.name
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private data class SetupRow(
        val container: LinearLayout,
        val state: TextView,
        val title: String,
        val description: String,
    )

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
        const val FOREGROUND_REFRESH_MILLIS = 30_000L
        const val MILLIS_PER_MINUTE = 60_000L
        const val STALE_UPDATE_MINUTES = 5L

        const val GLYPH_TOY_PACKAGE = "com.nothing.thirdparty"

        val GLYPH_TOY_MANAGER = ComponentName(
            GLYPH_TOY_PACKAGE,
            "com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity",
        )
    }
}
