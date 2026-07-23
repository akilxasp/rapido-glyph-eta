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
import com.nothing.ketchum.GlyphMatrixManager
import com.nothing.ketchum.GlyphToy

class EtaGlyphToyService : Service() {
    private var matrixManager: GlyphMatrixManager? = null
    private lateinit var etaStore: EtaStore

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == EtaStore.KEY_MINUTES || key == EtaStore.KEY_ETA_AT) render()
        }

    private val eventHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what == GlyphToy.MSG_GLYPH_TOY) {
                val event = message.data?.getString(EVENT_DATA_KEY)
                if (event == GlyphToy.EVENT_AOD) render()
            } else {
                super.handleMessage(message)
            }
        }
    }
    private val messenger = Messenger(eventHandler)

    private val callback = object : GlyphMatrixManager.Callback {
        override fun onServiceConnected(componentName: ComponentName?) {
            matrixManager?.register(Glyph.DEVICE_25111p)
            render()
        }

        override fun onServiceDisconnected(componentName: ComponentName?) = Unit
    }

    override fun onCreate() {
        super.onCreate()
        etaStore = EtaStore(this)
        etaStore.register(preferenceListener)
    }

    override fun onBind(intent: Intent?): IBinder {
        matrixManager = GlyphMatrixManager.getInstance(applicationContext)
        matrixManager?.init(callback)
        return messenger.binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        matrixManager?.turnOff()
        matrixManager?.unInit()
        matrixManager = null
        return false
    }

    override fun onDestroy() {
        etaStore.unregister(preferenceListener)
        matrixManager?.unInit()
        matrixManager = null
        super.onDestroy()
    }

    private fun render() {
        matrixManager?.setMatrixFrame(MatrixRenderer.eta(etaStore.read().displayMinutes()))
    }

    private companion object {
        const val EVENT_DATA_KEY = "data"
    }
}
