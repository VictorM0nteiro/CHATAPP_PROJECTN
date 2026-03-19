package com.example.app_mensagem.presentation.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.common.LifecycleObserver
import com.example.app_mensagem.presentation.viewmodel.ChatViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(navController: NavController, conversationId: String?) {
    val chatViewModel: ChatViewModel = viewModel()
    val uiState by chatViewModel.uiState.collectAsState()
    var text by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var selectedMessageId by remember { mutableStateOf<String?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                chatViewModel.onMediaSelected(uri, "IMAGE")
            }
        }
    )

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                chatViewModel.onMediaSelected(uri, "VIDEO")
            }
        }
    )

    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {
                }
                chatViewModel.onMediaSelected(uri, "DOCUMENT")
            }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            chatViewModel.onCameraPictureTaken(bitmap)
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && conversationId != null) {
            chatViewModel.sendLocation(conversationId)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            chatViewModel.startRecording()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        }
    }

    LaunchedEffect(conversationId) {
        if (conversationId != null) {
            chatViewModel.loadMessages(conversationId)
        }
    }

    LifecycleObserver { event ->
        if (event == Lifecycle.Event.ON_STOP && uiState.isRecording) {
            chatViewModel.cancelRecording()
        }
    }

    Scaffold(
        topBar = {
            val isGroup = uiState.conversation?.isGroup ?: false
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { chatViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Buscar na conversa...") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onPrimary,
                                focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                unfocusedTextColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onPrimary)
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = isGroup) {
                                if (conversationId != null) {
                                    navController.navigate("group_info/$conversationId")
                                }
                            }
                        ) {
                            AsyncImage(
                                model = uiState.conversation?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                                contentDescription = "Foto de Perfil de ${uiState.conversationTitle}",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.conversationTitle,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.conversation?.isGroup == true) {
                                    Text(
                                        text = "Grupo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    if (isSearchActive) {
                        IconButton(onClick = {
                            isSearchActive = false
                            chatViewModel.onSearchQueryChanged("")
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Fechar Busca",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Buscar Mensagem",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                if (conversationId != null) {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                                    if (hasPermission) {
                                        chatViewModel.sendLocation(conversationId)
                                    } else {
                                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = "Enviar Localização",
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                AnimatedVisibility(visible = uiState.mediaToSendUri != null) {
                    AttachmentPreview(
                        mediaType = uiState.mediaType,
                        fileName = uiState.mediaFileName,
                        uriString = uiState.mediaToSendUri?.toString().orEmpty(),
                        onRemove = { chatViewModel.clearSelectedMedia() }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                        Icon(
                            Icons.Default.VideoLibrary,
                            contentDescription = "Enviar vídeo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { documentPickerLauncher.launch(arrayOf("*/*")) }) {
                        Icon(
                            Icons.Default.Description,
                            contentDescription = "Enviar documento",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Tirar foto",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = {
                            if (uiState.isRecording) {
                                if (conversationId != null) {
                                    chatViewModel.stopRecording(conversationId)
                                }
                            } else {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                                if (hasPermission) {
                                    chatViewModel.startRecording()
                                } else {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (uiState.isRecording) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = if (uiState.isRecording) "Parar gravação" else "Gravar áudio",
                            tint = if (uiState.isRecording) Color.Red else MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (uiState.isRecording) "Gravando..." else "Digite uma mensagem...",
                                maxLines = 1
                            )
                        },
                        singleLine = true,
                        enabled = uiState.mediaToSendUri == null && !uiState.isRecording,
                        shape = RoundedCornerShape(24.dp)
                    )

                    IconButton(
                        onClick = {
                            if (conversationId != null) {
                                chatViewModel.sendMessage(conversationId, text.trim())
                                text = ""
                            }
                        },
                        enabled = (text.isNotBlank() || uiState.mediaToSendUri != null) && conversationId != null && !uiState.isRecording
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar",
                            tint = if ((text.isNotBlank() || uiState.mediaToSendUri != null) && conversationId != null && !uiState.isRecording) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            }
                        )
                    }
                }

                uiState.error?.let { errorText ->
                    Text(
                        text = errorText,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                uiState.pinnedMessage?.let { pinned ->
                    PinnedMessageBar(
                        message = pinned,
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid,
                        onClick = {},
                        onUnpinClick = {
                            if (conversationId != null) {
                                chatViewModel.onPinMessageClick(conversationId, pinned)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.chatItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "Nenhuma mensagem encontrada para \"${uiState.searchQuery}\""
                            } else {
                                "Envie a primeira mensagem."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        state = listState,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = uiState.chatItems,
                            key = {
                                when (it) {
                                    is ChatItem.DateHeader -> "header_${it.date}"
                                    is ChatItem.MessageItem -> it.message.id
                                }
                            }
                        ) { item ->
                            when (item) {
                                is ChatItem.DateHeader -> {
                                    DateChip(dateText = item.date)
                                }

                                is ChatItem.MessageItem -> {
                                    val message = item.message
                                    val isMine = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
                                    MessageBubble(
                                        message = message,
                                        isMine = isMine,
                                        highlightQuery = uiState.searchQuery,
                                        groupMembers = uiState.groupMembers,
                                        onLongPress = {
                                            selectedMessageId = message.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LaunchedEffect(uiState.chatItems.size) {
                        if (uiState.chatItems.isNotEmpty()) {
                            scope.launch {
                                listState.animateScrollToItem(uiState.chatItems.lastIndex)
                            }
                        }
                    }
                }
            }

            if (selectedMessageId != null) {
                val selectedMessage = uiState.messages.find { it.id == selectedMessageId }
                selectedMessage?.let { msg ->
                    MessageActionsBar(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 88.dp),
                        isPinned = uiState.pinnedMessage?.id == msg.id,
                        onDismiss = { selectedMessageId = null },
                        onPinClick = {
                            if (conversationId != null) {
                                chatViewModel.onPinMessageClick(conversationId, msg)
                            }
                            selectedMessageId = null
                        },
                        onReactionClick = { emoji ->
                            if (conversationId != null) {
                                chatViewModel.onReactionClick(conversationId, msg.id, emoji)
                            }
                            selectedMessageId = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreview(
    mediaType: String?,
    fileName: String?,
    uriString: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(start = 12.dp, end = 12.dp, top = 4.dp)
            .fillMaxWidth()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (mediaType) {
                    "IMAGE" -> {
                        Image(
                            painter = rememberAsyncImagePainter(uriString),
                            contentDescription = "Imagem selecionada",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    "VIDEO" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                        }
                    }

                    "DOCUMENT" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    else -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(84.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.AttachFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (mediaType) {
                            "IMAGE" -> "Imagem pronta para envio"
                            "VIDEO" -> "Vídeo pronto para envio"
                            "DOCUMENT" -> "Documento pronto para envio"
                            else -> "Arquivo pronto para envio"
                        },
                        style = MaterialTheme.typography.titleSmall
                    )

                    if (!fileName.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.08f), CircleShape)
                        .size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remover anexo",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DateChip(dateText: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.07f),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = dateText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun PinnedMessageBar(
    message: Message,
    currentUserId: String?,
    onClick: () -> Unit,
    onUnpinClick: () -> Unit
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.PushPin,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Mensagem fixada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = pinnedPreviewText(message, currentUserId),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
            IconButton(onClick = onUnpinClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Desafixar",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun pinnedPreviewText(message: Message, currentUserId: String?): String {
    return when (message.type) {
        "IMAGE" -> "📷 Imagem"
        "VIDEO" -> "🎥 Vídeo"
        "AUDIO" -> "🎤 Mensagem de voz"
        "DOCUMENT" -> "📄 ${message.fileName ?: "Documento"}"
        "LOCATION" -> "📍 Localização"
        "STICKER" -> "Figurinha"
        else -> message.content.take(80) + if (message.content.length > 80) "…" else ""
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isMine: Boolean,
    highlightQuery: String,
    groupMembers: Map<String, User>,
    onLongPress: () -> Unit
) {
    val backgroundColor =
        if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val contentColor =
        if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        if (!isMine) {
            Spacer(modifier = Modifier.width(32.dp))
        }

        Column(
            horizontalAlignment = if (isMine) Alignment.End else Alignment.Start,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            )
        ) {
            if (!isMine && groupMembers.isNotEmpty()) {
                val senderName = groupMembers[message.senderId]?.name
                if (!senderName.isNullOrBlank()) {
                    Text(
                        text = senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isMine) 18.dp else 4.dp,
                    bottomEnd = if (isMine) 4.dp else 18.dp
                ),
                color = backgroundColor,
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .widthIn(max = 280.dp)
                ) {
                    when (message.type) {
                        "IMAGE" -> {
                            AsyncImage(
                                model = message.content,
                                contentDescription = "Imagem",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }

                        "VIDEO" -> {
                            Column {
                                message.thumbnailUrl?.let { thumb ->
                                    AsyncImage(
                                        model = thumb,
                                        contentDescription = "Miniatura do vídeo",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = message.fileName ?: "Vídeo",
                                        color = contentColor
                                    )
                                }
                            }
                        }

                        "AUDIO" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = null,
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Mensagem de voz",
                                    color = contentColor
                                )
                            }
                        }

                        "DOCUMENT" -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Description,
                                    contentDescription = null,
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = message.fileName ?: "Documento",
                                    color = contentColor
                                )
                            }
                        }

                        "LOCATION" -> {
                            Text(
                                text = "📍 Localização",
                                color = contentColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            HighlightingText(
                                text = message.content,
                                query = highlightQuery,
                                color = contentColor
                            )
                        }

                        "STICKER" -> {
                            Text(
                                text = message.content,
                                fontSize = 28.sp
                            )
                        }

                        else -> {
                            HighlightingText(
                                text = message.content,
                                query = highlightQuery,
                                color = contentColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = contentColor.copy(alpha = 0.8f)
                        )

                        if (isMine) {
                            Spacer(modifier = Modifier.width(4.dp))
                            val statusIcon = when {
                                message.readTimestamp > 0L -> Icons.Default.DoneAll
                                message.deliveredTimestamp > 0L -> Icons.Default.DoneAll
                                message.status == "SENT" -> Icons.Default.Done
                                message.status == "FAILED" -> Icons.Default.Error
                                else -> Icons.Default.Schedule
                            }
                            val statusTint = when {
                                message.readTimestamp > 0L -> Color(0xFF4FC3F7)
                                message.status == "FAILED" -> Color.Red
                                else -> contentColor.copy(alpha = 0.8f)
                            }
                            Icon(
                                imageVector = statusIcon,
                                contentDescription = null,
                                tint = statusTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (message.reactions.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            message.reactions.values
                                .groupingBy { it }
                                .eachCount()
                                .forEach { (emoji, count) ->
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.Black.copy(alpha = 0.08f)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(text = emoji, fontSize = 14.sp)
                                            if (count > 1) {
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = count.toString(),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HighlightingText(
    text: String,
    query: String,
    color: Color
) {
    if (query.isBlank()) {
        Text(text = text, color = color)
        return
    }

    val pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE)
    val matcher = pattern.matcher(text)

    var lastIndex = 0
    val annotatedString = buildAnnotatedString {
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            if (start > lastIndex) {
                append(text.substring(lastIndex, start))
            }

            withStyle(
                style = SpanStyle(
                    background = Color.Yellow.copy(alpha = 0.3f),
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(text.substring(start, end))
            }

            lastIndex = end
        }

        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }

    Text(
        text = annotatedString,
        color = color
    )
}

@Composable
private fun MessageActionsBar(
    modifier: Modifier = Modifier,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onPinClick: () -> Unit,
    onReactionClick: (String) -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val reactions = listOf("👍", "❤️", "😂", "😮", "😢", "👏")
                reactions.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable {
                                onReactionClick(emoji)
                                onDismiss()
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(
                    onClick = {
                        onPinClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors()
                ) {
                    Icon(Icons.Default.PushPin, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isPinned) "Desafixar" else "Fixar")
                }

                TextButton(onClick = onDismiss) {
                    Text("Fechar")
                }
            }
        }
    }
}
