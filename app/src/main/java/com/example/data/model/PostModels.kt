package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class PostType {
    VIDEO,
    SHORT,
    PHOTO
}

enum class VideoStatus(val displayName: String, val badgeColorHex: Long) {
    UPLOADING("Uploading", 0xFF3B82F6),
    PROCESSING("Processing", 0xFF8B5CF6),
    PENDING("Pending Verification", 0xFFF59E0B),
    APPROVED("Approved & Published", 0xFF10B981),
    REJECTED("Rejected", 0xFFEF4444);

    companion object {
        fun fromString(value: String): VideoStatus = entries.find { it.name.equals(value, ignoreCase = true) } ?: APPROVED
    }
}

@Entity(tableName = "posts")
data class PostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: PostType = PostType.VIDEO,
    val title: String,
    val description: String = "",
    val mediaUrl: String = "",
    val thumbnailUrl: String = "",
    val channelId: String = "user_channel",
    val channelName: String,
    val channelAvatar: String = "",
    val subscriberCount: String = "1.2K subscribers",
    val views: String = "12K views",
    val viewCount: Long = 12400L,
    val likeCount: Long = 850L,
    val dislikeCount: Long = 12L,
    val commentCount: Long = 48L,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val category: String = "All",
    val duration: String = "10:24",
    val isVerified: Boolean = true,
    val tags: String = "#Satisfy #Trending",
    val isUserCreated: Boolean = false,
    val isLiked: Boolean = false,
    val isDisliked: Boolean = false,
    val isSaved: Boolean = false,
    val isSubscribed: Boolean = false,
    val isFeatured: Boolean = false,
    val isFlagged: Boolean = false,
    val watchTimeSeconds: Long = 0L,
    val pageId: Long? = null,
    val status: String = "APPROVED",
    val rejectionReason: String? = null,
    val creatorUid: String = "",
    val videoUri: String? = null,
    val isPremium: Boolean = false
)

@Entity(tableName = "creator_pages")
data class CreatorPageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val handle: String,
    val category: String = "Entertainment",
    val description: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val websiteLink: String = "",
    val followersCount: Long = 1,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val totalWatchTimeSeconds: Long = 0L,
    val totalViews: Long = 0L
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: Long,
    val authorName: String,
    val authorAvatar: String = "",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    val isCreatorHearted: Boolean = false
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: Long,
    val watchedAt: Long = System.currentTimeMillis()
)

data class UserProfile(
    val uid: String = "user_creator",
    val name: String = "Satisfy Creator",
    val handle: String = "@satisfy_creator",
    val email: String = "creator@satisfy.app",
    val bio: String = "Welcome to my Satisfy channel! Sharing satisfying video creations, 4K nature cinematography, and community photography. ✨",
    val avatarUrl: String = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
    val bannerUrl: String = "",
    val subscriberCount: String = "1.2K subscribers",
    val link: String = "satisfy.app/@satisfy_creator",
    val isPro: Boolean = false,
    val proExpiresAt: Long? = null,
    val proStartedAt: Long? = null,
    val referralCode: String = "SATISFY100",
    val referredByCode: String? = null
)
