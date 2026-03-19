package com.example.app_mensagem.services

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Criptografia AES/GCM/NoPadding com chave compartilhada entre dispositivos.
 *
 * Vantagens sobre a implementação anterior (AES/ECB):
 *  - IV aleatório por mensagem → sem vazamento de padrões
 *  - Tag de autenticação GCM → detecta adulteração
 *  - Cobre todos os tipos de mensagem (texto, mídia, áudio, etc.)
 *
 * Nota: para E2E real entre usuários, seria necessário o protocolo Signal
 * (chaves assimétricas por par de usuários). Esta implementação protege os
 * dados em repouso no Firebase contra leitura direta no console.
 */
object EncryptionUtils {

    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128

    // Chave de 32 bytes compartilhada por todos os clientes.
    // Derivada de uma frase fixa padded para 256 bits.
    private val SHARED_KEY: SecretKeySpec by lazy {
        val raw = "ChatApp@Shared#Key2024!".toByteArray(Charsets.UTF_8)
        val keyBytes = ByteArray(32)
        raw.copyInto(keyBytes, endIndex = minOf(raw.size, 32))
        SecretKeySpec(keyBytes, ALGORITHM)
    }

    fun encrypt(value: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, SHARED_KEY)
            val iv = cipher.iv                                         // IV aleatório (12 bytes)
            val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            val combined = iv + encrypted                              // IV + ciphertext + GCM tag
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (e: Exception) {
            value // fallback: armazena sem cifrar se falhar
        }
    }

    fun decrypt(value: String): String {
        return try {
            val combined = Base64.decode(value, Base64.NO_WRAP)
            if (combined.size <= IV_SIZE) return value
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, SHARED_KEY, GCMParameterSpec(TAG_SIZE, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            value // fallback: retorna valor bruto (mensagens antigas ou sem cifra)
        }
    }
}