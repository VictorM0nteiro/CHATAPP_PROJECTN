package com.example.app_mensagem.presentation.profile

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ModeEdit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
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
                title = { Text("Meu Perfil", fontWeight = FontWeight.Bold) },
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
                    .background(Color(0xFFFAFAFA))
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                Box(modifier = Modifier.size(110.dp)) {
                    AsyncImage(
                        model = imageUri ?: user?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                        contentDescription = "Foto de Perfil",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(3.dp, primaryColor.copy(alpha = 0.3f), CircleShape)
                            .clickable { showImageSourceDialog = true },
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(primaryColor)
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                when (currentPresence) {
                                    PresenceStatus.ONLINE -> Color(0xFF4CAF50)
                                    PresenceStatus.BUSY -> Color(0xFFFFC107)
                                    PresenceStatus.OFFLINE -> Color.Gray
                                }
                            )
                            .border(2.dp, Color.White, CircleShape)
                            .align(Alignment.TopEnd)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user?.name ?: "",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF212121)
                )

                if (user?.email?.isNotBlank() == true) {
                    Text(
                        text = user.email,
                        fontSize = 13.sp,
                        color = Color(0xFF9E9E9E)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box {
                    OutlinedButton(
                        onClick = { showStatusMenu = true },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    when (currentPresence) {
                                        PresenceStatus.ONLINE -> Color(0xFF4CAF50)
                                        PresenceStatus.BUSY -> Color(0xFFFFC107)
                                        PresenceStatus.OFFLINE -> Color.Gray
                                    }
                                )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(currentPresence.label)
                    }
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        PresenceStatus.entries.forEach { ps ->
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
                                            .background(
                                                when (ps) {
                                                    PresenceStatus.ONLINE -> Color(0xFF4CAF50)
                                                    PresenceStatus.BUSY -> Color(0xFFFFC107)
                                                    PresenceStatus.OFFLINE -> Color.Gray
                                                }
                                            )
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    SectionLabel("Informações pessoais")
                    Spacer(modifier = Modifier.height(14.dp))

                    ProfileField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Nome",
                        icon = Icons.Default.Person,
                        iconTint = primaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField(
                        value = user?.email ?: "",
                        onValueChange = {},
                        label = "E-mail",
                        icon = Icons.Default.Email,
                        iconTint = Color(0xFF9E9E9E),
                        enabled = false
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = "Telefone",
                        icon = Icons.Default.Phone,
                        iconTint = primaryColor,
                        keyboardType = KeyboardType.Phone
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField(
                        value = status,
                        onValueChange = { status = it },
                        label = "Frase de perfil",
                        icon = Icons.Default.StarBorder,
                        iconTint = primaryColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ProfileField(
                        value = updateStatus,
                        onValueChange = { updateStatus = it },
                        label = "Status atual",
                        icon = Icons.Default.Update,
                        iconTint = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White)
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ModeEdit, null, tint = primaryColor, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        SectionLabel("Tema do Aplicativo")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { (theme, label, color) ->
                                val isSelected = selectedTheme == theme
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) color.copy(alpha = 0.08f) else Color(0xFFF8F8F8))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) color else Color(0xFFEEEEEE),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable { themeViewModel.setTheme(theme) }
                                        .padding(vertical = 14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) color else Color(0xFF757575),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                        .padding(horizontal = 16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !uiState.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    } else {
                        Text("Salvar Alterações", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                }

                uiState.error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        color = Color(0xFF9E9E9E),
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun ProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = iconTint) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        shape = RoundedCornerShape(12.dp),
        colors = if (enabled) {
            OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = Color(0xFFE0E0E0),
                focusedLabelColor = primaryColor
            )
        } else {
            OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFEEEEEE),
                disabledLabelColor = Color(0xFF9E9E9E),
                disabledTextColor = Color(0xFF9E9E9E),
                disabledLeadingIconColor = Color(0xFF9E9E9E)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )
}
