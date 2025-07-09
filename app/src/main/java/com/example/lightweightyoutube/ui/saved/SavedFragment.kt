package com.example.lightweightyoutube.ui.saved

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.data.model.DownloadedVideoInfo
import com.example.lightweightyoutube.databinding.FragmentSavedBinding
import com.example.lightweightyoutube.ui.player.PlayerActivity
import java.io.File

/**
 * A [Fragment] for displaying and managing videos that the user has downloaded for offline playback.
 * It lists downloaded videos, allows playing them, and provides an option to delete them.
 * Observes [SavedViewModel] for the list of downloads and related UI states.
 */
class SavedFragment : Fragment() {

    private var _binding: FragmentSavedBinding? = null // ViewBinding instance
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: SavedViewModel by viewModels()
    private lateinit var downloadedVideoAdapter: DownloadedVideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeViewModelLiveData()
    }

    override fun onResume() {
        super.onResume()
        // Refresh the list of downloaded videos each time the fragment is resumed,
        // to reflect any changes (e.g., new downloads completed, deletions from other parts).
        viewModel.loadDownloadedVideos()
    }

    /**
     * Sets up the RecyclerView with its adapter and layout manager.
     * Defines click listeners for playing or deleting downloaded videos.
     */
    private fun setupRecyclerView() {
        downloadedVideoAdapter = DownloadedVideoAdapter(
            onVideoClicked = { video ->
                if (video.localFilePath.isNullOrBlank() || !File(video.localFilePath!!).exists()) {
                    Toast.makeText(context, getString(R.string.error_video_file_missing), Toast.LENGTH_LONG).show()
                    showDeleteConfirmationDialog(video, isBrokenEntry = true)
                    return@DownloadedVideoAdapter
                }
                // Convert local file path to a file URI string for PlayerActivity
                val fileUri = Uri.fromFile(File(video.localFilePath!!))
                val intent = PlayerActivity.newIntent(requireContext(), fileUri.toString(), video.title)
                startActivity(intent)
            },
            onDeleteClicked = { video ->
                showDeleteConfirmationDialog(video)
            }
        )
        binding.recyclerViewSavedVideos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = downloadedVideoAdapter
            // Optional: addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    /**
     * Observes [LiveData] from the [SavedViewModel] to update the list of downloaded videos,
     * handle empty states, and display toast messages.
     */
    private fun observeViewModelLiveData() {
        viewModel.downloadedVideos.observe(viewLifecycleOwner) { videos ->
            downloadedVideoAdapter.submitList(videos)
        }

        viewModel.isEmpty.observe(viewLifecycleOwner) { isEmpty ->
            binding.textViewNoDownloads.visibility = if (isEmpty) View.VISIBLE else View.GONE
            binding.recyclerViewSavedVideos.visibility = if (isEmpty) View.GONE else View.VISIBLE
        }

        viewModel.toastMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                viewModel.userShownToast() // Consume the message
            }
        }
    }

    /**
     * Shows an [AlertDialog] to confirm the deletion of a downloaded video or a broken entry.
     *
     * @param video The [DownloadedVideoInfo] of the video to be deleted/removed.
     * @param isBrokenEntry True if the dialog is for removing a broken/missing file entry, false for deleting an existing file.
     */
    private fun showDeleteConfirmationDialog(video: DownloadedVideoInfo, isBrokenEntry: Boolean = false) {
        val dialogTitle = if (isBrokenEntry) getString(R.string.remove_broken_entry_title) else getString(R.string.delete_video_dialog_title)
        val dialogMessage = if (isBrokenEntry) getString(R.string.remove_broken_entry_message, video.title)
                      else getString(R.string.delete_video_dialog_message, video.title)

        AlertDialog.Builder(requireContext())
            .setTitle(dialogTitle)
            .setMessage(dialogMessage)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(if(isBrokenEntry) getString(R.string.remove_action) else getString(R.string.delete)) { _, _ ->
                viewModel.deleteDownloadedVideo(video)
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewSavedVideos.adapter = null // Clear adapter for memory leak prevention
        _binding = null
    }
}
