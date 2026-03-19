package com.example.app_mensagem.data

import android.net.Uri
import android.util.Log
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class ProfileRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    suspend fun getUserProfile(): User? {
        val userId = auth.currentUser?.uid ?: return null
        val snapshot = database.getReference("users").child(userId).get().await()
        val data = snapshot.value as? Map<*, *> ?: return null
        return User(
            uid = data["uid"] as? String ?: userId,
            name = data["name"] as? String ?: "",
            email = data["email"] as? String ?: "",
            phoneNumber = data["phoneNumber"] as? String ?: "",
            fcmToken = data["fcmToken"] as? String ?: "",
            profilePictureUrl = data["profilePictureUrl"] as? String,
            status = data["status"] as? String ?: "",
            updateStatus = data["updateStatus"] as? String ?: "",
            updateStatusTimestamp = (data["updateStatusTimestamp"] as? Long) ?: 0L
        )
    }

    suspend fun updateProfile(
        name: String,
        phoneNumber: String,
        status: String,
        updateStatus: String,
        imageUri: Uri?
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = database.getReference("users").child(userId)
        var imageUrl: String? = null

        if (imageUri != null) {
            try {
                imageUrl = CloudinaryHelper.uploadImage(imageUri)
            } catch (e: Exception) {
                Log.e("ProfileRepository", "Erro no upload Cloudinary: ${e.message}")
            }
        }

        val updates = mutableMapOf<String, Any?>()
        updates["name"] = name
        updates["phoneNumber"] = phoneNumber
        updates["status"] = status
        updates["updateStatus"] = updateStatus
        updates["updateStatusTimestamp"] = System.currentTimeMillis()

        if (imageUrl != null) {
            updates["profilePictureUrl"] = imageUrl
        }

        userRef.updateChildren(updates).await()

        val updatedUserSnapshot = userRef.get().await()
        val updatedUser = updatedUserSnapshot.getValue(User::class.java) ?: return

        propagateProfileUpdates(updatedUser)
    }

    private suspend fun propagateProfileUpdates(updatedUser: User) {
        val userId = updatedUser.uid
        val currentUserConversationsRef = database.getReference("user-conversations").child(userId)
        val snapshot = try {
            currentUserConversationsRef.get().await()
        } catch (e: Exception) {
            null
        }

        snapshot?.children?.forEach { conversationSnapshot ->
            val conversation = conversationSnapshot.getValue(Conversation::class.java)
            if (conversation != null && !conversation.isGroup) {
                val otherUserId = conversation.id.replace(userId, "").replace("-", "")

                val otherUserConversationRef = database.getReference("user-conversations")
                    .child(otherUserId)
                    .child(conversation.id)

                val updates = mapOf(
                    "name" to updatedUser.name,
                    "profilePictureUrl" to updatedUser.profilePictureUrl
                )
                otherUserConversationRef.updateChildren(updates)
            }
        }
    }
}