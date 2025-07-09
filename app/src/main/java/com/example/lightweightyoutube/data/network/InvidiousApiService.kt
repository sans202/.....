package com.example.lightweightyoutube.data.network

import com.example.lightweightyoutube.data.model.InvidiousComments
import com.example.lightweightyoutube.data.model.InvidiousSearchItem
import com.example.lightweightyoutube.data.model.InvidiousStats
import com.example.lightweightyoutube.data.model.InvidiousVideo
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service interface for interacting with the Invidious API.
 * Defines endpoints for searching videos, fetching video details, comments, and instance statistics.
 * Base URL for these relative paths is provided by [RetrofitClient].
 */
interface InvidiousApiService {

    /**
     * Searches for videos on an Invidious instance.
     * @param query The search query string.
     * @param type The type of content to search for (e.g., "video", "playlist", "channel", "all"). Defaults to "video".
     * @param sortBy Sorting criteria (e.g., "relevance", "rating", "upload_date", "view_count"). Defaults to "relevance".
     * @param page Page number for pagination (optional).
     * @param region Region code for regional results (e.g., "US"). Defaults to "US".
     * @param fields Comma-separated list of fields to include in the response, optimizing data transfer.
     * @return A Retrofit [Response] wrapping a list of [InvidiousSearchItem]s.
     */
    @GET("api/v1/search")
    suspend fun searchVideos(
        @Query("q") query: String,
        @Query("type") type: String = "video",
        @Query("sort_by") sortBy: String = "relevance",
        @Query("page") page: Int? = null,
        @Query("region") region: String = "US",
        @Query("fields") fields: String = "type,title,videoId,author,videoThumbnails,publishedText,lengthSeconds,viewCount"
    ): Response<List<InvidiousSearchItem>>

    /**
     * Fetches detailed information for a specific video.
     * @param videoId The ID of the video.
     * @param fields Comma-separated list of fields to include in the response.
     * @return A Retrofit [Response] wrapping an [InvidiousVideo] object.
     */
    @GET("api/v1/videos/{videoId}")
    suspend fun getVideoDetails(
        @Path("videoId") videoId: String,
        @Query("fields") fields: String = "type,title,videoId,videoThumbnails,description,published,publishedText,keywords,viewCount,likeCount,dislikeCount,author,authorId,authorThumbnails,lengthSeconds,adaptiveFormats,formatStreams,allowRatings,rating,isListed"
    ): Response<InvidiousVideo>

    /**
     * Fetches comments for a specific video.
     * @param videoId The ID of the video.
     * @param continuation Token for fetching the next page of comments (optional).
     * @param fields Comma-separated list of fields to include in the response.
     * @return A Retrofit [Response] wrapping an [InvidiousComments] object.
     */
    @GET("api/v1/comments/{videoId}")
    suspend fun getComments(
        @Path("videoId") videoId: String,
        @Query("continuation") continuation: String? = null,
        @Query("fields") fields: String = "commentCount,videoId,comments,nextpage,author,authorThumbnails,authorId,isEdited,contentHtml,publishedText,likeCount,commentId,authorIsChannelOwner,creatorHeart"
    ): Response<InvidiousComments>

    /**
     * Fetches statistics for the Invidious instance.
     * Useful for checking instance health and version.
     * @return A Retrofit [Response] wrapping an [InvidiousStats] object.
     */
    @GET("api/v1/stats")
    suspend fun getInstanceStats(): Response<InvidiousStats>
}
