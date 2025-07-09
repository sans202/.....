package com.example.lightweightyoutube.data.model

/**
 * Data class to store information about a downloaded video.
 * This information is persisted (e.g., in SharedPreferences or a database) to track downloads.
 *
 * @property downloadManagerId The ID assigned by Android's [DownloadManager] when the download is enqueued.
 * @property invidiousVideoId The original Invidious video ID (e.g., YouTube video ID).
 * @property title The title of the video.
 * @property localFilePath The absolute local file system path where the video is stored after successful download. Null if not yet completed or path is unknown.
 * @property videoUrl The direct URL from which the video content was downloaded.
 * @property thumbnailUrl The URL of a thumbnail image for the video (can be the one displayed at time of download).
 * @property qualityLabel A user-friendly string describing the quality of the downloaded video (e.g., "720p", "Audio").
 * @property fileSize The size of the downloaded file in bytes. Set upon completion. Defaults to 0L.
 * @property status The current [DownloadStatus] of the video. Defaults to [DownloadStatus.PENDING].
 * @property downloadEnqueuedTimestamp Timestamp (in milliseconds) when the download was initially enqueued.
 */
data class DownloadedVideoInfo(
    val downloadManagerId: Long,
    val invidiousVideoId: String,
    val title: String,
    var localFilePath: String? = null,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val qualityLabel: String,
    var fileSize: Long = 0L,
    var status: DownloadStatus = DownloadStatus.PENDING,
    val downloadEnqueuedTimestamp: Long = System.currentTimeMillis()
)

/**
 * Represents the various states a video download can be in.
 */
enum class DownloadStatus {
    /** Download has been enqueued in [DownloadManager] but is not yet actively downloading. */
    PENDING,
    /** Download is actively in progress. */
    DOWNLOADING,
    /** Download completed successfully. */
    COMPLETED,
    /** Download failed. */
    FAILED,
    /** Download is paused (e.g., due to network loss, or user action if supported). */
    PAUSED,
    /** Download was cancelled by the user or system. */
    CANCELLED
}
