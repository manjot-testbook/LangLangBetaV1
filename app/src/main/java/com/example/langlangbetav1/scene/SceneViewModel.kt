package com.example.langlangbetav1.scene

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.langlangbetav1.config.PermissionGate
import com.example.langlangbetav1.config.SceneRepository
import com.example.langlangbetav1.config.SceneStep
import com.example.langlangbetav1.config.SpeechGate
import com.example.langlangbetav1.player.PlayerEvent
import com.example.langlangbetav1.player.PlayerManager
import com.example.langlangbetav1.scoring.PassedGateRecord
import com.example.langlangbetav1.scoring.ScoreRepository
import com.example.langlangbetav1.scoring.buildFinalScore
import com.example.langlangbetav1.speech.SpeechRecognizerManager
import com.example.langlangbetav1.speech.SpeechResult
import com.example.langlangbetav1.speech.WordMatchState
import com.example.langlangbetav1.speech.WordResult
import com.example.langlangbetav1.speech.alignWords
import com.example.langlangbetav1.speech.isAcceptableMatch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SceneViewModel(application: Application) : AndroidViewModel(application) {

    // ── External components ────────────────────────────────────────────────
    val playerManager = PlayerManager(application)
    val speechManager = SpeechRecognizerManager(application)

    // ── UI state ───────────────────────────────────────────────────────────
    private val _uiState = MutableStateFlow<SceneUiState>(SceneUiState.Loading)
    val uiState: StateFlow<SceneUiState> = _uiState.asStateFlow()

    /**
     * Per-word alignment results shown in [AnimatedPromptText].
     * Empty when no prompt is active.
     */
    private val _wordResults = MutableStateFlow<List<WordResult>>(emptyList())
    val wordResults: StateFlow<List<WordResult>> = _wordResults.asStateFlow()

    // ── Score tracking ────────────────────────────────────────────────────
    private val _passedGates = mutableListOf<PassedGateRecord>()
    val passedGates: List<PassedGateRecord> get() = _passedGates.toList()
    private var gateIndexCounter = 0
    private var consecutivePerfect = 0

    /** Live list of passed gates — drives the real-time HUD. */
    private val _passedGatesState = MutableStateFlow<List<PassedGateRecord>>(emptyList())
    val passedGatesState: StateFlow<List<PassedGateRecord>> = _passedGatesState.asStateFlow()

    /** Total number of speech gates in the loaded module. */
    private val _totalGates = MutableStateFlow(0)
    val totalGates: StateFlow<Int> = _totalGates.asStateFlow()

    /** One-shot event for the gate-complete badge overlay. */
    private val _gateCompleteEvent = MutableSharedFlow<GateCompleteEvent>(extraBufferCapacity = 1)
    val gateCompleteEvent: SharedFlow<GateCompleteEvent> = _gateCompleteEvent.asSharedFlow()

    /**
     * One-shot event that tells the UI to launch the system permission dialog.
     * Emitted both on the first permission request and on every retry.
     */
    private val _requestPermissionEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val requestPermissionEvent: SharedFlow<Unit> = _requestPermissionEvent.asSharedFlow()

    init {
        observePlayerEvents()
        observeSpeechResults()
    }

    // ── Public entry points ────────────────────────────────────────────────

    /** Called once by the composable with the module ID from the nav route (e.g. "module_0"). */
    fun loadModule(moduleId: String) {
        _passedGates.clear()
        gateIndexCounter = 0
        consecutivePerfect = 0
        ScoreRepository.clear()
        SceneRepository.load(getApplication(), moduleId)
        _totalGates.value       = SceneRepository.steps.count { it.speechGate != null }
        _passedGatesState.value = emptyList()
        val step = SceneRepository.getFirstStep()
        startStoryQueue(step)
    }

    /** Called when the user taps the microphone button. */
    fun onMicTapped() {
        val current = _uiState.value
        val (step, attempts) = when (current) {
            is SceneUiState.ShowingPrompt -> current.step to current.wrongAttemptCount
            is SceneUiState.Listening     -> return  // already listening
            else                          -> return
        }
        _uiState.value = SceneUiState.Listening(step, attempts)
        speechManager.reset()
        speechManager.startListening()
    }

    /**
     * Called by the UI when the system permission dialog resolves.
     * Only acts when the current state is [SceneUiState.AwaitingPermission].
     */
    fun onPermissionResult(granted: Boolean) {
        val step = (_uiState.value as? SceneUiState.AwaitingPermission)?.step ?: return
        val gate = step.permissionGate ?: return

        if (granted) {
            val resIds = gate.grantedVideoResNames.mapNotNull { name ->
                SceneRepository.resId(getApplication(), name).takeIf { it != 0 }
            }
            if (resIds.isEmpty()) {
                advanceToNextStep(step)
            } else {
                _uiState.value = SceneUiState.PlayingGrantedVideos(step)
                playerManager.playVideoQueue(getApplication(), resIds)
            }
        } else {
            _uiState.value = SceneUiState.PermissionDenied(step)
        }
    }

    /**
     * Called when the user taps the "Allow Microphone" CTA on the
     * [SceneUiState.PermissionDenied] screen.  Re-emits the permission event
     * without restarting the idle video.
     */
    fun onPermissionRetry() {
        val step = (_uiState.value as? SceneUiState.PermissionDenied)?.step ?: return
        _uiState.value = SceneUiState.AwaitingPermission(step)
        viewModelScope.launch { _requestPermissionEvent.emit(Unit) }
    }

    // ── Story video queue (playlist approach) ─────────────────────────────

    private fun startStoryQueue(step: SceneStep) {
        _uiState.value = SceneUiState.PlayingStoryVideo(step)
        _wordResults.value = emptyList()

        // Resolve all resource IDs up-front and hand the whole playlist to
        // ExoPlayer.  It pre-buffers each next item while the current plays,
        // giving seamless, gapless transitions with no black-frame flash.
        val resIds = step.videoResNames.mapNotNull { name ->
            SceneRepository.resId(getApplication(), name).takeIf { it != 0 }
        }

        if (resIds.isEmpty()) {
            onStoryQueueExhausted(step)
        } else {
            playerManager.playVideoQueue(getApplication(), resIds)
        }
    }

    private fun onStoryQueueExhausted(step: SceneStep) {
        when {
            step.permissionGate != null -> enterPermissionPhase(step)
            step.speechGate     != null -> enterPromptPhase(step, wrongAttempts = 0)
            else                        -> advanceToNextStep(step)
        }
    }

    // ── Permission gate ───────────────────────────────────────────────────

    private fun enterPermissionPhase(step: SceneStep) {
        val gate = step.permissionGate ?: return

        // Start idle boomerang behind the system dialog
        val fwdId = SceneRepository.resId(getApplication(), gate.idleVideoResName)
        val revId = SceneRepository.resId(getApplication(), "${gate.idleVideoResName}_rev")
        if (fwdId != 0) playerManager.playVideoBoomerang(getApplication(), fwdId, revId)

        _uiState.value = SceneUiState.AwaitingPermission(step)
        viewModelScope.launch { _requestPermissionEvent.emit(Unit) }
    }

    // ── Prompt / idle phase ───────────────────────────────────────────────

    /**
     * Transition to [SceneUiState.ShowingPrompt].
     *
     * @param startIdleVideo  Pass `false` when recovering from an STT error so
     *                        the boomerang loop keeps playing from where it was
     *                        (no jarring jump back to frame 0).
     */
    private fun enterPromptPhase(
        step          : SceneStep,
        wrongAttempts : Int,
        startIdleVideo: Boolean = true,
    ) {
        val gate = step.speechGate ?: return

        // Reset every word to PENDING
        _wordResults.value = gate.displayPrompt
            .trim().split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { WordResult(it, WordMatchState.PENDING) }

        if (startIdleVideo) {
            val idleName = gate.idleVideoResName
            if (idleName != null) {
                val fwdId = SceneRepository.resId(getApplication(), idleName)
                val revId = SceneRepository.resId(getApplication(), "${idleName}_rev")
                if (fwdId != 0) playerManager.playVideoBoomerang(getApplication(), fwdId, revId)
            }
        }

        _uiState.value = SceneUiState.ShowingPrompt(step, wrongAttempts)
    }

    // ── Player event observer ─────────────────────────────────────────────

    private fun observePlayerEvents() {
        viewModelScope.launch {
            playerManager.event.collect { event ->
                if (event != PlayerEvent.Ended) return@collect
                when (val current = _uiState.value) {
                    is SceneUiState.PlayingStoryVideo    -> onStoryQueueExhausted(current.step)
                    is SceneUiState.PlayingGrantedVideos -> advanceToNextStep(current.step)
                    is SceneUiState.PlayingWrongResponse -> enterPromptPhase(
                        step           = current.step,
                        wrongAttempts  = current.nextWrongAttemptCount,
                        startIdleVideo = true,   // restart boomerang after reaction video
                    )
                    else -> Unit
                }
            }
        }
    }

    // ── Speech result observer ────────────────────────────────────────────

    private fun observeSpeechResults() {
        viewModelScope.launch {
            speechManager.result.collect { result ->
                val current = _uiState.value as? SceneUiState.Listening ?: return@collect
                when (result) {

                    // ── Real-time partial highlighting ─────────────────────
                    // Update word states as the user speaks.  Unmatched words
                    // stay PENDING (not INCORRECT) because they might still be
                    // said — only the final result commits to INCORRECT.
                    is SpeechResult.Partial -> {
                        val gate = current.step.speechGate ?: return@collect
                        _wordResults.value = alignWords(
                            displayPrompt = gate.displayPrompt,
                            spoken        = result.text,
                            partialMode   = true,   // unspoken words stay PENDING
                        )
                    }

                    // ── Final result ───────────────────────────────────────
                    is SpeechResult.Success -> handleSpeechResult(
                        step          = current.step,
                        wrongAttempts = current.wrongAttemptCount,
                        spoken        = result.text,
                    )

                    // ── Error — reset UI but keep boomerang playing ────────
                    // startIdleVideo = false → the player is left untouched
                    // so the boomerang loop continues from its current frame.
                    is SpeechResult.Error -> {
                        enterPromptPhase(
                            step           = current.step,
                            wrongAttempts  = current.wrongAttemptCount,
                            startIdleVideo = false,
                        )
                    }

                    else -> Unit
                }
            }
        }
    }

    private fun handleSpeechResult(step: SceneStep, wrongAttempts: Int, spoken: String) {
        val gate = step.speechGate ?: return
        _wordResults.value = alignWords(gate.displayPrompt, spoken)

        val attemptNumber = wrongAttempts + 1

        if (isAcceptableMatch(gate.expectedText, spoken)) {
            // ── Record gate score (attempt determines per-word points) ────
            val pointsPerWord = when {
                attemptNumber == 1 -> 1.00f
                attemptNumber == 2 -> 0.75f
                else               -> 0.50f
            }
            val wordCount = gate.displayPrompt.trim().split(Regex("\\s+")).count { it.isNotBlank() }
            val record = PassedGateRecord(
                stepId        = step.id,
                gateIndex     = ++gateIndexCounter,
                displayPrompt = gate.displayPrompt,
                attemptNumber = attemptNumber,
                wordCount     = wordCount,
                pointsPerWord = pointsPerWord,
            )
            _passedGates.add(record)
            _passedGatesState.value = _passedGates.toList()

            // Track streak of consecutive 1st-attempt passes
            consecutivePerfect = if (attemptNumber == 1) consecutivePerfect + 1 else 0

            // Gate-complete badge event + pleasant sound
            com.example.langlangbetav1.audio.SoundPlayer.playGatePass()
            viewModelScope.launch {
                _gateCompleteEvent.emit(
                    GateCompleteEvent(
                        gatePoints         = record.gatePoints,
                        maxPoints          = record.maxPoints,
                        attemptNumber      = attemptNumber,
                        consecutivePerfect = consecutivePerfect,
                    )
                )
            }

            _uiState.value = SceneUiState.Correct(step)
            viewModelScope.launch {
                delay(1_600)
                _wordResults.value = emptyList()
                advanceToNextStep(step)
            }
        } else {
            val newCount = wrongAttempts + 1
            viewModelScope.launch {
                delay(900)
                playWrongResponseIfAvailable(step, gate, newCount)
            }
        }
    }

    private fun playWrongResponseIfAvailable(
        step           : SceneStep,
        gate           : SpeechGate,
        newAttemptCount: Int,
    ) {
        val wrongVideos = gate.wrongResponseResNames
        if (wrongVideos.isNotEmpty()) {
            val resName = wrongVideos[(newAttemptCount - 1) % wrongVideos.size]
            val resId   = SceneRepository.resId(getApplication(), resName)
            if (resId != 0) {
                _uiState.value = SceneUiState.PlayingWrongResponse(step, newAttemptCount)
                playerManager.playVideo(getApplication(), resId)
                return
            }
        }
        enterPromptPhase(step, newAttemptCount)
    }

    // ── Step advancement ──────────────────────────────────────────────────

    private fun advanceToNextStep(step: SceneStep) {
        val next = step.nextStepId?.let { SceneRepository.getStep(it) }
        if (next != null) {
            startStoryQueue(next)
        } else {
            // Lesson complete — publish the score before the UI reads it
            ScoreRepository.submit(buildFinalScore(_passedGates.toList()))
            _uiState.value = SceneUiState.Finished
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
        speechManager.release()
    }
}
