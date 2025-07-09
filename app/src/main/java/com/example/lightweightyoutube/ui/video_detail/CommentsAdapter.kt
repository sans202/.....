package com.example.lightweightyoutube.ui.video_detail

import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.CircleCropTransformation
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.data.model.InvidiousComment
import com.example.lightweightyoutube.databinding.ItemCommentBinding
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

/**
 * A [ListAdapter] for displaying a list of [InvidiousComment] objects in a RecyclerView.
 * Used in the [VideoDetailActivity] to show video comments.
 */
class CommentsAdapter : ListAdapter<InvidiousComment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        comment?.let { holder.bind(it) }
    }

    /**
     * ViewHolder for comment items.
     * Binds [InvidiousComment] data to the views in `item_comment.xml`.
     */
    class CommentViewHolder(private val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds data from an [InvidiousComment] to the ViewHolder's views.
         * @param comment The comment data to display.
         */
        fun bind(comment: InvidiousComment) {
            binding.textViewCommentAuthor.text = comment.author
            binding.textViewCommentPublished.text = comment.publishedText ?: ""

            // Parse and display HTML content for the comment body
            if (!comment.contentHtml.isNullOrEmpty()) {
                val commentContentHtml: Spanned = HtmlCompat.fromHtml(comment.contentHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
                binding.textViewCommentContent.text = commentContentHtml
                binding.textViewCommentContent.movementMethod = LinkMovementMethod.getInstance() // Make links clickable
            } else {
                 binding.textViewCommentContent.text = "" // Clear if no content, or show placeholder
            }

            // Display like count if available and positive
            if (comment.likeCount > 0) {
                binding.textViewCommentLikes.text = formatLikeCount(comment.likeCount)
                binding.textViewCommentLikes.visibility = View.VISIBLE
                // Assuming the like icon (ImageView) is always visible and only the count text changes visibility/content
            } else {
                binding.textViewCommentLikes.visibility = View.GONE
                // If icon also needs to be hidden:
                // binding.imageViewLikeIcon.visibility = View.GONE
            }

            // Load author thumbnail
            val authorThumbnailUrl = comment.authorThumbnails.firstOrNull()?.url
            binding.imageViewCommentAuthorThumbnail.load(authorThumbnailUrl) {
                placeholder(R.drawable.ic_profile_placeholder)
                error(R.drawable.ic_profile_placeholder) // Fallback to placeholder on error
                transformations(CircleCropTransformation()) // Circular thumbnail
                crossfade(true)
            }

            // TODO: Implement display for creatorHeart if needed by design.
            // Example:
            // comment.creatorHeart?.let { binding.imageViewCreatorHeartIcon.visibility = View.VISIBLE }
            // ?: run { binding.imageViewCreatorHeartIcon.visibility = View.GONE }
        }

        /**
         * Formats a raw like count into a human-readable string (e.g., "1.2K", "150").
         * @param count The raw like count.
         * @return A formatted string.
         */
        private fun formatLikeCount(count: Int): String {
            if (count <= 0) return ""
            if (count < 1000) return count.toString()
            val exp = (ln(count.toDouble()) / ln(1000.0)).toInt()
            // Ensure 'exp' is within the bounds of the "KMBTPE" unit array
            if (exp <= 0 || exp > "KMBTPE".length) return count.toString()
            val unit = "KMBTPE"[exp - 1]
            return String.format(Locale.US, "%.1f%c", count / 1000.0.pow(exp.toDouble()), unit)
        }
    }

    /**
     * [DiffUtil.ItemCallback] for calculating the diff between two [InvidiousComment] items.
     */
    class CommentDiffCallback : DiffUtil.ItemCallback<InvidiousComment>() {
        override fun areItemsTheSame(oldItem: InvidiousComment, newItem: InvidiousComment): Boolean {
            // Comments are the same if their IDs match.
            return oldItem.commentId == newItem.commentId
        }

        override fun areContentsTheSame(oldItem: InvidiousComment, newItem: InvidiousComment): Boolean {
            // Contents are the same if the objects are equal (data class equality).
            return oldItem == newItem
        }
    }
}
