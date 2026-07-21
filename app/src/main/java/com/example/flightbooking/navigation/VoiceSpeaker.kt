package com.example.flightbooking.navigation

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

/**
 * VoiceSpeaker
 *
 * Wraps Android [TextToSpeech] for airport voice navigation.
 *
 * Features:
 * - Lazy TTS initialisation with a ready-guard so calls before init are safe.
 * - De-duplication: the same message is never spoken twice in a row unless [force] is set.
 * - Mute toggle: [isMuted] suppresses playback without releasing the engine.
 * - [speechRate] and [pitch] for user accessibility preferences (1.0 = normal).
 * - [shutdown] must be called when navigation ends (e.g. in DisposableEffect).
 */
class VoiceSpeaker(context: Context) {

    private val tag = "VoiceSpeaker"

    /** When true, [speak] will queue utterances; when false, TTS is not yet ready. */
    private var isReady = false

    /** When true, all [speak] calls are silently ignored. */
    var isMuted: Boolean = false

    /**
     * Speech rate multiplier. 1.0 is normal speed, 0.75 is slower (better for non-native
     * speakers), 1.25 is faster. Applied immediately on the next [speak] call.
     */
    var speechRate: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            if (isReady) tts.setSpeechRate(field)
        }

    /**
     * Pitch multiplier. 1.0 is normal, lower values sound deeper.
     */
    var pitch: Float = 1.0f
        set(value) {
            field = value.coerceIn(0.5f, 2.0f)
            if (isReady) tts.setPitch(field)
        }

    /** Tracks the last spoken message to avoid back-to-back repetition. */
    private var lastSpokenMessage: String = ""

    // Bug 6 fix: language must be set inside the OnInitListener callback.
    // The TTS engine is async — it is not ready until the callback fires,
    // so setting language in init{} has no effect.
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language    = Locale.US
                tts.setSpeechRate(speechRate)
                tts.setPitch(pitch)
                isReady = true
                Log.d(tag, "TextToSpeech initialised successfully")
            } else {
                Log.e(tag, "TextToSpeech initialisation failed with status: $status")
            }
        }
    }

    /**
     * Speak [message] aloud.
     *
     * @param message      The text to synthesise.
     * @param flushQueue   If true, any currently playing utterance is interrupted immediately.
     *                     Defaults to false (queues behind current speech).
     * @param force        If true, bypass de-duplication and always speak even if it matches
     *                     the last message. Use for explicit user-triggered re-reads.
     */
    fun speak(message: String, flushQueue: Boolean = false, force: Boolean = false) {
        if (!isReady) {
            Log.d(tag, "TTS not ready yet, skipping: $message")
            return
        }
        if (isMuted) {
            Log.d(tag, "Muted, skipping: $message")
            return
        }
        if (message.isBlank()) return

        // De-duplicate: skip if this is the exact same message as last time
        if (message == lastSpokenMessage && !flushQueue && !force) {
            Log.d(tag, "Duplicate message suppressed: $message")
            return
        }

        lastSpokenMessage = message
        val queueMode = if (flushQueue) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts.speak(message, queueMode, null, message.hashCode().toString())
        Log.d(tag, "Speaking: $message")
    }

    /**
     * Clear the last-spoken cache so the next identical message will play again.
     * Useful when a route resets.
     */
    fun resetLastMessage() {
        lastSpokenMessage = ""
    }

    /**
     * Stop current speech and silence the queue without destroying the engine.
     */
    fun stopSpeaking() {
        if (isReady) tts.stop()
    }

    /**
     * Returns true if the TTS engine is currently speaking.
     */
    val isSpeaking: Boolean get() = isReady && tts.isSpeaking

    /**
     * Release TTS resources. Call this when the composable that owns this speaker is disposed.
     */
    fun shutdown() {
        if (isReady) {
            tts.stop()
            tts.shutdown()
            isReady = false
            Log.d(tag, "TextToSpeech shut down")
        }
    }
}
