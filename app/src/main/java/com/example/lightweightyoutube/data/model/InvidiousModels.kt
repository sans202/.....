package com.example.lightweightyoutube.data.model

import com.google.gson.annotations.SerializedName

// --- Data models for Invidious API responses ---
// These classes are designed to map JSON responses from various Invidious API endpoints.
// Based on Invidious API documentation (e.g., /api/v1/search, /api/v1/videos/:videoId, etc.)

/**
 * Represents an item in a search result list from the Invidious API (`/api/v1/search`).
 * This can be a video, playlist, or channel. The fields are a union of possible properties.
 *
 * @property type The type of the search item (e.g., "video", "playlist", "channel").
 * @property title The title of the item.
 * @property videoId The unique ID of the video (if type is "video").
 * @property author The author or channel name.
 * @property authorId The ID of the author or channel.
 * @property authorUrl Relative URL to the author's or channel's page.
 * @property videoThumbnails List of available thumbnails for the video.
 * @property description A short description of the video (if type is "video").
 * @property descriptionHtml HTML version of the description.
 * @property publishedText Human-readable text for when the video was published (e.g., "2 weeks ago").
 * @property lengthSeconds Duration of the video in seconds.
 * @property viewCount Number of views for the video.
 * @property liveNow Indicates if the video is currently live.
 * @property premium Indicates if the video is a premium content.
 * @property isUpcoming Indicates if the video is an upcoming premiere.
 */
data class InvidiousSearchItem(
    val type: String,
    val title: String?,
    @SerializedName("videoId") val videoId: String?,
    val author: String?,
    @SerializedName("authorId") val authorId: String?,
    @SerializedName("authorUrl") val authorUrl: String?,
    @SerializedName("videoThumbnails") val videoThumbnails: List<VideoThumbnail>?,
    val description: String?,
    @SerializedName("descriptionHtml") val descriptionHtml: String?,
    @SerializedName("publishedText") val publishedText: String?,
    @SerializedName("lengthSeconds") val lengthSeconds: Int?,
    @SerializedName("viewCount") val viewCount: Long?,
    @SerializedName("liveNow") val liveNow: Boolean?,
    @SerializedName("premium") val premium: Boolean?,
    @SerializedName("isUpcoming") val isUpcoming: Boolean?
)

/**
 * Represents a single video thumbnail with its quality, URL, and dimensions.
 * @property quality A string indicating the quality of the thumbnail (e.g., "hqdefault", "mqdefault").
 * @property url The URL of the thumbnail image.
 * @property width The width of the thumbnail in pixels.
 * @property height The height of the thumbnail in pixels.
 */
data class VideoThumbnail(
    val quality: String,
    val url: String,
    val width: Int,
    val height: Int
)

/**
 * Represents detailed information about a specific video from the Invidious API (`/api/v1/videos/:videoId`).
 *
 * @property type Type of the content, usually "video".
 * @property title The title of the video.
 * @property videoId The unique ID of the video.
 * @property videoThumbnails List of available [VideoThumbnail]s for the video.
 * @property description The full description of the video.
 * @property descriptionHtml HTML version of the full description.
 * @property published Unix timestamp of when the video was published.
 * @property publishedText Human-readable text for the publication date.
 * @property keywords List of keywords or tags associated with the video.
 * @property viewCount Total number of views.
 * @property likeCount Number of likes.
 * @property dislikeCount Number of dislikes.
 * @property liveNow True if the video is currently live.
 * @property paid True if the video is behind a paywall.
 * @property premium True if the video is YouTube Premium content.
 * @property isFamilyFriendly True if the video is marked as family-friendly.
 * @property allowedRegions List of region codes where the video is allowed.
 * @property genre Genre of the video.
 * @property blacklisted True if the video is blacklisted by the Invidious instance.
 * @property author The name of the video's author/channel.
 * @property authorId The ID of the author/channel.
 * @property authorUrl Relative URL to the author's channel page.
 * @property authorThumbnails List of thumbnails for the author/channel.
 * @property subCountText Human-readable subscriber count for the author.
 * @property lengthSeconds Duration of the video in seconds.
 * @property allowRatings True if ratings (likes/dislikes) are allowed for this video.
 * @property rating Average rating of the video.
 * @property isListed True if the video is publicly listed.
 * @property commentsNextpage Token for fetching the next page of comments, if available.
 * @property formatStreams List of non-adaptive, direct video file streams (often lower quality).
 * @property adaptiveFormats List of [AdaptiveFormat]s, including separate video and audio streams of various qualities.
 * @property recommendedVideos List of recommended videos related to this one.
 */
