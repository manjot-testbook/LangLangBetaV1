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

    private const val CONFIG_ASSET = "langlang_config.json"
    private const val TAG          = "SceneRepository"

    private val json = Json {
        ignoreUnknownKeys = true   // future-proof: extra JSON fields are silently skipped
        isLenient         = true   // tolerates minor formatting issues
    }

    private var _steps: List<SceneStep> = emptyList()
    val steps: List<SceneStep> get() = _steps

    /**
     * Call once (e.g. in [MainActivity.onCreate]) before navigation starts.
     * Reads and parses [langlang_config.json] from the assets folder.
     */
    fun load(context: Context) {
        try {
            val raw    = context.assets.open(CONFIG_ASSET).bufferedReader().use { it.readText() }
            val config = json.decodeFromString<LangLangConfig>(raw)
            _steps     = config.steps
            Log.i(TAG, "Loaded ${_steps.size} step(s) from $CONFIG_ASSET")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load $CONFIG_ASSET: ${e.message}", e)
            _steps = emptyList()
        }
    }

    fun getFirstStep(): SceneStep = steps.first()
    fun getStep(id: Int): SceneStep? = steps.find { it.id == id }

    fun resId(context: Context, resName: String): Int =
        context.resources.getIdentifier(resName, "raw", context.packageName)
}
