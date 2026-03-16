package com.example.langlangbetav1.config

import android.content.Context

/**
 * Central source of truth for the full LangLang experience.
 *
 * Asset → resource name mapping
 * ─────────────────────────────────────────────────────────────────────────────
 *  0.1-INTRO                       → onboard_intro
 *  0.2.1-ONBOARD-Line1             → onboard_line1
 *  0.2.1-ONBOARD-Line2             → onboard_line2
 *  0.2.1-ONBOARD-Line3-ALLOW_MIC   → onboard_line3_mic
 *  0.2.1-ONBOARD-Line4-LETGO       → onboard_line4_letgo
 *  0.2.2-IDLE                      → onboard_idle  (+_rev boomerang)
 *  0.3-OUTRO                       → onboard_outro
 *  1–3                             → scene_1 … scene_3
 *  4.0.1-HostQuestion              → scene_4_host_q
 *  4.0.2-IDLE_UserResponse         → scene_4_idle   (+_rev)
 *  4.1–4.3-WrongUserResponse       → scene_4_wrong_1 … _3
 *  5                               → scene_5
 *  6-IDLE_UserResponse             → scene_6_idle   (+_rev)
 *  6.1–6.3-WrongUserResponse       → scene_6_wrong_1 … _3
 *  7–10                            → scene_7 … scene_10
 *  11-IDLE_UserResponse            → scene_11_idle  (+_rev)
 *  12                              → scene_12
 *
 * ── How to edit ──────────────────────────────────────────────────────────────
 * • Fill in the real [displayPrompt] / [expectedText] for each [SpeechGate]
 *   once you know the dialogue from the videos.
 * • Keep [expectedText] normalised: lowercase, no punctuation, single spaces.
 * • [idleVideoResName]_rev is resolved automatically — place the ffmpeg-reversed
 *   file in res/raw with the "_rev" suffix and the boomerang activates for free.
 */
object SceneRepository {

    val steps: List<SceneStep> = listOf(

        // ── Step 0 — Onboarding → mic permission gate ─────────────────────
        // Plays intro + lines 1-3, then requests RECORD_AUDIO permission.
        // While the system dialog shows, onboard_idle boomerangs in the bg.
        // On GRANT  → plays line4_letgo + outro, then advances to Step 1.
        // On DENY   → PermissionDenied overlay with "Allow Microphone" CTA.
        SceneStep(
            id            = 0,
            videoResNames = listOf(
                "onboard_intro",
                "onboard_line1",
                "onboard_line2",
                "onboard_line3_mic",
            ),
            permissionGate = PermissionGate(
                idleVideoResName     = "onboard_idle",
                grantedVideoResNames = listOf("onboard_line4_letgo", "onboard_outro"),
            ),
            speechGate = null,
            nextStepId = 1,
        ),

        // ── Step 1 — Scene block 1-3 + host question → first speech gate ─
        SceneStep(
            id            = 1,
            videoResNames = listOf("scene_1", "scene_2", "scene_3", "scene_4_host_q"),
            speechGate    = SpeechGate(
                displayPrompt         = "Update this prompt to match your scene dialogue.",
                expectedText          = "update this prompt to match your scene dialogue",
                idleVideoResName      = "scene_4_idle",
                wrongResponseResNames = listOf(
                    "scene_4_wrong_1",
                    "scene_4_wrong_2",
                    "scene_4_wrong_3",
                ),
            ),
            nextStepId = 2,
        ),

        // ── Step 2 — Scene 5 → second speech gate ────────────────────────
        SceneStep(
            id            = 2,
            videoResNames = listOf("scene_5"),
            speechGate    = SpeechGate(
                displayPrompt         = "Update this prompt to match your scene dialogue.",
                expectedText          = "update this prompt to match your scene dialogue",
                idleVideoResName      = "scene_6_idle",
                wrongResponseResNames = listOf(
                    "scene_6_wrong_1",
                    "scene_6_wrong_2",
                    "scene_6_wrong_3",
                ),
            ),
            nextStepId = 3,
        ),

        // ── Step 3 — Scenes 7-10 → third speech gate ─────────────────────
        SceneStep(
            id            = 3,
            videoResNames = listOf("scene_7", "scene_8", "scene_9", "scene_10"),
            speechGate    = SpeechGate(
                displayPrompt         = "Update this prompt to match your scene dialogue.",
                expectedText          = "update this prompt to match your scene dialogue",
                idleVideoResName      = "scene_11_idle",
                wrongResponseResNames = emptyList(),
            ),
            nextStepId = 4,
        ),

        // ── Step 4 — Finale (auto-advance to Finished) ───────────────────
        SceneStep(
            id            = 4,
            videoResNames = listOf("scene_12"),
            speechGate    = null,
            nextStepId    = null,
        ),
    )

    fun getFirstStep(): SceneStep = steps.first()
    fun getStep(id: Int): SceneStep? = steps.find { it.id == id }

    fun resId(context: Context, resName: String): Int =
        context.resources.getIdentifier(resName, "raw", context.packageName)
}
