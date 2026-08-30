package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class NotificationType(val displayName: String, val badgeEmoji: String) {
    VIDEO_UPLOAD("New Video", "🎬"),
    COMMENT("New Comment", "💬"),
    COMMENT_REPLY("Reply", "↩️"),
    LIKE("New Like", "❤️"),
    SUBSCRIBER("New Subscriber", "🌟"),
    MONETIZATION_UPDATE("Monetization", "💰"),
    PRO_MEMBERSHIP("Pro Membership", "💎"),
    WALLET_PAYOUT("Wallet & Payout", "💳"),
    ADMIN_BROADCAST("Announcement", "📢"),
    SYSTEM_ALERT("System Alert", "⚡")
}

@Entity(tableName = "user_notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val firestoreId: String = UUID.randomUUID().toString(),
    val recipientUid: String = "user_creator",
    val senderUid: String = "",
    val senderName: String = "Satisfy Official",
    val senderAvatar: String = "",
    val type: NotificationType = NotificationType.SYSTEM_ALERT,
    val title: String,
    val body: String,
    val targetId: Long? = null,
    val targetType: String = "NONE", // "POST", "SHORT", "COMMENT", "PAGE", "MONETIZATION", "PRO", "WALLET", "BROADCAST", "NONE"
    val targetThumbnailUrl: String = "",
    val actionUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isPinned: Boolean = false,
    val priority: String = "NORMAL", // "HIGH", "NORMAL", "LOW"
    val deliveredVia: String = "FIREBASE_FIRESTORE" // "FIREBASE_FIRESTORE", "FCM_PUSH", "LOCAL_TRIGGER"
)

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val inAppBannerEnabled: Boolean = true,
    val soundVibrateEnabled: Boolean = true,
    val videoUploadAlerts: Boolean = true,
    val commentMentionAlerts: Boolean = true,
    val monetizationAlerts: Boolean = true,
    val adminBroadcastAlerts: Boolean = true,
    val proMembershipAlerts: Boolean = true
)

data class InAppNotificationToast(
    val notification: NotificationEntity,
    val id: String = UUID.randomUUID().toString()
)
