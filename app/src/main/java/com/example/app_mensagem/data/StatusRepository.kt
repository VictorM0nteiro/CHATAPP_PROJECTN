package com.example.app_mensagem.data

import android.net.Uri
import com.example.app_mensagem.data.model.StatusModel
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.util.UUID

class StatusRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val ttl = 24 * 60 * 60 * 1000L // 24 horas em ms

    fun getCurrentUserId() = auth.currentUser?.uid ?: ""

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return database.getReference("users").child(uid).get().await().getValue(User::class.java)
    }

    /** Retorna statuses ativos (< 24h) do usuário atual */
    suspend fun getMyStatuses(): List<StatusModel> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val now = System.currentTimeMillis()
        val snapshot = database.getReference("statuses").child(uid).get().await()
        return snapshot.children
            .mapNotNull { it.getValue(StatusModel::class.java) }
            .filter { it.expiresAt > now }
            .sortedBy { it.timestamp }
    }

    /** Retorna statuses de todos os outros usuários (agrupados por userId) */
    suspend fun getContactsStatuses(): Map<String, List<StatusModel>> {
        val uid = auth.currentUser?.uid ?: return emptyMap()
        val now = System.currentTimeMillis()
        val snapshot = database.getReference("statuses").get().await()
        val result = mutableMapOf<String, MutableList<StatusModel>>()
        snapshot.children.forEach { userNode ->
            if (userNode.key == uid) return@forEach
            userNode.children.forEach { statusNode ->
                val status = statusNode.getValue(StatusModel::class.java) ?: return@forEach
                if (status.expiresAt > now) {
                    result.getOrPut(status.userId) { mutableListOf() }.add(status)
                }
            }
        }
        return result.mapValues { it.value.sortedBy { s -> s.timestamp } }
    }

    suspend fun postTextStatus(text: String, backgroundColor: String) {
        val user = getCurrentUser() ?: return
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val status = StatusModel(
            id = id,
            userId = user.uid,
            userName = user.name,
            userPicture = user.profilePictureUrl,
            content = text,
            type = "TEXT",
            backgroundColor = backgroundColor,
            timestamp = now,
            expiresAt = now + ttl
        )
        database.getReference("statuses/${user.uid}/$id").setValue(status).await()
    }

    suspend fun postImageStatus(imageUri: Uri) {
        val user = getCurrentUser() ?: return
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val imageUrl = CloudinaryHelper.uploadMedia(imageUri, "statuses/${user.uid}", id, "image")
        val status = StatusModel(
            id = id,
            userId = user.uid,
            userName = user.name,
            userPicture = user.profilePictureUrl,
            content = imageUrl,
            type = "IMAGE",
            timestamp = now,
            expiresAt = now + ttl
        )
        database.getReference("statuses/${user.uid}/$id").setValue(status).await()
    }

    suspend fun markAsViewed(statusOwnerId: String, statusId: String) {
        val uid = auth.currentUser?.uid ?: return
        database.getReference("statuses/$statusOwnerId/$statusId/viewers/$uid")
            .setValue(System.currentTimeMillis()).await()
    }
}
