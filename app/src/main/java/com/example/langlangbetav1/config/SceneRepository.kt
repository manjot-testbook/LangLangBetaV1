package com.example.langlangbetav1.config

import android.content.Context
import android.util.Log
import kotlinx.serialization.json.Json

/**
 * Loads and caches the full LangLang experience from
 * [assets/langlang_config.json].
 *
 * ── How to customise ─────────────────────────────────────────────────────────
 *  Everything — videos, speech prompts, idle loops, wrong-response clips,
 *  and step order — lives in a single file:
 *
 *      app/src/main/assets/langlang_config.json
 *
 *  • Add a new step  → append an object to the "steps" array.
 *  • Swap a video    → change the string in "videos" / "idle_video" / etc.
 *  • Update a prompt → edit "display_prompt" and "expected_text".
 *  • Add/remove wrong-response clips → edit the "wrong_videos" array.
 *  • Reorder steps   → change "next_step_id" values.
 *
 *  Video names map 1-to-1 to files in res/raw (no extension needed).
 *  A boomerang reverse is auto-resolved: if "idle_video" is "scene_4_idle",
 *  place "scene_4_idle_rev.mp4" in res/raw and it activates automatically.
 * ─────────────────────────────────────────────────────────────────────────────
 */
object SceneRepository {

    private const val TAG = "SceneRepository"

    private val json = Json {
        ignoreUnknownKeys = true   // future-proof: extra JSON fields are silently skipped
        isLenient         = true   // tolerates minor formatting issues
    }

    private var _steps: List<SceneStep> = emptyList()
    val steps: List<SceneStep> get() = _steps

    /**
     * Load a module by ID.  Each module lives in assets as `<moduleId>.json`
     * (e.g. "module_0" → assets/module_0.json, "module_1" → assets/module_1.json).
     *
     * Called automatically by [SceneViewModel] when a module is started, so you
     * never need to call this manually — just navigate to "scene/module_0" (or any
     * other moduleId) and the right JSON is loaded on the fly.
     *
     * To add a new module:
     *  1. Drop `module_N.json` into app/src/main/assets/
     *  2. Navigate to "scene/module_N" from any button / home screen.
     */
    fun load(context: Context, moduleId: String) {
        val assetFile = "$moduleId.json"
        try {
            val raw    = context.assets.open(assetFile).bufferedReader().use { it.readText() }
            val config = json.decodeFromString<LangLangConfig>(raw)
            _steps     = config.steps
            Log.i(TAG, "Loaded ${_steps.size} step(s) from $assetFile")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $assetFile: ${e.message}", e)
            _steps = emptyList()
        }
    }

    fun getFirstStep(): SceneStep = steps.first()
    fun getStep(id: Int): SceneStep? = steps.find { it.id == id }

    fun resId(context: Context, resName: String): Int =
        context.resources.getIdentifier(resName, "raw", context.packageName)
}
