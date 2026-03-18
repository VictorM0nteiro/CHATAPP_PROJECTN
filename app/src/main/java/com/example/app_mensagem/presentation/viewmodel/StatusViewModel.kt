package com.example.app_mensagem.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.StatusRepository
import com.example.app_mensagem.data.model.StatusModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ContactStatusGroup(
    val userId: String,
    val userName: String,
    val userPicture: String?,
    val statuses: List<StatusModel>,
    val hasUnviewed: Boolean
)

sealed class StatusUiState {
    object Loading : StatusUiState()
    data class Success(
        val myStatuses: List<StatusModel>,
        val contactsGroups: List<ContactStatusGroup>
    ) : StatusUiState()
    data class Error(val message: String) : StatusUiState()
}

sealed class StatusPostState {
    object Idle : StatusPostState()
    object Posting : StatusPostState()
    object Done : StatusPostState()
    data class Error(val message: String) : StatusPostState()
}

class StatusViewModel : ViewModel() {

    private val repository = StatusRepository()

    private val _uiState = MutableStateFlow<StatusUiState>(StatusUiState.Loading)
    val uiState: StateFlow<StatusUiState> = _uiState

    private val _postState = MutableStateFlow<StatusPostState>(StatusPostState.Idle)
    val postState: StateFlow<StatusPostState> = _postState

    init { loadStatuses() }

    fun loadStatuses() {
        viewModelScope.launch {
            _uiState.value = StatusUiState.Loading
            try {
                val myStatuses = repository.getMyStatuses()
                val contactsMap = repository.getContactsStatuses()
                val currentUid = repository.getCurrentUserId()

                val groups = contactsMap.map { (userId, statuses) ->
                    val first = statuses.first()
                    val hasUnviewed = statuses.any { !it.viewers.containsKey(currentUid) }
                    ContactStatusGroup(userId, first.userName, first.userPicture, statuses, hasUnviewed)
                }.sortedByDescending { it.statuses.last().timestamp }

                _uiState.value = StatusUiState.Success(myStatuses, groups)
            } catch (e: Exception) {
                _uiState.value = StatusUiState.Error(e.message ?: "Erro ao carregar status")
            }
        }
    }

    fun postTextStatus(text: String, color: String) {
        viewModelScope.launch {
            _postState.value = StatusPostState.Posting
            try {
                repository.postTextStatus(text, color)
                _postState.value = StatusPostState.Done
                loadStatuses()
            } catch (e: Exception) {
                _postState.value = StatusPostState.Error(e.message ?: "Erro ao postar status")
            }
        }
    }

    fun postImageStatus(uri: Uri) {
        viewModelScope.launch {
            _postState.value = StatusPostState.Posting
            try {
                repository.postImageStatus(uri)
                _postState.value = StatusPostState.Done
                loadStatuses()
            } catch (e: Exception) {
                _postState.value = StatusPostState.Error(e.message ?: "Erro ao postar imagem")
            }
        }
    }

    fun markAsViewed(statusOwnerId: String, statusId: String) {
        viewModelScope.launch {
            try { repository.markAsViewed(statusOwnerId, statusId) } catch (_: Exception) {}
        }
    }

    fun resetPostState() { _postState.value = StatusPostState.Idle }
    fun getCurrentUserId() = repository.getCurrentUserId()
}
