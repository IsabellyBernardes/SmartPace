package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.UserProfile
import com.example.smartpace.repository.FirestoreRepository
import com.example.smartpace.repository.MockData
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _profile = MutableStateFlow(MockData.currentUser)
    val profile: StateFlow<UserProfile> = _profile

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            try {
                val firestoreProfile = repository.getUserProfile()
                if (firestoreProfile != null) {
                    _profile.value = firestoreProfile
                } else {
                    val firebaseUser = auth.currentUser
                    _profile.value = MockData.currentUser.copy(
                        id = firebaseUser?.uid ?: "",
                        name = firebaseUser?.displayName ?: MockData.currentUser.name,
                        email = firebaseUser?.email ?: MockData.currentUser.email
                    )
                }
            } catch (e: Exception) {
                val firebaseUser = auth.currentUser
                _profile.value = MockData.currentUser.copy(
                    name = firebaseUser?.displayName ?: MockData.currentUser.name,
                    email = firebaseUser?.email ?: MockData.currentUser.email
                )
            }
        }
    }

    fun createProfileAfterRegister(name: String, email: String) {
        viewModelScope.launch {
            try {
                repository.createUserProfileIfNotExists(name, email)
                loadProfile()
            } catch (e: Exception) { }
        }
    }
}
