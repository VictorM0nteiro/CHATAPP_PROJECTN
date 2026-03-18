package com.example.app_mensagem.presentation.status

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.app_mensagem.R
import com.example.app_mensagem.data.model.StatusModel
import com.example.app_mensagem.presentation.viewmodel.StatusViewModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val STATUS_DURATION_MS = 5000L

@Composable
fun StatusViewerScreen(
    navController: NavController,
    statusListJson: String,
    statusViewModel: StatusViewModel = viewModel()
) {
    val statuses: List<StatusModel> = try {
        val decoded = URLDecoder.decode(statusListJson, "UTF-8")
        val type = object : TypeToken<List<StatusModel>>() {}.type
        Gson().fromJson(decoded, type)
    } catch (e: Exception) {
        emptyList()
    }

    if (statuses.isEmpty()) {
        navController.popBackStack()
        return
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    val current = statuses.getOrNull(currentIndex) ?: return

    // Marca como visualizado
    LaunchedEffect(currentIndex) {
        statusViewModel.markAsViewed(current.userId, current.id)
        progress = 0f
        val steps = STATUS_DURATION_MS / 50
        repeat(steps.toInt()) {
            delay(50)
            progress = (it + 1).toFloat() / steps
        }
        if (currentIndex < statuses.size - 1) {
            currentIndex++
        } else {
            navController.popBackStack()
        }
    }

    val bgColor = try {
        Color(current.backgroundColor.toColorInt())
    } catch (e: Exception) {
        Color(0xFFFF4458.toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (current.type == "IMAGE") Color.Black else bgColor)
            .clickable {
                if (currentIndex < statuses.size - 1) currentIndex++
                else navController.popBackStack()
            }
    ) {
        // Conteúdo
        if (current.type == "IMAGE") {
            AsyncImage(
                model = current.content,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = current.content,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
        }

        // Overlay superior
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 8.dp)
        ) {
            // Barras de progresso
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                statuses.forEachIndexed { index, _ ->
                    val p = when {
                        index < currentIndex -> 1f
                        index == currentIndex -> progress
                        else -> 0f
                    }
                    val animatedP by animateFloatAsState(
                        targetValue = p,
                        animationSpec = tween(50, easing = LinearEasing),
                        label = "progress_$index"
                    )
                    LinearProgressIndicator(
                        progress = { animatedP },
                        modifier = Modifier.weight(1f).height(2.dp).clip(RoundedCornerShape(1.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.4f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Info do usuário
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = current.userPicture ?: R.drawable.ic_launcher_foreground,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(current.userName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        formatRelativeTime(current.timestamp),
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                }
            }
        }
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "agora"
        diff < 3_600_000 -> "${diff / 60_000}min atrás"
        diff < 86_400_000 -> "${diff / 3_600_000}h atrás"
        else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}
