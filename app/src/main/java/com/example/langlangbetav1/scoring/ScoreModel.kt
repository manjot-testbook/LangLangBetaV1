package com.example.langlangbetav1.scoring

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────────────────────
// Per-gate record
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Recorded when the user successfully passes a [SpeechGate].
 *
 * Points per word:
 *   1st attempt → 1.0 (gold)
 *   2nd attempt → 0.75 (silver)
 *   3rd+ attempt → 0.5 (bronze) — floor, never lower
 */
data class PassedGateRecord(
    val stepId        : Int,
    val gateIndex     : Int,          // human-readable gate number (1, 2, 3…)
    val displayPrompt : String,
    val attemptNumber : Int,
    val wordCount     : Int,
    val pointsPerWord : Float,
) {
    val gatePoints : Float  get() = wordCount * pointsPerWord
    val maxPoints  : Float  get() = wordCount * 1.0f

    val attemptColor: Color get() = when {
        attemptNumber == 1 -> Color(0xFF69F0AE)   // green  — perfect
        attemptNumber == 2 -> Color(0xFFFFD54F)   // amber  — good
        else               -> Color(0xFFFF8A65)   // orange — ok
    }

    val attemptLabel: String get() = when {
        attemptNumber == 1 -> "× 1.0  🟢"
        attemptNumber == 2 -> "× 0.75 🟡"
        else               -> "× 0.5  🟠"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Final score
// ─────────────────────────────────────────────────────────────────────────────

data class FinalScore(
    val records      : List<PassedGateRecord>,
    val totalPoints  : Float,
    val totalMaxPts  : Float,
    val scoreOutOf10 : Float,
    val grade        : String,
    val gradeColor   : Color,
    val stars        : Int,            // 1-5
    val perfectGates : Int,            // gates passed on 1st try
)

fun buildFinalScore(records: List<PassedGateRecord>): FinalScore {
    val totalPoints = records.sumOf { it.gatePoints.toDouble() }.toFloat()
    val totalMaxPts = records.sumOf { it.maxPoints.toDouble() }.toFloat()
    val score       = if (totalMaxPts > 0f) (totalPoints / totalMaxPts) * 10f else 0f
    val clamped     = score.coerceIn(0f, 10f)

    val (grade, gradeColor, stars) = when {
        clamped >= 9.5f -> Triple("S",  Color(0xFFFFD700), 5)  // gold S-rank
        clamped >= 8.5f -> Triple("A+", Color(0xFF69F0AE), 5)
        clamped >= 7.5f -> Triple("A",  Color(0xFF69F0AE), 4)
        clamped >= 6.5f -> Triple("B+", Color(0xFFFFD54F), 4)
        clamped >= 5.5f -> Triple("B",  Color(0xFFFFD54F), 3)
        clamped >= 4.5f -> Triple("C",  Color(0xFFFF8A65), 2)
        else            -> Triple("D",  Color(0xFFEF5350), 1)
    }

    return FinalScore(
        records      = records,
        totalPoints  = totalPoints,
        totalMaxPts  = totalMaxPts,
        scoreOutOf10 = clamped,
        grade        = grade,
        gradeColor   = gradeColor,
        stars        = stars,
        perfectGates = records.count { it.attemptNumber == 1 },
    )
}

