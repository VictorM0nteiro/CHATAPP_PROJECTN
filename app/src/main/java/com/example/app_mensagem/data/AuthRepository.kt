package com.example.app_mensagem.data

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.app_mensagem.data.model.User
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance()

    suspend fun loginUser(email: String, pass: String): AuthResult {
        val result = auth.signInWithEmailAndPassword(email, pass).await()
        updateFcmToken(result.user?.uid)
        registerSession(result.user?.uid)
        return result
    }

    suspend fun createUser(
        email: String,
        pass: String,
        name: String,
        status: String,
        imageUri: Uri?,
        phoneNumber: String = ""
    ): AuthResult {
        val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
        val firebaseUser = authResult.user

        if (firebaseUser != null) {
            var imageUrl: String? = null

            if (imageUri != null) {
                try {
                    Log.d("AuthRepository", "Iniciando upload para Cloudinary...")
                    imageUrl = CloudinaryHelper.uploadImage(imageUri)
                    Log.d("AuthRepository", "Upload concluído! URL: $imageUrl")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "FALHA NO UPLOAD: ${e.message}")
                }
            }

            val token = try {
                FirebaseMessaging.getInstance().token.await()
            } catch (e: Exception) {
                ""
            }

            val user = User(
                uid = firebaseUser.uid,
                name = name.ifBlank { email.substringBefore('@') },
                email = email,
                phoneNumber = phoneNumber,
                fcmToken = token,
                status = status,
                updateStatus = "",
                updateStatusTimestamp = 0L,
                profilePictureUrl = imageUrl
            )

            database.getReference("users").child(firebaseUser.uid).setValue(user).await()
            registerSession(firebaseUser.uid)
        }

        return authResult
    }

    suspend fun loginWithGoogle(idToken: String): AuthResult {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: return result

        // Create user profile if new
        val existingSnapshot = database.getReference("users/${user.uid}").get().await()
        if (!existingSnapshot.exists()) {
            val token = try { FirebaseMessaging.getInstance().token.await() } catch (_: Exception) { "" }
            val newUser = User(
                uid = user.uid,
                name = user.displayName ?: "",
                email = user.email ?: "",
                phoneNumber = user.phoneNumber ?: "",
                fcmToken = token,
                profilePictureUrl = user.photoUrl?.toString()
            )
            database.getReference("users/${user.uid}").setValue(newUser).await()
        }

        updateFcmToken(user.uid)
        registerSession(user.uid)
        return result
    }

    fun sendPhoneOtp(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun verifyPhoneOtp(verificationId: String, otp: String): AuthResult {
        val credential = PhoneAuthProvider.getCredential(verificationId, otp)
        return signInWithPhoneCredential(credential)
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): AuthResult {
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: return result

        // Create user profile if new
        val existingSnapshot = database.getReference("users/${user.uid}").get().await()
        if (!existingSnapshot.exists()) {
            val token = try { FirebaseMessaging.getInstance().token.await() } catch (_: Exception) { "" }
            val newUser = User(
                uid = user.uid,
                name = user.displayName ?: user.phoneNumber ?: "",
                email = user.email ?: "",
                phoneNumber = user.phoneNumber ?: "",
                fcmToken = token
            )
            database.getReference("users/${user.uid}").setValue(newUser).await()
        }

        updateFcmToken(user.uid)
        registerSession(user.uid)
        return result
    }

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun updateFcmToken(userId: String?) {
        if (userId == null) return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            database.getReference("users").child(userId).child("fcmToken").setValue(token)
        } catch (_: Exception) {
        }
    }

    private suspend fun registerSession(userId: String?) {
        if (userId == null) return
        try {
            val token = try { FirebaseMessaging.getInstance().token.await() } catch (_: Exception) { "" }
            val deviceId = "android_${userId.take(8)}_${Build.FINGERPRINT.take(16).replace("/", "_")}"
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
            val session = mapOf(
                "deviceId" to deviceId,
                "deviceName" to deviceName,
                "lastActive" to System.currentTimeMillis(),
                "fcmToken" to token,
                "platform" to "android"
            )
            database.getReference("user-sessions/$userId/$deviceId").setValue(session).await()
        } catch (_: Exception) {
        }
    }

    fun getCurrentDeviceId(userId: String): String {
        return "android_${userId.take(8)}_${Build.FINGERPRINT.take(16).replace("/", "_")}"
    }

    suspend fun getSessions(userId: String): List<Map<String, Any>> {
        val snapshot = database.getReference("user-sessions/$userId").get().await()
        return snapshot.children.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            it.value as? Map<String, Any>
        }
    }

    suspend fun revokeSession(userId: String, deviceId: String) {
        database.getReference("user-sessions/$userId/$deviceId").removeValue().await()
    }
}