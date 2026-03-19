package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Conversation
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

sealed class ConversationUiState {
    data object Loading : ConversationUiState()
    data class Success(
        val conversations: List<Conversation>,
        val searchQuery: String = "",
        val selectedTab: ConversationTab = ConversationTab.ALL,
        val presenceStatuses: Map<String, String> = emptyMap()
    ) : ConversationUiState()
    data class Error(val message: String) : ConversationUiState()
}

enum class ConversationTab {
    ALL,
    FAVORITES,
    GROUPS
}

class ConversationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private val _uiState = MutableStateFlow<ConversationUiState>(ConversationUiState.Loading)
    val uiState: StateFlow<ConversationUiState> = _uiState

    private var allConversations: List<Conversation> = emptyList()
    private var searchQuery: String = ""
    private var selectedTab: ConversationTab = ConversationTab.ALL
    private var presenceMap: Map<String, String> = emptyMap()
    private var presenceJob: Job? = null

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(
            db.conversationDao(),
            db.messageDao(),
            application
        )

        repository.startConversationListener()

        viewModelScope.launch {
            try {
                repository.syncUserConversations()
                repository.getConversations().collectLatest { conversations ->
                    allConversations = conversations
                    publishState()

                    if (presenceMap.isEmpty()) {
                        loadPresenceStatuses(conversations)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ConversationUiState.Error(
                    e.message ?: "Erro ao carregar conversas."
                )
            }
        }
    }

    private fun loadPresenceStatuses(conversations: List<Conversation>) {
        presenceJob?.cancel()
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val nonGroupConversations = conversations.filter { !it.isGroup }
        if (nonGroupConversations.isEmpty()) return

        presenceJob = viewModelScope.launch {
            try {
                val results = nonGroupConversations.map { conv ->
                    async {
                        val otherUid = conv.id.replace(currentUid, "").replace("-", "")
                        if (otherUid.isNotBlank()) {
                            val status = try {
                                repository.getUserPresenceStatus(otherUid)
                            } catch (_: Exception) {
                                "offline"
                            }
                            conv.id to status
                        } else null
                    }
                }.awaitAll().filterNotNull()

                presenceMap = results.toMap()
                publishState()
            } catch (_: Exception) { }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        publishState()
    }

    fun onTabSelected(tab: ConversationTab) {
        selectedTab = tab
        publishState()
    }

    fun toggleFavorite(conversationId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(conversationId)
        }
    }

    fun resyncConversations() {
        viewModelScope.launch {
            repository.syncUserConversations()
        }
        loadPresenceStatuses(allConversations)
    }

    private fun publishState() {
        val filteredByTab = when (selectedTab) {
            ConversationTab.ALL -> allConversations
            ConversationTab.FAVORITES -> allConversations.filter { it.isFavorite }
            ConversationTab.GROUPS -> allConversations.filter { it.isGroup }
        }

        val filtered = if (searchQuery.isBlank()) {
            filteredByTab
        } else {
            filteredByTab.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.lastMessage.contains(searchQuery, ignoreCase = true)
            }
        }

        _uiState.value = ConversationUiState.Success(
            conversations = filtered.sortedByDescending { it.timestamp },
            searchQuery = searchQuery,
            selectedTab = selectedTab,
            presenceStatuses = presenceMap
        )
    }

    override fun onCleared() {
        repository.stopConversationListener()
        super.onCleared()
    }
}
