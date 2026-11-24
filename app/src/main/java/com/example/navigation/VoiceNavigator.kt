package com.example.navigation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Lifecycle-safe Android TextToSpeech wrapper tuned for Arabic navigation guidance
 * and AI voice responses, falling back gracefully when an Arabic voice pack is absent.
 */
class VoiceNavigator(context: Context) {

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private var pendingUtterance: String? = null
    private var isShutdown = false

    private var tts: TextToSpeech? = null

    init {
        val appContext = context.applicationContext
        tts = TextToSpeech(appContext) { status ->
            if (isShutdown) return@TextToSpeech
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                if (engine != null) {
                    val arLocale = Locale.forLanguageTag("ar-SA")
                    val arResult = engine.setLanguage(arLocale)
                    if (arResult == TextToSpeech.LANG_MISSING_DATA ||
                        arResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        val genericAr = engine.setLanguage(Locale("ar"))
                        if (genericAr == TextToSpeech.LANG_MISSING_DATA ||
                            genericAr == TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            Log.w(TAG, "Arabic TTS unavailable on device; falling back to default locale.")
                            engine.language = Locale.getDefault()
                        }
                    }
                    engine.setSpeechRate(0.98f)
                    engine.setPitch(1.0f)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            _isSpeaking.value = false
                        }
                    })
                    _isReady.value = true
                    val queued = pendingUtterance
                    pendingUtterance = null
                    if (!queued.isNullOrBlank() && !_isMuted.value) {
                        speak(queued, flush = true)
                    }
                }
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status=$status")
                _isReady.value = false
            }
        }
    }

    fun speak(text: String, flush: Boolean = true) {
        val clean = text.trim()
        if (clean.isEmpty() || _isMuted.value || isShutdown) return

        val engine = tts
        if (engine == null || !_isReady.value) {
            pendingUtterance = clean
            return
        }

        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        val utteranceId = UUID.randomUUID().toString()
        engine.speak(clean, mode, null, utteranceId)
    }

    fun setMuted(muted: Boolean) {
        _isMuted.value = muted
        if (muted) {
            stop()
        }
    }

    fun stop() {
        pendingUtterance = null
        _isSpeaking.value = false
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        isShutdown = true
        pendingUtterance = null
        _isSpeaking.value = false
        _isReady.value = false
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        } finally {
            tts = null
        }
    }

    private companion object {
        const val TAG = "VoiceNavigator"
    }
}
