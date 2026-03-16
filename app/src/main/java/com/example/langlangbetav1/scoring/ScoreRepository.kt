package com.example.langlangbetav1.scoring

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide singleton that acts as a message bus between [SceneViewModel]
 * (which writes the score when the lesson ends) and [ScoreScreen]
 * (which reads it for the animated reveal).
 */
object ScoreRepository {

    private val _score = MutableStateFlow<FinalScore?>(null)
    val score: StateFlow<FinalScore?> = _score.asStateFlow()

    fun submit(finalScore: FinalScore) {
        _score.value = finalScore
    }

    /** Call before starting a new session so stale data is never shown. */
    fun clear() {
        _score.value = null
    }
}

