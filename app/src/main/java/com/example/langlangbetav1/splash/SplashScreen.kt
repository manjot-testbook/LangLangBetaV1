package com.example.langlangbetav1.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langlangbetav1.audio.SoundPlayer
import kotlinx.coroutines.delay

// ── Palette ───────────────────────────────────────────────────────────────────
private val NotebookCream   = Color(0xFFFEF6E4)   // warm parchment
private val RuleLineBlue    = Color(0xFFA8BEE0)   // classic college-rule blue
private val MarginLineRed   = Color(0xFFE89090)   // soft red margin
private val InkDark         = Color(0xFF1A1A2E)   // navy-black pen ink
private val MarkerRed       = Color(0xFFCC2222)   // deep red marker

/**
 * Splash screen — notebook aesthetic.
 *
 * Sequence (≈ 5.5 s):
 *  1. Cream notebook + ruled lines fade in                [0 – 600 ms]
 *  2. "Learn English" written in dark ink fades in        [750 – 1500 ms]
 *  3. Red marker line strikes through                     [2050 – 2530 ms]
 *  4. "Experience" appears in red                         [2880 ms]
 *  5. "English" appears in red                            [3390 ms]
 *  6. Hold → onSplashComplete()                           [4800 ms]
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // ── Animatables ────────────────────────────────────────────────────────
    val bgAlpha        = remember { Animatable(0f) }
    val learnAlpha     = remember { Animatable(0f) }
    val learnOffsetX   = remember { Animatable(-20f) }   // subtle slide-in from left
    val strikeProgress = remember { Animatable(0f) }
    val word1Alpha     = remember { Animatable(0f) }     // "Experience"
    val word1OffsetX   = remember { Animatable(-16f) }
    val word2Alpha     = remember { Animatable(0f) }     // "English"
    val word2OffsetX   = remember { Animatable(-16f) }

    // ── Sequence ───────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        // 1. Background + lines
        bgAlpha.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
        delay(150)

        // 2. "Learn English" slides + fades in
        learnAlpha.animateTo(1f, tween(750, easing = FastOutSlowInEasing))
        learnOffsetX.animateTo(0f, tween(700, easing = FastOutSlowInEasing))
        delay(550)

        // 3. Red marker strikethrough + pleasant two-note swoosh
        SoundPlayer.playMarkerSwoosh()
        strikeProgress.animateTo(1f, tween(480, easing = LinearEasing))
        delay(350)

        // 4. "Experience" materialises — soft pop
        SoundPlayer.playWordReveal()
        word1Alpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        word1OffsetX.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        delay(250)

        // 5. "English" materialises — soft pop
        SoundPlayer.playWordReveal()
        word2Alpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
        word2OffsetX.animateTo(0f, tween(450, easing = FastOutSlowInEasing))
        delay(1_400)

        onSplashComplete()
    }

    // ── Root container ─────────────────────────────────────────────────────
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(NotebookCream),
        contentAlignment = Alignment.Center,
    ) {
        val screenH        = maxHeight
        val screenW        = maxWidth
        val marginLineDp   = screenW * 0.17f   // ~17% from left edge
        val lineSpacingDp  = 34.dp             // college-ruled spacing

        // ── Ruled lines + red margin ──────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val a           = bgAlpha.value
            val spacingPx   = lineSpacingDp.toPx()
            val marginX     = marginLineDp.toPx()
            val lineCount   = (size.height / spacingPx).toInt() + 2

            // Horizontal blue rules
            repeat(lineCount) { i ->
                val y = i * spacingPx
                drawLine(
                    color       = RuleLineBlue.copy(alpha = a * 0.50f),
                    start       = Offset(0f, y),
                    end         = Offset(size.width, y),
                    strokeWidth = 1.3f,
                )
            }

            // Red vertical margin line
            drawLine(
                color       = MarginLineRed.copy(alpha = a * 0.80f),
                start       = Offset(marginX, 0f),
                end         = Offset(marginX, size.height),
                strokeWidth = 1.8f,
            )

            // Subtle top shadow (like a spiral-bound shadow)
            drawRect(
                color  = Color(0x18000000),
                topLeft = Offset(0f, 0f),
                size   = androidx.compose.ui.geometry.Size(size.width, spacingPx * 0.6f),
            )
        }

        // ── Text content — starts just past the margin line ───────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = marginLineDp + 18.dp,   // sit just past the margin
                    end   = screenW * 0.08f,
                ),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── "Learn English" (dark ink, serif italic) ──────────────────
            Box(contentAlignment = Alignment.CenterStart) {
                Text(
                    text     = "Learn English",
                    modifier = Modifier.offset(x = learnOffsetX.value.dp),
                    color    = InkDark.copy(alpha = learnAlpha.value),
                    style    = TextStyle(
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.Normal,
                        fontFamily    = FontFamily.Serif,
                        fontStyle     = FontStyle.Italic,
                        letterSpacing = 0.3.sp,
                    ),
                )

                // Red marker strikethrough
                if (strikeProgress.value > 0f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val sx  = -12f
                        val ex  = size.width + 12f
                        val cur = sx + (ex - sx) * strikeProgress.value

                        // Primary thick stroke — natural slight angle
                        drawLine(
                            color       = MarkerRed.copy(alpha = 0.90f),
                            start       = Offset(sx,  size.height * 0.50f),
                            end         = Offset(cur, size.height * 0.53f),
                            strokeWidth = 14f,
                            cap         = StrokeCap.Round,
                        )
                        // Bleed / secondary thinner stroke (marker always bleeds slightly)
                        drawLine(
                            color       = MarkerRed.copy(alpha = 0.25f),
                            start       = Offset(sx,  size.height * 0.63f),
                            end         = Offset(cur, size.height * 0.66f),
                            strokeWidth = 6f,
                            cap         = StrokeCap.Round,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // ── "Experience English" (red, word by word, cursive) ─────────
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text     = "Experience",
                    modifier = Modifier.offset(x = word1OffsetX.value.dp),
                    color    = MarkerRed.copy(alpha = word1Alpha.value),
                    style    = TextStyle(
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.Normal,
                        fontFamily    = FontFamily.Cursive,
                        letterSpacing = 0.sp,
                    ),
                )
                Text(
                    text     = "English",
                    modifier = Modifier.offset(x = word2OffsetX.value.dp),
                    color    = MarkerRed.copy(alpha = word2Alpha.value),
                    style    = TextStyle(
                        fontSize      = 38.sp,
                        fontWeight    = FontWeight.Normal,
                        fontFamily    = FontFamily.Cursive,
                        letterSpacing = 0.sp,
                    ),
                )
            }
        }
    }
}
