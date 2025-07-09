package com.example.lightweightyoutube.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.databinding.ActivityPlayerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Activity responsible for playing video streams using ExoPlayer.
 * It operates in a full-screen immersive mode.
 * Expects [EXTRA_VIDEO_URL] and optionally [EXTRA_VIDEO_TITLE] in its launch Intent.
 */
@UnstableApi // Due to ExoPlayer components that might change their API surface.
class PlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPlayerBinding
    private var exoPlayer: ExoPlayer? = null

    private var currentVideoUrl: String? = null
    private var currentVideoTitle: String? = null

    companion object {
        /** Intent extra key for the video URL to be played. */
        const val EXTRA_VIDEO_URL = "extra_video_url"
        /** Intent extra key for the title of the video (optional, for context). */
        const val EXTRA_VIDEO_TITLE = "extra_video_title"

        /**
         * Creates an Intent to start [PlayerActivity].
         * @param context The context from which the activity is started.
         * @param videoUrl The URL of the video stream to play.
         * @param videoTitle Optional title of the video.
         * @return An Intent configured to start [PlayerActivity].
         */
        fun newIntent(context: Context, videoUrl: String, videoTitle: String? = null): Intent {
            return Intent(context, PlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentVideoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        currentVideoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)

        if (currentVideoUrl.isNullOrBlank()) {
            // Log.e("PlayerActivity", "Video URL is missing. Finishing activity.")
            Snackbar.make(binding.root, getString(R.string.error_playing_video) + " (URL missing)", Snackbar.LENGTH_LONG).show()
            finish()
            return
        }

        hideSystemUI()
        initializePlayer()
    }

    /**
     * Initializes the ExoPlayer instance, sets up the PlayerView, and starts playback.
     */
    private fun initializePlayer() {
        if (currentVideoUrl.isNullOrBlank()) return // Should be caught by onCreate check

        exoPlayer = ExoPlayer.Builder(this).build().also { player ->
            binding.playerView.player = player
            val mediaItem = MediaItem.fromUri(Uri.parse(currentVideoUrl!!))
            player.setMediaItem(mediaItem)
            player.playWhenReady = true // Start playback as soon as prepared
            player.prepare()


            player.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    binding.progressBar.visibility = when (playbackState) {
                        Player.STATE_BUFFERING -> View.VISIBLE
                        Player.STATE_READY, Player.STATE_ENDED, Player.STATE_IDLE -> View.GONE
                        else -> View.GONE
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        // Handle video end: e.g., show replay button, or finish activity.
                        // For now, player will just stay at the end.
                    }
                }

                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    super.onPlayerError(error)
                    binding.progressBar.visibility = View.GONE
                    // Log.e("PlayerActivity", "ExoPlayer Error: ", error)
                    Snackbar.make(binding.root, getString(R.string.error_playing_video) + ": ${error.localizedMessage}", Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry)) { initializePlayer() } // Simple retry
                        .show()
                }
            })
        }
    }

    /**
     * Configures the activity for an immersive, full-screen experience by hiding system UI bars.
     * Also keeps the screen on during playback.
     */
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, binding.playerView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    // Player lifecycle management
    override fun onStart() {
        super.onStart()
        if (Build.VERSION.SDK_INT > 23) { // Android N (API 24) and above
            if (exoPlayer == null) { // Initialize if not already (e.g. after onStop)
                 initializePlayer()
            }
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT <= 23 || exoPlayer == null) {
             initializePlayer() // For older Android or if player was released
        }
        exoPlayer?.playWhenReady = true
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.playWhenReady = false
        if (Build.VERSION.SDK_INT <= 23) { // Release player on older Android versions when paused
            releasePlayer()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT > 23) {
            exoPlayer?.playWhenReady = false // Stop playback
            // Optionally release player here too, but onStart will reinitialize
            // releasePlayer()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer() // Always release player in onDestroy
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    /**
     * Releases the ExoPlayer instance.
     */
    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
        binding.playerView.player = null // Detach from view
    }
}
