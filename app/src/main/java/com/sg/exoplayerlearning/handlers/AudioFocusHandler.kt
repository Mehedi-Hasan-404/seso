package com.sg.exoplayerlearning.handlers

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AudioFocusHandler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var currentPlayer: ExoPlayer? = null
    private var isPlayingBeforePause: Boolean = false
    private var audioManager: AudioManager? = null
    private var audioFocusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when(focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> if (isPlayingBeforePause) {
                currentPlayer?.play()
            }

            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            -> {
                updateIsPlaying()
                currentPlayer?.pause()
            }
        }
    }

    private fun updateIsPlaying() {
        isPlayingBeforePause = currentPlayer?.isPlaying ?: false
    }

    fun updatePlayer(exoPlayer: ExoPlayer) {
        currentPlayer = exoPlayer
    }

    fun requestAudioManager() {
        if (audioManager == null) {
            audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }

        audioManager?.let {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                    .build()

                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(audioFocusChangeListener)
                    .build()

                audioFocusRequest?.let{ audioManager?.requestAudioFocus(it) }
            } else {
                audioManager?.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                ) ?: true
            }

            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                currentPlayer?.pause()
            }
        }
    }

    fun abandonAudioManager() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            audioManager?.abandonAudioFocus(audioFocusChangeListener)
        }
        currentPlayer = null
        isPlayingBeforePause = false
        audioManager = null
        audioFocusRequest = null
    }

}