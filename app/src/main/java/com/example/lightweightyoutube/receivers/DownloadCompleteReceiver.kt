package com.example.lightweightyoutube.receivers

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.lightweightyoutube.MainActivity
import com.example.lightweightyoutube.data.model.DownloadStatus
import com.example.lightweightyoutube.data.preferences.InvidiousPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * A [BroadcastReceiver] that listens for system broadcasts related to downloads managed by [DownloadManager].
 * It specifically handles:
 * - [DownloadManager.ACTION_DOWNLOAD_COMPLETE]: To update the status of completed or failed downloads.
 * - [DownloadManager.ACTION_NOTIFICATION_CLICKED]: To potentially open the app when a download notification is clicked.
 */
class DownloadCompleteReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "DownloadReceiver" // Logging tag
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Context or Intent is null, cannot process broadcast.")
            return
        }

        val action = intent.action
        // EXTRA_DOWNLOAD_ID is always present for ACTION_DOWNLOAD_COMPLETE.
        // For ACTION_NOTIFICATION_CLICKED, EXTRA_NOTIFICATION_CLICK_DOWNLOAD_IDS is an array.
        // We primarily care about single download completion here.
        val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)

        Log.d(TAG, "Received action: $action for download ID: $downloadId")


        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
            if (downloadId == -1L) {
                Log.e(TAG, "ACTION_DOWNLOAD_COMPLETE received with no valid Download ID.")
                return
            }

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val query = DownloadManager.Query().setFilterById(downloadId)

            val cursor = downloadManager.query(query)
            cursor?.use { // Ensures cursor is closed automatically
                if (it.moveToFirst()) {
                    val statusColumn = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val localUriColumn = it.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                    val totalSizeColumn = it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val reasonColumn = it.getColumnIndex(DownloadManager.COLUMN_REASON)

                    // Retrieve download details from cursor
                    val status = if (statusColumn != -1) it.getInt(statusColumn) else DownloadManager.STATUS_FAILED
                    val localUri = if (localUriColumn != -1) it.getString(localUriColumn) else null
                    val totalSize = if (totalSizeColumn != -1) it.getLong(totalSizeColumn) else 0L
                    val reason = if (reasonColumn != -1) it.getInt(reasonColumn) else 0


                    // Perform SharedPreferences update on a background thread
                    CoroutineScope(Dispatchers.IO).launch {
                        val prefs = InvidiousPreferences(context.applicationContext)
                        val downloadedVideoInfo = prefs.getDownloadedVideoByManagerId(downloadId)

                        if (downloadedVideoInfo != null) {
                            when (status) {
                                DownloadManager.STATUS_SUCCESSFUL -> {
                                    downloadedVideoInfo.status = DownloadStatus.COMPLETED
                                    downloadedVideoInfo.localFilePath = localUri
                                    downloadedVideoInfo.fileSize = totalSize
                                    Log.i(TAG, "Download $downloadId successful. Path: $localUri, Size: $totalSize for video: ${downloadedVideoInfo.title}")
                                }
                                DownloadManager.STATUS_FAILED -> {
                                    downloadedVideoInfo.status = DownloadStatus.FAILED
                                    Log.e(TAG, "Download $downloadId FAILED for video: ${downloadedVideoInfo.title}. Reason code: $reason, Error: ${getDownloadErrorReason(reason)}")
                                }
                                DownloadManager.STATUS_PAUSED -> {
                                    downloadedVideoInfo.status = DownloadStatus.PAUSED
                                    Log.w(TAG, "Download $downloadId PAUSED for video: ${downloadedVideoInfo.title}. Reason code: $reason")
                                }
                                else -> {
                                     Log.w(TAG, "Download $downloadId has unhandled status: $status")
                                     // Consider how to handle other statuses if they are relevant
                                }
                            }
                            prefs.addOrUpdateDownloadedVideo(downloadedVideoInfo)
                        } else {
                            Log.e(TAG, "No DownloadedVideoInfo found in Prefs for download ID: $downloadId. Cannot update status.")
                        }
                    }
                } else {
                    // Cursor is empty, meaning DownloadManager has no record of this ID.
                    // This could happen if the download was removed or failed extremely early.
                    Log.e(TAG, "Cursor empty for download ID: $downloadId. Download might have been removed or failed very early.")
                     CoroutineScope(Dispatchers.IO).launch {
                        val prefs = InvidiousPreferences(context.applicationContext)
                        val videoInfo = prefs.getDownloadedVideoByManagerId(downloadId)
                        // If we have a pending/downloading record, mark it as failed.
                        if (videoInfo != null && (videoInfo.status == DownloadStatus.PENDING || videoInfo.status == DownloadStatus.DOWNLOADING)) {
                            videoInfo.status = DownloadStatus.FAILED
                            prefs.addOrUpdateDownloadedVideo(videoInfo)
                            Log.w(TAG, "Marked PENDING/DOWNLOADING video $downloadId as FAILED as it was not found in DownloadManager.")
                        }
                     }
                }
            } ?: Log.e(TAG, "DownloadManager query returned null cursor for ID: $downloadId")

        } else if (DownloadManager.ACTION_NOTIFICATION_CLICKED == action) {
            // This intent is usually handled by the system to open its Downloads UI.
            // If custom behavior is desired (e.g., open this app's Saved tab):
            Log.d(TAG, "DownloadManager Notification clicked. Opening app's MainActivity.")
            val appIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                // Optionally, add an extra to navigate directly to the Saved tab in MainActivity
                // putExtra("NAVIGATE_TO_SAVED_TAB", true)
            }
            context.startActivity(appIntent)
        }
    }

    /**
     * Converts a [DownloadManager] error reason code into a human-readable string.
     * @param reason The error reason code from [DownloadManager.COLUMN_REASON].
     * @return A string describing the error.
     */
    private fun getDownloadErrorReason(reason: Int): String {
        return when (reason) {
            DownloadManager.ERROR_CANNOT_RESUME -> "ERROR_CANNOT_RESUME"
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> "ERROR_DEVICE_NOT_FOUND"
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> "ERROR_FILE_ALREADY_EXISTS"
            DownloadManager.ERROR_FILE_ERROR -> "ERROR_FILE_ERROR"
            DownloadManager.ERROR_HTTP_DATA_ERROR -> "ERROR_HTTP_DATA_ERROR"
            DownloadManager.ERROR_INSUFFICIENT_SPACE -> "ERROR_INSUFFICIENT_SPACE"
            DownloadManager.ERROR_TOO_MANY_REDIRECTS -> "ERROR_TOO_MANY_REDIRECTS"
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE -> "ERROR_UNHANDLED_HTTP_CODE"
            DownloadManager.ERROR_UNKNOWN -> "ERROR_UNKNOWN"
            else -> "Unknown error code: $reason"
        }
    }
}
