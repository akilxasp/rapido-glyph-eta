package dev.akil.rapidoglyph

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphMatrixFrame
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

class EtaGlyphToyService : Service() {
    private var matrixManager: GlyphMatrixManager? = null
    private var glyphReady = false
    private lateinit var etaStore: EtaStore
    private val animationToken = Any()

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                EtaStore.KEY_FORCE_REFRESH -> playEssentialKeyAnimation()
                EtaStore.KEY_ETA_AT -> renderEta()
                EtaStore.KEY_PREVIEW_REQUEST -> playPendingPreview()
            }
        }

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what == GlyphToy.MSG_GLYPH_TOY) {
                val event = message.data?.getString(EVENT_DATA_KEY)
                DiagnosticLog.record(
                    this@EtaGlyphToyService,
                    "Glyph message received: event=${event ?: "(null)"}",
                )
                if (event == GlyphToy.EVENT_AOD) {
                    renderEta()
                }
            } else {
                super.handleMessage(message)
            }
        }
    }
    private val messenger = Messenger(eventHandler)

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            DiagnosticLog.record(
                this@EtaGlyphToyService,
                "Glyph SDK connected: component=${componentName?.flattenToShortString()}",
            )
            runCatching {
                checkNotNull(matrixManager) { "Glyph manager unavailable during registration" }
                    .register(Glyph.DEVICE_25111p)
            }.onSuccess {
                glyphReady = true
                etaStore.markGlyphConfirmed()
                DiagnosticLog.record(this@EtaGlyphToyService, "Glyph device registered: 25111p")
            }.onFailure {
                glyphReady = false
                DiagnosticLog.record(this@EtaGlyphToyService, "Glyph register failed", it)
            }
            if (!playPendingPreview()) renderEta()
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            glyphReady = false
            DiagnosticLog.record(
                this@EtaGlyphToyService,
                "Glyph SDK disconnected: component=${componentName?.flattenToShortString()}",
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
        etaStore = EtaStore(this)
        etaStore.register(preferenceListener)
        DiagnosticLog.record(this, "Glyph Toy service created")
    }

    override fun onBind(intent: Intent?): IBinder {
        DiagnosticLog.record(this, "Glyph Toy service bound: action=${intent?.action}")
        runCatching {
            matrixManager = GlyphMatrixManager.getInstance(applicationContext)
            checkNotNull(matrixManager) { "GlyphMatrixManager.getInstance returned null" }
                .init(callback)
        }.onFailure {
            DiagnosticLog.record(this, "Glyph SDK initialization failed", it)
        }
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        DiagnosticLog.record(this, "Glyph Toy service unbound: action=${intent?.action}")
        eventHandler.removeCallbacksAndMessages(animationToken)
        glyphReady = false
        runCatching { matrixManager?.turnOff() }
            .onFailure { DiagnosticLog.record(this, "Glyph turnOff failed", it) }
        runCatching { matrixManager?.unInit() }
            .onFailure { DiagnosticLog.record(this, "Glyph unInit failed", it) }
        matrixManager = null
        return false
    }

    override fun onDestroy() {
        DiagnosticLog.record(this, "Glyph Toy service destroyed")
        eventHandler.removeCallbacksAndMessages(animationToken)
        glyphReady = false
        etaStore.unregister(preferenceListener)
        runCatching { matrixManager?.unInit() }
            .onFailure { DiagnosticLog.record(this, "Glyph destroy unInit failed", it) }
        matrixManager = null
        super.onDestroy()
    }

    private fun renderEta() {
        render(etaStore.read().displayMinutes())
    }

    private fun playEssentialKeyAnimation() {
        eventHandler.removeCallbacksAndMessages(animationToken)
        val frames = MatrixRenderer.essentialKeyAnimation()
        DiagnosticLog.record(this, "Essential Key edge animation started: frames=${frames.size}")
        frames.forEachIndexed { index, frame ->
            eventHandler.postDelayed(
                { submitFrame(frame) },
                animationToken,
                index * ANIMATION_FRAME_MILLIS,
            )
        }
        eventHandler.postDelayed(
            {
                renderEta()
                DiagnosticLog.record(this, "Essential Key animation completed; ETA restored")
            },
            animationToken,
            frames.size * ANIMATION_FRAME_MILLIS,
        )
    }

    private fun playPendingPreview(): Boolean {
        if (!glyphReady) return false
        val preview = etaStore.takePendingPreview() ?: return false
        eventHandler.removeCallbacksAndMessages(animationToken)
        DiagnosticLog.record(
            this,
            "7-minute test displayed: token=${preview.token} " +
                "requestedMinutes=${preview.minutes}",
        )
        renderEta()
        return true
    }

    private fun render(minutes: Int?) {
        if (submitFrame(MatrixRenderer.eta(minutes))) {
            DiagnosticLog.record(this, "Structured frame submitted: minutes=$minutes pixels=169")
        }
    }

    private fun submitFrame(colors: IntArray): Boolean {
        val manager = matrixManager
        if (manager == null) {
            DiagnosticLog.record(this, "Render skipped: manager=null")
            return false
        }
        return runCatching {
            val frame = GlyphMatrixFrame.Builder()
                .addTop(colors)
                .build(applicationContext)
            manager.setMatrixFrame(frame)
        }.onFailure {
            DiagnosticLog.record(this, "Frame submission failed", it)
        }.isSuccess
    }

    private companion object {
        const val EVENT_DATA_KEY = "data"
        const val ANIMATION_FRAME_MILLIS = 70L
    }
}
