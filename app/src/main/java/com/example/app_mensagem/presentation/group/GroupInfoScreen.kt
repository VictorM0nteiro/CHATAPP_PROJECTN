package com.example.app_mensagem.presentation.group

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.presentation.contacts.UserItem
import com.example.app_mensagem.presentation.viewmodel.GroupInfoViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(
    navController: NavController,
    groupId: String?,
    groupInfoViewModel: GroupInfoViewModel = viewModel()
) {
    val uiState by groupInfoViewModel.uiState.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var userToRemove by remember { mutableStateOf<User?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && groupId != null) {
            groupInfoViewModel.updateGroupProfilePicture(groupId, uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null && groupId != null) {
            val file = File(context.cacheDir, "group_photo_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            groupInfoViewModel.updateGroupProfilePicture(groupId, Uri.fromFile(file))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    if (showImageSourceDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Alterar foto do grupo") },
            text = { Text("Como deseja escolher a foto?") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val hasCameraPermission = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasCameraPermission) cameraLauncher.launch(null)
                    else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }) { Text("Câmera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    imagePicker.launch("image/*")
                }) { Text("Galeria") }
            }
        )
    }

    val selectedUserHandle = navController.currentBackStackEntry?.savedStateHandle
    LaunchedEffect(selectedUserHandle) {
        selectedUserHandle?.getLiveData<String>("selectedUserId")?.observeForever { userId ->
            if (userId != null && groupId != null) {
                val isAlreadyMember = uiState.members.any { it.uid == userId }
                if (!isAlreadyMember) {
                    groupInfoViewModel.addMember(groupId, userId)
                }
                selectedUserHandle.remove<String>("selectedUserId")
            }
        }
    }

    LaunchedEffect(groupId) {
        if (groupId != null) {
            groupInfoViewModel.loadGroupInfo(groupId)
        }
    }

    if (showEditDialog && groupId != null) {
        EditGroupNameDialog(
            currentName = uiState.group?.name ?: "",
            onDismiss = { showEditDialog = false },
            onConfirm = { newName ->
                groupInfoViewModel.updateGroupName(groupId, newName)
                showEditDialog = false
            }
        )
    }

    if (userToRemove != null && groupId != null) {
        RemoveMemberDialog(
            memberName = userToRemove!!.name,
            onDismiss = { userToRemove = null },
            onConfirm = {
                groupInfoViewModel.removeMember(groupId, userToRemove!!.uid)
                userToRemove = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dados do Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(
                    text = "Erro: ${uiState.error}",
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.group != null) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = MaterialTheme.shapes.large,
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.size(120.dp),
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                AsyncImage(
                                    model = uiState.group?.profilePictureUrl ?: R.drawable.ic_launcher_foreground,
                                    contentDescription = "Foto de perfil do grupo",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .then(
                                            if (uiState.group?.creatorId == currentUserId)
                                                Modifier.clickable { showImageSourceDialog = true }
                                            else Modifier
                                        ),
                                    contentScale = ContentScale.Crop
                                )
                                if (uiState.group?.creatorId == currentUserId) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                            .clickable { showImageSourceDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = "Alterar foto",
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.group!!.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                if (uiState.group?.creatorId == currentUserId) {
                                    IconButton(onClick = { showEditDialog = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar nome do grupo")
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${uiState.members.size} participantes")
                        }
                    }

                    if (uiState.group?.creatorId == currentUserId) {
                        TextButton(
                            onClick = { navController.navigate("contacts?selectionMode=true") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Adicionar Participante")
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.members) { member ->
                            MemberItem(
                                user = member,
                                isCreator = member.uid == uiState.group!!.creatorId,
                                showRemoveIcon = uiState.group!!.creatorId == currentUserId && member.uid != currentUserId,
                                onRemoveClick = { userToRemove = member }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MemberItem(
    user: User,
    isCreator: Boolean,
    showRemoveIcon: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            UserItem(user = user, isSelected = false, onClick = {})
        }

        if (isCreator) {
            Text(
                "Admin",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        if (showRemoveIcon) {
            IconButton(onClick = onRemoveClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remover membro",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun EditGroupNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Nome do Grupo") },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Nome do Grupo") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newName) },
                enabled = newName.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun RemoveMemberDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remover Membro") },
        text = { Text("Tem certeza que deseja remover $memberName do grupo?") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Remover")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
