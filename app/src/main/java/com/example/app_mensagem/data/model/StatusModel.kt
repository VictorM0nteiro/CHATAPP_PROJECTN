package com.example.app_mensagem.data.model

data class StatusModel(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPicture: String? = null,
    val content: String = "",          // texto ou URL da imagem
    val type: String = "TEXT",         // TEXT | IMAGE
    val backgroundColor: String = "#FF4458",
    val timestamp: Long = 0L,
    val expiresAt: Long = 0L,          // timestamp + 24h
    val viewers: Map<String, Any> = emptyMap()
)
