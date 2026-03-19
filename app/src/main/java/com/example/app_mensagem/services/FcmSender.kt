package com.example.app_mensagem.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object FcmSender {

    // Cole aqui a Server Key do Firebase Console →
    // Configurações do projeto → Cloud Messaging → Chave do servidor
    private const val SERVER_KEY = "COLE_SUA_SERVER_KEY_AQUI"

    private const val FCM_URL = "https://fcm.googleapis.com/fcm/send"

    /**
     * Envia notificação push para um token FCM específico.
     * Deve ser chamado em uma coroutine (usa Dispatchers.IO internamente).
     */
    suspend fun send(
        toToken: String,
        title: String,
        body: String,
        conversationId: String,
        isGroup: Boolean
    ) = withContext(Dispatchers.IO) {
        if (SERVER_KEY == "COLE_SUA_SERVER_KEY_AQUI") {
            Log.w("FcmSender", "Server Key não configurada — notificação não enviada")
            return@withContext
        }
        if (toToken.isBlank()) return@withContext

        try {
            val payload = JSONObject().apply {
                put("to", toToken)
                put("priority", "high")
                put("notification", JSONObject().apply {
                    put("title", title)
                    put("body", body)
                    put("sound", "default")
                    put("android_channel_id", if (isGroup) "channel_group_chat" else "channel_direct_chat")
                })
                put("data", JSONObject().apply {
                    put("conversationId", conversationId)
                    put("isGroup", isGroup.toString())
                    put("title", title)
                    put("body", body)
                })
            }

            val url = URL(FCM_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "key=$SERVER_KEY")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            OutputStreamWriter(conn.outputStream).use { it.write(payload.toString()) }

            val responseCode = conn.responseCode
            if (responseCode != 200) {
                Log.e("FcmSender", "FCM respondeu $responseCode")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e("FcmSender", "Erro ao enviar notificação: ${e.message}")
        }
    }
}