data class InvidiousVideo(
    val type: String,
    val title: String,
    @SerializedName("videoId") val videoId: String,
    @SerializedName("videoThumbnails") val videoThumbnails: List<VideoThumbnail>,
    val description: String,
    @SerializedName("descriptionHtml") val descriptionHtml: String,
    @SerializedName("published") val published: Long,
    @SerializedName("publishedText") val publishedText: String?,
    val keywords: List<String>,
    @SerializedName("viewCount") val viewCount: Long,
    @SerializedName("likeCount") val likeCount: Int,
    @SerializedName("dislikeCount") val dislikeCount: Int,
    @SerializedName("liveNow") val liveNow: Boolean,
    @SerializedName("paid") val paid: Boolean,
    @SerializedName("premium") val premium: Boolean,
    @SerializedName("isFamilyFriendly") val isFamilyFriendly: Boolean,
    @SerializedName("allowedRegions") val allowedRegions: List<String>,
    val genre: String,
    val blacklisted: Boolean,
    val author: String,
    @SerializedName("authorId") val authorId: String,
    @SerializedName("authorUrl") val authorUrl: String,
    @SerializedName("authorThumbnails") val authorThumbnails: List<AuthorThumbnail>,
    @SerializedName("subCountText") val subCountText: String?,
    @SerializedName("lengthSeconds") val lengthSeconds: Int,
    @SerializedName("allowRatings") val allowRatings: Boolean,
    val rating: Float,
    @SerializedName("isListed") val isListed: Boolean,
    @SerializedName("commentsNextpage") val commentsNextpage: String?,
    @SerializedName("formatStreams") val formatStreams: List<FormatStream>?,
    @SerializedName("adaptiveFormats") val adaptiveFormats: List<AdaptiveFormat>,
    @SerializedName("recommendedVideos") val recommendedVideos: List<RecommendedVideo>
)

/**
 * Represents a thumbnail for an author/channel.
 * @property url The URL of the thumbnail image.
 * @property width The width of the thumbnail in pixels.
 * @property height The height of the thumbnail in pixels.
 */
data class AuthorThumbnail(
    val url: String,
    val width: Int,
    val height: Int
)

/**
 * Represents an adaptive stream format (video or audio) for a video.
 * These are typically used with DASH or HLS manifests for adaptive bitrate streaming.
 *
 * @property index Often corresponds to an internal index or itag.
 * @property bitrate Approximate bitrate of the stream in bits per second.
 * @property init Initialization segment URL or range for the stream.
 * @property url The URL of the media stream segment. This is the primary URL for playback/download.
 * @property itag YouTube-specific itag identifier for the format.
 * @property type MIME type of the stream (e.g., "video/webm; codecs=\"vp9\"").
 * @property clen Content length in bytes.
 * @property lmt Last modified timestamp.
 * @property projectionType Type of video projection (e.g., for 360° videos).
 * @property width Width of the video in pixels (for video streams).
 * @property height Height of the video in pixels (for video streams).
 * @property quality Human-readable quality label (e.g., "1080p", "720p").
 * @property fps Frames per second (for video streams).
 * @property container Media container format (e.g., "mp4", "webm").
 * @property encoding Video or audio encoding (e.g., "H.264", "vp9", "opus").
 * @property resolution Resolution string (e.g., "1920x1080") (for video streams).
 * @property audioSampleRate Sample rate of the audio in Hz (for audio streams).
 * @property audioChannels Number of audio channels (for audio streams).
 * @property averageBitrate Average bitrate of the audio stream in bits per second (for audio streams).
 */
data class AdaptiveFormat(
    val index: String?,
    val bitrate: String?,
    val init: String?,
    val url: String,
    val itag: String?,
    val type: String,
    val clen: String?,
    val lmt: String?,
    @SerializedName("projection_type") val projectionType: Int?,
    val width: Int?,
    val height: Int?,
    val quality: String?,
    val fps: Int?,
    val container: String?,
    val encoding: String?,
    val resolution: String?,
    @SerializedName("audioSampleRate") val audioSampleRate: String?,
    @SerializedName("audioChannels") val audioChannels: Int?,
    @SerializedName("averageBitrate") val averageBitrate: Int?
)

/**
 * Represents a non-adaptive, direct video file stream.
 * These are typically complete files of a specific quality, often lower than adaptive streams.
 *
 * @property url The direct URL to the video file.
 * @property itag YouTube-specific itag identifier.
 * @property type MIME type of the stream.
 * @property container Media container format (e.g., "mp4").
 * @property quality Quality label (e.g., "360p").
 * @property resolution Resolution string (e.g., "640x360").
 * @property encoding Video encoding (e.g., "H.264").
 * @property size Human-readable file size (e.g., "20.5MiB"). Needs parsing if used numerically.
 * @property fps Frames per second.
 */
data class FormatStream(
    val url: String,
    val itag: String,
    val type: String,
    val container: String,
    val quality: String,
    val resolution: String?,
    val encoding: String?,
    val size: String?,
    val fps: Int?
)

/**
 * Represents a recommended video, often shown alongside or after a video.
 * @property videoId The ID of the recommended video.
 * @property title The title of the recommended video.
 * @property videoThumbnails List of thumbnails for the recommended video.
 * @property author The author of the recommended video.
 * @property lengthSeconds Duration of the recommended video in seconds.
 * @property viewCountText Human-readable view count (e.g., "1M views").
 */
