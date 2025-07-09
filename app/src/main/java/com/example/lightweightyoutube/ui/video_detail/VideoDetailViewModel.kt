package com.example.lightweightyoutube.ui.video_detail

import android.app.Application
import androidx.lifecycle.*
import com.example.lightweightyoutube.data.model.InvidiousComment
import com.example.lightweightyoutube.data.model.InvidiousVideo
import com.example.lightweightyoutube.data.model.AdaptiveFormat
import com.example.lightweightyoutube.data.repository.Resource
import com.example.lightweightyoutube.data.repository.VideoRepository
import kotlinx.coroutines.launch

/**
 * Represents a selectable video or audio quality stream.
 * @property qualityLabel A user-friendly string describing the quality (e.g., "720p (mp4)", "Audio (opus @128kbps)").
 * @property format The [AdaptiveFormat] object containing detailed stream information.
 * @property isVideo True if this stream is video-based, false if audio-only.
 */
data class VideoStreamQuality(
    val qualityLabel: String,
    val format: AdaptiveFormat,
    val isVideo: Boolean
)

/**
 * ViewModel for the Video Detail screen.
 * Fetches and manages detailed information about a specific video, including its metadata,
 * available playback/download qualities, and comments.
 *
 * @param application The application context.
 * @param videoId The unique identifier of the video to display.
 */
