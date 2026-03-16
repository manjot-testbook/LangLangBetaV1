package com.example.langlangbetav1.scene

import com.example.langlangbetav1.config.SceneStep

/**
 * Complete state machine for the LangLang lesson engine.
 *
 *  ┌─────────────────────┐
 *  │ PlayingStoryVideo   │──(has permissionGate)──► AwaitingPermission
 *  │                     │──(has speechGate)──────► ShowingPrompt
 *  │                     │──(no gate)─────────────► next step / Finished
 *  └─────────────────────┘
 *
 *  AwaitingPermission ──granted──► PlayingGrantedVideos ──► next step
 *                     ──denied───► PermissionDenied
 *
 *  PermissionDenied ──tap button──► AwaitingPermission (retry)
 *
 *  ShowingPrompt ──mic──► Listening ──match──► Correct ──► next step
 *                                   ──miss───► PlayingWrongResponse ──► ShowingPrompt
 */
sealed class SceneUiState {

    object Loading : SceneUiState()

    /**
     * Playing one or more sequential story/narration videos via ExoPlayer's
     * built-in playlist.  The player owns the queue; the ViewModel just waits
     * for a single [PlayerEvent.Ended] that fires when ALL items finish.
     */
    data class PlayingStoryVideo(val step: SceneStep) : SceneUiState()

    // ── Permission gate states ────────────────────────────────────────────

    /**
     * Story videos have finished; the idle boomerang is running while the
     * system permission dialog is (or was just) shown.
     */
    data class AwaitingPermission(val step: SceneStep) : SceneUiState()

    /**
     * User explicitly denied mic access.  Idle boomerang keeps running.
     * The UI should show a highlighted CTA button to retry.
     */
    data class PermissionDenied(val step: SceneStep) : SceneUiState()

    /**
     * Permission was granted; the continuation videos (e.g. "let's go" +
     * outro) are playing before the main lesson starts.
     */
    data class PlayingGrantedVideos(val step: SceneStep) : SceneUiState()

    // ── Speech gate states ────────────────────────────────────────────────

    /**
     * All story videos have played.  The idle video (if any) is now LOOPING
     * in the background while this prompt card is visible.
     * [wrongAttemptCount] tracks how many times the user has tried (and failed)
     * so we can pick the right wrong-response video on the next miss.
     */
    data class ShowingPrompt(
        val step: SceneStep,
        val wrongAttemptCount: Int = 0,
    ) : SceneUiState()

    /**
     * STT is actively capturing audio.
     * The idle video continues to loop behind the prompt card.
     */
    data class Listening(
        val step: SceneStep,
        val wrongAttemptCount: Int = 0,
    ) : SceneUiState()

    /**
     * A wrong-response reaction video is playing.
     * When it ends the engine will return to [ShowingPrompt].
     */
    data class PlayingWrongResponse(
        val step: SceneStep,
        val nextWrongAttemptCount: Int,
    ) : SceneUiState()

    /** Correct match — shown briefly before advancing. */
    data class Correct(val step: SceneStep) : SceneUiState()

    /** All steps complete — show the end-of-lesson screen. */
    object Finished : SceneUiState()
}
