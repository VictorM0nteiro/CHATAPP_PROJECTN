package com.example.app_mensagem.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.app_mensagem.MainActivity
import com.example.app_mensagem.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_CHAT = "channel_direct_chat"
        const val CHANNEL_GROUP = "channel_group_chat"
        private const val PREFS_MUTE = "muted_conversations"

        fun muteConversation(context: Context, conversationId: String) {
            val prefs = context.getSharedPreferences(PREFS_MUTE, Context.MODE_PRIVATE)
            val muted = prefs.getStringSet("muted", mutableSetOf())!!.toMutableSet()
            muted.add(conversationId)
            prefs.edit().putStringSet("muted", muted).apply()
        }

        fun unmuteConversation(context: Context, conversationId: String) {
            val prefs = context.getSharedPreferences(PREFS_MUTE, Context.MODE_PRIVATE)
            val muted = prefs.getStringSet("muted", mutableSetOf())!!.toMutableSet()
            muted.remove(conversationId)
            prefs.edit().putStringSet("muted", muted).apply()
        }

        fun isConversationMuted(context: Context, conversationId: String): Boolean {
            val prefs = context.getSharedPreferences(PREFS_MUTE, Context.MODE_PRIVATE)
            return prefs.getStringSet("muted", emptySet())!!.contains(conversationId)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val conversationId = remoteMessage.data["conversationId"]
        val isGroup = remoteMessage.data["isGroup"]?.toBoolean() ?: false

        // Don't show notification if conversation is muted
        if (conversationId != null && isConversationMuted(this, conversationId)) return

        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Nova mensagem"
        val body = remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""

        showNotification(title, body, conversationId, isGroup)
    }

    private fun showNotification(title: String, body: String, conversationId: String?, isGroup: Boolean) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channels
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_CHAT, "Mensagens Diretas", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificações de conversas individuais"
            }
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_GROUP, "Grupos", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notificações de grupos"
            }
        )

        val channelId = if (isGroup) CHANNEL_GROUP else CHANNEL_CHAT

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            conversationId?.let { putExtra("conversationId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(conversationId?.hashCode() ?: Random.nextInt(), notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users/$uid/fcmToken")
            .setValue(token)
    }
}