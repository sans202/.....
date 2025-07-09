package com.example.lightweightyoutube.data.repository

import android.content.Context
import com.example.lightweightyoutube.data.model.InvidiousComments
import com.example.lightweightyoutube.data.model.InvidiousInstance
import com.example.lightweightyoutube.data.model.InvidiousSearchItem
import com.example.lightweightyoutube.data.model.InvidiousVideo
import com.example.lightweightyoutube.data.network.InvidiousApiService
import com.example.lightweightyoutube.data.network.RetrofitClient
import com.example.lightweightyoutube.data.preferences.InvidiousPreferences
import com.example.lightweightyoutube.data.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * A generic wrapper class for data that can represent loading, success, or error states.
 * @param T The type of the data.
 * @property data The actual data, present in case of success. Null otherwise or during loading.
 * @property message An optional error message, present in case of an error.
 */
sealed class Resource<T>(val data: T? = null, val message: String? = null) {
    /** Represents a successful data retrieval. */
    class Success<T>(data: T) : Resource<T>(data)
    /** Represents an error during data retrieval. */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}

/**
 * Repository class for handling all video-related data operations.
 * It abstracts the data sources (network, local preferences) from the ViewModels.
 *
 * @param context The application context, used for accessing SharedPreferences and network services.
 */
class VideoRepository(private val context: Context) {

    private val applicationContext = context.applicationContext
    private val invidiousPreferences = InvidiousPreferences(applicationContext)

    /** Returns an [InvidiousApiService] instance configured with the currently selected Invidious instance URL. */
    private fun getApiService(): InvidiousApiService {
        return RetrofitClient.getApiService(applicationContext)
    }

    /** Returns an [InvidiousApiService] instance for a specific Invidious instance URL (e.g., for testing). */
    private fun getApiServiceForInstance(instanceUrl: String): InvidiousApiService {
        return RetrofitClient.getApiServiceForInstance(instanceUrl)
    }

