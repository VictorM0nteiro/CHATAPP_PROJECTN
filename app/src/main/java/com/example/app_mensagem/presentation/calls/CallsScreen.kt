package com.example.app_mensagem.presentation.calls

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.CallRecord
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.viewmodel.CallsUiState
import com.example.app_mensagem.presentation.viewmodel.CallsViewModel
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    navController: NavController,
    callsViewModel: CallsViewModel = viewModel()
) {
    val uiState by callsViewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    var selectedTab by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chamadas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { callsViewModel.loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Recentes", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Contatos", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) }
                )
            }

            when (val state = uiState) {
                is CallsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primary)
                    }
                }

                is CallsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }

                is CallsUiState.Success -> {
                    if (selectedTab == 0) {
                        // Aba Recentes
                        if (state.callHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Call,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = Color.LightGray
                                    )
                                    Text("Nenhuma chamada recente", color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize().background(Color.White)
                            ) {
                                items(state.callHistory) { call ->
                                    CallHistoryItem(
                                        call = call,
                                        currentUserId = "", // será resolvido no item
                                        onCallBack = { type ->
                                            scope.launch {
                                                val receiver = User(
                                                    uid = call.receiverId,
                                                    name = call.receiverName,
                                                    profilePictureUrl = call.receiverPicture
                                                )
                                                val record = callsViewModel.initiateCall(receiver, type)
                                                val json = Gson().toJson(record)
                                                navController.navigate("outgoing_call/${java.net.URLEncoder.encode(json, "UTF-8")}")
                                            }
                                        }
                                    )
                                    HorizontalDivider(modifier = Modifier.padding(start = 74.dp), color = Color(0xFFF0F0F0))
                                }
                            }
                        }
                    } else {
                        // Aba Contatos
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().background(Color.White),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(state.contacts) { user ->
                                ContactCallItem(
                                    user = user,
                                    onVoiceCall = {
                                        scope.launch {
                                            val record = callsViewModel.initiateCall(user, "VOICE")
                                            val json = Gson().toJson(record)
                                            navController.navigate("outgoing_call/${java.net.URLEncoder.encode(json, "UTF-8")}")
                                        }
                                    },
                                    onVideoCall = {
                                        scope.launch {
                                            val record = callsViewModel.initiateCall(user, "VIDEO")
                                            val json = Gson().toJson(record)
                                            navController.navigate("outgoing_call/${java.net.URLEncoder.encode(json, "UTF-8")}")
                                        }
                                    }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 74.dp), color = Color(0xFFF0F0F0))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CallHistoryItem(
    call: CallRecord,
    currentUserId: String,
    onCallBack: (String) -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val (icon, iconColor, label) = when (call.status) {
        "MISSED"   -> Triple(Icons.Default.CallMissed, Color.Red, "Perdida")
        "INCOMING" -> Triple(Icons.Default.CallReceived, Color(0xFF4CAF50), "Recebida")
        else       -> Triple(Icons.Default.CallMade, primary, "Efetuada")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = call.receiverPicture ?: R.drawable.ic_launcher_foreground,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(call.receiverName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$label · ${formatCallTime(call.timestamp)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        IconButton(onClick = { onCallBack(call.type) }) {
            Icon(
                if (call.type == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Retornar chamada",
                tint = primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun ContactCallItem(
    user: User,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
            contentDescription = null,
            modifier = Modifier.size(50.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                if (user.phoneNumber.isNotBlank()) user.phoneNumber else user.email,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        IconButton(onClick = onVoiceCall) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Call, contentDescription = "Voz", tint = primary, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(modifier = Modifier.width(4.dp))

        IconButton(onClick = onVideoCall) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Videocam, contentDescription = "Vídeo", tint = primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatCallTime(timestamp: Long): String {
    if (timestamp == 0L) return ""
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
}
