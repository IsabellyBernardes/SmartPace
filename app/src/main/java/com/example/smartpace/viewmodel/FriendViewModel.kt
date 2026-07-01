package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.Friend
import com.example.smartpace.model.FriendRequest
import com.example.smartpace.model.UserProfile
import com.example.smartpace.repository.FirestoreRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class SearchState {
    object Idle : SearchState()
    object Searching : SearchState()
    data class Found(val profile: UserProfile) : SearchState()
    object NotFound : SearchState()
}

class FriendViewModel : ViewModel() {

    private val repository = FirestoreRepository()

    private val _searchState = MutableStateFlow<SearchState>(SearchState.Idle)
    val searchState: StateFlow<SearchState> = _searchState

    private val _requests = MutableStateFlow<List<FriendRequest>>(emptyList())
    val requests: StateFlow<List<FriendRequest>> = _requests

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    val friends: StateFlow<List<Friend>> = _friends

    private val _requestSent = MutableStateFlow(false)
    val requestSent: StateFlow<Boolean> = _requestSent

    init {
        loadRequests()
        loadFriends()
    }

    fun search(username: String) {
        if (username.isBlank()) {
            _searchState.value = SearchState.Idle
            return
        }
        viewModelScope.launch {
            _searchState.value = SearchState.Searching
            val profile = repository.searchUserByUsername(username)
            _searchState.value = if (profile != null) SearchState.Found(profile) else SearchState.NotFound
        }
    }

    fun clearSearch() {
        _searchState.value = SearchState.Idle
        _requestSent.value = false
    }

    fun sendRequest(toUid: String) {
        viewModelScope.launch {
            try {
                repository.sendFriendRequest(toUid)
                _requestSent.value = true
            } catch (e: Exception) { }
        }
    }

    fun loadRequests() {
        viewModelScope.launch {
            _requests.value = repository.getIncomingRequests()
        }
    }

    fun loadFriends() {
        viewModelScope.launch {
            _friends.value = repository.getFriends()
        }
    }

    fun accept(request: FriendRequest) {
        viewModelScope.launch {
            try {
                repository.acceptFriendRequest(request)
                loadRequests()
                loadFriends()
            } catch (e: Exception) { }
        }
    }

    fun reject(request: FriendRequest) {
        viewModelScope.launch {
            try {
                repository.rejectFriendRequest(request.id)
                loadRequests()
            } catch (e: Exception) { }
        }
    }
}
