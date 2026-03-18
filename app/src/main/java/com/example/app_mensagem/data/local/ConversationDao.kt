package com.example.app_mensagem.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.app_mensagem.data.model.Conversation
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(conversation: Conversation)

    @Query("SELECT * FROM conversations ORDER BY timestamp DESC")
    fun getAllConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE isGroup = 1 ORDER BY timestamp DESC")
    fun getGroupConversations(): Flow<List<Conversation>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversationById(conversationId: String): Flow<Conversation?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): Conversation?

    @Query("DELETE FROM conversations")
    suspend fun clearAll()
}