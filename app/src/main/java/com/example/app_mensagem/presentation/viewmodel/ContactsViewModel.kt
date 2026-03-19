package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ContactsUiState {
    object Loading : ContactsUiState()
    data class Success(val users: List<User>) : ContactsUiState()
    data class Error(val message: String) : ContactsUiState()
}

sealed class ContactNavigationState {
    object Idle : ContactNavigationState()
    data class NavigateToChat(val conversationId: String) : ContactNavigationState()
}

sealed class PhoneSearchState {
    object Idle : PhoneSearchState()
    object Searching : PhoneSearchState()
    data class Found(val user: User) : PhoneSearchState()
    object NotFound : PhoneSearchState()
}

sealed class GroupCreationState {
    object Idle : GroupCreationState()
    object Creating : GroupCreationState()
    object Success : GroupCreationState()
    data class Error(val message: String) : GroupCreationState()
}

class ContactsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)
    val uiState: StateFlow<ContactsUiState> = _uiState

    private val _navigationState = MutableStateFlow<ContactNavigationState>(ContactNavigationState.Idle)
    val navigationState: StateFlow<ContactNavigationState> = _navigationState

    private val _phoneSearchState = MutableStateFlow<PhoneSearchState>(PhoneSearchState.Idle)
    val phoneSearchState: StateFlow<PhoneSearchState> = _phoneSearchState

    private val _groupCreationState = MutableStateFlow<GroupCreationState>(GroupCreationState.Idle)
    val groupCreationState: StateFlow<GroupCreationState> = _groupCreationState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao(), application)
        loadUsers()
    }

    fun loadUsers() {
        viewModelScope.launch {
            _uiState.value = ContactsUiState.Loading
            try {
                val users = repository.getUsers()
                _uiState.value = ContactsUiState.Success(users)
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.Error(e.message ?: "Falha ao carregar usuários.")
            }
        }
    }

    fun searchByPhone(phoneNumber: String) {
        if (phoneNumber.isBlank()) {
            _phoneSearchState.value = PhoneSearchState.Idle
            return
        }
        viewModelScope.launch {
            _phoneSearchState.value = PhoneSearchState.Searching
            try {
                val user = repository.searchUserByPhone(phoneNumber.trim())
                _phoneSearchState.value = if (user != null) {
                    PhoneSearchState.Found(user)
                } else {
                    PhoneSearchState.NotFound
                }
            } catch (e: Exception) {
                _phoneSearchState.value = PhoneSearchState.NotFound
            }
        }
    }

    fun resetPhoneSearch() {
        _phoneSearchState.value = PhoneSearchState.Idle
    }

    fun importContacts() {
        viewModelScope.launch {
            _uiState.value = ContactsUiState.Loading
            try {
                val deviceContacts = repository.importDeviceContacts()
                loadUsers()
                Toast.makeText(getApplication(), "${deviceContacts.size} contatos lidos da agenda.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.Error("Erro ao acessar agenda: ${e.message}")
            }
        }
    }

    fun onUserClicked(user: User) {
        viewModelScope.launch {
            try {
                val conversationId = repository.createOrGetConversation(user)
                _navigationState.value = ContactNavigationState.NavigateToChat(conversationId)
            } catch (e: Exception) {
                _uiState.value = ContactsUiState.Error(e.message ?: "Falha ao criar conversa")
            }
        }
    }

    fun createGroup(name: String, memberIds: List<String>) {
        viewModelScope.launch {
            _groupCreationState.value = GroupCreationState.Creating
            try {
                repository.createGroup(name, memberIds)
                _groupCreationState.value = GroupCreationState.Success
            } catch (e: Exception) {
                _groupCreationState.value = GroupCreationState.Error(
                    e.message ?: "Falha ao criar grupo"
                )
            }
        }
    }

    fun resetGroupCreationState() {
        _groupCreationState.value = GroupCreationState.Idle
    }

    fun onNavigated() {
        _navigationState.value = ContactNavigationState.Idle
    }
}
