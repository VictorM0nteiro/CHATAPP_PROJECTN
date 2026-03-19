package com.example.app_mensagem.presentation.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.app_mensagem.presentation.viewmodel.ContactsViewModel
import com.example.app_mensagem.presentation.viewmodel.GroupCreationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    navController: NavController,
    memberIds: List<String>,
    contactsViewModel: ContactsViewModel
) {
    var groupName by remember { mutableStateOf("") }
    val creationState by contactsViewModel.groupCreationState.collectAsState()

    LaunchedEffect(creationState) {
        if (creationState is GroupCreationState.Success) {
            contactsViewModel.resetGroupCreationState()
            navController.navigate("home") {
                popUpTo("home") { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Novo Grupo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Nome do Grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = creationState !is GroupCreationState.Creating
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "${memberIds.size} membro(s) selecionado(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            contactsViewModel.createGroup(groupName.trim(), memberIds)
                        },
                        enabled = groupName.isNotBlank() && creationState !is GroupCreationState.Creating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (creationState is GroupCreationState.Creating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Concluir")
                        }
                    }

                    if (creationState is GroupCreationState.Error) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (creationState as GroupCreationState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