    /**
     * Searches for videos on the selected Invidious instance.
     *
     * @param query The search query string.
     * @param page Optional page number for pagination (not fully implemented for pagination UI yet).
     * @return A [Resource] wrapping a list of [InvidiousSearchItem]s or an error.
     */
    suspend fun searchVideos(query: String, page: Int? = null): Resource<List<InvidiousSearchItem>> {
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return Resource.Error("No internet connection.")
        }
        return withContext(Dispatchers.IO) { // Perform network call on IO dispatcher
            try {
                val response = getApiService().searchVideos(query = query, page = page)
                if (response.isSuccessful) {
                    Resource.Success(response.body() ?: emptyList())
                } else {
                    Resource.Error("API Error: ${response.code()} - ${response.message()} on ${getSelectedInstance()}")
                }
            } catch (e: IOException) { // Network-related errors
                Resource.Error("Network Error: ${e.message ?: "Check connection"} on ${getSelectedInstance()}")
            } catch (e: Exception) { // Other errors (e.g., JSON parsing)
                Resource.Error("Error: ${e.message ?: "An unknown error occurred"} on ${getSelectedInstance()}")
            }
        }
    }

    /**
     * Fetches detailed information for a specific video.
     *
     * @param videoId The ID of the video to fetch.
     * @return A [Resource] wrapping an [InvidiousVideo] object or an error.
     */
    suspend fun getVideoDetails(videoId: String): Resource<InvidiousVideo> {
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return Resource.Error("No internet connection.")
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = getApiService().getVideoDetails(videoId = videoId)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error("API Error: ${response.code()} - ${response.message()} on ${getSelectedInstance()}")
                }
            } catch (e: IOException) {
                Resource.Error("Network Error: ${e.message ?: "Check connection"} on ${getSelectedInstance()}")
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message ?: "An unknown error occurred"} on ${getSelectedInstance()}")
            }
        }
    }

    /**
     * Fetches comments for a specific video.
     *
     * @param videoId The ID of the video for which to fetch comments.
     * @param continuation Optional token for comment pagination (not fully implemented for UI yet).
     * @return A [Resource] wrapping an [InvidiousComments] object or an error.
     */
    suspend fun getComments(videoId: String, continuation: String? = null): Resource<InvidiousComments> {
        if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return Resource.Error("No internet connection.")
        }
        return withContext(Dispatchers.IO) {
            try {
                val response = getApiService().getComments(videoId = videoId, continuation = continuation)
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(response.body()!!)
                } else {
                    Resource.Error("API Error: ${response.code()} - ${response.message()} on ${getSelectedInstance()}")
                }
            } catch (e: IOException) {
                Resource.Error("Network Error: ${e.message ?: "Check connection"} on ${getSelectedInstance()}")
            } catch (e: Exception) {
                Resource.Error("Error: ${e.message ?: "An unknown error occurred"} on ${getSelectedInstance()}")
            }
        }
    }

    /**
     * Checks the health and availability of a specific Invidious instance by fetching its stats.
     *
     * @param instanceUrl The base URL of the Invidious instance to check.
     * @return A [Resource] wrapping an [InvidiousInstance] object with updated health status, or an error.
     */
    suspend fun checkInstanceHealth(instanceUrl: String): Resource<InvidiousInstance> {
         if (!NetworkUtils.isNetworkAvailable(applicationContext)) {
            return Resource.Error(
                "No internet connection.",
                InvidiousInstance(domain = instanceUrl, isOnline = false, apiWorks = false, latency = -1L)
            )
        }
        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val service = getApiServiceForInstance(instanceUrl.trim())
                val response = service.getInstanceStats()
                val latency = System.currentTimeMillis() - startTime
                if (response.isSuccessful && response.body() != null) {
                    Resource.Success(
                        InvidiousInstance(
                            domain = instanceUrl.trim(),
                            isOnline = true,
                            apiWorks = true,
                            latency = latency
                        )
                    )
                } else {
                     Resource.Error(
                        "API Error: ${response.code()}", // Provide response code for better diagnostics
                        InvidiousInstance(domain = instanceUrl.trim(), isOnline = true, apiWorks = false, latency = latency)
                    )
                }
            } catch (e: IOException) { // Network errors
                 Resource.Error(
                    "Network Error: ${e.message}",
                    InvidiousInstance(domain = instanceUrl.trim(), isOnline = false, apiWorks = false, latency = System.currentTimeMillis() - startTime)
                )
            } catch (e: Exception) { // Other errors like SSL, parsing, etc.
                 Resource.Error(
                    "Instance Unreachable or Invalid: ${e.javaClass.simpleName}",
                    InvidiousInstance(domain = instanceUrl.trim(), isOnline = false, apiWorks = false, latency = System.currentTimeMillis() - startTime)
                )
            }
        }
    }

    /** Retrieves the list of known Invidious instances stored in preferences. */
    fun getStoredInstances(): List<InvidiousInstance> {
        return invidiousPreferences.knownInstances
    }

    /**
     * Saves a list of Invidious instances to preferences and updates the last refresh time.
     * @param instances The list of [InvidiousInstance]s to save.
     */
    fun saveInstances(instances: List<InvidiousInstance>) {
        invidiousPreferences.knownInstances = instances
        invidiousPreferences.lastInstanceRefreshTime = System.currentTimeMillis()
    }

    /**
     * Gets the currently selected Invidious instance URL from preferences.
     * Returns a default instance URL if none is selected.
     */
    fun getSelectedInstance(): String {
        return invidiousPreferences.selectedInstanceUrl ?: InvidiousPreferences.getDefaultInstanceUrl()
    }

    /**
     * Sets the selected Invidious instance URL in preferences.
     * @param url The base URL of the Invidious instance to select.
     */
    fun setSelectedInstance(url: String) {
        invidiousPreferences.selectedInstanceUrl = url.trimEnd('/') // Ensure no trailing slash for consistency
    }

    /** Checks if an Invidious instance has been explicitly selected by the user. */
    fun isInstanceSelected(): Boolean {
        return invidiousPreferences.selectedInstanceUrl != null
    }

    /** Gets the timestamp of when the Invidious instance list was last refreshed. */
    fun getLastInstanceRefreshTime(): Long {
        return invidiousPreferences.lastInstanceRefreshTime
    }

    /** Gets the default list of Invidious instance domains. */
    fun getDefaultInstanceDomains(): List<String> {
        return InvidiousPreferences.DEFAULT_INSTANCES_DOMAINS
    }
}
