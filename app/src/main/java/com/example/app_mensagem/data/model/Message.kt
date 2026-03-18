package com.example.app_mensagem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey val id: String = "",
    val conversationId: String = "",
    val senderId: String = "",
    val content: String = "",
    val type: String = "TEXT", // TEXT, IMAGE, VIDEO, AUDIO, STICKER, LOCATION, DOCUMENT
    val thumbnailUrl: String? = null,
    val fileName: String? = null,
    val mimeType: String? = null,
    val timestamp: Long = 0L,
    var status: String = "SENT",
    val deliveredTimestamp: Long = 0L,
    val readTimestamp: Long = 0L,
    val reactions: Map<String, String> = emptyMap()
)