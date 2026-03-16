package com.example.langlangbetav1.scene

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.langlangbetav1.player.VideoPlayerComposable
import com.example.langlangbetav1.scoring.PassedGateRecord
import kotlinx.coroutines.delay
import com.example.langlangbetav1.speech.WordMatchState
import com.example.langlangbetav1.speech.WordResult
import com.example.langlangbetav1.ui.components.AnimatedPromptText
import com.example.langlangbetav1.ui.theme.GreenCorrect
import com.example.langlangbetav1.ui.theme.OverlayDark
import com.example.langlangbetav1.ui.theme.PurplePrimary
import com.example.langlangbetav1.ui.theme.RedMarker

@Composable
fun SceneScreen(
    moduleId      : String,
    navController : NavController,
    viewModel     : SceneViewModel = viewModel(),
) {
    val context      = LocalContext.current
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val wordResults  by viewModel.wordResults.collectAsStateWithLifecycle()
    val totalGates   by viewModel.totalGates.collectAsStateWithLifecycle()
    val passedGates  by viewModel.passedGatesState.collectAsStateWithLifecycle()

    // ── Gate-complete badge ────────────────────────────────────────────────
    var badgeEvent by remember { mutableStateOf<GateCompleteEvent?>(null) }
    LaunchedEffect(Unit) {
        viewModel.gateCompleteEvent.collect { event ->
            badgeEvent = event
            delay(2_400)
            badgeEvent = null
        }
    }

    // ── Navigate to score screen when lesson ends ──────────────────────────
    LaunchedEffect(uiState) {
        if (uiState is SceneUiState.Finished) {
            delay(900)
            navController.navigate("score") {
                popUpTo("scene/{moduleId}") { inclusive = false }
            }
        }
    }

    // ── Single permission launcher ─────────────────────────────────────────
    // Used by both the onboarding PermissionGate AND the speech STT gate.
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        // Report to ViewModel when we're in the onboarding permission gate
        if (viewModel.uiState.value is SceneUiState.AwaitingPermission) {
            viewModel.onPermissionResult(granted)
        }
    }

    // ── Respond to ViewModel's "show the dialog" signal ───────────────────
    // Also handles the case where permission was already granted in a prior
    // session — skips the dialog and reports success immediately.
    LaunchedEffect(Unit) {
        viewModel.requestPermissionEvent.collect {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED
            ) {
                viewModel.onPermissionResult(granted = true)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    LaunchedEffect(moduleId) { viewModel.loadModule(moduleId) }

    // HUD visible during lesson, hidden during onboarding permission flow
    val showHud = totalGates > 0
        && uiState !is SceneUiState.Loading
        && uiState !is SceneUiState.AwaitingPermission
        && uiState !is SceneUiState.PermissionDenied
        && uiState !is SceneUiState.PlayingGrantedVideos

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        VideoPlayerComposable(
            playerManager = viewModel.playerManager,
            modifier      = Modifier.fillMaxSize(),
        )

        AnimatedContent(
            targetState    = uiState,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(250)) },
            label          = "scene_overlay",
        ) { state ->
            when (state) {
                is SceneUiState.AwaitingPermission ->
                    Box(modifier = Modifier.fillMaxSize())
                is SceneUiState.PermissionDenied ->
                    PermissionDeniedOverlay(onAllowClicked = { viewModel.onPermissionRetry() })
                is SceneUiState.ShowingPrompt -> PromptOverlay(
                    wordResults   = wordResults,
                    isListening   = false,
                    attemptCount  = state.wrongAttemptCount + 1,
                    onMicClick    = {
                        if (hasAudioPermission) viewModel.onMicTapped()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )
                is SceneUiState.Listening -> {
                    val hasResult = wordResults.any { it.state != WordMatchState.PENDING }
                    PromptOverlay(
                        wordResults  = wordResults,
                        isListening  = !hasResult,
                        attemptCount = state.wrongAttemptCount + 1,
                        onMicClick   = null,
                    )
                }
                is SceneUiState.Correct  -> CorrectOverlay()
                is SceneUiState.Finished -> FinishedOverlay()
                else -> Box(modifier = Modifier.fillMaxSize())
            }
        }

        // ── Lesson HUD — sits above video, below prompt card ──────────────
        if (showHud) {
            LessonHud(
                totalGates  = totalGates,
                passedGates = passedGates,
                modifier    = Modifier.align(Alignment.TopCenter),
            )
        }

        // ── Gate-complete badge — slides up from bottom-center ─────────────
        AnimatedVisibility(
            visible  = badgeEvent != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 220.dp),
            enter    = fadeIn(tween(180)) + scaleIn(tween(200, easing = FastOutSlowInEasing)),
            exit     = fadeOut(tween(280)) + scaleOut(tween(250)),
        ) {
            badgeEvent?.let { GateCompleteBadge(it) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lesson HUD  —  thin progress bar + gate dots + running score
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LessonHud(
    totalGates  : Int,
    passedGates : List<PassedGateRecord>,
    modifier    : Modifier = Modifier,
) {
    val passed  = passedGates.size
    val target  = (passed.toFloat() / totalGates.toFloat()).coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue   = target,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label         = "hud_bar",
    )

    // Running score out of 10 based only on gates completed so far
    val earned   = passedGates.sumOf { it.gatePoints.toDouble() }.toFloat()
    val maxSoFar = passedGates.sumOf { it.maxPoints.toDouble() }.toFloat()
    val score10  = if (maxSoFar > 0f) (earned / maxSoFar) * 10f else 0f

    val scoreColor = when {
        passed == 0          -> Color.White.copy(0.35f)
        score10 >= 8.5f      -> Color(0xFF69F0AE)   // green
        score10 >= 6.5f      -> Color(0xFFFFD54F)   // amber
        else                 -> Color(0xFFFF8A65)   // orange
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.52f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        // ── Thin progress bar ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.12f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF69F0AE), Color(0xFF26C6DA))
                        )
                    ),
            )
        }

        Spacer(Modifier.height(7.dp))

        // ── Gate dots + counter on left, score on right ────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            // Gate indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                repeat(totalGates) { i ->
                    val done = i < passed
                    Box(
                        modifier = Modifier
                            .size(if (done) 7.dp else 5.dp)
                            .background(
                                color = if (done) Color(0xFF69F0AE) else Color.White.copy(0.22f),
                                shape = CircleShape,
                            )
                    )
                }
                Spacer(Modifier.size(4.dp))
                Text(
                    text       = "$passed / $totalGates",
                    color      = Color.White.copy(0.65f),
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp,
                )
            }

            // Running score
            Text(
                text       = if (passed == 0) "—" else "★  ${"%.1f".format(score10)}",
                color      = scoreColor,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gate-complete badge  —  floats up briefly when a gate is passed
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun GateCompleteBadge(event: GateCompleteEvent) {
    val bgColor = when (event.attemptNumber) {
        1    -> Color(0xFF1B5E20)   // deep green
        2    -> Color(0xFF4E342E)   // warm brown
        else -> Color(0xFF37474F)   // dark slate
    }
    val accentColor = when (event.attemptNumber) {
        1    -> Color(0xFF69F0AE)
        2    -> Color(0xFFFFD54F)
        else -> Color(0xFFFF8A65)
    }
    val label = when (event.attemptNumber) {
        1    -> "Perfect!"
        2    -> "Nice!"
        else -> "Keep going!"
    }
    val streakLine = if (event.consecutivePerfect >= 2) "🔥 ${event.consecutivePerfect}× streak" else null

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor.copy(alpha = 0.92f))
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Text(text = "✓", color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Text(
                text       = "+${"%.0f".format(event.gatePoints)} pts",
                color      = accentColor,
                fontSize   = 20.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text(
            text       = label,
            color      = Color.White.copy(0.80f),
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
        )
        if (streakLine != null) {
            Text(
                text     = streakLine,
                color    = Color(0xFFFFD54F),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permission denied overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermissionDeniedOverlay(onAllowClicked: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text       = "🎤",
                fontSize   = 56.sp,
            )
            Text(
                text       = "Microphone Access Needed",
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
            )
            Text(
                text      = "LangLang needs to hear you to check your pronunciation and progress through the lesson.",
                color     = Color.White.copy(alpha = 0.75f),
                fontSize  = 15.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Highlighted CTA button
            Button(
                onClick  = onAllowClicked,
                shape    = RoundedCornerShape(50),
                colors   = ButtonDefaults.buttonColors(containerColor = PurplePrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
            ) {
                Text(
                    text       = "Allow Microphone Access  →",
                    fontSize   = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Prompt overlay
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PromptOverlay(
    wordResults : List<WordResult>,
    isListening : Boolean,
    attemptCount: Int = 1,
    onMicClick  : (() -> Unit)?,
) {
    Box(
        modifier         = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = OverlayDark,
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                )
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Attempt indicator dots + label
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text  = "Try $attemptCount",
                    color = when (attemptCount) {
                        1    -> Color(0xFF69F0AE)
                        2    -> Color(0xFFFFD54F)
                        else -> Color(0xFFFF8A65)
                    },
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                )
                repeat(3) { i ->
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(
                                if (i < attemptCount) when (attemptCount) {
                                    1    -> Color(0xFF69F0AE)
                                    2    -> Color(0xFFFFD54F)
                                    else -> Color(0xFFFF8A65)
                                } else Color.White.copy(0.2f),
                                CircleShape,
                            )
                    )
                }
            }

            Text(text = "Say it out loud:", style = MaterialTheme.typography.labelMedium)

            AnimatedPromptText(
                words    = wordResults,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            Spacer(modifier = Modifier.height(4.dp))

            MicButton(isListening = isListening, onClick = onMicClick)

            AnimatedVisibility(visible = isListening) {
                Text(
                    text       = "Listening…",
                    color      = RedMarker,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Mic button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MicButton(isListening: Boolean, onClick: (() -> Unit)?) {
    val infinite = rememberInfiniteTransition(label = "mic_pulse")
    val scale by infinite.animateFloat(
        initialValue  = 1f,
        targetValue   = if (isListening) 1.18f else 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "mic_scale",
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .size(72.dp)
            .background(if (isListening) RedMarker else PurplePrimary, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        IconButton(onClick = onClick ?: {}, enabled = onClick != null, modifier = Modifier.fillMaxSize()) {
            Text("🎤", fontSize = 30.sp)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Terminal overlays
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CorrectOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("✓  Great job!", color = GreenCorrect, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FinishedOverlay() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🎉", fontSize = 64.sp)
            Text(
                text       = "Well done!\nYou completed the lesson.",
                color      = Color.White,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
            )
        }
    }
}
