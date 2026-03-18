package com.example.app_mensagem.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.data.CallsRepository
import com.example.app_mensagem.data.model.CallRecord
import com.example.app_mensagem.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class CallsUiState {
    object Loading : CallsUiState()
    data class Success(
        val callHistory: List<CallRecord>,
        val contacts: List<User>
    ) : CallsUiState()
    data class Error(val message: String) : CallsUiState()
}

class CallsViewModel : ViewModel() {

    private val repository = CallsRepository()

    private val _uiState = MutableStateFlow<CallsUiState>(CallsUiState.Loading)
    val uiState: StateFlow<CallsUiState> = _uiState

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = CallsUiState.Loading
            try {
                val history = repository.getCallHistory()
                val contacts = repository.getUsers()
                _uiState.value = CallsUiState.Success(history, contacts)
            } catch (e: Exception) {
                _uiState.value = CallsUiState.Error(e.message ?: "Erro ao carregar chamadas")
            }
        }
    }

    suspend fun initiateCall(receiver: User, type: String): CallRecord {
        val currentUser = repository.getCurrentUser()
        val record = CallRecord(
            id = UUID.randomUUID().toString(),
            callerId = repository.getCurrentUserId(),
            callerName = currentUser?.name ?: "",
            callerPicture = currentUser?.profilePictureUrl,
            receiverId = receiver.uid,
            receiverName = receiver.name,
            receiverPicture = receiver.profilePictureUrl,
            type = type,
            status = "OUTGOING",
            timestamp = System.currentTimeMillis(),
            duration = 0L
        )
        repository.saveCallRecord(record)
        return record
    }
}
