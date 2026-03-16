package com.example.langlangbetav1.speech

// ─────────────────────────────────────────────────────────────────────────────
// Data types
// ─────────────────────────────────────────────────────────────────────────────

/** Per-word match state produced by [alignWords]. */
enum class WordMatchState {
    /** Not yet evaluated — initial / reset state. */
    PENDING,
    /** Word found in the LCS alignment — user said it correctly. */
    CORRECT,
    /** Word missing from the LCS alignment — user skipped or mispronounced it. */
    INCORRECT,
}

/** A display word paired with its current match state. */
data class WordResult(
    val word : String,
    val state: WordMatchState,
)

// ─────────────────────────────────────────────────────────────────────────────
// LCS + fuzzy alignment
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Aligns [displayPrompt] words against the STT output [spoken] using LCS with
 * a **fuzzy per-word comparison**: two words are considered equal if their
 * normalised Levenshtein distance is ≤ [FUZZY_MAX_DISTANCE].
 *
 * This handles common STT mis-transcriptions ("i'm" → "im", "doing" → "doin",
 * "you're" → "your") without relaxing the overall phrase threshold.
 *
 * For partial results pass [partialMode] = true — unmatched words are kept
 * PENDING instead of INCORRECT because the user may still say them.
 *
 * ```
 * Expected : "Hello how are you"
 * Spoken   : "Hello how is when you"
 * Result   :  ✓      ✓    ✗    ✓        (LCS skips "is when")
 * ```
 */
fun alignWords(
    displayPrompt: String,
    spoken       : String,
    partialMode  : Boolean = false,
): List<WordResult> {
    val displayWords = displayPrompt.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val expNorm      = displayWords.map { it.normalise() }
    val spkNorm      = spoken.trim().split(Regex("\\s+"))
        .map { it.normalise() }
        .filter { it.isNotBlank() }

    val n = expNorm.size
    val m = spkNorm.size

    if (n == 0) return emptyList()
    if (m == 0) return displayWords.map {
        WordResult(it, if (partialMode) WordMatchState.PENDING else WordMatchState.INCORRECT)
    }

    // ── LCS with fuzzy equality ───────────────────────────────────────────
    fun fuzzyEq(a: String, b: String): Boolean =
        a == b || levenshtein(a, b) <= FUZZY_MAX_DISTANCE

    val dp = Array(n + 1) { IntArray(m + 1) }
    for (i in 1..n) {
        for (j in 1..m) {
            dp[i][j] = if (fuzzyEq(expNorm[i - 1], spkNorm[j - 1])) {
                dp[i - 1][j - 1] + 1
            } else {
                maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    // ── Backtrack ─────────────────────────────────────────────────────────
    val matched = BooleanArray(n)
    var i = n; var j = m
    while (i > 0 && j > 0) {
        when {
            fuzzyEq(expNorm[i - 1], spkNorm[j - 1]) -> { matched[i - 1] = true; i--; j-- }
            dp[i - 1][j] >= dp[i][j - 1]            -> i--
            else                                      -> j--
        }
    }

    return displayWords.mapIndexed { idx, word ->
        val state = when {
            matched[idx]  -> WordMatchState.CORRECT
            partialMode   -> WordMatchState.PENDING   // still might be spoken
            else          -> WordMatchState.INCORRECT
        }
        WordResult(word, state)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Acceptance threshold
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Returns true if the fuzzy-LCS match ratio ≥ [ACCEPT_THRESHOLD].
 * A lower threshold (0.70) accommodates non-native pronunciation while
 * still requiring the majority of the phrase to be correct.
 */
fun isAcceptableMatch(expectedText: String, spoken: String): Boolean {
    val exp = expectedText.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val spk = spoken.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (exp.isEmpty()) return false
    if (spk.isEmpty()) return false

    fun fuzzyEq(a: String, b: String) = a == b || levenshtein(a, b) <= FUZZY_MAX_DISTANCE

    val n = exp.size; val m = spk.size
    val dp = Array(n + 1) { IntArray(m + 1) }
    for (r in 1..n) for (c in 1..m) {
        dp[r][c] = if (fuzzyEq(exp[r - 1], spk[c - 1])) dp[r-1][c-1] + 1
                   else maxOf(dp[r-1][c], dp[r][c-1])
    }
    return dp[n][m].toFloat() / n >= ACCEPT_THRESHOLD
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private const val FUZZY_MAX_DISTANCE = 2    // edit-distance tolerance per word
private const val ACCEPT_THRESHOLD   = 0.70f // 70 % of words must match

private fun String.normalise() =
    lowercase().replace(Regex("[^a-z0-9]"), "")

/** Standard iterative Levenshtein — O(a·b) time, O(b) space. */
private fun levenshtein(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    var prev = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        val curr = IntArray(b.length + 1)
        curr[0] = i
        for (j in 1..b.length) {
            curr[j] = if (a[i - 1] == b[j - 1]) prev[j - 1]
                      else 1 + minOf(prev[j], curr[j - 1], prev[j - 1])
        }
        prev = curr
    }
    return prev[b.length]
}
