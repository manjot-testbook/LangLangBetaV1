package com.example.langlangbetav1.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Events the video player emits to the scene ViewModel. */
sealed class PlayerEvent {
    object Idle    : PlayerEvent()
    object Playing : PlayerEvent()
    /** Fired only when the last item in a non-looping playlist finishes. */
    object Ended   : PlayerEvent()
}

/**
 * Wrapper around a single, long-lived [ExoPlayer] instance.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │  The player is NEVER recreated between clips.  All three play modes     │
 * │  simply replace the media items on the existing instance so the GPU     │
 * │  surface and codec pipeline stay warm → zero black-frame flash.         │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 *  • [playVideoQueue]   — loads a playlist and plays each item seamlessly.
 *                         ExoPlayer pre-buffers the next local file while the
 *                         current one is playing.  Emits [PlayerEvent.Ended]
 *                         only after the LAST item finishes.
 *  • [playVideo]        — single-shot clip (wrong-response videos).
 *  • [playVideoLooping] — single item with REPEAT_MODE_ONE (idle background).
 */
class PlayerManager(context: Context) {

    /** Exposed directly so the Compose view can attach to it via AndroidView. */
    val player: ExoPlayer = ExoPlayer.Builder(context).build()

    private val _event = MutableStateFlow<PlayerEvent>(PlayerEvent.Idle)
    val event: StateFlow<PlayerEvent> = _event.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_ENDED -> _event.value = PlayerEvent.Ended
                    Player.STATE_READY -> {
                        if (player.playWhenReady) _event.value = PlayerEvent.Playing
                    }
                    else -> Unit
                }
            }
        })
    }

    // ── Public play methods ───────────────────────────────────────────────

    /**
     * Load [rawResIds] as a Media3 playlist and start gapless playback.
     *
     * ExoPlayer buffers the next local item while the current one is playing,
     * so item-to-item transitions are completely seamless.
     * [PlayerEvent.Ended] fires once after the final item completes.
     */
    fun playVideoQueue(context: Context, rawResIds: List<Int>) {
        _event.value = PlayerEvent.Idle
        player.repeatMode = Player.REPEAT_MODE_OFF
        val items = rawResIds.map { id ->
            MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$id"))
        }
        // setMediaItems replaces the whole playlist on the *same* player
        // instance — no surface detach, no codec reset.
        player.setMediaItems(items)
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Play a single non-looping clip (used for wrong-response reaction videos).
     * Emits [PlayerEvent.Ended] when it finishes.
     */
    fun playVideo(context: Context, rawResId: Int) {
        _event.value = PlayerEvent.Idle
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItem(
            MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResId"))
        )
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Play a single clip that loops indefinitely (the idle background video).
     * Does NOT emit [PlayerEvent.Ended] — caller must explicitly replace it.
     */
    fun playVideoLooping(context: Context, rawResId: Int) {
        _event.value = PlayerEvent.Idle
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.setMediaItem(
            MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResId"))
        )
        player.prepare()
        player.playWhenReady = true
    }

    /**
     * Boomerang loop: plays [rawResIdFwd] forward then [rawResIdRev] (a
     * pre-rendered frame-reversed copy) in a 2-item REPEAT_MODE_ALL playlist.
     *
     * ExoPlayer pre-buffers the next item while the current plays, so the
     * fwd→rev and rev→fwd transitions are frame-perfect with no black flash.
     * Does NOT emit [PlayerEvent.Ended] — loops indefinitely.
     *
     * Falls back to a plain forward loop if [rawResIdRev] == 0.
     *
     * Sequence:  1 2 3 4 → 4 3 2 1 → 1 2 3 4 → 4 3 2 1 → …
     */
    fun playVideoBoomerang(context: Context, rawResIdFwd: Int, rawResIdRev: Int) {
        if (rawResIdRev == 0) {
            playVideoLooping(context, rawResIdFwd)
            return
        }
        _event.value = PlayerEvent.Idle
        player.repeatMode = Player.REPEAT_MODE_ALL
        player.setMediaItems(
            listOf(
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResIdFwd")),
                MediaItem.fromUri(Uri.parse("android.resource://${context.packageName}/$rawResIdRev")),
            )
        )
        player.prepare()
        player.playWhenReady = true
    }

    /** Stop and release all ExoPlayer resources. */
    fun release() {
        player.release()
    }
}
