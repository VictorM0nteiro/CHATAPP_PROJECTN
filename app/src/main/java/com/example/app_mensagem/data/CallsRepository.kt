package com.example.app_mensagem.data

import com.example.app_mensagem.data.model.CallRecord
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await

class CallsRepository {

    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()

    suspend fun getCallHistory(): List<CallRecord> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val snapshot = database.getReference("calls").child(userId).get().await()
        return snapshot.children
            .mapNotNull { it.getValue(CallRecord::class.java) }
            .sortedByDescending { it.timestamp }
    }

    suspend fun saveCallRecord(record: CallRecord) {
        val callerId = record.callerId
        val receiverId = record.receiverId

        // Salva para o chamador
        database.getReference("calls/$callerId/${record.id}").setValue(record).await()

        // Salva para o receptor como INCOMING (ou MISSED)
        val incomingRecord = record.copy(status = "MISSED")
        database.getReference("calls/$receiverId/${record.id}").setValue(incomingRecord).await()
    }

    suspend fun getUsers(): List<User> {
        val currentUserId = auth.currentUser?.uid
        val snapshot = database.getReference("users").get().await()
        return snapshot.children
            .mapNotNull { it.getValue(User::class.java) }
            .filter { it.uid != currentUserId }
    }

    fun getCurrentUserId() = auth.currentUser?.uid ?: ""

    suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return database.getReference("users").child(uid).get().await().getValue(User::class.java)
    }
}
