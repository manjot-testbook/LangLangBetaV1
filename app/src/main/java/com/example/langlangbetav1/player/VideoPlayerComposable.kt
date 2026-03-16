package com.example.langlangbetav1.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * Full-screen, controller-less video surface backed by [PlayerManager.player].
 *
 * Uses [RESIZE_MODE_ZOOM] so the video fills the screen without letterboxing.
 * The player lifecycle is managed externally by the ViewModel; this composable
 * only attaches/detaches the view.
 */
@Composable
fun VideoPlayerComposable(
    playerManager: PlayerManager,
    modifier: Modifier = Modifier,
) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                player        = playerManager.player
                useController = false
                resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        update = { view ->
            // Re-attach in case of recomposition (e.g. config change)
            view.player = playerManager.player
        },
        modifier = modifier,
    )
}

