package com.example.langlangbetav1.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langlangbetav1.audio.SoundPlayer
import com.example.langlangbetav1.speech.WordMatchState
import com.example.langlangbetav1.speech.WordResult
import com.example.langlangbetav1.ui.theme.GreenCorrect
import com.example.langlangbetav1.ui.theme.RedMarker
import kotlinx.coroutines.launch

/**
 * Renders a list of [WordResult]s as an animated, "bubbly" prompt display.
 *
 * Per-word behaviour:
 *  • PENDING   — white, normal weight
 *  • CORRECT   — spring bounce (scale 1 → 1.4 → 1 with low damping)
 *                + green "water fill" that rises from the bottom of each glyph
 *  • INCORRECT — red colour + underline
 *
 * Words wrap naturally via [FlowRow] and are centred.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnimatedPromptText(
    words: List<WordResult>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier              = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalArrangement   = Arrangement.Center,
    ) {
        words.forEachIndexed { index, wordResult ->
            AnimatedWord(
                wordIndex  = index,
                wordResult = wordResult,
                modifier   = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Single animated word
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnimatedWord(
    wordIndex : Int,
    wordResult: WordResult,
    modifier  : Modifier = Modifier,
) {
    // Animatables keyed to wordIndex so distinct instances are created per slot
    val scale        = remember(wordIndex) { Animatable(1f) }
    val fillProgress = remember(wordIndex) { Animatable(0f) }

    LaunchedEffect(wordIndex, wordResult.state) {
        when (wordResult.state) {

            WordMatchState.CORRECT -> {
                SoundPlayer.playWordPop()
                // ── Spring bounce ─────────────────────────────────────────
                launch {
                    scale.animateTo(
                        targetValue   = 1.4f,
                        animationSpec = tween(
                            durationMillis = 50,
                            easing         = FastOutLinearInEasing,
                        ),
                    )
                    scale.animateTo(
                        targetValue   = 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness    = Spring.StiffnessMedium,
                        ),
                    )
                }
                // ── Water fill: green rises from the bottom ───────────────
                fillProgress.animateTo(
                    targetValue   = 1f,
                    animationSpec = tween(
                        durationMillis = 160,
                        easing         = LinearOutSlowInEasing,
                    ),
                )
            }

            // Reset visuals instantly for both INCORRECT and PENDING
            WordMatchState.INCORRECT,
            WordMatchState.PENDING -> {
                scale.snapTo(1f)
                fillProgress.snapTo(0f)
            }
        }
    }

    Box(modifier = modifier.scale(scale.value)) {

        // ── Base layer ─────────────────────────────────────────────────────
        // Shows the word in white (PENDING / CORRECT base) or red (INCORRECT).
        val isIncorrect = wordResult.state == WordMatchState.INCORRECT
        Text(
            text  = wordResult.word,
            style = bubbleStyle(
                color          = if (isIncorrect) RedMarker else Color.White,
                textDecoration = if (isIncorrect) TextDecoration.Underline
                                 else             TextDecoration.None,
            ),
        )

        // ── Green water-fill overlay ───────────────────────────────────────
        // Rendered on top of the white text and clipped to the bottom
        // [fillProgress] fraction of the glyph height, creating the effect
        // of green "water" rising through the letters.
        //
        // Technique:  Canvas.save / clipRect(top = height*(1-fp)) / drawContent
        // / restore  — the clip is applied to the identical Text composable so
        // its glyphs align perfectly with the base layer below.
        val fp = fillProgress.value
        if (fp > 0f) {
            Text(
                text     = wordResult.word,
                style    = bubbleStyle(color = GreenCorrect),
                modifier = Modifier.drawWithContent {
                    // Save canvas state, apply a bottom-anchored clip,
                    // draw the composable content, then restore.
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(
                        left   = 0f,
                        top    = size.height * (1f - fp),
                        right  = size.width,
                        bottom = size.height,
                    )
                    drawContent()
                    drawContext.canvas.restore()
                },
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared text style
// ─────────────────────────────────────────────────────────────────────────────

/**
 * "Bubbly" style: ExtraBold weight + generous letter spacing + soft drop shadow.
 *
 * Swap [fontFamily] here to plug in a custom rounded typeface (e.g. Nunito,
 * Fredoka One via Google Fonts) without touching any other code.
 */
private fun bubbleStyle(
    color          : Color          = Color.White,
    textDecoration : TextDecoration = TextDecoration.None,
) = TextStyle(
    fontSize       = 26.sp,
    fontWeight     = FontWeight.ExtraBold,
    letterSpacing  = 0.4.sp,
    color          = color,
    textDecoration = textDecoration,
    shadow         = Shadow(
        color      = Color.Black.copy(alpha = 0.40f),
        offset     = Offset(1f, 3f),
        blurRadius = 7f,
    ),
)

