package com.example.langlangbetav1.score

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.langlangbetav1.audio.SoundPlayer
import com.example.langlangbetav1.scoring.FinalScore
import com.example.langlangbetav1.scoring.PassedGateRecord
import com.example.langlangbetav1.scoring.ScoreRepository
import com.example.langlangbetav1.ui.theme.DarkBackground
import com.example.langlangbetav1.ui.theme.GreenCorrect
import com.example.langlangbetav1.ui.theme.PurplePrimary
import kotlinx.coroutines.delay
import kotlin.math.sin
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// Phase index helper — pre-computes which animationPhase threshold each
// element should become visible at, based on the gate records.
// ─────────────────────────────────────────────────────────────────────────────

private data class ScorePhases(
    val titlePhase       : Int,
    val gateHeaderPhases : List<Int>,
    val gateWordPhases   : List<List<Int>>,
    val gateScorePhases  : List<Int>,
    val totalPhase       : Int,
    val gradePhase       : Int,
    val starPhases       : List<Int>,
    val continuePhase    : Int,
    val maxPhase         : Int,
)

private fun buildPhases(records: List<PassedGateRecord>): ScorePhases {
    var p = 0
    val titlePhase    = ++p
    val headers       = records.map { ++p }
    val words         = records.map { r -> (0 until r.wordCount).map { ++p } }
    val gateScores    = records.map { ++p }
    val totalPhase    = ++p
    val gradePhase    = ++p
    val starPhases    = (1..5).map { ++p }
    val continuePhase = ++p
    return ScorePhases(titlePhase, headers, words, gateScores, totalPhase, gradePhase, starPhases, continuePhase, p)
}

// ─────────────────────────────────────────────────────────────────────────────
// Entry composable
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ScoreScreen(navController: NavController) {
    val score by ScoreRepository.score.collectAsStateWithLifecycle()

    if (score == null) {
        Box(Modifier.fillMaxSize().background(DarkBackground))
        return
    }

    ScoreContent(score = score!!, navController = navController)
}

