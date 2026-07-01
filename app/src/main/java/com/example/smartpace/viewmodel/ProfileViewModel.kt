package com.example.smartpace.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartpace.model.UserProfile
import com.example.smartpace.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val repository = FirestoreRepository()
    private val auth = FirebaseAuth.getInstance()

    private val _profile = MutableStateFlow(UserProfile())
    val profile: StateFlow<UserProfile> = _profile

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val firebaseUser = auth.currentUser
            val name = firebaseUser?.displayName ?: ""
            val email = firebaseUser?.email ?: ""
            try {
                val firestoreProfile = repository.getUserProfile()
                if (firestoreProfile != null) {
                    _profile.value = firestoreProfile
                } else {
                    // Documento ainda não existe: cria com memberSince/metas padrão
                    // para que futuras gravações (peso, username) tenham onde persistir.
                    repository.createUserProfileIfNotExists(name, email)
                    _profile.value = repository.getUserProfile()
                        ?: UserProfile(id = firebaseUser?.uid ?: "", name = name, email = email)
                }
            } catch (e: Exception) {
                _profile.value = UserProfile(id = firebaseUser?.uid ?: "", name = name, email = email)
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

    fun updateWeight(weightKg: Double) {
        // Atualização otimista para refletir na UI imediatamente
        val updated = _profile.value.copy(weightKg = weightKg)
        _profile.value = updated
        viewModelScope.launch {
            try {
                // Grava o perfil inteiro (merge) — cria o doc se não existir,
                // sem sobrescrever os demais campos.
                repository.saveUserProfile(updated)
            } catch (e: Exception) { }
        }
    }

    /** Tenta reservar o username; chama onResult(true) se salvou, false se já em uso. */
    fun updateUsername(username: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.setUsername(username)
            if (ok) {
                _profile.value = _profile.value.copy(username = username.lowercase().trim())
            }
            onResult(ok)
        }
    }
}
