package com.example.data.repository

import com.example.data.local.CommentDao
import com.example.data.local.CreatorPageDao
import com.example.data.local.MonetizationDao
import com.example.data.local.PostDao
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
    private val userInteractionDao: UserInteractionDao
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
    }

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

        // Initialize clean user account if no account exists
        if (savedAccountDao.getAccountsCount() == 0) {
            val defaultAccount = SavedAccountEntity(
                uid = "user_me",
                name = "Satisfy Creator",
                handle = "@satisfy_creator",
                email = "creator@satisfy.app",
                bio = "Welcome to my Satisfy channel! ✨",
                avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
                bannerUrl = "",
                link = "satisfy.app/@satisfy_creator",
                subscriberCount = "0 subscribers",
                isPro = false,
                referralCode = "SATISFY100",
                lastActiveTimestamp = System.currentTimeMillis(),
                isActive = true
            )
            savedAccountDao.insertAccount(defaultAccount)
        }
    }
}


