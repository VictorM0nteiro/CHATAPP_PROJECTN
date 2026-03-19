package com.example.app_mensagem.data

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.util.Log
import android.util.Size
import com.example.app_mensagem.data.local.ConversationDao
import com.example.app_mensagem.data.local.MessageDao
import com.example.app_mensagem.data.model.Conversation
import com.example.app_mensagem.data.model.Group
import com.example.app_mensagem.data.model.Message
import com.example.app_mensagem.data.model.User
import com.example.app_mensagem.services.EncryptionUtils
import com.example.app_mensagem.services.FcmSender
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val context: Context
) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    private var conversationsListener: ValueEventListener? = null
    private var conversationsRef: DatabaseReference? = null

    fun startConversationListener() {
        val userId = auth.currentUser?.uid ?: return
        if (conversationsListener != null) return

        conversationsRef = database.getReference("user-conversations").child(userId)
        conversationsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val conversations = snapshot.children.mapNotNull {
                    it.getValue(Conversation::class.java)
                }

                CoroutineScope(Dispatchers.IO).launch {
                    conversations.forEach { conversationDao.insertOrUpdate(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Ouvinte de conversas cancelado: ${error.message}")
            }
        }

        conversationsRef?.addValueEventListener(conversationsListener!!)
    }

    fun stopConversationListener() {
        conversationsListener?.let { conversationsRef?.removeEventListener(it) }
        conversationsListener = null
        conversationsRef = null
    }

    fun getConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations()
    }

    fun getFavoriteConversations(): Flow<List<Conversation>> {
        return conversationDao.getFavoriteConversations()
    }

    fun getGroupConversations(): Flow<List<Conversation>> {
        return conversationDao.getGroupConversations()
    }

    fun observeConversation(conversationId: String): Flow<Conversation?> {
        return conversationDao.observeConversationById(conversationId)
    }

    suspend fun toggleFavorite(conversationId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentConversation = conversationDao.getConversationById(conversationId) ?: return
        val newValue = !currentConversation.isFavorite

        database.getReference("user-conversations")
            .child(currentUserId)
            .child(conversationId)
            .child("isFavorite")
            .setValue(newValue)
            .await()

        conversationDao.insertOrUpdate(currentConversation.copy(isFavorite = newValue))
    }

    suspend fun syncUserConversations() {
        val userId = auth.currentUser?.uid ?: return
        try {
            val snapshot = database.getReference("user-conversations")
                .child(userId)
                .get()
                .await()

            val conversations = snapshot.children.mapNotNull {
                it.getValue(Conversation::class.java)
            }

            conversations.forEach { conversationDao.insertOrUpdate(it) }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Falha na sincronização inicial: ${e.message}", e)
        }
    }

    suspend fun clearLocalCache() {
        conversationDao.clearAll()
        messageDao.clearAll()
    }

    suspend fun getUsers(): List<User> {
        val currentUserId = auth.currentUser?.uid
        val blockedIds = getBlockedUserIds()
        val snapshot = database.getReference("users").get().await()

        return snapshot.children
            .mapNotNull { it.getValue(User::class.java) }
            .filter { it.uid != currentUserId && it.uid !in blockedIds }
    }

    suspend fun getBlockedUserIds(): Set<String> {
        val currentUserId = auth.currentUser?.uid ?: return emptySet()
        val snapshot = database.getReference("user-blocks/$currentUserId").get().await()
        return snapshot.children.mapNotNull { it.key }.toSet()
    }

    suspend fun blockUser(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.getReference("user-blocks/$currentUserId/$targetUserId").setValue(true).await()
    }

    suspend fun unblockUser(targetUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        database.getReference("user-blocks/$currentUserId/$targetUserId").removeValue().await()
    }

    suspend fun searchUserByPhone(phoneNumber: String): User? {
        val currentUserId = auth.currentUser?.uid
        val snapshot = database.getReference("users")
            .orderByChild("phoneNumber")
            .equalTo(phoneNumber)
            .get()
            .await()

        return snapshot.children
            .mapNotNull { it.getValue(User::class.java) }
            .firstOrNull { it.uid != currentUserId }
    }

    suspend fun createGroup(name: String, memberIds: List<String>) {
        val currentUserId = auth.currentUser?.uid ?: return

        val currentUser = database.getReference("users")
            .child(currentUserId)
            .get()
            .await()
            .getValue(User::class.java)

        val groupsRef = database.getReference("groups")
        val groupId = groupsRef.push().key ?: return

        val allMemberIds = (memberIds + currentUserId).distinct()
        val membersMap = allMemberIds.associateWith { true }

        val group = Group(
            id = groupId,
            name = name,
            creatorId = currentUserId,
            members = membersMap
        )

        groupsRef.child(groupId).setValue(group).await()

        val groupConversation = Conversation(
            id = groupId,
            name = name,
            lastMessage = "Grupo criado por ${currentUser?.name ?: "alguém"}!",
            timestamp = System.currentTimeMillis(),
            pinnedMessageId = null,
            isFavorite = false,
            isGroup = true
        )

        allMemberIds.forEach { memberId ->
            database.getReference("user-conversations")
                .child(memberId)
                .child(groupId)
                .setValue(groupConversation)
                .await()
        }
    }

    suspend fun getGroupMembers(groupId: String): List<User> = coroutineScope {
        val groupSnapshot = database.getReference("groups")
            .child(groupId)
            .child("members")
            .get()
            .await()

        val memberIds = groupSnapshot.children.mapNotNull { it.key }

        memberIds.map { userId ->
            async(Dispatchers.IO) {
                database.getReference("users")
                    .child(userId)
                    .get()
                    .await()
                    .getValue(User::class.java)
            }
        }.awaitAll().filterNotNull()
    }

    fun getMessagesForConversation(conversationId: String, isGroup: Boolean): Flow<List<Message>> {
        val currentUserId = auth.currentUser?.uid
        val path = if (isGroup) "group-messages" else "messages"
        val messagesRef = database.getReference(path).child(conversationId)

        messagesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    val msg = child.getValue(Message::class.java)
                    msg?.let { decryptMessageIfNeeded(it, conversationId) }
                }

                CoroutineScope(Dispatchers.IO).launch {
                    messages.forEach { message ->
                        messageDao.insertOrUpdate(message)

                        if (!isGroup && message.senderId != currentUserId && message.deliveredTimestamp == 0L) {
                            confirmDelivery(conversationId, message.id)
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatRepository", "Erro ao ouvir mensagens: ${error.message}")
            }
        })

        return messageDao.getMessagesForConversation(conversationId)
    }

    private fun decryptMessageIfNeeded(message: Message, conversationId: String): Message {
        return try {
            message.copy(
                conversationId = conversationId,
                content = EncryptionUtils.decrypt(message.content)
            )
        } catch (_: Exception) {
            message.copy(conversationId = conversationId)
        }
    }

    private fun confirmDelivery(conversationId: String, messageId: String) {
        database.getReference("messages/$conversationId/$messageId")
            .child("deliveredTimestamp")
            .setValue(System.currentTimeMillis())
    }

    fun markMessagesAsRead(conversationId: String, messages: List<Message>, isGroup: Boolean) {
        if (isGroup) return
        val currentUserId = auth.currentUser?.uid ?: return

        messages.forEach { message ->
            if (message.senderId != currentUserId && message.readTimestamp == 0L) {
                database.getReference("messages/$conversationId/${message.id}")
                    .child("readTimestamp")
                    .setValue(System.currentTimeMillis())
            }
        }
    }

    private suspend fun updateLastMessageForConversation(
        conversationId: String,
        lastMessage: String,
        timestamp: Long,
        isGroup: Boolean
    ) {
        val membersToUpdate = if (isGroup) {
            database.getReference("groups")
                .child(conversationId)
                .child("members")
                .get()
                .await()
                .children
                .mapNotNull { it.key }
        } else {
            conversationId.split("-")
        }

        if (membersToUpdate.isEmpty()) return

        membersToUpdate.forEach { memberId ->
            val conversationRef = database.getReference("user-conversations/$memberId/$conversationId")
            val currentConversation = conversationRef.get().await().getValue(Conversation::class.java)

            if (currentConversation != null) {
                val updatedConversation = currentConversation.copy(
                    lastMessage = lastMessage,
                    timestamp = timestamp
                )
                conversationRef.setValue(updatedConversation).await()
            }
        }
    }

    suspend fun sendMessage(
        conversationId: String,
        content: String,
        isGroup: Boolean,
        type: String = "TEXT"
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val path = if (isGroup) "group-messages" else "messages"
        val messagesRef = database.getReference(path).child(conversationId)
        val messageId = messagesRef.push().key ?: return

        val encryptedContent = EncryptionUtils.encrypt(content)

        val localMessage = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            content = content,
            type = type,
            timestamp = System.currentTimeMillis(),
            status = "SENDING"
        )

        messageDao.insertOrUpdate(localMessage)

        val remoteMessage = localMessage.copy(
            status = "SENT",
            content = encryptedContent
        )

        try {
            messagesRef.child(messageId).setValue(remoteMessage).await()
            messageDao.insertOrUpdate(localMessage.copy(status = "SENT"))

            val displayLastMsg = when (type) {
                "LOCATION" -> "📍 Localização"
                else -> content
            }

            updateLastMessageForConversation(
                conversationId = conversationId,
                lastMessage = displayLastMsg,
                timestamp = localMessage.timestamp,
                isGroup = isGroup
            )
            sendPushNotification(conversationId, isGroup, type)
        } catch (e: Exception) {
            messageDao.insertOrUpdate(localMessage.copy(status = "FAILED"))
            Log.e("ChatRepository", "Erro ao enviar mensagem: ${e.message}", e)
        }
    }

    suspend fun sendStickerMessage(conversationId: String, stickerId: String, isGroup: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val path = if (isGroup) "group-messages" else "messages"
        val messagesRef = database.getReference(path).child(conversationId)
        val messageId = messagesRef.push().key ?: return

        val message = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            content = stickerId,
            type = "STICKER",
            timestamp = System.currentTimeMillis(),
            status = "SENT"
        )

        val encryptedMessage = message.copy(content = EncryptionUtils.encrypt(stickerId))
        messagesRef.child(messageId).setValue(encryptedMessage).await()
        messageDao.insertOrUpdate(message)
        updateLastMessageForConversation(conversationId, "Figurinha", message.timestamp, isGroup)
        sendPushNotification(conversationId, isGroup, "STICKER")
    }

    private suspend fun generateAndUploadThumbnailToCloudinary(
        conversationId: String,
        videoUri: Uri,
        messageId: String
    ): String? {
        val thumbnailBitmap: Bitmap? = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(videoUri, Size(480, 480), null)
            } else {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(context, videoUri)
                val bitmap = retriever.getFrameAtTime(
                    1_000_000,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                )
                retriever.release()
                bitmap
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Erro ao gerar thumbnail do vídeo", e)
            null
        }

        if (thumbnailBitmap == null) return null

        return try {
            val tempFile = File.createTempFile("thumb_$messageId", ".jpg", context.cacheDir)

            tempFile.outputStream().use { output ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, output)
            }

            val thumbnailUri = Uri.fromFile(tempFile)

            val uploadedUrl = CloudinaryHelper.uploadMedia(
                uri = thumbnailUri,
                folder = "chat/video_thumbnails/$conversationId",
                publicId = "thumb_$messageId",
                resourceType = "image"
            )

            tempFile.delete()
            uploadedUrl
        } catch (e: Exception) {
            Log.e("ChatRepository", "Erro ao enviar thumbnail para Cloudinary", e)
            null
        }
    }

    suspend fun sendMediaMessage(
        conversationId: String,
        uri: Uri,
        type: String,
        isGroup: Boolean
    ) {
        val currentUserId = auth.currentUser?.uid ?: return
        val path = if (isGroup) "group-messages" else "messages"
        val messagesRef = database.getReference(path).child(conversationId)
        val messageId = messagesRef.push().key ?: return
        val timestamp = System.currentTimeMillis()

        val normalizedType = type.uppercase()

        val sendingMessage = Message(
            id = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            // Para áudio, usa o URI local para permitir reprodução imediata enquanto faz upload.
            // Para outros tipos, fica vazio até o upload terminar.
            content = if (normalizedType == "AUDIO") uri.toString() else "",
            type = normalizedType,
            thumbnailUrl = null,
            timestamp = timestamp,
            status = "SENDING"
        )

        messageDao.insertOrUpdate(sendingMessage)

        try {
            val folder = when (normalizedType) {
                "IMAGE" -> "chat/images/$conversationId"
                "VIDEO" -> "chat/videos/$conversationId"
                "AUDIO" -> "chat/audios/$conversationId"
                else -> "chat/files/$conversationId"
            }

            val resourceType = when (normalizedType) {
                "IMAGE" -> "image"
                "VIDEO" -> "video"
                "AUDIO" -> "video"
                else -> "raw"
            }

            val mediaUrl = CloudinaryHelper.uploadMedia(
                uri = uri,
                folder = folder,
                publicId = "${normalizedType.lowercase()}_$messageId",
                resourceType = resourceType
            )

            val thumbnailUrl = if (normalizedType == "VIDEO") {
                generateAndUploadThumbnailToCloudinary(
                    conversationId = conversationId,
                    videoUri = uri,
                    messageId = messageId
                )
            } else {
                null
            }

            val sentMessage = Message(
                id = messageId,
                conversationId = conversationId,
                senderId = currentUserId,
                content = mediaUrl,
                type = normalizedType,
                thumbnailUrl = thumbnailUrl,
                timestamp = timestamp,
                status = "SENT"
            )

            val encryptedSentMessage = sentMessage.copy(content = EncryptionUtils.encrypt(mediaUrl))
            messagesRef.child(messageId).setValue(encryptedSentMessage).await()
            messageDao.insertOrUpdate(sentMessage)

            val lastMessageText = when (normalizedType) {
                "IMAGE" -> "📷 Imagem"
                "VIDEO" -> "🎥 Vídeo"
                "AUDIO" -> "🎤 Áudio"
                else -> "📎 Arquivo"
            }

            updateLastMessageForConversation(
                conversationId = conversationId,
                lastMessage = lastMessageText,
                timestamp = timestamp,
                isGroup = isGroup
            )
            sendPushNotification(conversationId, isGroup, normalizedType)
        } catch (e: Exception) {
            Log.e("ChatRepository", "Erro ao enviar mídia: ${e.message}", e)
            messageDao.insertOrUpdate(sendingMessage.copy(status = "FAILED"))
        }
    }

    suspend fun createOrGetConversation(targetUser: User): String {
        val currentUserId = auth.currentUser?.uid
            ?: throw IllegalStateException("Usuário não autenticado")

        val conversationId = getConversationId(currentUserId, targetUser.uid)
        val existingConversation = conversationDao.getConversationById(conversationId)
        if (existingConversation != null) return conversationId

        val currentUser = database.getReference("users")
            .child(currentUserId)
            .get()
            .await()
            .getValue(User::class.java)

        val conversationForCurrentUser = Conversation(
            id = conversationId,
            name = targetUser.name,
            profilePictureUrl = targetUser.profilePictureUrl,
            lastMessage = "Inicie a conversa!",
            timestamp = System.currentTimeMillis(),
            pinnedMessageId = null,
            isFavorite = false,
            isGroup = false
        )

        val conversationForTargetUser = Conversation(
            id = conversationId,
            name = currentUser?.name ?: "Usuário",
            profilePictureUrl = currentUser?.profilePictureUrl,
            lastMessage = "Inicie a conversa!",
            timestamp = System.currentTimeMillis(),
            pinnedMessageId = null,
            isFavorite = false,
            isGroup = false
        )

        database.getReference("user-conversations/$currentUserId/$conversationId")
            .setValue(conversationForCurrentUser)
            .await()

        database.getReference("user-conversations/${targetUser.uid}/$conversationId")
            .setValue(conversationForTargetUser)
            .await()

        conversationDao.insertOrUpdate(conversationForCurrentUser)
        return conversationId
    }

    private suspend fun sendPushNotification(
        conversationId: String,
        isGroup: Boolean,
        messageType: String
    ) {
        val currentUserId = auth.currentUser?.uid ?: return

        val bodyText = when (messageType.uppercase()) {
            "IMAGE" -> "📷 Foto"
            "VIDEO" -> "🎥 Vídeo"
            "AUDIO" -> "🎵 Áudio"
            "DOCUMENT" -> "📄 Documento"
            "LOCATION" -> "📍 Localização"
            "STICKER" -> "🎭 Figurinha"
            else -> "Nova mensagem"
        }

        val senderName = database.getReference("users/$currentUserId/name")
            .get().await().getValue(String::class.java) ?: "Alguém"

        val recipientIds: List<String>
        val title: String
        val body: String

        if (isGroup) {
            val groupSnap = database.getReference("groups/$conversationId").get().await()
            val groupName = groupSnap.child("name").getValue(String::class.java) ?: "Grupo"
            recipientIds = groupSnap.child("members").children
                .mapNotNull { it.key }
                .filter { it != currentUserId }
            title = groupName
            body = "$senderName: $bodyText"
        } else {
            // ConversationId para 1:1 é "uid1-uid2" — extrai o outro participante diretamente
            recipientIds = conversationId.split("-")
                .filter { it != currentUserId && it.isNotBlank() }
            title = senderName
            body = bodyText
        }

        recipientIds.forEach { uid ->
            val token = database.getReference("users/$uid/fcmToken")
                .get().await().getValue(String::class.java) ?: return@forEach
            FcmSender.send(token, title, body, conversationId, isGroup)
        }
    }

    private fun getConversationId(userId1: String, userId2: String): String {
        return if (userId1 > userId2) "$userId1-$userId2" else "$userId2-$userId1"
    }

    suspend fun getConversationDetails(conversationId: String): Conversation? {
        return conversationDao.getConversationById(conversationId)
    }

    suspend fun getMessageById(
        conversationId: String,
        messageId: String,
        isGroup: Boolean
    ): Message? {
        val path = if (isGroup) "group-messages" else "messages"
        val snapshot = database.getReference("$path/$conversationId/$messageId").get().await()
        val message = snapshot.getValue(Message::class.java) ?: return null
        return decryptMessageIfNeeded(message, conversationId)
    }

    suspend fun togglePinMessage(conversationId: String, message: Message, isGroup: Boolean) {
        val conversation = conversationDao.getConversationById(conversationId)
        val newPinnedId = if (conversation?.pinnedMessageId == message.id) null else message.id
        val pinUpdate = mapOf<String, Any?>("pinnedMessageId" to newPinnedId)

        if (isGroup) {
            val memberIds = database.getReference("groups")
                .child(conversationId)
                .child("members")
                .get()
                .await()
                .children
                .mapNotNull { it.key }

            memberIds.forEach { memberId ->
                database.getReference("user-conversations/$memberId/$conversationId")
                    .updateChildren(pinUpdate)
                    .await()
            }
        } else {
            val userIds = conversationId.split("-")
            if (userIds.size == 2) {
                database.getReference("user-conversations/${userIds[0]}/$conversationId")
                    .updateChildren(pinUpdate)
                    .await()

                database.getReference("user-conversations/${userIds[1]}/$conversationId")
                    .updateChildren(pinUpdate)
                    .await()
            }
        }

        conversation?.let {
            conversationDao.insertOrUpdate(it.copy(pinnedMessageId = newPinnedId))
        }
    }

    suspend fun toggleReaction(conversationId: String, messageId: String, emoji: String, isGroup: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: return
        val path = if (isGroup) "group-messages" else "messages"
        val messageRef = database.getReference("$path/$conversationId/$messageId")

        val snapshot = messageRef.child("reactions").get().await()
        val reactions = snapshot.getValue<MutableMap<String, String>>() ?: mutableMapOf()

        if (reactions[currentUserId] == emoji) {
            reactions.remove(currentUserId)
        } else {
            reactions[currentUserId] = emoji
        }

        messageRef.child("reactions").setValue(reactions).await()
    }

    suspend fun getGroupDetails(groupId: String): Group? {
        val snapshot = database.getReference("groups").child(groupId).get().await()
        return snapshot.getValue(Group::class.java)
    }

    suspend fun updateGroupName(groupId: String, newName: String) {
        database.getReference("groups").child(groupId).child("name").setValue(newName).await()

        val memberIds = database.getReference("groups")
            .child(groupId)
            .child("members")
            .get()
            .await()
            .children
            .mapNotNull { it.key }

        memberIds.forEach { memberId ->
            database.getReference("user-conversations/$memberId/$groupId")
                .child("name")
                .setValue(newName)
                .await()
        }
    }

    suspend fun addMemberToGroup(groupId: String, userId: String) {
        database.getReference("groups/$groupId/members")
            .child(userId)
            .setValue(true)
            .await()

        val groupConversation = database.getReference("user-conversations")
            .child(auth.currentUser?.uid ?: "")
            .child(groupId)
            .get()
            .await()
            .getValue(Conversation::class.java)

        if (groupConversation != null) {
            database.getReference("user-conversations/$userId/$groupId")
                .setValue(groupConversation)
                .await()
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, userId: String) {
        database.getReference("groups/$groupId/members")
            .child(userId)
            .removeValue()
            .await()

        database.getReference("user-conversations/$userId/$groupId")
            .removeValue()
            .await()
    }

    suspend fun uploadGroupProfilePicture(groupId: String, imageUri: Uri): String {
        val downloadUrl = CloudinaryHelper.uploadMedia(
            uri = imageUri,
            folder = "groups/profile_pictures",
            publicId = "group_$groupId",
            resourceType = "image"
        )

        database.getReference("groups/$groupId")
            .child("profilePictureUrl")
            .setValue(downloadUrl)
            .await()

        val groupSnapshot = database.getReference("groups")
            .child(groupId)
            .child("members")
            .get()
            .await()

        val memberIds = groupSnapshot.children.mapNotNull { it.key }

        memberIds.forEach { memberId ->
            database.getReference("user-conversations/$memberId/$groupId")
                .child("profilePictureUrl")
                .setValue(downloadUrl)
                .await()
        }

        return downloadUrl
    }

    suspend fun importDeviceContacts(): List<Pair<String, String>> {
        val contacts = mutableListOf<Pair<String, String>>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

            while (it.moveToNext()) {
                val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                val number = if (numberIndex >= 0) it.getString(numberIndex) else ""
                contacts.add(name to number)
            }
        }

        return contacts
    }
}