package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserAccountEntity(
    @PrimaryKey
    val uid: String,
    val name: String,
    val email: String,
    val avatarUrl: String = "",
    val role: String = "user", // "admin", "moderator", "creator", "user"
    val isBanned: Boolean = false,
    val banReason: String = "",
    val postsCount: Int = 0,
    val reportsCount: Int = 0,
    val joinedDate: String = "Aug 2026",
    val lastActive: String = "Just now",
    val fcmToken: String = ""
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetId: Long,
    val targetType: String = "POST", // "POST", "USER", "COMMENT"
    val targetTitle: String,
    val reporterName: String,
    val reportedUser: String,
    val reason: String, // "Inappropriate Content", "Spam / Misleading", "Harassment", "Copyright Infringement", "Other"
    val details: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val status: String = "PENDING", // "PENDING", "RESOLVED", "DISMISSED"
    val priority: String = "MEDIUM", // "HIGH", "MEDIUM", "LOW"
    val actionTaken: String = ""
)

@Entity(tableName = "push_notifications")
data class PushNotificationLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val targetTopic: String = "all_users",
    val targetAudienceLabel: String = "All Users",
    val sentAt: Long = System.currentTimeMillis(),
    val sentTimeFormatted: String = "Just now",
    val deliveredCount: Int = 0,
    val status: String = "DELIVERED",
    val actionUrl: String = ""
)

@Entity(tableName = "app_settings")
data class AppSystemSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isMaintenanceMode: Boolean = false,
    val allowNewRegistrations: Boolean = true,
    val autoModerationEnabled: Boolean = true,
    val maxUploadSizeMb: Int = 500,
    val communityGuidelinesUrl: String = "https://satisfy.app/guidelines",
    val announcementBanner: String = "⚡ Welcome to Satisfy v2.0 - Experience Ultra Fast Media Sharing!",
    val announcementEnabled: Boolean = true,
    val minAppVersion: String = "1.0.0"
)

@Entity(tableName = "audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val action: String,
    val adminEmail: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = "Just now"
)

data class AdminAuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val role: String = "admin",
    val photoUrl: String = "",
    val isVerifiedAdmin: Boolean = true
)

data class DashboardAnalyticsSummary(
    val totalUsers: Int = 0,
    val activeUsersToday: Int = 0,
    val totalPosts: Int = 0,
    val totalVideos: Int = 0,
    val totalShorts: Int = 0,
    val totalPhotos: Int = 0,
    val totalViews: Long = 0L,
    val totalLikes: Long = 0L,
    val pendingReports: Int = 0,
    val resolvedReports: Int = 0,
    val pushBroadcastsSent: Int = 0,
    val serverStatus: String = "Healthy (100% Uptime)"
)
