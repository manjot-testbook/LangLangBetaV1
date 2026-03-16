package com.example.langlangbetav1.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.langlangbetav1.audio.SoundPlayer
import com.example.langlangbetav1.ui.theme.DarkBackground
import com.example.langlangbetav1.ui.theme.RedMarker
import kotlinx.coroutines.delay

/**
 * Cinematic splash screen.
 *
 * Sequence (≈ 5 s):
 *  1. Background gradient fades to dark purple                    [0 – 500 ms]
 *  2. "LANGLANG" logo fades + slides in                           [400 – 1100 ms]
 *  3. "Learn English" fades in                                    [1200 – 1900 ms]
 *  4. Red marker line draws across + MARKER SOUND                 [2000 – 2600 ms]
 *  5. "Experience English" fades in                               [2700 – 3400 ms]
 *  6. Hold                                                        [3400 – 4800 ms]
 *  7. onSplashComplete()
 */
@Composable
fun SplashScreen(onSplashComplete: () -> Unit) {

    // ── Animatables ────────────────────────────────────────────────────────
    val bgAlpha         = remember { Animatable(0f) }
    val logoAlpha       = remember { Animatable(0f) }
    val logoOffsetY     = remember { Animatable(30f) }
    val learnAlpha      = remember { Animatable(0f) }
    val strikeProgress  = remember { Animatable(0f) }
    val experienceAlpha = remember { Animatable(0f) }
    val taglineAlpha    = remember { Animatable(0f) }

    // ── Sequence ───────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        bgAlpha.animateTo(1f, tween(500))
        delay(100)

        // Logo entrance
        logoAlpha.animateTo(1f, tween(700))
        logoOffsetY.animateTo(0f, tween(600))
        delay(200)

        // "Learn English"
        learnAlpha.animateTo(1f, tween(700))
        delay(300)

        // Marker strikethrough + SOUND
        SoundPlayer.playMarkerScribble()
        strikeProgress.animateTo(1f, tween(560, easing = LinearEasing))
        delay(200)

        // "Experience English"
        experienceAlpha.animateTo(1f, tween(700))
        taglineAlpha.animateTo(1f, tween(600))
        delay(1_400)

        onSplashComplete()
    }

    // ── Background ────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center,
    ) {
        // Radial purple glow behind the text
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(
                        Color(0x44430070),
                        Color(0x00000000),
                    ),
                    center = Offset(size.width / 2f, size.height * 0.45f),
                    radius = size.width * 0.75f,
                ),
                radius = size.width * 0.75f,
            )
        }

        // Content column
        Column(
            modifier            = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Logo / wordmark ─────────────────────────────────────────
            Text(
                text     = "LANGLANG",
                modifier = Modifier
                    .padding(bottom = 4.dp),
                style    = TextStyle(
                    fontSize      = 48.sp,
                    fontWeight    = FontWeight.Black,
                    letterSpacing = 6.sp,
                    brush         = Brush.horizontalGradient(
                        listOf(Color(0xFF9C47FF), Color(0xFF6200EA))
                    ),
                    shadow        = Shadow(Color(0x88000000), Offset(0f, 6f), 12f),
                    textAlign     = TextAlign.Center,
                ),
                color    = Color.White.copy(alpha = logoAlpha.value),
            )

            Text(
                text  = "WHERE STORIES TEACH",
                color = Color.White.copy(alpha = taglineAlpha.value * 0.45f),
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(56.dp))

            // ── "Learn English" + red marker ────────────────────────────
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text  = "Learn English",
                    color = Color.White.copy(alpha = learnAlpha.value),
                    style = TextStyle(
                        fontSize   = 34.sp,
                        fontWeight = FontWeight.Bold,
                        shadow     = Shadow(Color.Black.copy(0.3f), Offset(0f, 4f), 8f),
                    ),
                )
                if (strikeProgress.value > 0f) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val startX  = -size.width * 0.10f
                        val endX    = size.width  * 1.10f
                        val curEnd  = startX + (endX - startX) * strikeProgress.value
                        // Slightly wobbly two-line marker effect
                        drawLine(
                            color       = RedMarker.copy(alpha = 0.85f),
                            start       = Offset(startX, size.height * 0.53f),
                            end         = Offset(curEnd, size.height * 0.55f),
                            strokeWidth = 10f,
                            cap         = StrokeCap.Round,
                        )
                        drawLine(
                            color       = RedMarker.copy(alpha = 0.40f),
                            start       = Offset(startX, size.height * 0.62f),
                            end         = Offset(curEnd, size.height * 0.64f),
                            strokeWidth = 5f,
                            cap         = StrokeCap.Round,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── "Experience English" ─────────────────────────────────────
            Text(
                text  = "Experience English",
                color = Color.White.copy(alpha = experienceAlpha.value),
                style = TextStyle(
                    fontSize   = 34.sp,
                    fontWeight = FontWeight.Bold,
                    brush      = Brush.horizontalGradient(
                        listOf(Color(0xFFE0E0E0), Color(0xFF9C47FF))
                    ),
                    shadow     = Shadow(Color.Black.copy(0.3f), Offset(0f, 4f), 8f),
                ),
            )
        }
    }
}
