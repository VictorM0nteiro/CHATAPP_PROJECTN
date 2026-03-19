package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.ProfileRepository
import com.example.app_mensagem.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isSaved: Boolean = false
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository()

    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSaved = false)
            try {
                val user = repository.getUserProfile()
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user = user
                )
            } catch (e: Exception) {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    error = e.message ?: "Erro ao carregar perfil."
                )
            }
        }
    }

    fun updateProfile(name: String, phoneNumber: String, status: String, updateStatus: String, imageUri: Uri?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, isSaved = false)
            try {
                repository.updateProfile(name, phoneNumber, status, updateStatus, imageUri)
                val user = repository.getUserProfile()
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    user = user,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Erro ao salvar perfil."
                )
            }
        }
    }

    fun updatePresenceStatus(presenceStatus: String) {
        viewModelScope.launch {
            try {
                repository.updatePresenceStatus(presenceStatus)
                val currentUser = _uiState.value.user
                if (currentUser != null) {
                    _uiState.value = _uiState.value.copy(
                        user = currentUser.copy(presenceStatus = presenceStatus)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao atualizar status."
                )
            }
        }
    }

    fun clearSavedFlag() {
        _uiState.value = _uiState.value.copy(isSaved = false)
    }
}
