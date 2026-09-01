package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Locale

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
    val subscriberCount: String = "0 subscribers",
    val views: String = "0 views",
    val viewCount: Long = 0L,
    val likeCount: Long = 0L,
    val dislikeCount: Long = 0L,
    val commentCount: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val category: String = "All",
    val duration: String = "00:00",
    val durationSeconds: Long = 0L,
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
    val isPremium: Boolean = false,
    val approvedAt: Long? = null,
    val rejectedAt: Long? = null,
    val qualities: String = "Auto,1080p,720p,480p,360p",
    val aiQualityScore: Int = 92,
    val aiModerationReason: String? = null,
    val aiModerationRiskScore: Float = 0.0f,
    val isSpamLimited: Boolean = false,
    val sharesCount: Long = 0L,
    val avgRetentionRate: Float = 0.72f
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
    val followersCount: Long = 0L,
    val isVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val totalWatchTimeSeconds: Long = 0L,
    val totalViews: Long = 0L,
    val creatorUid: String = "user_creator"
)

@Entity(tableName = "saved_accounts")
data class SavedAccountEntity(
    @PrimaryKey
    val uid: String,
    val name: String,
    val handle: String,
    val email: String,
    val bio: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String = "",
    val link: String = "",
    val subscriberCount: String = "0 subscribers",
    val isPro: Boolean = false,
    val activePlanId: String = "plan_pro_5",
    val activePlanName: String = "Free",
    val activePlanTier: String = "NONE",
    val subscriptionStatus: String = "INACTIVE",
    val proStartedAt: Long? = null,
    val proExpiresAt: Long? = null,
    val referralCode: String = "",
    val referredByCode: String? = null,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = false
)

@Entity(tableName = "user_likes", primaryKeys = ["userId", "postId"])
data class UserLikeEntity(
    val userId: String,
    val postId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_saves", primaryKeys = ["userId", "postId"])
data class UserSavedEntity(
    val userId: String,
    val postId: Long,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_subscriptions", primaryKeys = ["userId", "channelName"])
data class UserSubscriptionEntity(
    val userId: String,
    val channelName: String,
    val subscribedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "comments")
data class CommentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val postId: Long,
    val authorUid: String = "user_creator",
    val authorName: String,
    val authorAvatar: String = "",
    val text: String,
    val parentCommentId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val timeAgo: String = "Just now",
    val likeCount: Long = 0,
    val isLiked: Boolean = false,
    val isCreatorHearted: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "user_creator",
    val postId: Long,
    val lastPositionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
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
    val subscriberCount: String = "0 subscribers",
    val link: String = "satisfy.app/@satisfy_creator",
    val isPro: Boolean = false,
    val activePlanId: String = "plan_pro_5",
    val activePlanName: String = "Free",
    val activePlanTier: String = "NONE",
    val subscriptionStatus: String = "INACTIVE",
    val proExpiresAt: Long? = null,
    val proStartedAt: Long? = null,
    val referralCode: String = "SATISFY100",
    val referredByCode: String? = null,
    // Real-time Online / Offline / Last Seen & Privacy Settings
    val isOnline: Boolean = true,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val showOnlineStatus: Boolean = true,
    val showLastSeen: Boolean = true,
    val presencePrivacy: PresencePrivacySetting = PresencePrivacySetting.EVERYONE,
    val customStatusMessage: String = "Active on Satisfy ✨"
)

@Entity(tableName = "monetization_applications")
data class MonetizationApplicationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String = "user_creator",
    val channelName: String,
    val channelHandle: String,
    val channelAvatar: String = "",
    val subscriberCount: Long = 0L,
    val normalVideoWatchHours: Double = 0.0,
    val shortsWatchHours: Double = 0.0,
    val totalShortsCount: Int = 0,
    val totalVideosCount: Int = 0,
    val appliedAt: Long = System.currentTimeMillis(),
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewedAt: Long? = null,
    val rejectionReason: String? = null,
    val adminNotes: String = ""
)

