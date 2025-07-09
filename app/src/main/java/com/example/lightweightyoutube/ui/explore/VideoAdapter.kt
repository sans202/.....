package com.example.lightweightyoutube.ui.explore

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.data.model.InvidiousSearchItem
import com.example.lightweightyoutube.data.model.VideoThumbnail
import com.example.lightweightyoutube.databinding.ItemVideoBinding
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * A [ListAdapter] for displaying a list of [InvidiousSearchItem] objects (videos)
 * in a RecyclerView, typically used in the Explore screen.
 *
 * @param onVideoClicked A lambda function to be invoked when a video item is clicked.
 *                       It receives the clicked [InvidiousSearchItem] as a parameter.
 */
class VideoAdapter(private val onVideoClicked: (InvidiousSearchItem) -> Unit) :
    ListAdapter<InvidiousSearchItem, VideoAdapter.VideoViewHolder>(VideoDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = getItem(position)
        video?.let { holder.bind(it, onVideoClicked) }
    }

    /**
     * ViewHolder for video items in the RecyclerView.
     * Binds [InvidiousSearchItem] data to the views defined in `item_video.xml`.
     */
    class VideoViewHolder(private val binding: ItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the data from an [InvidiousSearchItem] to the ViewHolder's views.
         * @param video The video item to display.
         * @param onVideoClicked Lambda to call when this item is clicked.
         */
        fun bind(video: InvidiousSearchItem, onVideoClicked: (InvidiousSearchItem) -> Unit) {
            binding.textViewVideoTitle.text = video.title ?: itemView.context.getString(R.string.default_no_title)
            binding.textViewChannelName.text = video.author ?: itemView.context.getString(R.string.default_unknown_author)

            val viewCountText = video.viewCount?.let { count ->
                if (count > 0) formatViewCount(count) else ""
            } ?: ""
            val publishedText = video.publishedText?.takeIf { it.isNotBlank() } ?: ""

            val infoSeparator = if (viewCountText.isNotEmpty() && publishedText.isNotEmpty()) " • " else ""
            binding.textViewVideoInfo.text = "$viewCountText$infoSeparator$publishedText"


            val thumbnailUrl = getBestThumbnailUrl(video.videoThumbnails)

            binding.imageViewThumbnail.load(thumbnailUrl) {
                placeholder(R.drawable.ic_placeholder_video)
                error(R.drawable.ic_placeholder_video_error)
                crossfade(true)
                // TODO: Consider adding transformations if needed, e.g., rounded corners
                // transformations(RoundedCornersTransformation(8f.dpToPx(itemView.context)))
            }

            itemView.setOnClickListener {
                // Trigger click only if videoId is valid
                video.videoId?.let { _ -> onVideoClicked(video) }
            }
        }

        /**
         * Selects the most appropriate thumbnail URL from the list of available thumbnails.
         * Prioritizes common high-quality keys, then largest dimensions.
         * @param thumbnails List of [VideoThumbnail] objects.
         * @return The URL string of the best thumbnail, or null.
         */
        private fun getBestThumbnailUrl(thumbnails: List<VideoThumbnail>?): String? {
            if (thumbnails.isNullOrEmpty()) return null
            return thumbnails.find { it.quality == "hqdefault" }?.url // Often 480x360
                ?: thumbnails.find { it.quality == "sddefault" }?.url // Often 640x480
                ?: thumbnails.find { it.quality == "mqdefault" }?.url // Often 320x180
                ?: thumbnails.maxByOrNull { it.width * it.height }?.url // Fallback to largest
                ?: thumbnails.firstOrNull()?.url // Fallback to first available
        }

        /**
         * Formats a raw view count into a human-readable string (e.g., "1.2K views", "3M views").
         * @param count The raw view count.
         * @return A formatted string.
         */
        private fun formatViewCount(count: Long): String {
            if (count < 1000) return "$count views"
            val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
            // Ensure 'exp' is within the bounds of the "KMBTPE" unit array
            if (exp <= 0 || exp > "KMBTPE".length) return "$count views"
            val unit = "KMBTPE"[exp - 1]
            return String.format(Locale.US, "%.1f%c views", count / 1000.0.pow(exp.toDouble()), unit)
        }
    }

    /**
     * [DiffUtil.ItemCallback] for calculating the diff between two non-null items in a list.
     * Used by [ListAdapter] to efficiently update the RecyclerView.
     */
    class VideoDiffCallback : DiffUtil.ItemCallback<InvidiousSearchItem>() {
        override fun areItemsTheSame(oldItem: InvidiousSearchItem, newItem: InvidiousSearchItem): Boolean {
            // Items are considered the same if their video IDs are identical.
            return oldItem.videoId != null && oldItem.videoId == newItem.videoId
        }

        override fun areContentsTheSame(oldItem: InvidiousSearchItem, newItem: InvidiousSearchItem): Boolean {
            // Contents are considered the same if the objects are equal (data class equality).
            return oldItem == newItem
        }
    }
}
// Example for dp to Px conversion if needed (e.g. for Coil's RoundedCornersTransformation)
// import android.content.Context
// import android.util.TypedValue
// fun Float.dpToPx(context: Context): Float {
//    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, this, context.resources.displayMetrics)
// }