@Composable
private fun ScoreContent(score: FinalScore, navController: NavController) {
    val phases         = remember(score) { buildPhases(score.records) }
    var animPhase      by remember { mutableIntStateOf(0) }
    var showCounter    by remember { mutableStateOf(false) }
    var showConfetti   by remember { mutableStateOf(false) }

    val animatedScore by animateFloatAsState(
        targetValue   = if (showCounter) score.scoreOutOf10 else 0f,
        animationSpec = tween(2200, easing = FastOutSlowInEasing),
        label         = "score",
    )

    // ── Drive the reveal sequence ────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(400)
        animPhase = phases.titlePhase

        score.records.forEachIndexed { gi, record ->
            delay(380)
            animPhase = phases.gateHeaderPhases[gi]
            phases.gateWordPhases[gi].forEach { wordPhase ->
                delay(115)
                animPhase = wordPhase
                SoundPlayer.playScoreWordPop()
            }
            delay(240)
            animPhase = phases.gateScorePhases[gi]
            SoundPlayer.playGateScore()
        }

        delay(500)
        animPhase   = phases.totalPhase
        showCounter = true
        delay(2400)

        SoundPlayer.playGradeReveal()
        animPhase = phases.gradePhase

        phases.starPhases.forEachIndexed { i, starPhase ->
            delay(190)
            animPhase = starPhase
            SoundPlayer.playStar()
        }

        if (score.stars >= 4) {
            showConfetti = true
        }

        delay(400)
        animPhase = phases.continuePhase
    }

    // ── Confetti particles ───────────────────────────────────────────────
    val confettiParticles = remember { buildConfetti(80) }
    var confettiTime by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(showConfetti) {
        if (!showConfetti) return@LaunchedEffect
        val t0 = System.currentTimeMillis()
        while (System.currentTimeMillis() - t0 < 5000L) {
            confettiTime = (System.currentTimeMillis() - t0) / 1000f
            delay(16L)
        }
        showConfetti = false
    }

    // ── Background ───────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D001F), Color(0xFF0A0A0A))
                )
            )
    ) {
        // Confetti canvas
        if (showConfetti) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                confettiParticles.forEach { p ->
                    val x  = p.startX * size.width  + p.vx * confettiTime * size.width
                    val y  = p.startY * size.height + p.vy * confettiTime * size.height + 300f * confettiTime * confettiTime
                    val op = (1f - confettiTime / 5f).coerceIn(0f, 1f)
                    if (y < size.height && op > 0f) {
                        rotate(p.rot + confettiTime * p.rotSpeed, Offset(x, y)) {
                            drawRect(
                                color = p.color.copy(alpha = op),
                                topLeft = Offset(x, y),
                                size    = Size(p.w, p.h),
                            )
                        }
                    }
                }
            }
        }

        // Scrollable score content
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // ── Title ────────────────────────────────────────────────────
            AnimatedVisibility(
                visible = animPhase >= phases.titlePhase,
                enter   = fadeIn(tween(500)) + slideInVertically { -60 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "RESULTS",
                        color      = Color.White.copy(0.45f),
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text       = "Your Score",
                        color      = Color.White,
                        fontSize   = 36.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Gate breakdown cards ─────────────────────────────────────
            score.records.forEachIndexed { gi, record ->
                GateCard(
                    record          = record,
                    headerVisible   = animPhase >= phases.gateHeaderPhases[gi],
                    wordVisibility  = phases.gateWordPhases[gi].map { animPhase >= it },
                    scoreVisible    = animPhase >= phases.gateScorePhases[gi],
                )
                Spacer(Modifier.height(16.dp))
            }

            // ── Total score section ──────────────────────────────────────
            AnimatedVisibility(
                visible = animPhase >= phases.totalPhase,
                enter   = fadeIn(tween(400)) + slideInVertically { 40 },
            ) {
                Column(
                    modifier            = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1C0A35))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text      = "FINAL SCORE",
                        color     = Color.White.copy(0.5f),
                        fontSize  = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp,
                    )

                    // Animated score counter
                    Text(
                        text       = "${"%.1f".format(animatedScore)} / 10",
                        color      = Color.White,
                        fontSize   = 64.sp,
                        fontWeight = FontWeight.Black,
                    )

                    // Score bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(0.12f)),
                    ) {
                        val barFraction = (animatedScore / 10f).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barFraction)
                                .height(10.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(PurplePrimary, score.gradeColor)
                                    ),
                                    RoundedCornerShape(50),
                                )
                        )
                    }

                    // Points breakdown
                    Text(
                        text  = "${"%.2f".format(score.totalPoints)} / ${"%.1f".format(score.totalMaxPts)} pts",
                        color = Color.White.copy(0.6f),
                        fontSize = 14.sp,
                    )
                    if (score.perfectGates > 0) {
                        Text(
                            text  = "⚡ ${score.perfectGates} perfect gate${if (score.perfectGates > 1) "s" else ""}!",
                            color = GreenCorrect,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // ── Grade badge ──────────────────────────────────────────────
            AnimatedVisibility(
                visible = animPhase >= phases.gradePhase,
                enter   = scaleIn(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium)) + fadeIn(),
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .background(score.gradeColor.copy(0.15f), CircleShape)
                        .clip(CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text       = score.grade,
                        color      = score.gradeColor,
                        fontSize   = 44.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Stars ────────────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                phases.starPhases.forEachIndexed { i, starPhase ->
                    val starVisible = animPhase >= starPhase
                    val filled      = i < score.stars
                    StarBubble(visible = starVisible, filled = filled, delay = 0)
                }
            }

            Spacer(Modifier.height(36.dp))

            // ── Continue button ──────────────────────────────────────────
            AnimatedVisibility(
                visible = animPhase >= phases.continuePhase,
                enter   = fadeIn(tween(400)) + slideInVertically { 60 },
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick  = { navController.navigate("upi") },
                        shape    = RoundedCornerShape(50),
                        colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                    ) {
                        Text(
                            text       = "Continue to Premium  →",
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color      = Color.White,
                        )
                    }
                    Text(
                        text      = "Unlock all lessons with LangLang Premium",
                        color     = Color.White.copy(0.45f),
                        fontSize  = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gate card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GateCard(
    record         : PassedGateRecord,
    headerVisible  : Boolean,
    wordVisibility : List<Boolean>,
    scoreVisible   : Boolean,
) {
    AnimatedVisibility(
        visible = headerVisible,
        enter   = fadeIn(tween(300)) + slideInVertically { 30 },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(0.06f))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Gate header row
            Row(
                modifier            = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment   = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "Gate ${record.gateIndex}",
                    color      = Color.White.copy(0.5f),
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                Text(
                    text      = record.attemptLabel,
                    color     = record.attemptColor,
                    fontSize  = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            // Words as bubbles
            val displayWords = record.displayPrompt.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement   = Arrangement.spacedBy(6.dp),
            ) {
                displayWords.forEachIndexed { i, word ->
                    val visible = wordVisibility.getOrElse(i) { false }
                    ScoreWordBubble(
                        word    = word,
                        color   = if (visible) record.attemptColor else Color.Transparent,
                        visible = visible,
                    )
                }
            }

            // Gate score summary
            AnimatedVisibility(
                visible = scoreVisible,
                enter   = fadeIn(tween(300)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(record.attemptColor.copy(0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text  = "Attempt ${record.attemptNumber}  •  ${record.wordCount} words",
                        color = record.attemptColor.copy(0.85f),
                        fontSize = 13.sp,
                    )
                    Text(
                        text       = "${"%.2f".format(record.gatePoints)} pts",
                        color      = record.attemptColor,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Score word bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScoreWordBubble(word: String, color: Color, visible: Boolean) {
    val scale = remember(word) { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            scale.animateTo(1.35f, tween(70, easing = FastOutLinearInEasing))
            scale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
        }
    }
    Text(
        text     = word,
        modifier = Modifier.scale(scale.value),
        style    = TextStyle(
            fontSize   = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            color      = color,
            shadow     = Shadow(Color.Black.copy(0.35f), Offset(1f, 2f), 5f),
        ),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Star bubble
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StarBubble(visible: Boolean, filled: Boolean, delay: Int) {
    val scale = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            delay(delay.toLong())
            scale.animateTo(1.5f, tween(80, easing = FastOutLinearInEasing))
            scale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessMedium))
        }
    }
    Text(
        text     = if (filled) "⭐" else "☆",
        modifier = Modifier.scale(scale.value),
        fontSize = 34.sp,
        color    = if (filled) Color(0xFFFFD700) else Color.White.copy(0.25f),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Confetti particles
// ─────────────────────────────────────────────────────────────────────────────

private data class ConfettiParticle(
    val startX: Float, val startY: Float,
    val vx: Float,     val vy: Float,
    val color: Color,  val w: Float, val h: Float,
    val rot: Float,    val rotSpeed: Float,
)

private fun buildConfetti(count: Int): List<ConfettiParticle> {
    val colors = listOf(
        Color(0xFF69F0AE), Color(0xFFFFD700), Color(0xFFFF69B4),
        Color(0xFF64B5F6), Color(0xFFFF8A65), Color(0xFFCE93D8),
    )
    return List(count) {
        ConfettiParticle(
            startX   = Random.nextFloat(),
            startY   = -0.05f - Random.nextFloat() * 0.3f,
            vx       = (Random.nextFloat() - 0.5f) * 0.12f,
            vy       = 0.15f + Random.nextFloat() * 0.25f,
            color    = colors.random(),
            w        = 8f + Random.nextFloat() * 10f,
            h        = 4f + Random.nextFloat() * 6f,
            rot      = Random.nextFloat() * 360f,
            rotSpeed = (Random.nextFloat() - 0.5f) * 400f,
        )
    }
}

