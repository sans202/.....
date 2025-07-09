package com.example.lightweightyoutube.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.lightweightyoutube.data.model.InvidiousInstance
import com.example.lightweightyoutube.data.model.DownloadedVideoInfo

/**
 * Manages application preferences related to Invidious instances and downloaded videos
 * using [SharedPreferences].
 *
 * @param context The application context.
 */
class InvidiousPreferences(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson() // For serializing/deserializing lists of objects

    /**
     * The URL of the Invidious instance currently selected by the user.
     * Null if no instance has been explicitly selected (app will use a default).
     * URLs are stored without a trailing slash.
     */
    var selectedInstanceUrl: String?
        get() = prefs.getString(KEY_SELECTED_INSTANCE_URL, null)
        set(value) {
            prefs.edit().putString(KEY_SELECTED_INSTANCE_URL, value?.trimEnd('/')).apply()
        }

    /**
     * A list of known Invidious instances, including their domain and health status.
     * Stored as a JSON string in SharedPreferences.
     * If no list is stored, returns a list derived from [DEFAULT_INSTANCES_DOMAINS].
     */
    var knownInstances: List<InvidiousInstance>
        get() {
            val json = prefs.getString(KEY_KNOWN_INSTANCES_LIST, null)
            return if (json != null) {
                val type = object : TypeToken<List<InvidiousInstance>>() {}.type
                try { gson.fromJson(json, type) } catch (e: Exception) { getDefaultInstancesList() }
            } else {
                getDefaultInstancesList()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_KNOWN_INSTANCES_LIST, json).apply()
        }

    private fun getDefaultInstancesList(): List<InvidiousInstance> {
        return DEFAULT_INSTANCES_DOMAINS.map { InvidiousInstance(domain = it) }
    }

    /**
     * Timestamp (in milliseconds) of when the list of known Invidious instances
     * was last refreshed or checked.
     */
    var lastInstanceRefreshTime: Long
        get() = prefs.getLong(KEY_LAST_INSTANCE_REFRESH_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_INSTANCE_REFRESH_TIME, value).apply()

    /** Clears all preferences managed by this class. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "invidious_prefs"
        private const val KEY_SELECTED_INSTANCE_URL = "selected_instance_url"
        private const val KEY_KNOWN_INSTANCES_LIST = "known_instances_list"
        private const val KEY_LAST_INSTANCE_REFRESH_TIME = "last_instance_refresh_time"
        private const val KEY_DOWNLOADED_VIDEOS_LIST = "downloaded_videos_list"

        /**
         * Initial default list of Invidious instance domains.
         * This list is used if no instances are stored in preferences or as a fallback.
         * Users should be encouraged to verify and select instances that work best for them.
         */
        val DEFAULT_INSTANCES_DOMAINS = listOf(
            "https://yewtu.be",
            "https://inv.nadeko.net",
            "https://invidious.nerdvpn.de",
            "https://invidious.projectsegfau.lt", // Added some more common ones
            "https://vid.puffyan.us",
            "https://invidious.privacydev.net",
            // "https://invidious.fdn.fr", // Check if API is enabled
            // "https://invidious.kavin.rocks", // Often has API disabled or rate limited
            "https://invidious.lunar.computer"
            // Note: Instance availability and API status can change frequently.
        ).distinct() // Ensure no duplicates from manual editing

        /**
         * Helper to get the default Invidious instance URL.
         * @return The URL of the first instance in [DEFAULT_INSTANCES_DOMAINS] or a hardcoded fallback.
         */
        fun getDefaultInstanceUrl(): String {
            return DEFAULT_INSTANCES_DOMAINS.firstOrNull() ?: "https://yewtu.be" // Hardcoded ultimate fallback
        }
    }

    // --- Downloaded Videos Management ---
    private val downloadListType = object : TypeToken<MutableList<DownloadedVideoInfo>>() {}.type

    /**
     * A list of [DownloadedVideoInfo] objects representing videos downloaded by the user.
     * Stored as a JSON string in SharedPreferences.
     */
    var downloadedVideos: MutableList<DownloadedVideoInfo>
        get() {
            val json = prefs.getString(KEY_DOWNLOADED_VIDEOS_LIST, null)
            return if (json != null) {
                try {
                    gson.fromJson(json, downloadListType) ?: mutableListOf()
                } catch (e: Exception) { // Handle potential deserialization errors (e.g., if model changed)
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
        }
        set(value) {
            val json = gson.toJson(value)
            prefs.edit().putString(KEY_DOWNLOADED_VIDEOS_LIST, json).apply()
        }

    /**
     * Adds a new downloaded video's information to the stored list,
     * or updates an existing entry if found by [DownloadedVideoInfo.downloadManagerId] or [DownloadedVideoInfo.invidiousVideoId].
     * New entries are added to the beginning of the list.
     *
     * @param videoInfo The [DownloadedVideoInfo] to add or update.
     */
    fun addOrUpdateDownloadedVideo(videoInfo: DownloadedVideoInfo) {
        val currentList = downloadedVideos
        val existingIndex = currentList.indexOfFirst {
            it.downloadManagerId == videoInfo.downloadManagerId ||
            (it.invidiousVideoId == videoInfo.invidiousVideoId && it.qualityLabel == videoInfo.qualityLabel) // More specific update
        }
        if (existingIndex != -1) {
            currentList[existingIndex] = videoInfo
        } else {
            currentList.add(0, videoInfo) // Add new downloads to the top for recent visibility
        }
        downloadedVideos = currentList // Setter saves the updated list
    }

    /**
     * Retrieves [DownloadedVideoInfo] for a given DownloadManager ID.
     * @param id The DownloadManager ID of the download.
     * @return The [DownloadedVideoInfo] if found, otherwise null.
     */
    fun getDownloadedVideoByManagerId(id: Long): DownloadedVideoInfo? {
        return downloadedVideos.find { it.downloadManagerId == id }
    }

    /**
     * Retrieves [DownloadedVideoInfo] for a given Invidious video ID.
     * Note: This might return the first match if multiple qualities of the same video are downloaded.
     * @param invidiousId The Invidious video ID.
     * @return The [DownloadedVideoInfo] if found, otherwise null.
     */
    fun getDownloadedVideoByInvidiousId(invidiousId: String): DownloadedVideoInfo? {
        return downloadedVideos.find { it.invidiousVideoId == invidiousId }
    }

    /**
     * Removes a downloaded video's information from the list based on its DownloadManager ID.
     * @param downloadManagerId The DownloadManager ID of the video to remove.
     * @return True if an item was removed, false otherwise.
     */
    fun removeDownloadedVideo(downloadManagerId: Long): Boolean {
        val currentList = downloadedVideos
        val removed = currentList.removeAll { it.downloadManagerId == downloadManagerId }
        if (removed) {
            downloadedVideos = currentList
        }
        return removed
    }

    /**
     * Removes all downloaded video entries associated with a specific Invidious video ID.
     * Useful if a video is deleted and all its downloaded qualities should be removed.
     * @param invidiousId The Invidious video ID.
     * @return True if any item was removed, false otherwise.
     */
     fun removeDownloadedVideoByInvidiousId(invidiousId: String): Boolean {
        val currentList = downloadedVideos
        val removed = currentList.removeAll { it.invidiousVideoId == invidiousId }
        if (removed) {
            downloadedVideos = currentList
        }
        return removed
    }
}
