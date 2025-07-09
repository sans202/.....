package com.example.lightweightyoutube.ui.explore

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lightweightyoutube.R
import com.example.lightweightyoutube.databinding.FragmentExploreBinding
import com.example.lightweightyoutube.ui.video_detail.VideoDetailActivity

/**
 * A [Fragment] for exploring and searching videos.
 * It displays a search bar and a list of video results.
 * Handles user input for search, observes [ExploreViewModel] for data,
 * and navigates to [VideoDetailActivity] when a video is selected.
 */
class ExploreFragment : Fragment() {

    private var _binding: FragmentExploreBinding? = null // ViewBinding instance
    // This property is only valid between onCreateView and onDestroyView.
    private val binding get() = _binding!!

    private val viewModel: ExploreViewModel by viewModels()
    private lateinit var videoAdapter: VideoAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentExploreBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearch()
        observeViewModelLiveData()

        binding.buttonRetry.setOnClickListener {
            viewModel.retryLastSearch()
        }

        // Restore search query on configuration change if it existed and search view is empty
        if (savedInstanceState != null && binding.searchView.query.isNullOrBlank()) {
            val previousQuery = savedInstanceState.getString(CURRENT_QUERY_KEY)
            if (!previousQuery.isNullOrBlank()) {
                // Set query without submitting immediately, onQueryTextChange will handle it
                binding.searchView.setQuery(previousQuery, false)
            }
        } else if (viewModel.searchResults.value.isNullOrEmpty() && binding.searchView.query.isNullOrBlank()){
             // Optional: Perform an initial default search if the list is empty and no query exists.
             // viewModel.onSearchQueryChanged("Popular videos")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save current search query to restore on configuration change
        if (binding.searchView.query.toString().isNotBlank()) {
            outState.putString(CURRENT_QUERY_KEY, binding.searchView.query.toString())
        }
    }

    /**
     * Sets up the RecyclerView with its adapter and layout manager.
     * Defines the click listener for video items to navigate to [VideoDetailActivity].
     */
    private fun setupRecyclerView() {
        videoAdapter = VideoAdapter { video ->
            video.videoId?.let { videoId ->
                val intent = VideoDetailActivity.newIntent(requireContext(), videoId)
                startActivity(intent)
            } ?: Toast.makeText(context, getString(R.string.error_video_id_missing), Toast.LENGTH_LONG).show()
        }

        binding.recyclerViewVideos.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = videoAdapter
            // Optional: addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
    }

    /**
     * Configures the SearchView, including its query text listener
     * to trigger searches via the [ExploreViewModel].
     */
    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let {
                    if (it.trim().isNotBlank()) {
                        viewModel.onSearchQueryChanged(it.trim())
                        binding.searchView.clearFocus() // Hide keyboard after submission
                    }
                }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // ViewModel handles debouncing for live search
                viewModel.onSearchQueryChanged(newText?.trim() ?: "")
                return true
            }
        })
    }

    /**
     * Observes [LiveData] from the [ExploreViewModel] to update the UI
     * based on search results, loading state, and errors.
     */
    private fun observeViewModelLiveData() {
        viewModel.searchResults.observe(viewLifecycleOwner) { videos ->
            videoAdapter.submitList(videos)
            // Visibility of RecyclerView itself is managed by isLoading, error, and noResults observers
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            if (isLoading) { // When loading, hide other potentially conflicting views
                binding.textViewError.visibility = View.GONE
                binding.buttonRetry.visibility = View.GONE
                binding.textViewNoResults.visibility = View.GONE
                binding.recyclerViewVideos.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            val currentlyLoading = viewModel.isLoading.value ?: false
            if (errorMessage != null && !currentlyLoading) { // Show error only if not loading
                binding.textViewError.text = errorMessage
                binding.textViewError.visibility = View.VISIBLE
                binding.buttonRetry.visibility = View.VISIBLE
                binding.recyclerViewVideos.visibility = View.GONE
                binding.textViewNoResults.visibility = View.GONE
            } else if (errorMessage == null) { // No error, ensure error views are hidden
                 binding.textViewError.visibility = View.GONE
                 binding.buttonRetry.visibility = View.GONE
            }
        }

        viewModel.noResults.observe(viewLifecycleOwner) { noResults ->
            val currentlyLoading = viewModel.isLoading.value ?: false
            val currentError = viewModel.error.value
            // Show "no results" only if not loading, no error, and search results are indeed empty
            if (noResults && !currentlyLoading && currentError == null) {
                binding.textViewNoResults.visibility = View.VISIBLE
                binding.recyclerViewVideos.visibility = View.GONE
            } else if (!noResults) {
                binding.textViewNoResults.visibility = View.GONE
                // Show RecyclerView if there are results and not loading/error
                 if (currentError == null && !currentlyLoading && viewModel.searchResults.value?.isNotEmpty() == true) {
                    binding.recyclerViewVideos.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerViewVideos.adapter = null // Clear adapter to help prevent memory leaks
        _binding = null
    }

    companion object {
        private const val CURRENT_QUERY_KEY = "current_query"
    }
}
