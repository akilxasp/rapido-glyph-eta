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
    private lateinit var etaStore: EtaStore
    private var sweepScheduled = false
    private var sweepMinutes = 1

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            when (key) {
                EtaStore.KEY_SWEEP_ENABLED -> syncSweepMode()
                EtaStore.KEY_FORCE_REFRESH -> {
                    renderCurrentFrame()
                    DiagnosticLog.record(this, "Forced Glyph refresh processed")
                }
                EtaStore.KEY_MINUTES, EtaStore.KEY_ETA_AT ->
                    if (!etaStore.readSweep().enabled) renderEta()
            }
        }

    private val sweepRunnable = object : Runnable {
        override fun run() {
            if (!etaStore.readSweep().enabled) {
                sweepScheduled = false
                return
            }
            etaStore.setSweepMinutes(sweepMinutes)
            render(sweepMinutes)
            sweepMinutes = EtaStore.nextSweepMinute(sweepMinutes)
            eventHandler.postDelayed(this, SWEEP_INTERVAL_MILLIS)
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
                    if (etaStore.readSweep().enabled) {
                        if (!sweepScheduled) startSweepLoop()
                    } else {
                        renderEta()
                    }
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
                matrixManager?.register(Glyph.DEVICE_25111p)
            }.onSuccess {
                DiagnosticLog.record(this@EtaGlyphToyService, "Glyph device registered: 25111p")
            }.onFailure {
                DiagnosticLog.record(this@EtaGlyphToyService, "Glyph register failed", it)
            }
            if (etaStore.readSweep().enabled) startSweepLoop() else renderEta()
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
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
        stopSweepLoop()
        runCatching { matrixManager?.turnOff() }
            .onFailure { DiagnosticLog.record(this, "Glyph turnOff failed", it) }
        runCatching { matrixManager?.unInit() }
            .onFailure { DiagnosticLog.record(this, "Glyph unInit failed", it) }
        matrixManager = null
        return false
    }

    override fun onDestroy() {
        DiagnosticLog.record(this, "Glyph Toy service destroyed")
        stopSweepLoop()
        etaStore.unregister(preferenceListener)
        runCatching { matrixManager?.unInit() }
            .onFailure { DiagnosticLog.record(this, "Glyph destroy unInit failed", it) }
        matrixManager = null
        super.onDestroy()
    }

    private fun syncSweepMode() {
        if (etaStore.readSweep().enabled) {
            startSweepLoop()
        } else {
            stopSweepLoop()
            renderEta()
            DiagnosticLog.record(this, "Number sweep stopped")
        }
    }

    private fun startSweepLoop() {
        stopSweepLoop()
        sweepMinutes = etaStore.readSweep().minutes
        sweepScheduled = true
        eventHandler.post(sweepRunnable)
        DiagnosticLog.record(this, "Number sweep started at ${sweepMinutes}m")
    }

    private fun stopSweepLoop() {
        eventHandler.removeCallbacks(sweepRunnable)
        sweepScheduled = false
    }

    private fun renderEta() {
        render(etaStore.read().displayMinutes())
    }

    private fun renderCurrentFrame() {
        val sweep = etaStore.readSweep()
        if (sweep.enabled) render(sweep.minutes) else renderEta()
    }

    private fun render(minutes: Int?) {
        val manager = matrixManager
        if (manager == null) {
            DiagnosticLog.record(this, "Render skipped: manager=null minutes=$minutes")
            return
        }
        runCatching {
            val frame = GlyphMatrixFrame.Builder()
                .addTop(MatrixRenderer.eta(minutes))
                .build(applicationContext)
            manager.setMatrixFrame(frame)
        }.onSuccess {
            DiagnosticLog.record(
                this,
                "Structured frame submitted: minutes=$minutes pixels=169",
            )
        }.onFailure {
            DiagnosticLog.record(this, "Frame submission failed: minutes=$minutes", it)
        }
    }

    private companion object {
        const val EVENT_DATA_KEY = "data"
        const val SWEEP_INTERVAL_MILLIS = 3_000L
    }
}
