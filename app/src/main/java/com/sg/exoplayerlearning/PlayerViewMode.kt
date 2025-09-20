package com.sg.exoplayerlearning

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.media3.common.C
import androidx.media3.common.C.TRACK_TYPE_TEXT
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.sg.exoplayerlearning.analytics.LearningsPlayerAnalytics
import com.sg.exoplayerlearning.analytics.logEvent
import com.sg.exoplayerlearning.cache.ExoPlayerCache
import com.sg.exoplayerlearning.handlers.AudioFocusHandler
import com.sg.exoplayerlearning.models.ActionType
import com.sg.exoplayerlearning.models.PlayerAction
import com.sg.exoplayerlearning.models.VideoItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import javax.inject.Inject

@UnstableApi
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val audioFocusHandler: AudioFocusHandler
): ViewModel() {

    companion object {
        // Source for videos: https://gist.github.com/jsturgis/3b19447b304616f18657
        const val Video_1 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        const val Video_2 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        const val Video_3 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4"
        const val Video_4 = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        const val Video_5 = "https://html5demos.com/assets/dizzy.mp4"
        const val Video_5_sub_title = "https://storage.googleapis.com/exoplayer-test-media-1/ttml/netflix_ttml_sample.xml"
    }

    //region Variables
    private val _playerState = MutableStateFlow<ExoPlayer?>(null)
    val playerState: StateFlow<ExoPlayer?> = _playerState

    private val _playerConfigState = MutableStateFlow<PlayerConfigState>(PlayerConfigState())
    val playerConfigState: StateFlow<PlayerConfigState> = _playerConfigState

    private val hashMapVideoStates = mutableMapOf<String,VideoItem>()
    private lateinit var analytics: LearningsPlayerAnalytics

    private var currentMediaItem: MediaItem? = null

    //endregion

    // region player and media items creation
    fun createPlayerWithMediaItems(context: Context) {
        if (_playerState.value == null) {

            val cacheFactory = buildOkHttoDataSourceFactory(context)

            // Create Media items list
            val mediaItems = listOf(
                MediaItem.Builder().setUri(Video_1).setMediaId("Video_1").setTag("Video_1").build(),
                MediaItem.Builder().setUri(Video_2).setMediaId("Video_2").setTag("Video_2").build(),
                MediaItem.Builder().setUri(Video_3).setMediaId("Video_3").setTag("Video_3").build(),
                MediaItem.Builder().setUri(Video_4).setMediaId("Video_4").setTag("Video_4").build(),
                getMediaItemWithSubTitle(),
            )

            // Create hashmap with video items to persist current playing position when shifting between videos
            mediaItems.forEach {
                hashMapVideoStates[it.mediaId] = if (it.mediaId.equals("Video_5", ignoreCase = true)) {
                    VideoItem(captionsAvailable = true)
                } else VideoItem()
            }

            // Create the player instance and update it to UI via stateFlow
            _playerState.update {
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(cacheFactory))
                    .build().apply {
                        audioFocusHandler.updatePlayer(this)
                        audioFocusHandler.requestAudioManager()
                        setMediaItems(mediaItems)
                        prepare()
                        playWhenReady = true
                    }
            }

            trackMediaItemTransitions()
            addAnalytics()
        }
    }

    fun buildOkHttoDataSourceFactory(context: Context): DataSource.Factory {
        val simpleCache = ExoPlayerCache.getSimpleCache(context)

        val okHttpClient = OkHttpClient.Builder().build()
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(okHttpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        return cacheDataSourceFactory
    }
    // endregion

    //region User actions
    fun executeAction(playerAction: PlayerAction) {
        when(playerAction.actionType) {
            ActionType.PLAY -> {
                logEvent("Action Play")
                audioFocusHandler.requestAudioManager()
                _playerState.value?.play()
            }
            ActionType.PAUSE -> {
                logEvent("Action Pause")
                audioFocusHandler.abandonAudioManager()
                _playerState.value?.pause()
            }
            ActionType.REWIND -> _playerState.value?.rewind()
            ActionType.FORWARD -> _playerState.value?.forward()
            ActionType.NEXT -> _playerState.value?.playNext()
            ActionType.PREVIOUS -> _playerState.value?.playPrevious()
            ActionType.SEEK -> _playerState.value?.seekWithValidation(playerAction.data as? Long)
            ActionType.SETTINGS -> logEvent("Action Settings")
            ActionType.LOCK -> lockScreen()
            ActionType.UNLOCK -> unLockScreen()
            ActionType.CAPTIONS -> toggleCaptions()
        }
    }

    private fun toggleCaptions() {
        logEvent("Action Captions")
        _playerConfigState.update {
            it.copy(
                captionsOn = it.captionsOn.not()
            )
        }
    }

    private fun lockScreen() {
        logEvent("Action Lock")
        _playerConfigState.update {
            it.copy(isScreenLocked = true)
        }
    }

    private fun unLockScreen() {
        logEvent("Action Un-Lock")
        _playerConfigState.update {
            it.copy(isScreenLocked = false)
        }
    }

    private fun ExoPlayer.seekWithValidation(position: Long?) {
        logEvent("seeking")
        position?.let {
            seekTo(position)
        }
    }

    private fun ExoPlayer.rewind() {
        logEvent("Action rewind")
        val newPosition = (currentPosition - 10_000).coerceAtLeast(0)
        seekTo(newPosition)
    }

    private fun ExoPlayer.forward() {
        logEvent("Action forward")
        val newPosition = (currentPosition + 10_000)
            .coerceAtMost(duration)
        seekTo(newPosition)
    }

    private fun ExoPlayer.playNext() {
        if (hasNextMediaItem()) {
            logEvent("Action next")
            val nextIndex = currentMediaItemIndex + 1
            val mediaItemId = getMediaItemAt(nextIndex)
            val seekPosition = hashMapVideoStates[mediaItemId.mediaId]?.currentPosition ?: 0L
            seekTo(nextIndex, seekPosition)
        }
    }

    private fun ExoPlayer.playPrevious() {
        if (
            isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM) &&
            hasPreviousMediaItem()
        ) {
            logEvent("Action previous")
            val previousIndex = currentMediaItemIndex - 1
            val mediaItemId = getMediaItemAt(previousIndex)
            val seekPosition = hashMapVideoStates[mediaItemId.mediaId]?.currentPosition ?: 0L
            seekTo(previousIndex, seekPosition)
        }
    }
    //endregion

    //region player listeners
    private fun trackMediaItemTransitions() {
        _playerState.value?.addListener(
            object : Player.Listener {
                override fun onTracksChanged(tracks: Tracks) {
                    super.onTracksChanged(tracks)
                    currentMediaItem?.let {
                        checkIfSubTitlesNeedToBeEnabled(it)
                    }
                }

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    currentMediaItem = mediaItem
                    _playerState.value?.currentMediaItemIndex?.let {
                        checkAndResetPreviousMediaItemProgress(it)
                    }
                }
            }
        )
    }

    private fun addAnalytics() {
        if (::analytics.isInitialized.not()) {
            analytics = LearningsPlayerAnalytics(_playerState.value)
        }
        _playerState.value?.addAnalyticsListener(analytics)
    }
    // endregion

    //region Player position updates
    private fun checkAndResetPreviousMediaItemProgress(currentMediaItemIndex: Int) {
        val previousIndex = currentMediaItemIndex - 1
        if (previousIndex >= 0) {
            _playerState.value?.getMediaItemAt(previousIndex)?.let { previousMediaItem ->
                hashMapVideoStates[previousMediaItem.mediaId]?.let { previousVideoItem ->
                    if (previousVideoItem.duration - previousVideoItem.currentPosition <= 3000) {
                        hashMapVideoStates[previousMediaItem.mediaId] = previousVideoItem.copy(currentPosition = 0)
                    }
                }
            }
        }
    }

    fun updateCurrentPosition(id: String, position: Long, duration: Long) {
        hashMapVideoStates[id] = hashMapVideoStates[id]?.copy(currentPosition = position, duration = duration)
            ?: VideoItem(currentPosition = position, duration = duration)
    }
    //endregion

    // region player playback speed configuration

    fun updatePlaybackSpeed(value: PlayerConfigState) {
        _playerConfigState.update { value }
        _playerState.value?.setPlaybackSpeed(value.playbackSpeed)
    }

    // endregion

    // region Sub title or captions configuration
    private fun getMediaItemWithSubTitle(): MediaItem {
        val subtitleConfiguration = MediaItem.SubtitleConfiguration.Builder(
            Uri.parse(Video_5_sub_title)
        )
            .setLanguage("en")
            .setMimeType("application/ttml+xml")
            .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
            .build()

        return MediaItem.Builder()
            .setUri(Video_5)
            .setMediaId("Video_5")
            .setTag("Video_5")
            .setSubtitleConfigurations(listOf(subtitleConfiguration))
            .build()
    }

    fun isCaptionsAvailableForVideo(url: String): Boolean = hashMapVideoStates[url]?.captionsAvailable ?: false

    private fun checkIfSubTitlesNeedToBeEnabled(mediaItem: MediaItem) {
        playerState.value?.let { _player ->
            if (isCaptionsAvailableForVideo(mediaItem.mediaId)) {
                enableSubtitles(_player)
            } else {
                disableSubtitles(_player)
            }
        }

    }

    fun disableSubtitles(player: ExoPlayer) {
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .clearOverridesOfType(TRACK_TYPE_TEXT)
            .build()
    }

    fun enableSubtitles(player: ExoPlayer) {
        val mappedTrackInfo = player.currentTracks
        val textTrackGroup = mappedTrackInfo.groups
            .firstOrNull { it.type == TRACK_TYPE_TEXT }
            ?.mediaTrackGroup ?: run {
            return
        }

        val override = TrackSelectionOverride(textTrackGroup, /* track indices */ listOf(0))
        player.trackSelectionParameters = player.trackSelectionParameters
            .buildUpon()
            .setOverrideForType(override)
            .build()
    }
    // endregion

    override fun onCleared() {
        super.onCleared()
        audioFocusHandler.abandonAudioManager()
        _playerState.value?.release()
    }
}

data class PlayerConfigState(
    val playbackSpeed: Float = 1f,
    val isScreenLocked: Boolean = false,
    val captionsOn: Boolean = true,
)

enum class BottomSheetType {
    SETTINGS, PLAYBACK_SPEED, NONE,
}