package com.example.lightweightyoutube.ui.saved

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lightweightyoutube.data.model.DownloadedVideoInfo
import com.example.lightweightyoutube.data.model.DownloadStatus
import com.example.lightweightyoutube.data.preferences.InvidiousPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for the Saved Videos screen.
 * Manages loading the list of downloaded videos and handling their deletion.
 *
 * @param application The application context.
 */
class SavedViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = InvidiousPreferences(application)
    private val downloadManager = application.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _downloadedVideos = MutableLiveData<List<DownloadedVideoInfo>>()
    /** LiveData holding the list of successfully downloaded videos whose files exist. */
    val downloadedVideos: LiveData<List<DownloadedVideoInfo>> = _downloadedVideos

    private val _toastMessage = MutableLiveData<String?>()
    /** LiveData for posting messages to be shown as Toasts (e.g., success/failure of deletion). Null if no message. */
    val toastMessage: LiveData<String?> = _toastMessage

    private val _isEmpty = MutableLiveData<Boolean>()
    /** LiveData indicating if the list of downloaded videos is currently empty. */
    val isEmpty: LiveData<Boolean> = _isEmpty


    companion object {
        private const val TAG = "SavedViewModel"
    }

    /**
     * Loads the list of downloaded videos from preferences.
     * Filters for videos that are marked as [DownloadStatus.COMPLETED], have a valid local file path,
     * and whose corresponding file actually exists on the disk.
     * Updates [_downloadedVideos] and [_isEmpty] LiveData.
     */
    fun loadDownloadedVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            val allDownloads = prefs.downloadedVideos
            val completedDownloads = allDownloads.filter {
                it.status == DownloadStatus.COMPLETED &&
                !it.localFilePath.isNullOrBlank() &&
                try { File(it.localFilePath!!).exists() } catch (e: Exception) {
                    Log.w(TAG, "Error checking file existence for ${it.localFilePath}: ${e.message}")
                    false
                }
            }
            withContext(Dispatchers.Main) {
                _downloadedVideos.value = completedDownloads
                _isEmpty.value = completedDownloads.isEmpty()
            }
        }
    }

    /**
     * Deletes a downloaded video.
     * This involves:
     * 1. Deleting the actual video file from storage.
     * 2. Removing the download task from Android's [DownloadManager] (if still present).
     * 3. Removing the video's metadata record from app preferences.
     * Posts messages to [_toastMessage] LiveData indicating the outcome.
     * Refreshes the list of downloaded videos upon successful deletion.
     *
     * @param videoInfo The [DownloadedVideoInfo] object representing the video to be deleted.
     */
    fun deleteDownloadedVideo(videoInfo: DownloadedVideoInfo) {
        viewModelScope.launch {
            var fileActionSuccessful = false

            if (!videoInfo.localFilePath.isNullOrBlank()) {
                try {
                    val file = File(videoInfo.localFilePath!!)
                     if (file.exists()) {
                        if (withContext(Dispatchers.IO) { file.delete() }) {
                            Log.d(TAG, "Successfully deleted file: ${videoInfo.localFilePath}")
                            fileActionSuccessful = true
                        } else {
                            Log.e(TAG, "Failed to delete file: ${videoInfo.localFilePath}")
                            _toastMessage.postValue("Failed to delete video file.")
                        }
                    } else {
                         Log.w(TAG, "File not found for deletion, but record exists: ${videoInfo.localFilePath}")
                         fileActionSuccessful = true
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security error deleting file ${videoInfo.localFilePath}", e)
                    _toastMessage.postValue("Permission error deleting file.")
                }
                catch (e: Exception) {
                    Log.e(TAG, "Error deleting file ${videoInfo.localFilePath}", e)
                    _toastMessage.postValue("Error deleting video file.")
                }
            } else {
                Log.w(TAG, "No local file path for video to delete: ${videoInfo.title}. Removing record only.")
                fileActionSuccessful = true;
            }

            if (fileActionSuccessful) {
                try {
                    val numRemovedFromDM = downloadManager.remove(videoInfo.downloadManagerId)
                    Log.d(TAG, "Removed $numRemovedFromDM task(s) from DownloadManager for ID: ${videoInfo.downloadManagerId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Error removing download from DownloadManager for ID: ${videoInfo.downloadManagerId}", e)
                }

                val successfullyRemovedFromPrefs = prefs.removeDownloadedVideo(videoInfo.downloadManagerId)
                 if(successfullyRemovedFromPrefs) {
                    _toastMessage.postValue("Video '${videoInfo.title}' deleted.")
                    loadDownloadedVideos()
                } else {
                     _toastMessage.postValue("Video file handled, but failed to remove app record.")
                     Log.e(TAG, "File handled but failed to remove record from prefs for DM ID: ${videoInfo.downloadManagerId}")
                     loadDownloadedVideos()
                }
            }
        }
    }

    /**
     * Call this method after a toast message (from [_toastMessage] LiveData) has been displayed,
     * to clear the message state and prevent it from being shown again on configuration changes.
     */
    fun userShownToast() {
        _toastMessage.value = null
    }
}
