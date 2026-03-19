package com.example.app_mensagem.presentation.viewmodel

import android.app.Activity
import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_mensagem.MyApplication
import com.example.app_mensagem.data.AuthRepository
import com.example.app_mensagem.data.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val uid: String) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
    object SignedOut : AuthUiState()
    object PasswordResetSent : AuthUiState()
    data class PhoneOtpSent(val verificationId: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val chatRepository: ChatRepository
    private val firebaseAuth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    init {
        val db = (application as MyApplication).database
        chatRepository = ChatRepository(db.conversationDao(), db.messageDao(), application)
    }

    fun login(email: String, pass: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.loginUser(email, pass)
                chatRepository.syncUserConversations()
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ocorreu um erro desconhecido.")
            }
        }
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.loginWithGoogle(idToken)
                chatRepository.syncUserConversations()
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Falha no login com Google.")
            }
        }
    }

    fun sendPhoneOtp(phoneNumber: String, activity: Activity) {
        _uiState.value = AuthUiState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verification (instant verify on some devices)
                viewModelScope.launch {
                    try {
                        val result = authRepository.signInWithPhoneCredential(credential)
                        chatRepository.syncUserConversations()
                        _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
                    } catch (e: Exception) {
                        _uiState.value = AuthUiState.Error(e.message ?: "Erro na verificação.")
                    }
                }
            }

            override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
                _uiState.value = AuthUiState.Error(e.message ?: "Falha ao enviar OTP.")
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                _uiState.value = AuthUiState.PhoneOtpSent(verificationId)
            }
        }

        authRepository.sendPhoneOtp(phoneNumber, activity, callbacks)
    }

    fun verifyPhoneOtp(verificationId: String, otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.verifyPhoneOtp(verificationId, otp)
                chatRepository.syncUserConversations()
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Código inválido.")
            }
        }
    }

    fun signUp(email: String, pass: String, name: String, status: String, imageUri: Uri?, phoneNumber: String = "") {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val result = authRepository.createUser(email, pass, name, status, imageUri, phoneNumber)
                _uiState.value = AuthUiState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Ocorreu um erro ao criar a conta.")
            }
        }
    }

    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                authRepository.sendPasswordResetEmail(email)
                _uiState.value = AuthUiState.PasswordResetSent
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Falha ao enviar e-mail.")
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepository.clearLocalCache()
            firebaseAuth.signOut()
            withContext(Dispatchers.Main) {
                _uiState.value = AuthUiState.SignedOut
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}