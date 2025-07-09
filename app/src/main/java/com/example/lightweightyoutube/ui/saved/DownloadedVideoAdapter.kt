package com.example.lightweightyoutube.ui.saved

import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.data.model.DownloadedVideoInfo
import com.example.lightweightyoutube.databinding.ItemDownloadedVideoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

/**
 * A [ListAdapter] for displaying a list of [DownloadedVideoInfo] objects in a RecyclerView.
 * Used in the [SavedFragment] to show videos downloaded by the user.
 *
 * @param onVideoClicked Lambda function invoked when a downloaded video item is clicked, for playback.
 * @param onDeleteClicked Lambda function invoked when the delete button for a video item is clicked.
 */
class DownloadedVideoAdapter(
    private val onVideoClicked: (DownloadedVideoInfo) -> Unit,
    private val onDeleteClicked: (DownloadedVideoInfo) -> Unit
) : ListAdapter<DownloadedVideoInfo, DownloadedVideoAdapter.DownloadedVideoViewHolder>(DownloadedVideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DownloadedVideoViewHolder {
        val binding = ItemDownloadedVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DownloadedVideoViewHolder(binding, onVideoClicked, onDeleteClicked)
    }

    override fun onBindViewHolder(holder: DownloadedVideoViewHolder, position: Int) {
        val video = getItem(position)
        video?.let { holder.bind(it) }
    }

    /**
     * ViewHolder for downloaded video items.
     * Binds [DownloadedVideoInfo] data to the views in `item_downloaded_video.xml`.
     */
    class DownloadedVideoViewHolder(
        private val binding: ItemDownloadedVideoBinding,
        private val onVideoClicked: (DownloadedVideoInfo) -> Unit,
        private val onDeleteClicked: (DownloadedVideoInfo) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            private const val TAG = "DownloadedVideoVH" // Logging tag
        }

        /**
         * Binds data from a [DownloadedVideoInfo] object to the ViewHolder's views.
         * @param video The downloaded video information to display.
         */
        fun bind(video: DownloadedVideoInfo) {
            binding.textViewVideoTitle.text = video.title

            // Display formatted file size if available
            if (video.fileSize > 0) {
                binding.textViewVideoSize.text = formatFileSize(video.fileSize)
                binding.textViewVideoSize.visibility = android.view.View.VISIBLE
            } else {
                // Attempt to get file size if not already set in videoInfo (e.g., if it was recently completed)
                if (!video.localFilePath.isNullOrBlank()) {
                    try {
                        val fileSize = File(video.localFilePath!!).length()
                        if (fileSize > 0) {
                            binding.textViewVideoSize.text = formatFileSize(fileSize)
                            binding.textViewVideoSize.visibility = android.view.View.VISIBLE
                        } else {
                            binding.textViewVideoSize.visibility = android.view.View.GONE
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not get file size for ${video.localFilePath}: ${e.message}")
                        binding.textViewVideoSize.visibility = android.view.View.GONE
                    }
                } else {
                    binding.textViewVideoSize.visibility = android.view.View.GONE
                }
            }

            // Thumbnail loading: Prioritize local file thumbnail, fallback to network URL.
            // Tag ensures image isn't reloaded unnecessarily if view is recycled for the same item.
            val currentThumbnailDataSource = video.localFilePath ?: video.thumbnailUrl
            if (binding.imageViewThumbnail.tag != currentThumbnailDataSource) {
                binding.imageViewThumbnail.tag = currentThumbnailDataSource
                binding.imageViewThumbnail.setImageResource(R.drawable.ic_placeholder_video) // Immediate placeholder

                if (!video.localFilePath.isNullOrBlank()) {
                    // Asynchronously try to load thumbnail from the local video file.
                    // Using GlobalScope for simplicity here; in a larger app, inject a CoroutineScope.
                    GlobalScope.launch(Dispatchers.Main) {
                        val bitmap = getVideoThumbnail(video.localFilePath!!)
                        // Check if the view holder is still bound to the same item
                        if (binding.imageViewThumbnail.tag == currentThumbnailDataSource) {
                            if (bitmap != null) {
                                binding.imageViewThumbnail.load(bitmap) {
                                    error(R.drawable.ic_placeholder_video_error) // Error during bitmap load
                                    crossfade(true)
                                }
                            } else {
                                loadNetworkThumbnail(video.thumbnailUrl) // Fallback if local extraction fails
                            }
                        }
                    }
                } else {
                    loadNetworkThumbnail(video.thumbnailUrl) // No local file, use network URL
                }
            }

            itemView.setOnClickListener { onVideoClicked(video) }
            binding.buttonDelete.setOnClickListener { onDeleteClicked(video) }
        }

        /**
         * Suspended function to extract a thumbnail from a local video file path.
         * Runs on [Dispatchers.IO].
         * @param filePath The absolute path to the local video file.
         * @return A [Bitmap] thumbnail, or null if extraction fails.
         */
        private suspend fun getVideoThumbnail(filePath: String): Bitmap? = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // Modern method for thumbnail extraction
                    ThumbnailUtils.createVideoThumbnail(File(filePath), Size(120, 90), null)
                } else {
                    // Legacy method for older Android versions
                    @Suppress("DEPRECATION")
                    ThumbnailUtils.createVideoThumbnail(filePath, MediaStore.Images.Thumbnails.MINI_KIND)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error generating thumbnail for $filePath: ${e.message}")
                null // Return null on any error
            }
        }

        /**
         * Loads a thumbnail from a network URL using Coil.
         * @param thumbnailUrl The URL of the thumbnail image.
         */
        private fun loadNetworkThumbnail(thumbnailUrl: String?) {
             binding.imageViewThumbnail.load(thumbnailUrl) {
                placeholder(R.drawable.ic_placeholder_video)
                error(R.drawable.ic_placeholder_video_error) // Fallback if network URL also fails
                crossfade(true)
            }
        }

        /**
         * Formats a file size in bytes into a human-readable string (e.g., "1.2 MB", "100 KB").
         * @param sizeBytes The file size in bytes.
         * @return A formatted string representation of the file size.
         */
        private fun formatFileSize(sizeBytes: Long): String {
            if (sizeBytes <= 0) return "" // Or "0 B" if preferred for zero size
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            if (sizeBytes < 1024) return "$sizeBytes B" // Show bytes directly if less than 1KB
            val digitGroups = (Math.log10(sizeBytes.toDouble()) / Math.log10(1024.0)).toInt()

            // Ensure digitGroups is within the bounds of the units array
            val unitIndex = if (digitGroups >= units.size) units.size - 1 else digitGroups

            return String.format(Locale.getDefault(), "%.1f %s", sizeBytes / Math.pow(1024.0, unitIndex.toDouble()), units[unitIndex])
        }
    }

    /**
     * [DiffUtil.ItemCallback] for calculating the diff between two [DownloadedVideoInfo] items.
     * Used by [ListAdapter] for efficient RecyclerView updates.
     */
    class DownloadedVideoDiffCallback : DiffUtil.ItemCallback<DownloadedVideoInfo>() {
        override fun areItemsTheSame(oldItem: DownloadedVideoInfo, newItem: DownloadedVideoInfo): Boolean {
            // Items are the same if their DownloadManager IDs match.
            return oldItem.downloadManagerId == newItem.downloadManagerId
        }

        override fun areContentsTheSame(oldItem: DownloadedVideoInfo, newItem: DownloadedVideoInfo): Boolean {
            // Contents are considered the same if key properties match.
            // Using data class equality is often sufficient if all relevant fields contribute to it.
            return oldItem == newItem
        }
    }
}
