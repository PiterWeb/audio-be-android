package com.piterdev.audiobe


import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val customCommandFavorites = SessionCommand("ACTION_PREVIOUS", Bundle.EMPTY)

    // Create your Player and MediaSession in the onCreate lifecycle event
    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val previousButton = CommandButton.Builder(CommandButton.ICON_PREVIOUS).setDisplayName("Previous").setSessionCommand(customCommandFavorites).build()

        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).setMediaButtonPreferences(ImmutableList.of(previousButton)).build()
    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession
}
