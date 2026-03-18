package com.example.app_mensagem.data.model

data class CallRecord(
    val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPicture: String? = null,
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPicture: String? = null,
    val type: String = "VOICE",      // VOICE | VIDEO
    val status: String = "OUTGOING", // OUTGOING | INCOMING | MISSED
    val timestamp: Long = 0L,
    val duration: Long = 0L          // segundos
)
