const { onValueCreated } = require("firebase-functions/v2/database");
const { initializeApp } = require("firebase-admin/app");
const { getDatabase } = require("firebase-admin/database");
const { getMessaging } = require("firebase-admin/messaging");

initializeApp();

/**
 * Disparado quando uma nova mensagem é criada em qualquer conversa.
 * Caminho: /conversations/{conversationId}/messages/{messageId}
 */
exports.sendChatNotification = onValueCreated(
  {
    ref: "/conversations/{conversationId}/messages/{messageId}",
    region: "us-central1",
  },
  async (event) => {
    const message = event.data.val();
    const { conversationId, messageId } = event.params;

    if (!message) return null;

    const senderId = message.senderId;
    const messageType = (message.type || "TEXT").toUpperCase();

    // Monta o texto de preview da notificação conforme o tipo
    let bodyText;
    switch (messageType) {
      case "IMAGE":
        bodyText = "📷 Foto";
        break;
      case "VIDEO":
        bodyText = "🎥 Vídeo";
        break;
      case "AUDIO":
        bodyText = "🎵 Áudio";
        break;
      case "DOCUMENT":
        bodyText = "📄 Documento";
        break;
      case "LOCATION":
        bodyText = "📍 Localização";
        break;
      case "STICKER":
        bodyText = "🎭 Figurinha";
        break;
      default:
        // TEXT: descriptografa não é possível aqui (chave no cliente),
        // então mostramos mensagem genérica
        bodyText = "Nova mensagem";
    }

    const db = getDatabase();

    // Busca dados da conversa para saber os participantes
    const convSnap = await db.ref(`/conversations/${conversationId}`).get();
    const convData = convSnap.val();
    if (!convData) return null;

    const isGroup = convData.isGroup === true;
    const groupName = convData.groupName || convData.name || "Grupo";

    // Determina os destinatários (todos exceto o remetente)
    let recipientIds = [];
    if (isGroup) {
      const members = convData.members || convData.participantIds || {};
      recipientIds = Object.keys(members).filter((uid) => uid !== senderId);
    } else {
      const participants = convData.participantIds || convData.members || {};
      recipientIds = Object.keys(participants).filter((uid) => uid !== senderId);
    }

    if (recipientIds.length === 0) return null;

    // Busca o nome do remetente
    const senderSnap = await db.ref(`/users/${senderId}/name`).get();
    const senderName = senderSnap.val() || "Alguém";

    const notifTitle = isGroup ? `${groupName}` : senderName;
    const notifBody = isGroup ? `${senderName}: ${bodyText}` : bodyText;

    // Busca os tokens FCM de cada destinatário e envia
    const tokenFetches = recipientIds.map((uid) =>
      db.ref(`/users/${uid}/fcmToken`).get()
    );
    const tokenSnaps = await Promise.all(tokenFetches);

    const tokens = tokenSnaps
      .map((snap) => snap.val())
      .filter((token) => !!token);

    if (tokens.length === 0) return null;

    // Envia para cada token individualmente (suporta tokens inválidos sem falhar tudo)
    const sends = tokens.map((token) =>
      getMessaging()
        .send({
          token,
          notification: {
            title: notifTitle,
            body: notifBody,
          },
          data: {
            conversationId,
            isGroup: isGroup ? "true" : "false",
            title: notifTitle,
            body: notifBody,
          },
          android: {
            priority: "high",
            notification: {
              channelId: isGroup ? "channel_group_chat" : "channel_direct_chat",
              sound: "default",
            },
          },
        })
        .catch((err) => {
          // Token inválido: remove do banco
          if (
            err.code === "messaging/invalid-registration-token" ||
            err.code === "messaging/registration-token-not-registered"
          ) {
            const uid = recipientIds[tokens.indexOf(token)];
            if (uid) db.ref(`/users/${uid}/fcmToken`).remove();
          }
        })
    );

    await Promise.all(sends);
    return null;
  }
);