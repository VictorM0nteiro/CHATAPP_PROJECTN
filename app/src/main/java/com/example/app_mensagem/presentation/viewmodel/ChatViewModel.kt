package com.example.app_mensagem.presentation.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.ChatRepository
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.chat.ChatItem
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ChatUiState(
    val chatItems: List<ChatItem> = emptyList(),
    val messages: List<Message> = emptyList(),
    val filteredMessages: List<Message> = emptyList(),
    val searchQuery: String = "",
    val conversationTitle: String = "",
    val conversation: Conversation? = null,
    val pinnedMessage: Message? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val mediaToSendUri: Uri? = null,
    val mediaType: String? = null,
    val mediaFileName: String? = null,
    val groupMembers: Map<String, User> = emptyMap(),
    val isRecording: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ChatRepository
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var messagesJob: Job? = null
    private var conversationJob: Job? = null

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        repository = ChatRepository(db.conversationDao(), db.messageDao(), application)
    }

    private fun groupMessagesByDate(messages: List<Message>): List<ChatItem> {
        val items = mutableListOf<ChatItem>()
        if (messages.isEmpty()) return items

        var lastHeaderDate = ""
        messages.forEach { message ->
            val messageDateString = formatDateHeader(message.timestamp)
            if (messageDateString != lastHeaderDate) {
                items.add(ChatItem.DateHeader(messageDateString))
                lastHeaderDate = messageDateString
            }
            items.add(ChatItem.MessageItem(message))
        }
        return items
    }

    private fun formatDateHeader(timestamp: Long): String {
        val messageCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCalendar = Calendar.getInstance()
        val yesterdayCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

        return when {
            isSameDay(messageCalendar, todayCalendar) -> "Hoje"
            isSameDay(messageCalendar, yesterdayCalendar) -> "Ontem"
            else -> SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(messageCalendar.time)
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun onMediaSelected(uri: Uri?, type: String) {
        _uiState.value = _uiState.value.copy(
            mediaToSendUri = uri,
            mediaType = if (uri == null) null else type,
            mediaFileName = if (uri == null) null else resolveFileName(uri)
        )
    }

    fun clearSelectedMedia() {
        _uiState.value = _uiState.value.copy(
            mediaToSendUri = null,
            mediaType = null,
            mediaFileName = null
        )
    }

    fun onSearchQueryChanged(query: String) {
        val filteredList = if (query.isBlank()) {
            _uiState.value.messages
        } else {
            _uiState.value.messages.filter {
                (it.type == "TEXT" || it.type == "LOCATION") &&
                        it.content.contains(query, ignoreCase = true)
            }
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredMessages = filteredList,
            chatItems = groupMessagesByDate(filteredList)
        )
    }

    fun loadMessages(conversationId: String) {
        messagesJob?.cancel()
        conversationJob?.cancel()

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            messages = emptyList(),
            filteredMessages = emptyList(),
            chatItems = emptyList()
        )

        conversationJob = viewModelScope.launch {
            repository.observeConversation(conversationId).collectLatest { conversation ->
                if (conversation == null) {
                    _uiState.value = _uiState.value.copy(
                        error = "Conversa não encontrada.",
                        isLoading = false
                    )
                    return@collectLatest
                }

                var membersMap = _uiState.value.groupMembers
                if (conversation.isGroup && membersMap.isEmpty()) {
                    val members = repository.getGroupMembers(conversationId)
                    membersMap = members.associateBy { it.uid }
                }

                val pinnedMessage = if (!conversation.pinnedMessageId.isNullOrBlank()) {
                    repository.getMessageById(
                        conversationId = conversationId,
                        messageId = conversation.pinnedMessageId,
                        isGroup = conversation.isGroup
                    )
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    conversationTitle = conversation.name,
                    conversation = conversation,
                    groupMembers = membersMap,
                    pinnedMessage = pinnedMessage
                )
            }
        }

        messagesJob = viewModelScope.launch {
            val initialConversation = repository.getConversationDetails(conversationId)
            if (initialConversation != null) {
                _uiState.value = _uiState.value.copy(
                    conversationTitle = initialConversation.name,
                    conversation = initialConversation
                )
            }

            repository.getMessagesForConversation(
                conversationId,
                _uiState.value.conversation?.isGroup ?: false
            )
                .catch { exception ->
                    _uiState.value = _uiState.value.copy(
                        error = exception.message ?: "Erro ao carregar mensagens",
                        isLoading = false
                    )
                }
                .collectLatest { messages ->
                    val filteredList = if (_uiState.value.searchQuery.isBlank()) {
                        messages
                    } else {
                        messages.filter {
                            (it.type == "TEXT" || it.type == "LOCATION") &&
                                    it.content.contains(_uiState.value.searchQuery, ignoreCase = true)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        messages = messages,
                        filteredMessages = filteredList,
                        chatItems = groupMessagesByDate(filteredList),
                        isLoading = false
                    )

                    val isGroup = _uiState.value.conversation?.isGroup ?: false
                    if (!isGroup) {
                        repository.markMessagesAsRead(conversationId, messages, false)
                    }
                }
        }
    }

    fun sendMessage(conversationId: String, text: String) {
        viewModelScope.launch {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            val mediaUri = _uiState.value.mediaToSendUri

            try {
                if (mediaUri != null) {
                    val mediaType = _uiState.value.mediaType ?: "IMAGE"
                    repository.sendMediaMessage(conversationId, mediaUri, mediaType, isGroup)
                    clearSelectedMedia()
                } else if (text.isNotBlank()) {
                    repository.sendMessage(conversationId, text, isGroup)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar mensagem"
                )
            }
        }
    }

    fun sendLocation(conversationId: String) {
        viewModelScope.launch {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(getApplication<Application>())
            try {
                val location = fusedLocationClient.lastLocation.await()
                if (location != null) {
                    val locationString =
                        "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"
                    repository.sendMessage(
                        conversationId = conversationId,
                        content = locationString,
                        isGroup = _uiState.value.conversation?.isGroup ?: false,
                        type = "LOCATION"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "Não foi possível obter a localização atual")
                }
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(error = "Permissão de localização negada")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Falha ao obter localização")
            }
        }
    }

    fun onCameraPictureTaken(bitmap: Bitmap?) {
        if (bitmap == null) return

        try {
            val file = File(
                getApplication<Application>().cacheDir,
                "camera_${System.currentTimeMillis()}.jpg"
            )
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            val uri = Uri.fromFile(file)
            _uiState.value = _uiState.value.copy(
                mediaToSendUri = uri,
                mediaType = "IMAGE",
                mediaFileName = file.name
            )
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Erro ao processar foto da câmera", e)
            _uiState.value = _uiState.value.copy(error = "Erro ao processar foto da câmera")
        }
    }

    fun startRecording() {
        if (_uiState.value.isRecording) return

        try {
            val file = File(
                getApplication<Application>().cacheDir,
                "audio_record_${System.currentTimeMillis()}.m4a"
            )
            audioFile = file

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(96000)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }

            _uiState.value = _uiState.value.copy(
                isRecording = true,
                error = null
            )
        } catch (e: Exception) {
            mediaRecorder?.release()
            mediaRecorder = null
            audioFile = null
            Log.e("ChatViewModel", "Erro ao iniciar gravação", e)
            _uiState.value = _uiState.value.copy(error = "Não foi possível iniciar a gravação")
        }
    }

    fun stopRecording(conversationId: String) {
        if (!_uiState.value.isRecording) return

        val recorder = mediaRecorder
        mediaRecorder = null

        try {
            recorder?.stop()
        } catch (e: RuntimeException) {
            Log.e("ChatViewModel", "Erro ao parar gravação", e)
            audioFile?.delete()
            audioFile = null
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                error = "Gravação muito curta ou inválida"
            )
            recorder?.release()
            return
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Erro ao parar gravação", e)
            audioFile?.delete()
            audioFile = null
            _uiState.value = _uiState.value.copy(
                isRecording = false,
                error = "Erro ao finalizar gravação"
            )
            recorder?.release()
            return
        }

        recorder?.release()
        _uiState.value = _uiState.value.copy(isRecording = false)

        val file = audioFile
        audioFile = null

        if (file != null && file.exists() && file.length() > 0L) {
            viewModelScope.launch {
                try {
                    repository.sendMediaMessage(
                        conversationId = conversationId,
                        uri = Uri.fromFile(file),
                        type = "AUDIO",
                        isGroup = _uiState.value.conversation?.isGroup ?: false
                    )
                } catch (e: Exception) {
                    Log.e("ChatViewModel", "Erro ao enviar áudio", e)
                    _uiState.value = _uiState.value.copy(
                        error = "Erro ao enviar áudio: ${e.message ?: "falha desconhecida"}"
                    )
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(error = "Áudio inválido ou vazio")
        }
    }

    fun cancelRecording() {
        val recorder = mediaRecorder
        mediaRecorder = null

        try {
            recorder?.reset()
        } catch (_: Exception) {
        }

        try {
            recorder?.release()
        } catch (_: Exception) {
        }

        audioFile?.delete()
        audioFile = null

        if (_uiState.value.isRecording) {
            _uiState.value = _uiState.value.copy(isRecording = false)
        }
    }

    fun sendSticker(conversationId: String, stickerId: String) {
        viewModelScope.launch {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            try {
                repository.sendStickerMessage(conversationId, stickerId, isGroup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao enviar sticker"
                )
            }
        }
    }

    fun onReactionClick(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isGroup = _uiState.value.conversation?.isGroup ?: false
            try {
                repository.toggleReaction(conversationId, messageId, emoji, isGroup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Erro ao reagir à mensagem"
                )
            }
        }
    }

    fun onPinMessageClick(conversationId: String, message: Message) {
        viewModelScope.launch {
            try {
                val isGroup = _uiState.value.conversation?.isGroup ?: false
                repository.togglePinMessage(conversationId, message, isGroup)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Falha ao fixar mensagem"
                )
            }
        }
    }

    private fun resolveFileName(uri: Uri): String? {
        val context = getApplication<Application>()
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && index >= 0) {
                    cursor.getString(index)
                } else {
                    uri.lastPathSegment
                }
            } ?: uri.lastPathSegment
        } catch (_: Exception) {
            uri.lastPathSegment
        }
    }

    override fun onCleared() {
        cancelRecording()
        messagesJob?.cancel()
        conversationJob?.cancel()
        super.onCleared()
    }
}
