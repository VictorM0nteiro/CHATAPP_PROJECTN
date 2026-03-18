package com.example.app_mensagem.presentation.calls

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.CallRecord
import com.google.gson.Gson
import kotlinx.coroutines.delay
import java.net.URLDecoder

@Composable
fun OutgoingCallScreen(
    navController: NavController,
    callRecordJson: String
) {
    val record = try {
        val decoded = URLDecoder.decode(callRecordJson, "UTF-8")
        Gson().fromJson(decoded, CallRecord::class.java)
    } catch (e: Exception) {
        null
    }

    // Timer da chamada
    var seconds by remember { mutableIntStateOf(0) }
    val isConnected = seconds >= 5 // simula conexão após 5s

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }

    // Animação de pulso
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFFFF7854), Color(0xFFFD267A)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Text(
                text = if (record?.type == "VIDEO") "Chamada de Vídeo" else "Chamada de Voz",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Anel pulsante
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(150.dp)
            ) {
                if (!isConnected) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    )
                }

                AsyncImage(
                    model = record?.receiverPicture ?: R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = record?.receiverName ?: "Usuário",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isConnected) formatDuration(seconds - 5) else "Chamando...",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Controles
            Row(
                horizontalArrangement = Arrangement.spacedBy(32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mudo
                CallControlButton(
                    icon = { Icon(Icons.Default.MicOff, contentDescription = "Mudo", tint = Color.White, modifier = Modifier.size(22.dp)) },
                    background = Color.White.copy(alpha = 0.25f),
                    size = 56.dp
                ) {}

                // Encerrar
                CallControlButton(
                    icon = { Icon(Icons.Default.CallEnd, contentDescription = "Encerrar", tint = Color.White, modifier = Modifier.size(28.dp)) },
                    background = Color(0xFFE53935),
                    size = 72.dp
                ) {
                    navController.popBackStack()
                }

                // Alto-falante
                CallControlButton(
                    icon = { Icon(Icons.Default.VolumeUp, contentDescription = "Alto-falante", tint = Color.White, modifier = Modifier.size(22.dp)) },
                    background = Color.White.copy(alpha = 0.25f),
                    size = 56.dp
                ) {}
            }
        }
    }
}

@Composable
private fun CallControlButton(
    icon: @Composable () -> Unit,
    background: Color,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(background)
    ) {
        icon()
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}
