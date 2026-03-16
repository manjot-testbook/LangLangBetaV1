package com.example.langlangbetav1.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Wraps Android's [SpeechRecognizer] in a coroutine-friendly class.
 *
 * Key behaviours:
 *  • Partial results are enabled → emits [SpeechResult.Partial] in real-time
 *    so the UI can highlight words as the user speaks.
 *  • ERROR_RECOGNIZER_BUSY is silently retried (up to [MAX_BUSY_RETRIES] times)
 *    so the first mic tap always works without video restarts.
 *
 * ⚠️  Must be created and used on the **main thread**.
 */
class SpeechRecognizerManager(private val context: Context) {

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var busyRetryCount = 0

    private val _result = MutableStateFlow<SpeechResult>(SpeechResult.Idle)
    val result: StateFlow<SpeechResult> = _result.asStateFlow()

    // ── Public API ────────────────────────────────────────────────────────

    fun startListening() {
        busyRetryCount = 0
        doStartListening()
    }

    fun stopListening() {
        recognizer?.stopListening()
    }

    fun reset() {
        _result.value = SpeechResult.Idle
    }

    fun release() {
        mainHandler.removeCallbacksAndMessages(null)
        destroyRecognizer()
    }

    // ── Internal ──────────────────────────────────────────────────────────

    private fun doStartListening() {
        destroyRecognizer()
        _result.value = SpeechResult.Listening
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(createListener())
            startListening(buildIntent())
        }
    }

    private fun buildIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH.toString())
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        // ── Enable partial results for real-time word highlighting ────────
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        // ── Keep the mic open long enough for full sentences ──────────────
        // MINIMUM_LENGTH forces the recogniser to record for at least 3.5 s
        // before it can close, regardless of silence.
        // COMPLETE_SILENCE / POSSIBLY_COMPLETE govern how long a pause at the
        // END of speech is allowed before the session closes.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,              3500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,     6000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 6000L)
    }

    private fun createListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        // ── Partial result — fired continuously while speaking ────────────
        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.normalise()
                ?.takeIf { it.isNotBlank() }
                ?: return
            _result.value = SpeechResult.Partial(text)
        }

        // ── Final result ──────────────────────────────────────────────────
        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.map { it.normalise() }
                ?.firstOrNull { it.isNotBlank() }
                ?: ""
            _result.value = SpeechResult.Success(text)
        }

        // ── Error handling ────────────────────────────────────────────────
        override fun onError(error: Int) {
            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                // The previous session hasn't fully torn down yet.
                // Silently destroy and retry — the user's mic tap still works,
                // the idle video keeps playing (no restart).
                if (busyRetryCount < MAX_BUSY_RETRIES) {
                    busyRetryCount++
                    destroyRecognizer()
                    mainHandler.postDelayed({
                        if (_result.value == SpeechResult.Listening) doStartListening()
                    }, BUSY_RETRY_DELAY_MS)
                    return  // ← do NOT emit error; stay in Listening state
                }
            }

            busyRetryCount = 0
            val msg = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH       -> "No speech detected — try again."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Timed out — please speak sooner."
                SpeechRecognizer.ERROR_AUDIO          -> "Audio error — check microphone."
                SpeechRecognizer.ERROR_NETWORK        -> "Network error."
                else                                  -> "Recognition error ($error)."
            }
            _result.value = SpeechResult.Error(msg)
        }
    }

    private fun destroyRecognizer() {
        recognizer?.destroy()
        recognizer = null
    }

    private fun String.normalise(): String =
        lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
            .replace(Regex("\\s+"), " ")

    companion object {
        private const val MAX_BUSY_RETRIES   = 3
        private const val BUSY_RETRY_DELAY_MS = 450L
    }
}