data class CreatorAnalyticsSummary(
    val totalShortsUploaded: Int = 0,
    val totalShortsViews: Long = 0L,
    val totalShortsWatchTimeSeconds: Long = 0L,
    val totalVideosUploaded: Int = 0,
    val totalVideoViews: Long = 0L,
    val totalVideoWatchTimeSeconds: Long = 0L,
    val totalSubscribers: Long = 0L,
    val individualShorts: List<PostEntity> = emptyList(),
    val individualVideos: List<PostEntity> = emptyList()
)

data class MonetizationEligibility(
    val currentSubscribers: Long = 0L,
    val requiredSubscribers: Long = 500L,
    val isSubscriberRequirementMet: Boolean = false,

    val currentNormalWatchHours: Double = 0.0,
    val requiredNormalWatchHours: Double = 4000.0,
    val isNormalWatchRequirementMet: Boolean = false,

    val currentShortsWatchHours: Double = 0.0,
    val requiredShortsWatchHours: Double = 10000.0,
    val isShortsWatchRequirementMet: Boolean = false,

    val isPathwayAMet: Boolean = false, // 500 subs + 4000 normal hrs
    val isPathwayBMet: Boolean = false, // 500 subs + 10000 shorts hrs
    val isEligible: Boolean = false,

    val remainingSubscribers: Long = 0L,
    val remainingNormalWatchHours: Double = 0.0,
    val remainingShortsWatchHours: Double = 0.0
)

// Dynamic Formatter Helpers to eliminate dummy/hardcoded numbers everywhere
fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format(Locale.US, "%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

fun formatViews(count: Long): String {
    val formatted = formatCount(count)
    return if (count == 1L) "$formatted view" else "$formatted views"
}

fun formatSubscribers(count: Long): String {
    val formatted = formatCount(count)
    return if (count == 1L) "$formatted subscriber" else "$formatted subscribers"
}

fun formatWatchTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m ${seconds}s"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}

fun formatWatchHoursDouble(hours: Double): String {
    return if (hours >= 1000.0) {
        String.format(Locale.US, "%,.1f hrs", hours)
    } else {
        String.format(Locale.US, "%.1f hrs", hours)
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30
    val years = days / 365

    return when {
        years > 0 -> if (years == 1L) "1 year ago" else "$years years ago"
        months > 0 -> if (months == 1L) "1 month ago" else "$months months ago"
        weeks > 0 -> if (weeks == 1L) "1 week ago" else "$weeks weeks ago"
        days > 0 -> if (days == 1L) "1 day ago" else "$days days ago"
        hours > 0 -> if (hours == 1L) "1 hour ago" else "$hours hours ago"
        minutes > 0 -> if (minutes == 1L) "1 min ago" else "$minutes mins ago"
        else -> "Just now"
    }
}

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class CreatorSearchResult(
    val channelName: String,
    val channelAvatar: String,
    val creatorUid: String,
    val pageId: Long? = null,
    val subscriberCount: String,
    val isVerified: Boolean = false,
    val videoCount: Int = 0,
    val topCategory: String = "Creator"
)

data class HashtagSearchResult(
    val hashtag: String,
    val count: Int
)

data class TrendingSearchItem(
    val query: String,
    val category: String,
    val subtitle: String,
    val views: String,
    val rank: Int,
    val isHot: Boolean = false
)

enum class SuggestionType {
    QUERY,
    CREATOR,
    HASHTAG,
    VIDEO
}

data class SearchSuggestion(
    val text: String,
    val type: SuggestionType,
    val subtitle: String = "",
    val countOrBadge: String = "",
    val avatarOrThumb: String = "",
    val postEntity: PostEntity? = null,
    val creatorResult: CreatorSearchResult? = null
)

data class ContinueWatchingItem(
    val post: PostEntity,
    val history: WatchHistoryEntity,
    val progressPercent: Float = 0f,
    val lastPositionSeconds: Long = 0L,
    val durationSeconds: Long = 0L,
    val formattedPosition: String = "00:00",
    val formattedRemaining: String = "00:00"
)


