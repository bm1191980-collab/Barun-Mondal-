package com.example.data.service

import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.data.model.RecentSearchEntity
import java.util.Locale
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.max

data class UserRecommendationProfile(
    val userId: String = "user_creator",
    val subscribedChannels: Set<String> = emptySet(),
    val likedPostIds: Set<Long> = emptySet(),
    val savedPostIds: Set<Long> = emptySet(),
    val watchedPostIds: Set<Long> = emptySet(),
    val watchedCategories: Map<String, Int> = emptyMap(),
    val likedTags: Set<String> = emptySet(),
    val recentQueries: List<String> = emptyList()
)

data class ScoredPost(
    val post: PostEntity,
    val totalScore: Double,
    val algorithmBadge: String = "",
    val watchTimeScore: Double = 0.0,
    val retentionScore: Double = 0.0,
    val engagementScore: Double = 0.0,
    val interestMatchScore: Double = 0.0,
    val subscriptionAffinityScore: Double = 0.0,
    val qualityScore: Double = 0.0,
    val isPersonalizedMatch: Boolean = false
)

object SatisfyRecommendationEngine {

    /**
     * Calculates the multidimensional AI recommendation score for a given post based on:
     * 1. Watch Time (Accumulated seconds)
     * 2. Audience Retention Rate (Completion ratio)
     * 3. Likes & Like-to-Dislike Engagement Ratio
     * 4. Comments & Discussion Velocity
     * 5. Shares & Saves Volume
     * 6. Channel Subscriptions & Creator Affinity
     * 7. User Interests & Search Intent Alignment
     * 8. Content Quality & Metadata Score
     * 9. Spam / Harm / Duplicate Penalty Filtration
     * 10. Recency & Freshness Decay Curve
     */
    fun scorePost(post: PostEntity, userContext: UserRecommendationProfile): ScoredPost {
        // --- 1. Filter Out Suppressed Content ---
        if (post.status == "REJECTED") {
            return ScoredPost(post, totalScore = -9999.0)
        }

        // --- 2. Watch Time Score (Logarithmic scaling) ---
        // Range: ~ 0 to 45 points
        val effectiveWatchSeconds = max(0L, post.watchTimeSeconds)
        val watchTimeScore = log10(1.0 + effectiveWatchSeconds) * 18.0

        // --- 3. Audience Retention Score (Completion rate %) ---
        // Range: ~ 0 to 50 points
        val retentionRate = post.avgRetentionRate.coerceIn(0.1f, 1.0f)
        val retentionScore = retentionRate * 45.0

        // --- 4. Likes & Engagement Ratio Score ---
        val totalVotes = post.likeCount + post.dislikeCount
        val likeRatio = if (totalVotes > 0) (post.likeCount.toDouble() / totalVotes.toDouble()) else 0.90
        val likesLog = log10(1.0 + post.likeCount) * 14.0
        val likeScore = likesLog * likeRatio

        // --- 5. Comments & Community Discussion Velocity ---
        val commentScore = log10(1.0 + post.commentCount) * 12.0

        // --- 6. Shares & Saves Virality Factor ---
        val rawShares = max(0L, post.sharesCount)
        val isSavedByUser = userContext.savedPostIds.contains(post.id) || post.isSaved
        val savesBonus = if (isSavedByUser) 5.0 else 0.0
        val shareScore = (log10(1.0 + rawShares) * 16.0) + savesBonus

        val combinedEngagementScore = likeScore + commentScore + shareScore

        // --- 7. Channel Subscription & Affinity ---
        val isSubscribed = userContext.subscribedChannels.contains(post.channelName.trim()) || post.isSubscribed
        val subscriptionAffinityScore = if (isSubscribed) 35.0 else 0.0

        // --- 8. User Interests & Search Intent Alignment ---
        var interestMatchScore = 0.0
        var hasCategoryInterest = false
        var hasTagInterest = false
        var hasQueryMatch = false

        // Category frequency in user watch history
        val catCount = userContext.watchedCategories[post.category] ?: 0
        if (catCount > 0) {
            interestMatchScore += (catCount * 8.0).coerceAtMost(30.0)
            hasCategoryInterest = true
        }

        // Tag overlap with user liked tags
        val postTags = post.tags.lowercase(Locale.ROOT).split(" ", "#", ",").filter { it.isNotBlank() }
        val matchingTags = postTags.filter { userContext.likedTags.contains(it) }
        if (matchingTags.isNotEmpty()) {
            interestMatchScore += (matchingTags.size * 6.0).coerceAtMost(24.0)
            hasTagInterest = true
        }

        // Recent user search query alignment
        val titleLower = post.title.lowercase(Locale.ROOT)
        for (query in userContext.recentQueries.take(5)) {
            val qLower = query.lowercase(Locale.ROOT).trim()
            if (qLower.isNotBlank() && (titleLower.contains(qLower) || post.tags.lowercase(Locale.ROOT).contains(qLower))) {
                interestMatchScore += 20.0
                hasQueryMatch = true
                break
            }
        }

        // --- 9. Content Quality & Technical Score ---
        val baseQuality = (post.aiQualityScore / 100.0) * 15.0
        val hdBonus = if (post.qualities.contains("4K") || post.qualities.contains("1080p")) 6.0 else 0.0
        val qualityScore = baseQuality + hdBonus

        // --- 10. Recency & Discovery Boost Curve ---
        val ageHours = ((System.currentTimeMillis() - post.timestamp) / 3600000.0).coerceAtLeast(0.0)
        // High freshness boost for uploads under 24 hours, decaying smoothly over a week
        val freshnessScore = exp(-ageHours / 96.0) * 16.0

        // --- 11. Penalties (AI Flagged, Spam Limited Reach, User Reports) ---
        var penalty = 0.0
        if (post.isFlagged) {
            penalty += 120.0 // Heavy suppression for unreviewed AI flagged or reported items
        }
        if (post.isSpamLimited) {
            penalty += 60.0  // Throttled distribution for promotional/spam items
        }

        // Calculate Total AI Ranking Score
        val totalScore = (
            watchTimeScore +
            retentionScore +
            combinedEngagementScore +
            subscriptionAffinityScore +
            interestMatchScore +
            qualityScore +
            freshnessScore -
            penalty
        ).coerceAtLeast(-500.0)

        // Generate Human-Readable Algorithm Badge
        val algorithmBadge = when {
            post.isFlagged -> "⚠️ Under Review"
            post.isSpamLimited -> "ℹ️ Limited Reach"
            hasQueryMatch -> "🔍 Matches Your Search"
            hasCategoryInterest && hasTagInterest -> "✨ 99% Match For You"
            isSubscribed -> "🔔 Subscribed Creator"
            retentionRate >= 0.85f -> "⚡ High Retention (90%+)"
            watchTimeScore > 20.0 && combinedEngagementScore > 25.0 -> "🔥 Viral Trending"
            post.aiQualityScore >= 95 -> "🌟 4K High Quality"
            freshnessScore > 10.0 -> "🚀 Fresh Upload"
            else -> "✨ Recommended"
        }

        val isPersonalized = isSubscribed || hasCategoryInterest || hasTagInterest || hasQueryMatch

        return ScoredPost(
            post = post,
            totalScore = totalScore,
            algorithmBadge = algorithmBadge,
            watchTimeScore = watchTimeScore,
            retentionScore = retentionScore,
            engagementScore = combinedEngagementScore,
            interestMatchScore = interestMatchScore,
            subscriptionAffinityScore = subscriptionAffinityScore,
            qualityScore = qualityScore,
            isPersonalizedMatch = isPersonalized
        )
    }

