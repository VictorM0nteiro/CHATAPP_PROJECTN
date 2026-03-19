package com.example.app_mensagem.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.PresenceStatus
import com.example.app_mensagem.presentation.viewmodel.ProfileViewModel
import com.example.app_mensagem.presentation.viewmodel.ThemeViewModel
import com.example.app_mensagem.ui.theme.AppColorTheme
import com.example.app_mensagem.ui.theme.toPalette
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    profileViewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val user = uiState.user
    val selectedTheme by themeViewModel.selectedTheme
    val primaryColor = MaterialTheme.colorScheme.primary

    var name by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var updateStatus by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    LaunchedEffect(uiState.user) {
        uiState.user?.let { u ->
            name = u.name
            phoneNumber = u.phoneNumber
            status = u.status
            updateStatus = u.updateStatus
        }
    }

    var showImageSourceDialog by remember { mutableStateOf(false) }
    var showStatusMenu by remember { mutableStateOf(false) }
    val currentPresence = PresenceStatus.fromKey(user?.presenceStatus)

    val presenceColor = when (currentPresence) {
        PresenceStatus.ONLINE -> Color(0xFF25D366)
        PresenceStatus.BUSY -> Color(0xFFFFC107)
        PresenceStatus.OFFLINE -> Color.Gray
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> imageUri = uri }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            val file = File(context.cacheDir, "profile_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            imageUri = Uri.fromFile(file)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(null) }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Escolher foto") },
            text = { Text("Como deseja adicionar a foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        cameraLauncher.launch(null)
                    else
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Câmera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    imagePickerLauncher.launch("image/*")
                }) { Text("Galeria") }
            }
        )
    }

    LaunchedEffect(Unit) { profileViewModel.loadProfile() }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            Toast.makeText(context, "Perfil atualizado com sucesso!", Toast.LENGTH_SHORT).show()
            profileViewModel.clearSavedFlag()
        }
    }

    Scaffold(
        containerColor = Color.White,
        topBar = {
            TopAppBar(
                title = { Text("Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading && user == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = primaryColor) }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // ── Header row: Avatar | Name+Recado | Status ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(72.dp)) {
                        AsyncImage(
                            model = imageUri ?: user?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                            contentDescription = "Foto de Perfil",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0xFFE0E0E0))
                                .clickable { showImageSourceDialog = true },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(primaryColor)
                                .align(Alignment.BottomEnd),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = user?.name ?: "",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (status.isNotBlank()) {
                            Text(
                                text = status,
                                fontSize = 13.sp,
                                color = Color(0xFF8696A0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(presenceColor.copy(alpha = 0.12f))
                                .clickable { showStatusMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(presenceColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                currentPresence.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = presenceColor
                            )
                        }
                        DropdownMenu(
                            expanded = showStatusMenu,
                            onDismissRequest = { showStatusMenu = false }
                        ) {
                            PresenceStatus.entries.forEach { ps ->
                                val psColor = when (ps) {
                                    PresenceStatus.ONLINE -> Color(0xFF25D366)
                                    PresenceStatus.BUSY -> Color(0xFFFFC107)
                                    PresenceStatus.OFFLINE -> Color.Gray
                                }
                                DropdownMenuItem(
                                    text = { Text(ps.label) },
                                    onClick = {
                                        profileViewModel.updatePresenceStatus(ps.key)
                                        showStatusMenu = false
                                    },
                                    leadingIcon = {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(psColor)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF0F0F0))

                // ── Editable info rows ──
                EditableInfoRow(
                    label = "Nome",
                    value = name,
                    onValueChange = { name = it },
                    accentColor = primaryColor
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))

                InfoRow(label = "E-mail", value = user?.email ?: "")
                HorizontalDivider(color = Color(0xFFF0F0F0))

                EditableInfoRow(
                    label = "Telefone",
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    keyboardType = KeyboardType.Phone,
                    accentColor = primaryColor
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))

                EditableInfoRow(
                    label = "Recado",
                    value = status,
                    onValueChange = { status = it },
                    accentColor = primaryColor
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))

                EditableInfoRow(
                    label = "Status",
                    value = updateStatus,
                    onValueChange = { updateStatus = it },
                    accentColor = primaryColor
                )
                HorizontalDivider(color = Color(0xFFF0F0F0))

                Spacer(modifier = Modifier.height(24.dp))

                // ── Theme section ──
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ModeEdit, null, tint = Color(0xFF8696A0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Tema do Aplicativo", fontSize = 13.sp, color = Color(0xFF8696A0))
                }

                Spacer(modifier = Modifier.height(12.dp))

                val themes = listOf(
                    Triple(AppColorTheme.BLUE, "Padrão", AppColorTheme.BLUE.toPalette().primary),
                    Triple(AppColorTheme.RED, "Vermelho", AppColorTheme.RED.toPalette().primary),
                    Triple(AppColorTheme.GREEN, "Verde", AppColorTheme.GREEN.toPalette().primary),
                    Triple(AppColorTheme.YELLOW, "Amarelo", AppColorTheme.YELLOW.toPalette().primary),
                    Triple(AppColorTheme.PURPLE, "Roxo", AppColorTheme.PURPLE.toPalette().primary),
                    Triple(AppColorTheme.PINK, "Rosa", AppColorTheme.PINK.toPalette().primary),
                )

                themes.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { (theme, label, color) ->
                            val isSelected = selectedTheme == theme
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) color.copy(alpha = 0.1f) else Color(0xFFF5F5F5))
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) color else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { themeViewModel.setTheme(theme) }
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) color else Color(0xFF8696A0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ── Save button ──
                Button(
                    onClick = {
                        profileViewModel.updateProfile(
                            name = name,
                            phoneNumber = phoneNumber,
                            status = status,
                            updateStatus = updateStatus,
                            imageUri = imageUri
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Salvar", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF8696A0),
            modifier = Modifier.width(80.dp)
        )
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF111B21)
        )
    }
}

@Composable
private fun EditableInfoRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF8696A0),
            modifier = Modifier.width(80.dp)
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            textStyle = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color(0xFF111B21)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accentColor,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = accentColor
            ),
            modifier = Modifier.weight(1f)
        )
    }
}