class VideoDetailViewModel(application: Application, private val videoId: String) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)

    private val _videoDetails = MutableLiveData<Resource<InvidiousVideo>>()
    /** LiveData holding the detailed information of the video. */
    val videoDetails: LiveData<Resource<InvidiousVideo>> = _videoDetails

    private val _comments = MutableLiveData<Resource<List<InvidiousComment>>>()
    /** LiveData holding the list of comments for the video. */
    val comments: LiveData<Resource<List<InvidiousComment>>> = _comments

    private val _availableQualities = MutableLiveData<List<VideoStreamQuality>>()
    /** LiveData holding the list of available video and audio qualities for playback/download. */
    val availableQualities: LiveData<List<VideoStreamQuality>> = _availableQualities

    private val _isLoading = MutableLiveData<Boolean>()
    /** LiveData indicating if the main video details are currently loading. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    /** LiveData holding error messages related to fetching video details or comments. Null if no error. */
    val error: LiveData<String?> = _error

    init {
        if (videoId.isBlank()) {
            _error.postValue("Video ID is missing.")
            _isLoading.postValue(false)
        } else {
            fetchVideoDetailsAndComments()
        }
    }

    /**
     * Fetches both the video details and its associated comments from the repository.
     * Updates relevant LiveData objects with the results or error states.
     */
    private fun fetchVideoDetailsAndComments() {
        viewModelScope.launch {
            _isLoading.postValue(true)
            _error.postValue(null)

            val detailsResult = repository.getVideoDetails(videoId)
            _videoDetails.postValue(detailsResult)

            if (detailsResult is Resource.Success && detailsResult.data != null) {
                extractAndPostQualities(detailsResult.data)
                fetchComments()
            } else if (detailsResult is Resource.Error) {
                _error.postValue(detailsResult.message ?: "Failed to load video details")
                _comments.postValue(Resource.Error(detailsResult.message ?: "Dependent data failed to load"))
            }
            _isLoading.postValue(false)
        }
    }

    /**
     * Extracts and processes video and audio stream information from the [InvidiousVideo] object
     * to populate the [_availableQualities] LiveData.
     *
     * @param video The [InvidiousVideo] object containing stream details.
     */
    private fun extractAndPostQualities(video: InvidiousVideo) {
        val qualities = mutableListOf<VideoStreamQuality>()

        // Add video streams from adaptiveFormats (preferred)
        video.adaptiveFormats
            .filter {
                it.type?.startsWith("video/") == true &&
                !it.resolution.isNullOrBlank() &&
                it.url.isNotBlank() &&
                it.height != null && it.height > 0
            }
            .sortedWith(compareByDescending<AdaptiveFormat> { it.height }.thenByDescending { it.fps ?: 0 }) // Sort by height, then FPS
            .distinctBy { "${it.height}p${it.fps ?: ""}" }
            .forEach { format ->
                val fpsLabel = format.fps?.let { "${it}fps" } ?: ""
                val containerLabel = format.container ?: format.type?.substringAfterLast('/')?.substringBefore(';') ?: ""
                val label = "${format.height}p $fpsLabel ($containerLabel)".replace("  ", " ").trim()
                qualities.add(VideoStreamQuality(label, format, true))
            }

        // Add audio-only streams from adaptiveFormats
        video.adaptiveFormats
            .filter {
                it.type?.startsWith("audio/") == true &&
                it.url.isNotBlank() &&
                (it.averageBitrate != null || it.bitrate != null)
            }
            .sortedByDescending { it.averageBitrate ?: it.bitrate?.toIntOrNull() ?: 0 } // Sort by bitrate
            .forEach { format ->
                val bitrateKbps = (format.averageBitrate ?: format.bitrate?.toIntOrNull() ?: 0) / 1000
                val container = format.encoding ?: format.container ?: format.type?.substringAfterLast('/')?.substringBefore(';') ?: ""
                val label = "Audio ($container @${bitrateKbps}kbps)".replace(" ()", "") // Clean up label
                qualities.add(VideoStreamQuality(label, format, false))
            }

        // Fallback to formatStreams if adaptiveFormats did not provide any video streams
        if (qualities.none { it.isVideo } && !video.formatStreams.isNullOrEmpty()) { // Check if no video streams were added from adaptive
            video.formatStreams
                .filter { !it.resolution.isNullOrBlank() && it.url.isNotBlank() } // Basic validation
                 .sortedWith(compareByDescending<com.example.lightweightyoutube.data.model.FormatStream> {
                    it.quality.filter { q -> q.isDigit() }.toIntOrNull() ?: 0 // Sort by quality (numeric part)
                }.thenByDescending { it.fps ?: 0 }) // Then by FPS
                .forEach { format ->
                    val height = format.quality.filter { q -> q.isDigit() }.toIntOrNull()
                    if (height != null && height > 0) {
                        qualities.add(VideoStreamQuality(
                            "${format.quality} (${format.container})", // Label for formatStream
                            AdaptiveFormat( // Convert FormatStream to AdaptiveFormat for consistency
                                url = format.url, type = format.type, quality = format.quality,
                                resolution = format.resolution, container = format.container,
                                encoding = format.encoding, fps = format.fps,
                                itag = format.itag, index = null, bitrate = null, init = null,
                                clen = null, lmt = null, projectionType = null,
                                width = null, // Can attempt to parse from resolution if needed
                                height = height, // Approximate height from quality string
                                audioSampleRate = null, audioChannels = null, averageBitrate = null
                            ),
                            true // It's a video stream
                        ))
                    }
                }
        }
        _availableQualities.postValue(qualities.distinctBy { it.qualityLabel }) // Ensure unique labels in the final list
    }

    /**
     * Fetches comments for the current video.
     * Supports pagination through a continuation token.
     *
     * @param continuationToken Token for fetching the next page of comments, null for the first page.
     */
    fun fetchComments(continuationToken: String? = null) {
        if (videoId.isBlank()) return // Should not happen if init guard works

        viewModelScope.launch {
            // TODO: Implement proper loading state for comments if needed (e.g., a separate LiveData)
            // if (continuationToken == null && _videoDetails.value is Resource.Success) {
            //     _isLoadingComments.postValue(true)
            // }
            val commentsResult = repository.getComments(videoId, continuationToken)

            if (commentsResult is Resource.Success) {
                // TODO: Handle pagination for comments by appending results if continuationToken is not null.
                // For now, it replaces comments with the new page.
                _comments.postValue(Resource.Success(commentsResult.data?.comments ?: emptyList()))
            } else if (commentsResult is Resource.Error) {
                 _comments.postValue(Resource.Error(commentsResult.message ?: "Failed to load comments"))
            }
            // _isLoadingComments.postValue(false)
        }
    }

    /**
     * Retries fetching video details and comments.
     * Useful if the initial fetch failed.
     */
    fun retry() {
        if (videoId.isNotBlank()) {
            fetchVideoDetailsAndComments()
        } else {
            // This state should ideally be prevented by initial checks.
            _error.postValue("Cannot retry: Video ID is missing.")
        }
    }

    /**
     * Call this method after an error message (from [_error] LiveData) has been displayed to the user,
     * to clear the error state.
     */
    fun userShownError() {
        _error.postValue(null)
    }

    /**
     * Factory for creating [VideoDetailViewModel] with dependencies.
     * @param application The application context.
     * @param videoId The ID of the video for which details are to be fetched.
     */
    class Factory(private val application: Application, private val videoId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(VideoDetailViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return VideoDetailViewModel(application, videoId) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
