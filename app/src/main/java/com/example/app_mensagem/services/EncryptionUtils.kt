package com.example.app_mensagem.services

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object EncryptionUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    // Chave compartilhada (igual em todos os dispositivos) — AES-256 GCM
    private val SHARED_KEY: SecretKeySpec by lazy {
        val raw = "ChatApp@Shared#Key2024!".toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(32)
        raw.copyInto(keyBytes, endIndex = minOf(raw.size, 32))
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    // Chave legada usada antes (AES/ECB hardcoded)
    private val LEGACY_KEY = SecretKeySpec("1234567890123456".toByteArray(), ALGORITHM)

    fun encrypt(value: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SHARED_KEY)
            val iv = cipher.iv
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(iv + encrypted, Base64.NO_WRAP)
        } catch (_: Exception) {
            value
        }
    }

    fun decrypt(value: String): String {
        // Estratégia 1: AES/GCM com chave compartilhada (mensagens novas)
        try {
            val combined = Base64.decode(value, Base64.NO_WRAP)
            if (combined.size > IV_SIZE) {
                val iv = combined.copyOfRange(0, IV_SIZE)
                val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, SHARED_KEY, GCMParameterSpec(TAG_SIZE, iv))
                return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
            }
        } catch (_: Exception) {}

        // Estratégia 2: AES/ECB com chave legada (mensagens antigas)
        try {
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, LEGACY_KEY)
            val decoded = Base64.decode(value, Base64.NO_WRAP)
            return String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (_: Exception) {}

        // Estratégia 3: já é texto plano
        return value
    }
}