data class RecommendedVideo(
    @SerializedName("videoId") val videoId: String,
    val title: String,
    @SerializedName("videoThumbnails") val videoThumbnails: List<VideoThumbnail>,
    val author: String,
    @SerializedName("lengthSeconds") val lengthSeconds: Int,
    @SerializedName("viewCountText") val viewCountText: String?
)


/**
 * Represents a collection of comments for a video from the Invidious API (`/api/v1/comments/:videoId`).
 * @property commentCount Total number of comments for the video.
 * @property videoId The ID of the video these comments belong to.
 * @property comments List of [InvidiousComment] objects.
 * @property nextPageToken Token for fetching the next page of comments, if available.
 */
data class InvidiousComments(
    @SerializedName("commentCount") val commentCount: Int?,
    @SerializedName("videoId") val videoId: String,
    val comments: List<InvidiousComment>,
    @SerializedName("nextpage") val nextPageToken: String?
)

/**
 * Represents a single comment on a video.
 *
 * @property author The name of the comment's author.
 * @property authorThumbnails List of thumbnails for the comment author.
 * @property authorId The ID of the comment author.
 * @property authorUrl Relative URL to the author's channel page.
 * @property isEdited True if the comment has been edited.
 * @property isPinned True if the comment is pinned by the video creator.
 * @property content Raw text content of the comment (may differ from HTML version).
 * @property contentHtml HTML content of the comment, suitable for rendering.
 * @property published Unix timestamp of when the comment was published.
 * @property publishedText Human-readable text for the publication time (e.g., "1 day ago").
 * @property likeCount Number of likes for the comment.
 * @property commentId The unique ID of the comment.
 * @property authorIsChannelOwner True if the comment author is the owner of the video's channel.
 * @property creatorHeart Information about a "heart" reaction from the video creator, if present.
 * @property replies Information about replies to this comment, including a token to fetch them.
 */
data class InvidiousComment(
    val author: String,
    @SerializedName("authorThumbnails") val authorThumbnails: List<AuthorThumbnail>,
    @SerializedName("authorId") val authorId: String,
    @SerializedName("authorUrl") val authorUrl: String,
    @SerializedName("isEdited") val isEdited: Boolean,
    @SerializedName("isPinned") val isPinned: Boolean,
    val content: String,
    @SerializedName("contentHtml") val contentHtml: String,
    @SerializedName("published") val published: Long,
    @SerializedName("publishedText") val publishedText: String?,
    @SerializedName("likeCount") val likeCount: Int,
    @SerializedName("commentId") val commentId: String,
    @SerializedName("authorIsChannelOwner") val authorIsChannelOwner: Boolean,
    @SerializedName("creatorHeart") val creatorHeart: CreatorHeart?,
    val replies: CommentReplies?
)

/**
 * Represents a "heart" reaction from a video creator on a comment.
 * @property creatorThumbnail URL of the creator's thumbnail who gave the heart.
 * @property creatorName Name of the creator.
 */
data class CreatorHeart(
    @SerializedName("creatorThumbnail") val creatorThumbnail: String,
    @SerializedName("creatorName") val creatorName: String
)

/**
 * Contains information for fetching replies to a comment.
 * @property token The continuation token required to fetch the replies.
 * @property commentId The ID of the parent comment these replies belong to.
 */
data class CommentReplies(
    @SerializedName("token") val token: String,
    @SerializedName("commentId") val commentId: String
)


/**
 * Represents statistics for an Invidious instance (`/api/v1/stats`).
 * Useful for checking instance health, version, and usage.
 *
 * @property version The version string of the Invidious software.
 * @property software Detailed information about the Invidious software.
 * @property openRegistrations True if user registrations are open on this instance.
 * @property usage Statistics about user activity on the instance.
 */
data class InvidiousStats(
    val version: String,
    val software: SoftwareStats,
    @SerializedName("openRegistrations") val openRegistrations: Boolean,
    val usage: UsageStats
) {
    /** Contains details about the Invidious software running on the instance. */
    data class SoftwareStats(
        val name: String,
        val version: String,
        val branch: String
    )
    /** Contains user activity statistics for the instance. */
    data class UsageStats(
        val users: UserUsageStats
    ) {
        /** Specific user counts (total, active in last half-year, active in last month). */
        data class UserUsageStats(
            val total: Int,
            @SerializedName("activeHalfyear") val activeHalfyear: Int,
            @SerializedName("activeMonth") val activeMonth: Int
        )
    }
}

/**
 * Represents an Invidious instance and its status, used internally by the app
 * for instance selection and health checking. This is not directly from the API
 * but is constructed by the app.
 *
 * @property domain The base domain URL of the Invidious instance (e.g., "https://yewtu.be").
 * @property isOnline True if the instance is reachable (network-wise).
 * @property apiWorks True if the instance's API (specifically `/api/v1/stats`) is responsive and returns valid data.
 * @property latency The response time in milliseconds for the health check (e.g., to `/api/v1/stats`). -1 if not measured.
 */
data class InvidiousInstance(
    val domain: String,
    var isOnline: Boolean = false,
    var apiWorks: Boolean = false,
    var latency: Long = -1L
)
