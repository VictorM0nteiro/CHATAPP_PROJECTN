package com.example.app_mensagem.presentation.status

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.presentation.viewmodel.StatusPostState
import com.example.app_mensagem.presentation.viewmodel.StatusViewModel

private val statusColors = listOf(
    "#FF4458", "#FD267A", "#FF7854",
    "#1565C0", "#2E7D32", "#6A1B9A",
    "#E65100", "#212121", "#00695C"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStatusScreen(
    navController: NavController,
    statusViewModel: StatusViewModel = viewModel()
) {
    val postState by statusViewModel.postState.collectAsState()
    val primary = MaterialTheme.colorScheme.primary
    var selectedTab by remember { mutableIntStateOf(0) }
    var statusText by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(statusColors.first()) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    LaunchedEffect(postState) {
        if (postState is StatusPostState.Done) {
            statusViewModel.resetPostState()
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primary, titleContentColor = Color.White
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
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Texto") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Imagem") })
            }

            if (selectedTab == 0) {
                // ── Aba Texto ─────────────────────────────────────────────
                val bgColor = try { Color(selectedColor.toColorInt()) } catch (e: Exception) { primary }

                // Preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = statusText.ifBlank { "Sua mensagem aparecerá aqui" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = statusText,
                        onValueChange = { if (it.length <= 200) statusText = it },
                        placeholder = { Text("Escreva seu status...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        ),
                        supportingText = { Text("${statusText.length}/200", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cor do fundo", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(statusColors) { colorHex ->
                            val c = try { Color(colorHex.toColorInt()) } catch (e: Exception) { primary }
                            val isSelected = colorHex == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .then(if (isSelected) Modifier.border(3.dp, Color.White, CircleShape) else Modifier)
                                    .clickable { selectedColor = colorHex }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (statusText.isNotBlank()) statusViewModel.postTextStatus(statusText, selectedColor)
                        },
                        enabled = statusText.isNotBlank() && postState !is StatusPostState.Posting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary)
                    ) {
                        if (postState is StatusPostState.Posting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("  PUBLICAR STATUS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }

            } else {
                // ── Aba Imagem ────────────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFE0E0E0))
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Image, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Toque para escolher uma imagem", color = Color.Gray, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { selectedImageUri?.let { statusViewModel.postImageStatus(it) } },
                        enabled = selectedImageUri != null && postState !is StatusPostState.Posting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary)
                    ) {
                        if (postState is StatusPostState.Posting) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Text("  PUBLICAR STATUS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }

            if (postState is StatusPostState.Error) {
                Text(
                    text = (postState as StatusPostState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

