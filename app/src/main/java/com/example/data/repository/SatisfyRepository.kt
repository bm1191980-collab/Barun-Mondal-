package com.example.data.repository

import com.example.data.local.CommentDao
import com.example.data.local.CreatorPageDao
import com.example.data.local.MonetizationDao
import com.example.data.local.PostDao
import com.example.data.local.RecentSearchDao
import com.example.data.local.SavedAccountDao
import com.example.data.local.UserInteractionDao
import com.example.data.local.WatchHistoryDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class SatisfyRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val historyDao: WatchHistoryDao,
    private val creatorPageDao: CreatorPageDao,
    private val monetizationDao: MonetizationDao,
    private val savedAccountDao: SavedAccountDao,
    private val userInteractionDao: UserInteractionDao,
    private val recentSearchDao: RecentSearchDao? = null
) {
    val allPosts: Flow<List<PostEntity>> = postDao.getAllPosts()
    val pendingVerificationPosts: Flow<List<PostEntity>> = postDao.getPendingVerificationPosts()
    val approvedPosts: Flow<List<PostEntity>> = postDao.getApprovedPosts()
    val videoPosts: Flow<List<PostEntity>> = postDao.getApprovedPostsByType(PostType.VIDEO)
    val shortPosts: Flow<List<PostEntity>> = postDao.getApprovedPostsByType(PostType.SHORT)
    val photoPosts: Flow<List<PostEntity>> = postDao.getApprovedPostsByType(PostType.PHOTO)
    val savedPosts: Flow<List<PostEntity>> = postDao.getSavedPosts()
    val userCreatedPosts: Flow<List<PostEntity>> = postDao.getUserCreatedPosts()
    val likedPosts: Flow<List<PostEntity>> = postDao.getLikedPosts()
    val watchHistory: Flow<List<PostEntity>> = historyDao.getWatchHistory()
    val creatorPages: Flow<List<CreatorPageEntity>> = creatorPageDao.getAllPages()
    val monetizationApplications: Flow<List<MonetizationApplicationEntity>> = monetizationDao.getAllApplications()
    val recentSearches: Flow<List<RecentSearchEntity>> = recentSearchDao?.getRecentSearches() ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isNotBlank()) {
            recentSearchDao?.insertRecentSearch(RecentSearchEntity(query = trimmed, timestamp = System.currentTimeMillis()))
        }
    }

    suspend fun removeRecentSearch(query: String) {
        recentSearchDao?.deleteRecentSearch(query.trim())
    }

    suspend fun clearRecentSearches() {
        recentSearchDao?.clearAllRecentSearches()
    }

    // Multi-Account & Saved Profiles
    val allSavedAccounts: Flow<List<SavedAccountEntity>> = savedAccountDao.getAllAccounts()
    val activeSavedAccount: Flow<SavedAccountEntity?> = savedAccountDao.observeActiveAccount()

    suspend fun getActiveAccount(): SavedAccountEntity? = savedAccountDao.getActiveAccount()

    suspend fun getAccountByUid(uid: String): SavedAccountEntity? = savedAccountDao.getAccountByUid(uid)

    suspend fun switchAccount(uid: String) {
        savedAccountDao.setActiveAccount(uid, System.currentTimeMillis())
    }

    suspend fun saveAccount(account: SavedAccountEntity) {
        savedAccountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: SavedAccountEntity) {
        savedAccountDao.updateAccount(account)
    }

    suspend fun deleteAccount(uid: String) {
        savedAccountDao.deleteAccountByUid(uid)
    }

    suspend fun deactivateAllAccounts() {
        savedAccountDao.deactivateAllAccounts()
    }

    // User-isolated flows
    fun getUserWatchHistory(userId: String): Flow<List<PostEntity>> =
        historyDao.getWatchHistoryForUser(userId)

    fun getUserLikedPosts(userId: String): Flow<List<PostEntity>> =
        userInteractionDao.getLikedPostsForUser(userId)

    fun getUserSavedPosts(userId: String): Flow<List<PostEntity>> =
        userInteractionDao.getSavedPostsForUser(userId)

    fun getUserCreatedPosts(userId: String): Flow<List<PostEntity>> =
        userInteractionDao.getPostsByCreator(userId)

    fun getUserCreatorPages(userId: String): Flow<List<CreatorPageEntity>> =
        creatorPageDao.getPagesForUser(userId)

    fun getLikedPostIds(userId: String): Flow<List<Long>> =
        userInteractionDao.getLikedPostIds(userId)

    fun getSavedPostIds(userId: String): Flow<List<Long>> =
        userInteractionDao.getSavedPostIds(userId)

    fun getSubscribedChannels(userId: String): Flow<List<String>> =
        userInteractionDao.getSubscribedChannels(userId)

    fun getSubscriberCountFlow(channelName: String): Flow<Int> =
        userInteractionDao.getSubscriberCountForChannel(channelName)

    suspend fun getSubscriberCountDirect(channelName: String): Int =
        userInteractionDao.getSubscriberCountForChannelDirect(channelName)

    suspend fun deleteUserInteractions(userId: String) {
        userInteractionDao.deleteSubscriptionsForUser(userId)
        userInteractionDao.deleteLikesForUser(userId)
        userInteractionDao.deleteSavesForUser(userId)
    }

    suspend fun isPostLikedByUser(userId: String, postId: Long): Boolean =
        userInteractionDao.isPostLikedByUser(userId, postId)

    suspend fun isPostSavedByUser(userId: String, postId: Long): Boolean =
        userInteractionDao.isPostSavedByUser(userId, postId)

    suspend fun isSubscribedToChannel(userId: String, channelName: String): Boolean =
        userInteractionDao.isSubscribedToChannel(userId, channelName)

    suspend fun toggleUserLike(userId: String, post: PostEntity) {
        val isLiked = userInteractionDao.isPostLikedByUser(userId, post.id)
        if (isLiked) {
            userInteractionDao.deleteLike(userId, post.id)
            val newCount = maxOf(0L, post.likeCount - 1L)
            postDao.updateLike(post.id, false, newCount)
        } else {
            userInteractionDao.insertLike(UserLikeEntity(userId, post.id))
            val newCount = post.likeCount + 1L
            postDao.updateLike(post.id, true, newCount)
        }
    }

    suspend fun toggleUserSave(userId: String, post: PostEntity) {
        val isSaved = userInteractionDao.isPostSavedByUser(userId, post.id)
        if (isSaved) {
            userInteractionDao.deleteSave(userId, post.id)
            postDao.updateSaved(post.id, false)
        } else {
            userInteractionDao.insertSave(UserSavedEntity(userId, post.id))
            postDao.updateSaved(post.id, true)
        }
    }

    suspend fun toggleUserSubscribe(userId: String, channelName: String) {
        val isSubscribed = userInteractionDao.isSubscribedToChannel(userId, channelName)
        if (isSubscribed) {
            userInteractionDao.deleteSubscription(userId, channelName)
            postDao.updateSubscribeByChannel(channelName, false)
        } else {
            userInteractionDao.insertSubscription(UserSubscriptionEntity(userId, channelName))
            postDao.updateSubscribeByChannel(channelName, true)
        }
    }

    suspend fun recordUserWatchHistory(userId: String, postId: Long, lastPositionSeconds: Long = 0L, durationSeconds: Long = 0L) {
        val existing = historyDao.getWatchProgressForPostAndUser(postId, userId)
        if (existing != null) {
            historyDao.updateWatchProgress(
                id = existing.id,
                lastPos = lastPositionSeconds,
                duration = if (durationSeconds > 0) durationSeconds else existing.durationSeconds,
                watchedAt = System.currentTimeMillis()
            )
        } else {
            historyDao.recordWatch(
                WatchHistoryEntity(
                    userId = userId,
                    postId = postId,
                    lastPositionSeconds = lastPositionSeconds,
                    durationSeconds = durationSeconds,
                    watchedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun saveWatchProgress(userId: String, postId: Long, lastPositionSeconds: Long, durationSeconds: Long) {
        if (lastPositionSeconds <= 0L && durationSeconds <= 0L) return
        recordUserWatchHistory(userId, postId, lastPositionSeconds, durationSeconds)
    }

    suspend fun getWatchProgressForPostAndUser(postId: Long, userId: String): WatchHistoryEntity? {
        return historyDao.getWatchProgressForPostAndUser(postId, userId)
    }

    fun getAllWatchHistoryForUser(userId: String): Flow<List<WatchHistoryEntity>> {
        return historyDao.getAllWatchHistoryForUser(userId)
    }

    suspend fun deleteWatchProgressForPostAndUser(postId: Long, userId: String) {
        historyDao.deleteWatchProgressForPostAndUser(postId, userId)
    }

    suspend fun clearUserHistory(userId: String) {
        historyDao.clearHistoryForUser(userId)
    }

    fun observeUserMonetizationApplication(userId: String): Flow<MonetizationApplicationEntity?> =
        monetizationDao.observeUserApplication(userId)

    suspend fun getUserMonetizationApplication(userId: String): MonetizationApplicationEntity? =
        monetizationDao.getUserApplication(userId)

    suspend fun submitMonetizationApplication(application: MonetizationApplicationEntity): Long =
        monetizationDao.insertApplication(application)

    suspend fun updateMonetizationStatus(id: Long, status: String, reviewedAt: Long?, rejectionReason: String?, adminNotes: String = "") {
        monetizationDao.updateApplicationStatus(id, status, reviewedAt, rejectionReason, adminNotes)
    }

    fun searchPosts(query: String): Flow<List<PostEntity>> = postDao.searchPosts(query)

    fun getRelatedPosts(currentPostId: Long, category: String): Flow<List<PostEntity>> =
        postDao.getRelatedPosts(currentPostId, category)

    fun getPostsByPage(pageId: Long): Flow<List<PostEntity>> = creatorPageDao.getPostsByPage(pageId)

    suspend fun createPage(page: CreatorPageEntity): Long = creatorPageDao.insertPage(page)

    suspend fun updatePage(page: CreatorPageEntity) = creatorPageDao.updatePage(page)

    suspend fun deletePage(page: CreatorPageEntity) = creatorPageDao.deletePage(page)

    suspend fun getPageById(id: Long): CreatorPageEntity? = creatorPageDao.getPageById(id)

    suspend fun recordVideoWatchTime(postId: Long, deltaSeconds: Long, pageId: Long? = null) {
        if (deltaSeconds <= 0) return
        creatorPageDao.addPostWatchTime(postId, deltaSeconds)
        if (pageId != null && pageId > 0) {
            creatorPageDao.addWatchTime(pageId, deltaSeconds)
        }
        val post = postDao.getPostById(postId)
        if (post != null) {
            val dur = if (post.durationSeconds > 0) post.durationSeconds else 180L
            val currentWatched = post.watchTimeSeconds + deltaSeconds
            // Calculate empirical retention rate (capped between 0.2 and 1.0)
            val newRetention = ((currentWatched.toFloat() / dur.toFloat()).coerceIn(0.2f, 1.0f) * 0.4f) + (post.avgRetentionRate * 0.6f)
            postDao.updateRetentionAndWatchTime(postId, newRetention.coerceIn(0.1f, 1.0f), deltaSeconds)
        }
    }

    suspend fun incrementShares(postId: Long) {
        postDao.incrementShares(postId)
    }

    suspend fun resolveAiFlag(postId: Long, reason: String = "AI Flag Dismissed & Reach Restored by Admin") {
        postDao.resolveAiFlag(postId, reason)
    }

    suspend fun flagPostByAi(postId: Long, reason: String, riskScore: Float) {
        postDao.flagPostByAi(postId, reason, riskScore)
    }

    suspend fun updateSpamLimited(postId: Long, isLimited: Boolean) {
        postDao.updateSpamLimited(postId, isLimited)
    }

    val flaggedPosts: Flow<List<PostEntity>> = postDao.getFlaggedPosts()
    val spamLimitedPosts: Flow<List<PostEntity>> = postDao.getSpamLimitedPosts()

    suspend fun incrementViewCount(postId: Long) {
        val post = postDao.getPostById(postId) ?: return
        val newCount = post.viewCount + 1L
        val formatted = formatViews(newCount)
        postDao.updateViews(postId, newCount, formatted)
    }

    fun getCommentsForPost(postId: Long): Flow<List<CommentEntity>> =
        commentDao.getCommentsForPost(postId)

    suspend fun getPostById(id: Long): PostEntity? = postDao.getPostById(id)

    suspend fun createPost(post: PostEntity): Long = postDao.insertPost(post)

    suspend fun deletePost(post: PostEntity) = postDao.deletePost(post)

    suspend fun deletePostById(postId: Long) = postDao.deletePostById(postId)

    suspend fun toggleLike(post: PostEntity) {
        val wasLiked = post.isLiked
        val wasDisliked = post.isDisliked
        val newIsLiked = !wasLiked
        val newIsDisliked = if (newIsLiked) false else wasDisliked

        val newLikeCount = if (newIsLiked) {
            post.likeCount + 1L
        } else {
            maxOf(0L, post.likeCount - 1L)
        }

        val newDislikeCount = if (wasDisliked && newIsLiked) {
            maxOf(0L, post.dislikeCount - 1L)
        } else {
            post.dislikeCount
        }

        postDao.updateLikeDislike(post.id, newIsLiked, newIsDisliked, newLikeCount, newDislikeCount)
    }

    suspend fun toggleDislike(post: PostEntity) {
        val wasLiked = post.isLiked
        val wasDisliked = post.isDisliked
        val newIsDisliked = !wasDisliked
        val newIsLiked = if (newIsDisliked) false else wasLiked

        val newDislikeCount = if (newIsDisliked) {
            post.dislikeCount + 1L
        } else {
            maxOf(0L, post.dislikeCount - 1L)
        }

        val newLikeCount = if (wasLiked && newIsDisliked) {
            maxOf(0L, post.likeCount - 1L)
        } else {
            post.likeCount
        }

        postDao.updateLikeDislike(post.id, newIsLiked, newIsDisliked, newLikeCount, newDislikeCount)
    }

    suspend fun toggleSave(post: PostEntity) {
        val newIsSaved = !post.isSaved
        postDao.updateSaved(post.id, newIsSaved)
    }

    suspend fun toggleSubscribe(channelName: String, currentSubscribed: Boolean) {
        postDao.updateSubscribeByChannel(channelName, !currentSubscribed)
    }

    suspend fun addComment(postId: Long, text: String, authorName: String = "You", parentCommentId: Long? = null): Long {
        val comment = CommentEntity(
            postId = postId,
            authorUid = "user_creator",
            authorName = authorName,
            authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
            text = text,
            parentCommentId = parentCommentId,
            timestamp = System.currentTimeMillis(),
            timeAgo = "Just now",
            likeCount = 0,
            isLiked = false
        )
        val id = commentDao.insertComment(comment)
        postDao.incrementCommentCount(postId)
        return id
    }

    suspend fun deleteComment(commentId: Long, postId: Long) {
        commentDao.deleteCommentById(commentId)
        postDao.decrementCommentCount(postId)
    }

    suspend fun reportComment(commentId: Long) {
        commentDao.reportComment(commentId)
    }

    suspend fun toggleCommentLike(comment: CommentEntity) {
        val newIsLiked = !comment.isLiked
        val newLikeCount = if (newIsLiked) comment.likeCount + 1 else maxOf(0L, comment.likeCount - 1)
        commentDao.updateCommentLike(comment.id, newIsLiked, newLikeCount)
    }

    suspend fun recordWatchHistory(postId: Long, lastPositionSeconds: Long = 0L, durationSeconds: Long = 0L) {
        historyDao.recordWatch(
            WatchHistoryEntity(
                postId = postId,
                lastPositionSeconds = lastPositionSeconds,
                durationSeconds = durationSeconds,
                watchedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getWatchProgress(postId: Long): WatchHistoryEntity? {
        return historyDao.getWatchProgressForPost(postId)
    }

    suspend fun clearHistory() {
        historyDao.clearHistory()
    }

    suspend fun checkAndSeedInitialData() {
        // Clean out legacy demo seed posts so only real user uploaded and approved posts exist
        try {
            val allExisting = postDao.getAllPostsList()
            allExisting.filter { it.channelId.startsWith("ch_") || (!it.isUserCreated && it.creatorUid.isBlank()) }.forEach {
                postDao.deletePost(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Initialize realistic logged-in accounts if less than 2 accounts exist
        if (savedAccountDao.getAccountsCount() < 2) {
            val defaultAccounts = listOf(
                SavedAccountEntity(
                    uid = "user_creator",
                    name = "Satisfy Creator",
                    handle = "@satisfy_creator",
                    email = "creator@satisfy.app",
                    bio = "Welcome to my Satisfy channel! Sharing satisfying video creations, 4K nature cinematography, and community photography. ✨",
                    avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                    bannerUrl = "",
                    link = "satisfy.app/@satisfy_creator",
                    subscriberCount = "1.2K subscribers",
                    isPro = true,
                    activePlanId = "plan_pro_12",
                    activePlanName = "Satisfy PRO Annual",
                    activePlanTier = "ANNUAL",
                    subscriptionStatus = "ACTIVE",
                    proStartedAt = System.currentTimeMillis() - 86400000L * 30,
                    proExpiresAt = System.currentTimeMillis() + 86400000L * 335,
                    referralCode = "SATISFY100",
                    lastActiveTimestamp = System.currentTimeMillis(),
                    isActive = true
                ),
                SavedAccountEntity(
                    uid = "user_aura",
                    name = "Aura Aesthetics",
                    handle = "@aura_aesthetics",
                    email = "aura@satisfy.app",
                    bio = "Mindful soundscapes, kinetic sand, and soothing visual harmony 🌿",
                    avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                    bannerUrl = "https://images.unsplash.com/photo-1518241353330-0f7941c2d9b5?w=800",
                    link = "satisfy.app/@aura_aesthetics",
                    subscriberCount = "8.4K subscribers",
                    isPro = true,
                    activePlanId = "plan_pro_5",
                    activePlanName = "Satisfy PRO Monthly",
                    activePlanTier = "MONTHLY",
                    subscriptionStatus = "ACTIVE",
                    proStartedAt = System.currentTimeMillis() - 86400000L * 10,
                    proExpiresAt = System.currentTimeMillis() + 86400000L * 20,
                    referralCode = "AURA777",
                    lastActiveTimestamp = System.currentTimeMillis() - 3600000L * 2,
                    isActive = false
                ),
                SavedAccountEntity(
                    uid = "user_apex",
                    name = "Apex Gaming Hub",
                    handle = "@apexgaming",
                    email = "apex@satisfy.app",
                    bio = "Satisfying 120FPS physics simulations, clean speedruns & ultra-smooth gaming edits 🎮",
                    avatarUrl = "https://images.unsplash.com/photo-1566492031773-4f4e44671857?w=200",
                    bannerUrl = "https://images.unsplash.com/photo-1550684848-fac1c5b4e853?w=800",
                    link = "satisfy.app/@apexgaming",
                    subscriberCount = "15.2K subscribers",
                    isPro = false,
                    referralCode = "APEX99",
                    lastActiveTimestamp = System.currentTimeMillis() - 3600000L * 5,
                    isActive = false
                ),
                SavedAccountEntity(
                    uid = "user_maya",
                    name = "Dr. Maya Lin",
                    handle = "@mayalin",
                    email = "maya.lin@satisfy.app",
                    bio = "Microscopic wonders, ferrofluid art & satisfying fluid chemistry experiments 🔬",
                    avatarUrl = "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?w=200",
                    bannerUrl = "https://images.unsplash.com/photo-1507668077129-56e32842fceb?w=800",
                    link = "satisfy.app/@mayalin",
                    subscriberCount = "3.1K subscribers",
                    isPro = false,
                    referralCode = "MAYA50",
                    lastActiveTimestamp = System.currentTimeMillis() - 86400000L,
                    isActive = false
                )
            )
            savedAccountDao.insertAll(defaultAccounts)
        }
    }
}


