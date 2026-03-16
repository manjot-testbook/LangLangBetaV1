package com.example.langlangbetav1.config

/**
 * The speech-check gate that sits at the end of a [SceneStep].
 *
 * @param displayPrompt          Sentence shown on screen for the user to read aloud.
 * @param expectedText           Pre-normalised (lowercase, no punctuation) target.
 * @param idleVideoResName       Optional video that LOOPS in the background while the
 *                               prompt is visible and the user is preparing to speak.
 * @param wrongResponseResNames  Videos played in sequence when the user answers
 *                               incorrectly (e.g. the character reacts with a hint).
 *                               Wraps around if the user exceeds the list length.
 */
data class SpeechGate(
    val displayPrompt: String,
    val expectedText: String,
    val idleVideoResName: String? = null,
    val wrongResponseResNames: List<String> = emptyList(),
)

/**
 * Gate that requests microphone permission at the end of a story-video sequence.
 *
 * @param idleVideoResName       Boomerang idle that loops while the system dialog
 *                               is showing (and again if the user is on the
 *                               [PermissionDenied] screen waiting to retry).
 * @param grantedVideoResNames   Videos played immediately after the user grants
 *                               permission (e.g. "let's go" + outro sequence).
 */
data class PermissionGate(
    val idleVideoResName: String,
    val grantedVideoResNames: List<String>,
)

/**
 * One logical step in the LangLang experience.
 *
 * @param id              Unique step identifier.
 * @param videoResNames   Ordered list of story/narration video resource names.
 * @param permissionGate  If non-null, mic permission is requested after [videoResNames]
 *                        play.  Takes precedence over [speechGate].
 * @param speechGate      If non-null (and no [permissionGate]), the user must speak
 *                        the correct phrase to advance.
 * @param nextStepId      ID of the step that follows.  null = end of experience.
 */
data class SceneStep(
    val id: Int,
    val videoResNames: List<String>,
    val permissionGate: PermissionGate? = null,
    val speechGate: SpeechGate? = null,
    val nextStepId: Int? = null,
)
