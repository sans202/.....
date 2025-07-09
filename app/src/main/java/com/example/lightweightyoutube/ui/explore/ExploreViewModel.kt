package com.example.lightweightyoutube.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.lightweightyoutube.data.model.InvidiousSearchItem
import com.example.lightweightyoutube.data.repository.Resource
import com.example.lightweightyoutube.data.repository.VideoRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ViewModel for the Explore screen.
 * Handles video searching logic, manages UI state related to search results, loading, and errors.
 *
 * @param application The application context, needed for [AndroidViewModel] and [VideoRepository].
 */
class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRepository = VideoRepository(application) // Repository for fetching video data

    private val _searchResults = MutableLiveData<List<InvidiousSearchItem>>()
    /** LiveData holding the list of video search results. */
    val searchResults: LiveData<List<InvidiousSearchItem>> = _searchResults

    private val _isLoading = MutableLiveData<Boolean>()
    /** LiveData indicating if a search operation is in progress. */
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    /** LiveData holding error messages from search operations. Null if no error. */
    val error: LiveData<String?> = _error

    private val _noResults = MutableLiveData<Boolean>()
    /** LiveData indicating if the last search yielded no results. */
    val noResults: LiveData<Boolean> = _noResults

    private var currentQuery: String = "" // Stores the current search query
    private var searchJob: Job? = null // Coroutine job for the current search operation

    private val debouncePeriod = 500L // Debounce period in milliseconds for search input

    init {
        // Optionally, load initial popular videos or recommendations here.
        // Example: onSearchQueryChanged("Popular Technology")
    }

    /**
     * Called when the search query text changes in the UI.
     * Initiates a debounced search operation for the given query.
     * If the query is blank, it clears current results and states.
     *
     * @param query The search string from the UI.
     */
    fun onSearchQueryChanged(query: String) {
        val trimmedQuery = query.trim()
        // Avoid re-searching if query hasn't changed meaningfully and results are already present
        if (trimmedQuery == currentQuery && (_searchResults.value != null && _searchResults.value!!.isNotEmpty())) {
            return
        }
        currentQuery = trimmedQuery
        searchJob?.cancel() // Cancel any existing search job

        if (trimmedQuery.isBlank()) {
            _searchResults.postValue(emptyList())
            _noResults.postValue(false)
            _isLoading.postValue(false)
            _error.postValue(null)
            return
        }

        searchJob = viewModelScope.launch {
            delay(debouncePeriod) // Apply debounce to avoid rapid API calls
            _isLoading.postValue(true)
            _error.postValue(null)
            _noResults.postValue(false)

            val result = videoRepository.searchVideos(trimmedQuery) // Perform search via repository

            when (result) {
                is Resource.Success -> {
                    val videos = result.data ?: emptyList()
                    // Filter for actual video items and ensure they have a videoId
                    _searchResults.postValue(videos.filter { it.type == "video" && !it.videoId.isNullOrBlank() })
                    _noResults.postValue(videos.isEmpty())
                }
                is Resource.Error -> {
                    _error.postValue(result.message ?: "Unknown error while searching")
                    _searchResults.postValue(emptyList()) // Clear previous results on error
                    _noResults.postValue(false) // Not "no results" but an error state
                }
            }
            _isLoading.postValue(false)
        }
    }

    /**
     * Retries the last executed search query.
     * Useful for network errors or other transient issues.
     */
    fun retryLastSearch() {
        if (currentQuery.isNotBlank()) {
            // Force re-evaluation by effectively treating it as a new query submission
            val queryToRetry = currentQuery
            searchJob?.cancel() // Cancel any pending job for the same query
            currentQuery = "" // Temporarily change to ensure onSearchQueryChanged re-triggers fully
            onSearchQueryChanged(queryToRetry)
        }
    }

    /**
     * Call this method after an error message has been displayed to the user (e.g., in a Toast or Snackbar),
     * to clear the error state and prevent it from being shown again on configuration changes.
     */
    fun userShownErrorMessage() {
        _error.postValue(null)
    }
}
