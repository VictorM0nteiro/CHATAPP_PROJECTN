package com.example.app_mensagem.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.presentation.common.LifecycleObserver
import com.example.app_mensagem.presentation.viewmodel.AuthViewModel
import com.example.app_mensagem.presentation.viewmodel.ConversationTab
import com.example.app_mensagem.presentation.viewmodel.ConversationUiState
import com.example.app_mensagem.presentation.viewmodel.ConversationsViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    conversationsViewModel: ConversationsViewModel = viewModel()
) {
    var selectedNavIndex by remember { mutableIntStateOf(0) }
    val conversationState by conversationsViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary

    var searchQuery by remember { mutableStateOf("") }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        } else {
            android.widget.Toast.makeText(context, "Permissão de câmera negada", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(Intent(MediaStore.ACTION_IMAGE_CAPTURE))
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LifecycleObserver { event ->
        if (event == Lifecycle.Event.ON_RESUME) {
            conversationsViewModel.resyncConversations()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Mensagens",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                },
                actions = {
                    IconButton(onClick = { authViewModel.logout() }) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Sair",
                            tint = Color(0xFF9E9E9E)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("contacts") },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Nova Conversa")
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Chamadas", Icons.Default.Phone, 1),
                    Triple("Câmera", Icons.Default.CameraAlt, 2),
                    Triple("Status", Icons.Default.WbSunny, 3),
                    Triple("Contatos", Icons.Default.People, 4),
                    Triple("Perfil", Icons.Default.Person, 5)
                )
                navItems.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedNavIndex == index,
                        onClick = {
                            selectedNavIndex = index
                            when (index) {
                                1 -> navController.navigate("calls")
                                2 -> openCamera()
                                3 -> navController.navigate("status")
                                4 -> navController.navigate("contacts")
                                5 -> navController.navigate("profile")
                                else -> {}
                            }
                        },
                        icon = {
                            Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp))
                        },
                        label = {
                            Text(label, fontSize = 10.sp)
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = primaryColor,
                            selectedTextColor = primaryColor,
                            indicatorColor = primaryColor.copy(alpha = 0.12f),
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    conversationsViewModel.onSearchQueryChanged(it)
                },
                placeholder = { Text("Buscar conversas...", color = Color(0xFF9E9E9E)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF9E9E9E)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedContainerColor = Color(0xFFF5F5F5),
                    focusedContainerColor = Color(0xFFF5F5F5)
                )
            )

            when (val state = conversationState) {
                is ConversationUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                }

                is ConversationUiState.Success -> {
                    StoriesRow(
                        conversations = state.conversations,
                        onAddStory = { navController.navigate("contacts") }
                    )

                    ScrollableTabRow(
                        selectedTabIndex = state.selectedTab.ordinal,
                        edgePadding = 12.dp,
                        containerColor = Color.White,
                        contentColor = primaryColor,
                        divider = {}
                    ) {
                        Tab(
                            selected = state.selectedTab == ConversationTab.ALL,
                            onClick = { conversationsViewModel.onTabSelected(ConversationTab.ALL) },
                            text = { Text("Todas", fontWeight = if (state.selectedTab == ConversationTab.ALL) FontWeight.Bold else FontWeight.Normal) }
                        )
                        Tab(
                            selected = state.selectedTab == ConversationTab.FAVORITES,
                            onClick = { conversationsViewModel.onTabSelected(ConversationTab.FAVORITES) },
                            text = { Text("Favoritas", fontWeight = if (state.selectedTab == ConversationTab.FAVORITES) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = state.selectedTab == ConversationTab.GROUPS,
                            onClick = { conversationsViewModel.onTabSelected(ConversationTab.GROUPS) },
                            text = { Text("Grupos", fontWeight = if (state.selectedTab == ConversationTab.GROUPS) FontWeight.Bold else FontWeight.Normal) },
                            icon = { Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE0E0E0))

                    if (state.conversations.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "\uD83D\uDCAC", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (state.searchQuery.isNotEmpty()) "Nenhum resultado encontrado."
                                    else "Nenhuma conversa ainda",
                                    color = Color(0xFF9E9E9E),
                                    fontSize = 15.sp
                                )
                                if (state.searchQuery.isEmpty()) {
                                    Text(
                                        text = "Toque no botão + para começar",
                                        color = Color(0xFF9E9E9E),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                        ) {
                            items(state.conversations) { conversation ->
                                ConversationItem(
                                    conversation = conversation,
                                    onClick = { navController.navigate("chat/${conversation.id}") },
                                    onFavoriteClick = {
                                        if (!conversation.isGroup) conversationsViewModel.toggleFavorite(conversation.id)
                                    }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 80.dp),
                                    color = Color(0xFFF0F0F0),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }

                is ConversationUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Erro: ${state.message}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
fun StoriesRow(
    conversations: List<Conversation>,
    onAddStory: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(54.dp)
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE8EAF6))
                            .clickable { onAddStory() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(46.dp),
                            tint = Color(0xFFBDBDBD)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(primaryColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text("Minha\nHistória", fontSize = 10.sp, textAlign = TextAlign.Center, maxLines = 2, lineHeight = 12.sp, color = Color.DarkGray)
            }
        }

        items(conversations.take(8)) { conversation ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(54.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Brush.sweepGradient(listOf(primaryColor, primaryColor.copy(alpha = 0.5f))))
                        .padding(2.dp)
                ) {
                    AsyncImage(
                        model = conversation.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                        contentDescription = conversation.name,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    conversation.name.split(" ").first(),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.DarkGray
                )
                Text(formatTimestamp(conversation.timestamp), fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
fun ConversationItem(
    conversation: Conversation,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(52.dp)) {
            AsyncImage(
                model = conversation.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                contentDescription = "Foto de ${conversation.name}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            if (conversation.isGroup) {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(primaryColor)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = conversation.lastMessage.ifBlank { "Sem mensagens" },
                fontSize = 13.sp,
                color = Color(0xFF9E9E9E),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = formatTimestamp(conversation.timestamp),
                fontSize = 11.sp,
                color = Color(0xFF9E9E9E)
            )
            if (!conversation.isGroup) {
                IconButton(onClick = onFavoriteClick, modifier = Modifier.size(20.dp)) {
                    Icon(
                        imageVector = if (conversation.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = null,
                        tint = if (conversation.isFavorite) primaryColor else Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return ""
    val messageDate = Date(timestamp)
    val today = Calendar.getInstance()
    val msgCal = Calendar.getInstance().apply { time = messageDate }
    return if (today.get(Calendar.DATE) == msgCal.get(Calendar.DATE) &&
        today.get(Calendar.MONTH) == msgCal.get(Calendar.MONTH) &&
        today.get(Calendar.YEAR) == msgCal.get(Calendar.YEAR)
    ) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(messageDate)
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(messageDate)
    }
}
