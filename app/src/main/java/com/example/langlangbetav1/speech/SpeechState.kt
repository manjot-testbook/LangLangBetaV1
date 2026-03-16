package com.example.langlangbetav1.speech

/** Represents every possible state of the speech recogniser pipeline. */
sealed class SpeechResult {
    /** Nothing is happening — initial / reset state. */
    object Idle : SpeechResult()

    /** The recogniser is actively listening for audio. */
    object Listening : SpeechResult()

    /**
     * An intermediate partial transcription arrived while the user is still
     * speaking.  Used to highlight words in real-time.
     * [text] is already normalised (lowercase, punctuation stripped).
     */
    data class Partial(val text: String) : SpeechResult()

    /**
     * The recogniser returned a final result.
     * [text] is already normalised (lowercase, punctuation stripped).
     */
    data class Success(val text: String) : SpeechResult()

    /** The recogniser encountered an unrecoverable error. */
    data class Error(val message: String) : SpeechResult()
}

