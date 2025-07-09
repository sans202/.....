package com.example.lightweightyoutube.ui.video_detail

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import coil.transform.CircleCropTransformation
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.data.model.DownloadedVideoInfo
import com.example.lightweightyoutube.data.model.DownloadStatus
import com.example.lightweightyoutube.data.model.InvidiousVideo
import com.example.lightweightyoutube.data.model.VideoThumbnail
import com.example.lightweightyoutube.data.preferences.InvidiousPreferences
import com.example.lightweightyoutube.data.repository.Resource
import com.example.lightweightyoutube.data.utils.isNetworkAvailable
import com.example.lightweightyoutube.databinding.ActivityVideoDetailBinding
import com.example.lightweightyoutube.ui.player.PlayerActivity
import com.google.android.material.appbar.AppBarLayout
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Activity for displaying the details of a specific video.
 * Shows video metadata (title, author, description, stats), comments,
 * and provides options to play or download the video after quality selection.
 *
 * Requires [EXTRA_VIDEO_ID] to be passed in the launching Intent.
 */
class VideoDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoDetailBinding
    private var currentVideoId: String? = null // The ID of the video being displayed

    private val viewModel: VideoDetailViewModel by viewModels {
        // Factory to pass videoId to the ViewModel constructor
        VideoDetailViewModel.Factory(application, currentVideoId ?: "")
    }
    private lateinit var commentsAdapter: CommentsAdapter // Adapter for displaying comments

    private var isDescriptionExpanded = false // Tracks state of the description view

    companion object {
        /** Intent extra key for passing the Invidious video ID to this activity. */
        const val EXTRA_VIDEO_ID = "extra_video_id"
        private const val TAG = "VideoDetailActivity" // Logging tag

        /**
         * Creates an Intent to start [VideoDetailActivity].
         * @param context The context from which the activity is started.
         * @param videoId The Invidious video ID to display details for.
         * @return An Intent configured to start [VideoDetailActivity].
         */
        fun newIntent(context: Context, videoId: String): Intent {
            return Intent(context, VideoDetailActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_ID, videoId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentVideoId = intent.getStringExtra(EXTRA_VIDEO_ID)

        // Ensure a video ID was passed, otherwise, finish the activity
        if (currentVideoId.isNullOrBlank()) {
            Log.e(TAG, "Video ID is null or blank. Finishing activity.")
            Toast.makeText(this, getString(R.string.error_video_id_missing), Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupToolbar()
        setupCommentsRecyclerView()
        observeViewModelLiveData()
        setupClickListeners()
    }

    /**
     * Sets up the Toolbar, including the back navigation button and title behavior
     * for the [CollapsingToolbarLayout].
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "" // Title is managed by CollapsingToolbarLayout

        // Dynamically set CollapsingToolbarLayout title based on scroll position
        var isToolbarTitleVisible = false
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val totalScrollRange = appBarLayout.totalScrollRange
            // Threshold to show title when toolbar is almost collapsed
            val threshold = totalScrollRange - (binding.toolbar.height * 1.2)

            if (abs(verticalOffset) >= threshold) { // If toolbar is collapsed enough
                if (!isToolbarTitleVisible) {
                    val videoTitle = viewModel.videoDetails.value?.data?.title ?: getString(R.string.video_details_title)
                    binding.collapsingToolbarLayout.title = videoTitle
                    isToolbarTitleVisible = true
                }
            } else { // Toolbar is expanded
                if (isToolbarTitleVisible) {
                    binding.collapsingToolbarLayout.title = " " // Clear title when expanded
                    isToolbarTitleVisible = false
                }
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle Up/Back button press in toolbar
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Initializes the RecyclerView for displaying comments.
     */
    private fun setupCommentsRecyclerView() {
        commentsAdapter = CommentsAdapter()
        binding.recyclerViewComments.apply {
            layoutManager = LinearLayoutManager(this@VideoDetailActivity)
            adapter = commentsAdapter
            isNestedScrollingEnabled = false // Important for RecyclerView within NestedScrollView
        }
    }

    /**
     * Sets up click listeners for various UI elements like buttons and description toggle.
     */
    private fun setupClickListeners() {
        binding.textViewToggleDescription.setOnClickListener {
            toggleDescription()
        }
        binding.buttonPlay.setOnClickListener {
            showQualitySelectionDialog(isForDownload = false)
        }
        binding.buttonDownload.setOnClickListener {
            // TODO: Consider adding permission check for WRITE_EXTERNAL_STORAGE if targeting < Android Q
            // and saving to public directories (not the case here as we use app-specific external dir).
            showQualitySelectionDialog(isForDownload = true)
        }
        binding.buttonRetryVideoDetail.setOnClickListener {
            viewModel.retry()
        }
    }

    /**
     * Observes [LiveData] from [VideoDetailViewModel] to update the UI with video details,
     * comments, loading states, and error messages.
     */
    private fun observeViewModelLiveData() {
        viewModel.isLoading.observe(this) { isLoading ->
            val hasData = viewModel.videoDetails.value?.data != null
            // Show full-screen loader only if loading initial main content
            if (isLoading && !hasData) {
                binding.progressBarVideoDetail.visibility = View.VISIBLE
                binding.nestedScrollViewContent.visibility = View.GONE
                binding.layoutErrorVideoDetail.visibility = View.GONE
            } else if (!isLoading) { // Loading finished
                 binding.progressBarVideoDetail.visibility = View.GONE
            }
        }

        viewModel.error.observe(this) { errorMessage ->
             val hasData = viewModel.videoDetails.value?.data != null
             // Show full-screen error only if main content loading failed
             if (errorMessage != null && !hasData) {
                binding.layoutErrorVideoDetail.visibility = View.VISIBLE
                binding.textViewErrorVideoDetail.text = errorMessage
                binding.nestedScrollViewContent.visibility = View.GONE
                binding.progressBarVideoDetail.visibility = View.GONE
            } else if (errorMessage == null) { // No error, hide error layout
                binding.layoutErrorVideoDetail.visibility = View.GONE
            } else if (errorMessage != null && hasData) { // Error occurred but some data is present (e.g., comments failed)
                Toast.makeText(this, "Minor error: $errorMessage", Toast.LENGTH_SHORT).show()
                viewModel.userShownError() // Consume one-time error
            }
        }


        viewModel.videoDetails.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    binding.progressBarVideoDetail.visibility = View.GONE
                    resource.data?.let { video ->
                        binding.nestedScrollViewContent.visibility = View.VISIBLE
                        binding.layoutErrorVideoDetail.visibility = View.GONE
                        populateVideoDetails(video)
                    } ?: run { // Success state but data is null (should ideally be an Error state from repo)
                        if (viewModel.error.value == null) { // Avoid overriding a more specific error
                            binding.textViewErrorVideoDetail.text = getString(R.string.an_error_occurred) + " (No data)"
                            binding.layoutErrorVideoDetail.visibility = View.VISIBLE
                            binding.nestedScrollViewContent.visibility = View.GONE
                        }
                    }
                }
                is Resource.Error -> {
                    // Show full-screen error only if no data is currently displayed
                    if (resource.data == null && binding.layoutErrorVideoDetail.visibility == View.GONE) {
                         binding.textViewErrorVideoDetail.text = resource.message ?: getString(R.string.error_loading_videos)
                         binding.layoutErrorVideoDetail.visibility = View.VISIBLE
                         binding.nestedScrollViewContent.visibility = View.GONE
                         binding.progressBarVideoDetail.visibility = View.GONE
                    } else if (resource.data != null) { // Error occurred, but we might have stale data to show
                         Toast.makeText(this, resource.message ?: getString(R.string.error_loading_videos), Toast.LENGTH_LONG).show()
                    }
                }
                is Resource.Loading -> {
                    // Primary loading state handled by `viewModel.isLoading`
                }
            }
        }

        viewModel.comments.observe(this) { resource ->
            when (resource) {
                is Resource.Success -> {
                    binding.progressBarComments.visibility = View.GONE
                    val comments = resource.data
                    if (comments.isNullOrEmpty()) {
                        binding.textViewNoComments.text = getString(R.string.no_comments_available)
                        binding.textViewNoComments.visibility = View.VISIBLE
                        binding.recyclerViewComments.visibility = View.GONE
                    } else {
                        binding.textViewNoComments.visibility = View.GONE
                        binding.recyclerViewComments.visibility = View.VISIBLE
                        commentsAdapter.submitList(comments)
                    }
                }
                is Resource.Error -> {
                    binding.progressBarComments.visibility = View.GONE
                    binding.textViewNoComments.text = resource.message ?: getString(R.string.error_loading_comments)
                    binding.textViewNoComments.visibility = View.VISIBLE
                    binding.recyclerViewComments.visibility = View.GONE
                }
                 is Resource.Loading -> {
                    binding.progressBarComments.visibility = View.VISIBLE
                    binding.textViewNoComments.visibility = View.GONE
                    binding.recyclerViewComments.visibility = View.GONE
                }
            }
        }
    }

    /**
     * Populates the UI elements with data from the [InvidiousVideo] object.
     * @param video The video data to display.
     */
    private fun populateVideoDetails(video: InvidiousVideo) {
        // Set toolbar title if it's already collapsed when data arrives
        val isCollapsed = abs(binding.appBar.top) >= (binding.appBar.totalScrollRange - (binding.toolbar.height * 1.2))
        if (isCollapsed) {
            binding.collapsingToolbarLayout.title = video.title
        } else {
            binding.collapsingToolbarLayout.title = " " // Clear title if expanded
        }

        binding.textViewVideoTitle.text = video.title

        val bestThumbnail = getBestThumbnailUrl(video.videoThumbnails)
        binding.imageViewVideoThumbnailLarge.load(bestThumbnail ?: R.drawable.ic_placeholder_video_error) {
            placeholder(R.drawable.ic_placeholder_video)
            error(R.drawable.ic_placeholder_video_error)
            crossfade(true)
        }

        binding.textViewViewCount.text = video.viewCount.let { NumberFormat.getInstance().format(it) + " views" }
        binding.textViewPublishedDate.text = video.publishedText ?: video.published.let {
            try { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it * 1000L)) }
            catch (e: Exception) { "N/A" } // Fallback for invalid timestamp
        }

        binding.textViewLikes.text = if(video.likeCount >= 0) formatCount(video.likeCount) else "N/A"
        binding.textViewDislikes.text = if(video.dislikeCount >= 0) formatCount(video.dislikeCount) else "N/A"


        binding.textViewAuthorName.text = video.author
        val authorThumbUrl = video.authorThumbnails.firstOrNull()?.url
        binding.imageViewAuthorThumbnail.load(authorThumbUrl) {
            placeholder(R.drawable.ic_profile_placeholder)
            error(R.drawable.ic_profile_placeholder)
            transformations(CircleCropTransformation())
            crossfade(true)
        }

        // Populate and manage visibility of description section
        if (video.descriptionHtml.isNotBlank()) {
            val descriptionSpanned: Spanned = HtmlCompat.fromHtml(video.descriptionHtml, HtmlCompat.FROM_HTML_MODE_COMPACT)
            binding.textViewVideoDescription.text = descriptionSpanned
            binding.textViewVideoDescription.movementMethod = LinkMovementMethod.getInstance() // Make links clickable
            binding.textViewVideoDescriptionHeader.visibility = View.VISIBLE
            binding.textViewVideoDescription.visibility = View.VISIBLE

            // Determine if "Show more/less" toggle is needed based on content length/lines
            binding.textViewVideoDescription.post { // Post to run after layout pass to get line count
                val lineCount = binding.textViewVideoDescription.lineCount
                // Show toggle if text is longer than maxLines (3) or if it's very long even if rendered on fewer lines due to width
                binding.textViewToggleDescription.visibility = if (lineCount > 3 || (lineCount == 0 && descriptionSpanned.length > 200) ) View.VISIBLE else View.GONE
            }
        } else {
            binding.textViewVideoDescriptionHeader.visibility = View.GONE
            binding.textViewVideoDescription.visibility = View.GONE
            binding.textViewToggleDescription.visibility = View.GONE
        }
        updateDescriptionToggleText() // Set initial text for toggle ("Show more")
    }

    /**
     * Selects the best available thumbnail URL from a list of [VideoThumbnail] objects.
     * Prioritizes higher quality thumbnails.
     * @param thumbnails List of available thumbnails.
     * @return The URL string of the best thumbnail, or null if none available.
     */
    private fun getBestThumbnailUrl(thumbnails: List<VideoThumbnail>?): String? {
        if (thumbnails.isNullOrEmpty()) return null
        // Prioritize specific quality keys, then largest dimensions, then first available
        return thumbnails.find { it.quality == "maxresdefault" }?.url
            ?: thumbnails.find { it.quality == "sddefault" }?.url
            ?: thumbnails.find { it.quality == "hqdefault" }?.url
            ?: thumbnails.find { it.quality == "mqdefault" }?.url
            ?: thumbnails.maxByOrNull { it.width * it.height }?.url
            ?: thumbnails.firstOrNull()?.url
    }

    /**
     * Formats a raw count (e.g., likes, dislikes) into a human-readable string (e.g., "1.2K", "3M").
     * Returns an empty string if count is negative (indicating data not available or disabled).
     * @param count The raw integer count.
     * @return A formatted string representation of the count.
     */
    private fun formatCount(count: Int): String {
        if (count < 0) return "" // Data likely unavailable or ratings disabled
        if (count < 1000) return count.toString()
        val exp = (Math.log(count.toDouble()) / Math.log(1000.0)).toInt()
        // Ensure 'exp' is within the bounds of the "KMBTPE" unit array
        if (exp <= 0 || exp > "KMBTPE".length) return count.toString()
        return String.format(Locale.US, "%.1f%c", count / Math.pow(1000.0, exp.toDouble()), "KMBTPE"[exp - 1])
    }

    /**
     * Toggles the expanded/collapsed state of the video description TextView.
     */
    private fun toggleDescription() {
        isDescriptionExpanded = !isDescriptionExpanded
        binding.textViewVideoDescription.maxLines = if (isDescriptionExpanded) Integer.MAX_VALUE else 3
        updateDescriptionToggleText()
    }

    /**
     * Updates the text of the "Show more/less" TextView for the description
     * based on the current [isDescriptionExpanded] state.
     */
    private fun updateDescriptionToggleText() {
        binding.textViewToggleDescription.text = getString(if (isDescriptionExpanded) R.string.hide_description else R.string.show_description)
    }

    /**
     * Displays an AlertDialog prompting the user to select a video quality
     * for either playback or download.
     *
     * @param isForDownload True if the quality selection is for downloading, false for playback.
     */
    private fun showQualitySelectionDialog(isForDownload: Boolean) {
        val qualities = viewModel.availableQualities.value
        if (qualities.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.error_fetching_streams), Toast.LENGTH_SHORT).show()
            return
        }

        val qualityLabels = qualities.map { it.qualityLabel }.toTypedArray()
        val actionType = if (isForDownload) getString(R.string.download_video) else getString(R.string.play_video)

        AlertDialog.Builder(this)
            .setTitle("${getString(R.string.select_video_quality)} ($actionType)")
            .setItems(qualityLabels) { dialog, which ->
                val selectedQuality = qualities[which]
                val videoDetails = viewModel.videoDetails.value?.data

                if (videoDetails == null) { // Should not happen if buttons are enabled only after details load
                    Toast.makeText(this, "Video details not available.", Toast.LENGTH_SHORT).show()
                    return@setItems
                }

                if (isForDownload) {
                    startDownload(videoDetails, selectedQuality)
                } else {
                    val intent = PlayerActivity.newIntent(this, selectedQuality.format.url, videoDetails.title)
                    startActivity(intent)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * Initiates the video download process using [DownloadManager].
     * @param video The [InvidiousVideo] object containing metadata.
     * @param quality The selected [VideoStreamQuality] for download.
     */
    private fun startDownload(video: InvidiousVideo, quality: VideoStreamQuality) {
        if (!isNetworkAvailable(this)) {
            Toast.makeText(this, getString(R.string.offline_message), Toast.LENGTH_SHORT).show()
            return
        }

        val downloadManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        // Sanitize title and quality label for use in filename
        val sanitizedTitle = video.title.replace(Regex("[^a-zA-Z0-9\\.\\-_]"), "_").take(100)
        val qualitySanitized = quality.qualityLabel.replace(Regex("[^a-zA-Z0-9\\.\\-_]"), "_")
        val fileExtension = quality.format.container?.lowercase() ?: quality.format.type?.substringAfterLast('/')?.substringBefore(';')?.lowercase() ?: "mp4"
        val fileName = "${sanitizedTitle}_${qualitySanitized}.$fileExtension"

        val request = DownloadManager.Request(Uri.parse(quality.format.url))
            .setTitle(video.title)
            .setDescription("Downloading: ${quality.qualityLabel}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Save to app-specific directory in external storage (Movies folder)
            .setDestinationInExternalFilesDir(this, Environment.DIRECTORY_MOVIES, fileName)
            .setAllowedOverMetered(true) // Allow download over metered connections
            .setAllowedOverRoaming(true)

        try {
            val downloadId = downloadManager.enqueue(request)
            Log.d(TAG, "Enqueued download $downloadId for $fileName, URL: ${quality.format.url}")

            // Store download information
            val bestThumbnailUrl = getBestThumbnailUrl(video.videoThumbnails)
            val downloadInfo = DownloadedVideoInfo(
                downloadManagerId = downloadId,
                invidiousVideoId = video.videoId,
                title = video.title,
                videoUrl = quality.format.url,
                thumbnailUrl = bestThumbnailUrl,
                qualityLabel = quality.qualityLabel,
                status = DownloadStatus.PENDING // Initial status
            )

            val prefs = InvidiousPreferences(applicationContext)
            prefs.addOrUpdateDownloadedVideo(downloadInfo)

            Toast.makeText(this, getString(R.string.download_started, video.title), Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Log.e(TAG, "Error starting download for ${video.title}", e)
            Toast.makeText(this, "Failed to start download: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
```