    /**
     * Ranks a collection of posts using the recommendation engine for personalized feeds.
     */
    fun rankPosts(
        posts: List<PostEntity>,
        userContext: UserRecommendationProfile = UserRecommendationProfile(),
        categoryFilter: String = "All"
    ): List<PostEntity> {
        val filtered = if (categoryFilter.equals("All", ignoreCase = true)) {
            posts
        } else {
            posts.filter {
                it.category.equals(categoryFilter, ignoreCase = true) ||
                it.tags.contains(categoryFilter, ignoreCase = true)
            }
        }

        return filtered
            .map { scorePost(it, userContext) }
            // Filter out heavily penalized flagged posts from public consumer feed
            .filter { it.totalScore > -100.0 }
            .sortedByDescending { it.totalScore }
            .map { it.post }
    }

    /**
     * Builds the "For You" AI-Curated feed.
     */
    fun getForYouFeed(
        posts: List<PostEntity>,
        userContext: UserRecommendationProfile = UserRecommendationProfile()
    ): List<ScoredPost> {
        return posts
            .map { scorePost(it, userContext) }
            .filter { it.totalScore > -100.0 }
            .sortedByDescending { it.totalScore }
    }

    /**
     * Builds the "Trending" feed ranking based purely on engagement velocity, watch time, and retention.
     */
    fun getTrendingFeed(posts: List<PostEntity>): List<ScoredPost> {
        val neutralContext = UserRecommendationProfile()
        return posts
            .map { scorePost(it, neutralContext) }
            .filter { it.totalScore > -100.0 }
            .sortedByDescending { (it.engagementScore * 1.5) + (it.retentionScore * 1.2) + it.watchTimeScore }
    }

    /**
     * Computes "Up Next / Related Recommendations" given a current playing video.
     */
    fun getRelatedRecommendations(
        currentPost: PostEntity,
        allPosts: List<PostEntity>,
        userContext: UserRecommendationProfile = UserRecommendationProfile()
    ): List<PostEntity> {
        val candidates = allPosts.filter { it.id != currentPost.id && it.status != "REJECTED" && !it.isFlagged }
        val currentCategory = currentPost.category.lowercase(Locale.ROOT)
        val currentTags = currentPost.tags.lowercase(Locale.ROOT).split(" ", "#", ",").filter { it.isNotBlank() }

        return candidates.map { post ->
            val baseScored = scorePost(post, userContext)
            var similarityBoost = 0.0

            // Category match bonus
            if (post.category.equals(currentCategory, ignoreCase = true)) {
                similarityBoost += 30.0
            }
            // Same creator bonus
            if (post.channelName.equals(currentPost.channelName, ignoreCase = true)) {
                similarityBoost += 25.0
            }
            // Tag overlap bonus
            val postTags = post.tags.lowercase(Locale.ROOT).split(" ", "#", ",").filter { it.isNotBlank() }
            val sharedTags = postTags.intersect(currentTags.toSet()).size
            similarityBoost += (sharedTags * 12.0)

            Pair(post, baseScored.totalScore + similarityBoost)
        }
        .sortedByDescending { it.second }
        .map { it.first }
    }
}
