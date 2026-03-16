package com.example.langlangbetav1.config

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Root wrapper that mirrors the top-level structure of langlang_config.json.
 * This is the only file you need to edit to add/remove steps, swap videos,
 * or change speech prompts.
 */
@Serializable
data class LangLangConfig(
    val steps: List<SceneStep>,
)

/**
 * The speech-check gate that sits at the end of a [SceneStep].
 *
 * @param displayPrompt          Sentence shown on screen for the user to read aloud.
 * @param expectedText           Pre-normalised (lowercase, no punctuation) target.
 * @param idleVideoResName       Optional video that LOOPS in the background while the
 *                               prompt is visible and the user is preparing to speak.
 * @param wrongResponseResNames  Videos played in sequence when the user answers
 *                               incorrectly. Wraps around if the user exceeds list length.
 */
@Serializable
data class SpeechGate(
    @SerialName("display_prompt") val displayPrompt: String,
    @SerialName("expected_text")  val expectedText: String,
    @SerialName("idle_video")     val idleVideoResName: String? = null,
    @SerialName("wrong_videos")   val wrongResponseResNames: List<String> = emptyList(),
)

/**
 * Gate that requests microphone permission at the end of a story-video sequence.
 *
 * @param idleVideoResName       Boomerang idle that loops while the system dialog
 *                               is showing (and again on the PermissionDenied screen).
 * @param grantedVideoResNames   Videos played immediately after the user grants permission.
 */
@Serializable
data class PermissionGate(
    @SerialName("idle_video")     val idleVideoResName: String,
    @SerialName("granted_videos") val grantedVideoResNames: List<String>,
)

/**
 * One logical step in the LangLang experience.
 *
 * @param id              Unique step identifier.
 * @param videoResNames   Ordered list of story/narration video resource names.
 * @param permissionGate  If non-null, mic permission is requested after [videoResNames] play.
 *                        Takes precedence over [speechGate].
 * @param speechGate      If non-null (and no [permissionGate]), the user must speak
 *                        the correct phrase to advance.
 * @param nextStepId      ID of the step that follows. null = end of experience.
 */
@Serializable
data class SceneStep(
    val id: Int,
    @SerialName("videos")          val videoResNames: List<String>,
    @SerialName("permission_gate") val permissionGate: PermissionGate? = null,
    @SerialName("speech_gate")     val speechGate: SpeechGate? = null,
    @SerialName("next_step_id")    val nextStepId: Int? = null,
)
