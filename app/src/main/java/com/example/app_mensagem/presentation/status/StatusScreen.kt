package com.example.app_mensagem.presentation.status

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.presentation.viewmodel.ContactStatusGroup
import com.example.app_mensagem.presentation.viewmodel.StatusUiState
import com.example.app_mensagem.presentation.viewmodel.StatusViewModel
import com.google.gson.Gson
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    navController: NavController,
    statusViewModel: StatusViewModel = viewModel()
) {
    val uiState by statusViewModel.uiState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) { statusViewModel.loadStatuses() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { statusViewModel.loadStatuses() }) {
                        Icon(Icons.Default.Refresh, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primary, titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_status") },
                containerColor = primary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Novo Status", tint = Color.White)
            }
        }
    ) { padding ->
        when (val state = uiState) {
            is StatusUiState.Loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = primary) }

            is StatusUiState.Error -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text(state.message, color = MaterialTheme.colorScheme.error) }

            is StatusUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF5F5F5))
                ) {
                    // Meu status
                    item {
                        Text(
                            "Meu Status",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }

                    item {
                        MyStatusItem(
                            hasStatus = state.myStatuses.isNotEmpty(),
                            lastTimestamp = state.myStatuses.lastOrNull()?.timestamp ?: 0L,
                            statusCount = state.myStatuses.size,
                            onView = {
                                if (state.myStatuses.isNotEmpty()) {
                                    val json = URLEncoder.encode(Gson().toJson(state.myStatuses), "UTF-8")
                                    navController.navigate("view_status/$json")
                                }
                            },
                            onAdd = { navController.navigate("add_status") }
                        )
                    }

                    if (state.contactsGroups.isNotEmpty()) {
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            Text(
                                "Atualizações Recentes",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }

                        items(state.contactsGroups) { group ->
                            ContactStatusItem(
                                group = group,
                                onClick = {
                                    val json = URLEncoder.encode(Gson().toJson(group.statuses), "UTF-8")
                                    navController.navigate("view_status/$json")
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 74.dp), color = Color(0xFFF0F0F0))
                        }
                    } else {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Nenhuma atualização de contatos", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyStatusItem(
    hasStatus: Boolean,
    lastTimestamp: Long,
    statusCount: Int,
    onView: () -> Unit,
    onAdd: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = if (hasStatus) onView else onAdd)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                if (hasStatus) {
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape)
                            .border(3.dp, Brush.sweepGradient(listOf(Color(0xFFFF7854), Color(0xFFFD267A))), CircleShape)
                    )
                }
                Icon(Icons.Default.Add, contentDescription = null, tint = primary, modifier = Modifier.size(28.dp))
            }
            Box(
                modifier = Modifier.size(18.dp).clip(CircleShape).background(primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Meu Status", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                if (hasStatus) "$statusCount atualização(ões) · ${formatTime(lastTimestamp)}"
                else "Toque para adicionar uma atualização",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        IconButton(onClick = onAdd) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun ContactStatusItem(group: ContactStatusGroup, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val ringBrush = if (group.hasUnviewed)
        Brush.sweepGradient(listOf(Color(0xFFFF7854), Color(0xFFFD267A)))
    else
        Brush.sweepGradient(listOf(Color.LightGray, Color.LightGray))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(56.dp).clip(CircleShape)
                .border(3.dp, ringBrush, CircleShape)
                .padding(3.dp)
        ) {
            AsyncImage(
                model = group.userPicture ?: R.drawable.ic_launcher_foreground,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(group.userName, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "${group.statuses.size} atualização(ões) · ${formatTime(group.statuses.last().timestamp)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatTime(ts: Long): String {
    if (ts == 0L) return ""
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
}
