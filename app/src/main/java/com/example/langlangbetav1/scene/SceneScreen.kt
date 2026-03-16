package com.example.langlangbetav1.scene

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.scale
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
    val context     = LocalContext.current
    val uiState     by viewModel.uiState.collectAsStateWithLifecycle()
    val wordResults by viewModel.wordResults.collectAsStateWithLifecycle()

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

                // ── Onboarding idle: system dialog is showing, no UI overlay ─
                is SceneUiState.AwaitingPermission ->
                    Box(modifier = Modifier.fillMaxSize())

                // ── Permission denied: show highlighted CTA ────────────────
                is SceneUiState.PermissionDenied ->
                    PermissionDeniedOverlay(onAllowClicked = { viewModel.onPermissionRetry() })

                // ── Speech prompt (idle boomerang loops behind it) ─────────
                is SceneUiState.ShowingPrompt -> PromptOverlay(
                    wordResults   = wordResults,
                    isListening   = false,
                    attemptCount  = state.wrongAttemptCount + 1,
                    onMicClick    = {
                        if (hasAudioPermission) viewModel.onMicTapped()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                )

                // ── STT active ─────────────────────────────────────────────
                is SceneUiState.Listening -> {
                    val hasResult = wordResults.any { it.state != WordMatchState.PENDING }
                    PromptOverlay(
                        wordResults  = wordResults,
                        isListening  = !hasResult,
                        attemptCount = state.wrongAttemptCount + 1,
                        onMicClick   = null,
                    )
                }

                is SceneUiState.Correct   -> CorrectOverlay()
                is SceneUiState.Finished  -> FinishedOverlay()

                // Story / granted videos / wrong-response / loading: no overlay
                else -> Box(modifier = Modifier.fillMaxSize())
            }
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
