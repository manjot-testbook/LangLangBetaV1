package com.example.langlangbetav1.audio

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Thin singleton around [ToneGenerator] that provides named, single-call
 * sound effects throughout the app.
 *
 * Call [init] from MainActivity.onCreate() and [release] from onDestroy().
 *
 * Sound map:
 *  playWordPop()         → short high beep when a word turns CORRECT live
 *  playMarkerScribble()  → buzzy reorder tone for the splash strikethrough
 *  playScoreWordPop()    → softer pop on each word appearing in score screen
 *  playGateScore()       → double beep when a gate summary appears
 *  playScoreCount()      → tick while the final counter animates
 *  playGradeReveal()     → ascending alert for the grade badge reveal
 *  playStar()            → short high blip for each star appearing
 */
object SoundPlayer {

    private var toneGen: ToneGenerator? = null

    fun init() {
        runCatching {
            toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 65)
        }
    }

    fun playWordPop()        = play(ToneGenerator.TONE_CDMA_HIGH_SS,       90)
    fun playMarkerScribble() = play(ToneGenerator.TONE_CDMA_ABBR_REORDER, 280)
    fun playScoreWordPop()   = play(ToneGenerator.TONE_PROP_BEEP,          80)
    fun playGateScore()      = play(ToneGenerator.TONE_PROP_BEEP2,        180)
    fun playScoreCount()     = play(ToneGenerator.TONE_CDMA_HIGH_SS,       60)
    fun playGradeReveal()    = play(ToneGenerator.TONE_CDMA_ABBR_ALERT,   700)
    fun playStar()           = play(ToneGenerator.TONE_CDMA_HIGH_SS_2,    110)

    private fun play(tone: Int, durationMs: Int) {
        runCatching { toneGen?.startTone(tone, durationMs) }
    }

    fun release() {
        runCatching { toneGen?.release() }
        toneGen = null
    }
}

