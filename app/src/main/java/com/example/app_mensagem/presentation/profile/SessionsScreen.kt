package com.example.app_mensagem.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.app_mensagem.data.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── ViewModel ──────────────────────────────────────────────────────────────

sealed class SessionsUiState {
    object Loading : SessionsUiState()
    data class Success(val sessions: List<Map<String, Any>>, val currentDeviceId: String) : SessionsUiState()
    data class Error(val message: String) : SessionsUiState()
}

class SessionsViewModel : ViewModel() {

    private val authRepository = AuthRepository()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _uiState = MutableStateFlow<SessionsUiState>(SessionsUiState.Loading)
    val uiState: StateFlow<SessionsUiState> = _uiState

    init {
        loadSessions()
    }

    fun loadSessions() {
        viewModelScope.launch {
            _uiState.value = SessionsUiState.Loading
            try {
                val sessions = authRepository.getSessions(currentUserId)
                val deviceId = authRepository.getCurrentDeviceId(currentUserId)
                _uiState.value = SessionsUiState.Success(sessions, deviceId)
            } catch (e: Exception) {
                _uiState.value = SessionsUiState.Error(e.message ?: "Erro ao carregar sessões.")
            }
        }
    }

    fun revokeSession(deviceId: String) {
        viewModelScope.launch {
            try {
                authRepository.revokeSession(currentUserId, deviceId)
                loadSessions()
            } catch (e: Exception) {
                _uiState.value = SessionsUiState.Error(e.message ?: "Falha ao revogar sessão.")
            }
        }
    }
}

// ─── Screen ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    navController: NavController,
    viewModel: SessionsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    var sessionToRevoke by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.loadSessions() }

    sessionToRevoke?.let { deviceId ->
        AlertDialog(
            onDismissRequest = { sessionToRevoke = null },
            title = { Text("Encerrar sessão") },
            text = { Text("Deseja encerrar esta sessão neste dispositivo?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.revokeSession(deviceId)
                    sessionToRevoke = null
                }) { Text("Encerrar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRevoke = null }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dispositivos conectados", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
        ) {
            when (val state = uiState) {
                is SessionsUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SessionsUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is SessionsUiState.Success -> {
                    if (state.sessions.isEmpty()) {
                        Text(
                            "Nenhuma sessão ativa.",
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            item {
                                Text(
                                    "Sessões ativas (${state.sessions.size})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )
                            }
                            items(state.sessions) { session ->
                                val deviceId = session["deviceId"] as? String ?: ""
                                val deviceName = session["deviceName"] as? String ?: "Dispositivo"
                                val lastActive = (session["lastActive"] as? Long) ?: 0L
                                val isCurrentDevice = deviceId == state.currentDeviceId

                                SessionCard(
                                    deviceName = deviceName,
                                    lastActive = lastActive,
                                    isCurrentDevice = isCurrentDevice,
                                    onRevoke = if (!isCurrentDevice) {
                                        { sessionToRevoke = deviceId }
                                    } else null,
                                    primary = primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionCard(
    deviceName: String,
    lastActive: Long,
    isCurrentDevice: Boolean,
    onRevoke: (() -> Unit)?,
    primary: androidx.compose.ui.graphics.Color
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentDevice) primary.copy(alpha = 0.08f) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PhoneAndroid, null, tint = primary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(deviceName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    if (isCurrentDevice) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "• Este dispositivo",
                            fontSize = 11.sp,
                            color = primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Último acesso: ${formatTimestamp(lastActive)}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (onRevoke != null) {
                IconButton(onClick = onRevoke) {
                    Icon(Icons.Default.Delete, contentDescription = "Encerrar sessão", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    if (timestamp == 0L) return "Desconhecido"
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}