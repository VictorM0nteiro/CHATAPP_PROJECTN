package com.example.app_mensagem.presentation.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri as AndroidUri
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlinx.coroutines.delay
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
    var showMediaSheet by remember { mutableStateOf(false) }
    var showStickerPanel by remember { mutableStateOf(false) }
    var playingAudioId by remember { mutableStateOf<String?>(null) }
    var audioProgress by remember { mutableFloatStateOf(0f) }
    val mediaPlayer = remember { MediaPlayer() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.release()
        }
    }

    fun playAudio(messageId: String, url: String) {
        if (playingAudioId == messageId) {
            mediaPlayer.stop()
            mediaPlayer.reset()
            playingAudioId = null
            audioProgress = 0f
            return
        }

        mediaPlayer.reset()
        playingAudioId = messageId
        audioProgress = 0f

        try {
            mediaPlayer.setDataSource(url)
            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener { mp ->
                mp.start()
                scope.launch {
                    while (playingAudioId == messageId && mp.isPlaying) {
                        audioProgress = mp.currentPosition.toFloat() / mp.duration.coerceAtLeast(1).toFloat()
                        delay(200)
                    }
                }
            }
            mediaPlayer.setOnCompletionListener {
                playingAudioId = null
                audioProgress = 0f
            }
            mediaPlayer.setOnErrorListener { _, _, _ ->
                playingAudioId = null
                audioProgress = 0f
                true
            }
        } catch (e: Exception) {
            playingAudioId = null
            audioProgress = 0f
        }
    }

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

    val isGroup = uiState.conversation?.isGroup ?: false

    Scaffold(
        containerColor = Color.White,
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar"
                            )
                        }
                    },
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable(enabled = isGroup) {
                                if (conversationId != null) {
                                    navController.navigate("group_info/$conversationId")
                                }
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor),
                                contentAlignment = Alignment.Center
                            ) {
                                val photoUrl = uiState.conversation?.profilePictureUrl
                                if (!photoUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = photoUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = uiState.conversationTitle
                                            .split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .joinToString("") { it.first().uppercase() }
                                            .ifEmpty { "?" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = uiState.conversationTitle.ifBlank { "..." },
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isGroup) {
                                    Text(
                                        text = "Grupo",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF9E9E9E)
                                    )
                                }
                            }
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = !isSearchActive }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                        if (isGroup) {
                            IconButton(onClick = {
                                if (conversationId != null) {
                                    navController.navigate("group_info/$conversationId")
                                }
                            }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Detalhes")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )

                AnimatedVisibility(visible = isSearchActive) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = uiState.searchQuery,
                            onValueChange = { chatViewModel.onSearchQueryChanged(it) },
                            placeholder = { Text("Buscar na conversa...", color = Color(0xFF9E9E9E)) },
                            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9E9E9E)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                focusedContainerColor = Color(0xFFF5F5F5)
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = {
                            isSearchActive = false
                            chatViewModel.onSearchQueryChanged("")
                        }) {
                            Icon(Icons.Default.Close, "Fechar busca", tint = Color(0xFF757575))
                        }
                    }
                }

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

                HorizontalDivider(color = Color(0xFFF0F0F0))
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                HorizontalDivider(color = Color(0xFFF0F0F0))

                AnimatedVisibility(visible = uiState.mediaToSendUri != null) {
                    AttachmentPreview(
                        mediaType = uiState.mediaType,
                        fileName = uiState.mediaFileName,
                        uriString = uiState.mediaToSendUri?.toString().orEmpty(),
                        onRemove = { chatViewModel.clearSelectedMedia() }
                    )
                }

                if (uiState.isRecording) {
                    RecordingBar(
                        onStop = {
                            if (conversationId != null) {
                                chatViewModel.stopRecording(conversationId)
                            }
                        },
                        onCancel = { chatViewModel.cancelRecording() }
                    )
                } else {
                    AnimatedVisibility(visible = showStickerPanel) {
                        StickerPanel(
                            onStickerSelected = { emoji ->
                                if (conversationId != null) {
                                    chatViewModel.sendSticker(conversationId, emoji)
                                }
                                showStickerPanel = false
                            }
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                            .navigationBarsPadding()
                            .imePadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { showMediaSheet = true }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Anexar",
                                tint = primaryColor
                            )
                        }

                        IconButton(onClick = { showStickerPanel = !showStickerPanel }) {
                            Icon(
                                Icons.Default.EmojiEmotions,
                                contentDescription = "Stickers",
                                tint = if (showStickerPanel) primaryColor else Color.Gray
                            )
                        }

                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("Mensagem...", color = Color(0xFF9E9E9E), fontSize = 14.sp) },
                            singleLine = false,
                            maxLines = 4,
                            modifier = Modifier.weight(1f),
                            enabled = uiState.mediaToSendUri == null,
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor,
                                unfocusedBorderColor = Color(0xFFE0E0E0),
                                unfocusedContainerColor = Color(0xFFF5F5F5),
                                focusedContainerColor = Color(0xFFF5F5F5)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        FloatingActionButton(
                            onClick = {
                                if (text.isNotBlank() || uiState.mediaToSendUri != null) {
                                    if (conversationId != null) {
                                        chatViewModel.sendMessage(conversationId, text.trim())
                                        text = ""
                                    }
                                } else {
                                    val hasPermission = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPermission) {
                                        chatViewModel.startRecording()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            modifier = Modifier.size(46.dp),
                            containerColor = primaryColor,
                            contentColor = Color.White,
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(
                                imageVector = if (text.isNotBlank() || uiState.mediaToSendUri != null)
                                    Icons.AutoMirrored.Filled.Send
                                else Icons.Default.Mic,
                                contentDescription = if (text.isNotBlank() || uiState.mediaToSendUri != null) "Enviar" else "Gravar áudio",
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                .background(Color(0xFFFAFAFA))
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(4.dp))

                if (uiState.isLoading && uiState.chatItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
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
                            color = Color(0xFF9E9E9E),
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
                                        isAudioPlaying = playingAudioId == message.id,
                                        audioProgress = if (playingAudioId == message.id) audioProgress else 0f,
                                        onPlayAudio = { playAudio(message.id, message.content) },
                                        onPlayVideo = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(AndroidUri.parse(message.content), "video/*")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, AndroidUri.parse(message.content))
                                                context.startActivity(browserIntent)
                                            }
                                        },
                                        onOpenDocument = {
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    data = AndroidUri.parse(message.content)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, AndroidUri.parse(message.content))
                                                context.startActivity(browserIntent)
                                            }
                                        },
                                        onImageClick = { url ->
                                            try {
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(AndroidUri.parse(url), "image/*")
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                val browserIntent = Intent(Intent.ACTION_VIEW, AndroidUri.parse(url))
                                                context.startActivity(browserIntent)
                                            }
                                        },
                                        onLongPress = {
                                            selectedMessageId = message.id
                                        }
                                    )
                                }
                            }
                        }
                    }

                    LaunchedEffect(uiState.chatItems.size) {
                        val lastIndex = uiState.chatItems.lastIndex
                        if (lastIndex >= 0) {
                            scope.launch {
                                try {
                                    listState.animateScrollToItem(lastIndex)
                                } catch (_: Exception) {}
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

    if (showMediaSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMediaSheet = false },
            containerColor = Color.White
        ) {
            MediaPickerContent(
                onGallery = {
                    imagePickerLauncher.launch("image/*")
                    showMediaSheet = false
                },
                onCamera = {
                    showMediaSheet = false
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onVideo = {
                    videoPickerLauncher.launch("video/*")
                    showMediaSheet = false
                },
                onDocument = {
                    documentPickerLauncher.launch(arrayOf("*/*"))
                    showMediaSheet = false
                },
                onAudio = {
                    showMediaSheet = false
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        chatViewModel.startRecording()
                    } else {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                onLocation = {
                    showMediaSheet = false
                    if (conversationId != null) {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            chatViewModel.sendLocation(conversationId)
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MediaPickerContent(
    onGallery: () -> Unit,
    onCamera: () -> Unit,
    onVideo: () -> Unit,
    onDocument: () -> Unit,
    onAudio: () -> Unit,
    onLocation: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .navigationBarsPadding()
    ) {
        Text(
            "Enviar",
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color(0xFF212121)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MediaOption(Icons.Default.AttachFile, "Galeria", Color(0xFF4CAF50), onGallery)
            MediaOption(Icons.Default.PhotoCamera, "Câmera", Color(0xFF2196F3), onCamera)
            MediaOption(Icons.Default.VideoLibrary, "Vídeo", Color(0xFF9C27B0), onVideo)
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MediaOption(Icons.Default.Description, "Documento", Color(0xFFFF9800), onDocument)
            MediaOption(Icons.Default.Mic, "Áudio", Color(0xFFF44336), onAudio)
            MediaOption(Icons.Default.LocationOn, "Local", Color(0xFF00BCD4), onLocation)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun MediaOption(
    icon: ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

@Composable
private fun RecordingBar(
    onStop: () -> Unit,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .navigationBarsPadding()
            .imePadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(Color.Red)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "Gravando...",
            color = Color.Red,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, "Cancelar gravação", tint = Color.Gray)
        }
        IconButton(onClick = onStop) {
            Icon(Icons.Default.StopCircle, "Parar e enviar", tint = Color.Red)
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
            color = Color(0xFFE0E0E0),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = dateText,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 11.sp,
                color = Color(0xFF757575)
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
        "IMAGE" -> "\uD83D\uDCF7 Imagem"
        "VIDEO" -> "\uD83C\uDFA5 Vídeo"
        "AUDIO" -> "\uD83C\uDFA4 Mensagem de voz"
        "DOCUMENT" -> "\uD83D\uDCC4 ${message.fileName ?: "Documento"}"
        "LOCATION" -> "\uD83D\uDCCD Localização"
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
    isAudioPlaying: Boolean = false,
    audioProgress: Float = 0f,
    onPlayAudio: () -> Unit = {},
    onPlayVideo: () -> Unit = {},
    onOpenDocument: () -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onLongPress: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = if (isMine) primaryColor else Color.White
    val contentColor = if (isMine) Color.White else Color(0xFF212121)
    val context = LocalContext.current

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
                        color = primaryColor,
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
                tonalElevation = if (isMine) 0.dp else 1.dp
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
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message.content) },
                                contentScale = ContentScale.Crop
                            )
                        }

                        "VIDEO" -> {
                            Column(
                                modifier = Modifier.clickable { onPlayVideo() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    message.thumbnailUrl?.let { thumb ->
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = "Miniatura do vídeo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } ?: Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(contentColor.copy(alpha = 0.1f)),
                                        contentAlignment = Alignment.Center
                                    ) {}
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.5f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Reproduzir vídeo",
                                            tint = Color.White,
                                            modifier = Modifier.size(30.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = message.fileName ?: "Vídeo",
                                    fontSize = 12.sp,
                                    color = contentColor.copy(alpha = 0.8f)
                                )
                            }
                        }

                        "AUDIO" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onPlayAudio() }
                                    .padding(vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = if (isAudioPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isAudioPlaying) "Pausar" else "Reproduzir",
                                    tint = contentColor,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = { audioProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = contentColor,
                                        trackColor = contentColor.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Mensagem de voz",
                                        fontSize = 12.sp,
                                        color = contentColor.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        "DOCUMENT" -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onOpenDocument() }
                            ) {
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
                            val coords = extractCoordsFromUrl(message.content)
                            Column(
                                modifier = Modifier.clickable {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            AndroidUri.parse(message.content)
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFE8F5E9),
                                                    Color(0xFFC8E6C9),
                                                    Color(0xFFA5D6A7)
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFD32F2F),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        if (coords != null) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "${coords.first}, ${coords.second}",
                                                fontSize = 10.sp,
                                                color = Color(0xFF555555),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = contentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Abrir no Google Maps",
                                        fontSize = 12.sp,
                                        color = contentColor.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        "STICKER" -> {
                            Text(
                                text = message.content,
                                fontSize = 72.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
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
                            color = contentColor.copy(alpha = 0.7f)
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
                                else -> contentColor.copy(alpha = 0.7f)
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
        color = Color.White,
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
                val reactions = listOf("\uD83D\uDC4D", "❤\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDC4F")
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

private val stickerList = listOf(
    "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0D", "\uD83E\uDD29", "\uD83E\uDD23",
    "\uD83D\uDE0E", "\uD83D\uDE1C", "\uD83E\uDD17", "\uD83D\uDE4F", "\uD83D\uDC4D",
    "\uD83D\uDC4B", "\uD83C\uDF89", "\uD83D\uDD25", "\u2764\uFE0F", "\uD83D\uDC94",
    "\uD83D\uDCAF", "\uD83C\uDF1F", "\uD83D\uDE80", "\uD83C\uDF08", "\uD83C\uDF82",
    "\uD83C\uDF83", "\uD83D\uDC7B", "\uD83E\uDD21", "\uD83D\uDC36", "\uD83D\uDC31"
)

@Composable
private fun StickerPanel(
    onStickerSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(12.dp)
    ) {
        Text(
            text = "Stickers",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.height(220.dp)
        ) {
            gridItems(stickerList) { sticker ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onStickerSelected(sticker) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = sticker, fontSize = 32.sp)
                }
            }
        }
    }
}

private fun extractCoordsFromUrl(url: String): Pair<String, String>? {
    val regex = Regex("""query=(-?\d+\.?\d*),(-?\d+\.?\d*)""")
    val match = regex.find(url) ?: return null
    return Pair(match.groupValues[1], match.groupValues[2])
}
