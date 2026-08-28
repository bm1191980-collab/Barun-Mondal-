package com.example.data.local

import androidx.room.*
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.data.model.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Query("SELECT * FROM posts ORDER BY id DESC")
    fun getAllPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts")
    suspend fun getAllPostsList(): List<PostEntity>

    @Query("UPDATE posts SET mediaUrl = :mediaUrl WHERE id = :postId")
    suspend fun updateMediaUrl(postId: Long, mediaUrl: String)

    @Query("SELECT * FROM posts WHERE status = 'APPROVED' AND type = :type ORDER BY id DESC")
    fun getApprovedPostsByType(type: PostType): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE type = :type ORDER BY id DESC")
    fun getPostsByType(type: PostType): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE id = :id")
    suspend fun getPostById(id: Long): PostEntity?

    @Query("SELECT * FROM posts WHERE isSaved = 1 ORDER BY id DESC")
    fun getSavedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isUserCreated = 1 ORDER BY id DESC")
    fun getUserCreatedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isLiked = 1 ORDER BY id DESC")
    fun getLikedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE status = 'APPROVED' AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR channelName LIKE '%' || :query || '%') ORDER BY id DESC")
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE status = 'APPROVED' AND id != :currentPostId AND (category = :category OR tags LIKE '%' || :category || '%') ORDER BY viewCount DESC, id DESC LIMIT 15")
    fun getRelatedPosts(currentPostId: Long, category: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deletePostById(postId: Long)

    @Query("UPDATE posts SET viewCount = :newViewCount, views = :newViews WHERE id = :postId")
    suspend fun updateViews(postId: Long, newViewCount: Long, newViews: String)

    @Query("UPDATE posts SET isLiked = :isLiked, isDisliked = :isDisliked, likeCount = :likeCount, dislikeCount = :dislikeCount WHERE id = :postId")
    suspend fun updateLikeDislike(postId: Long, isLiked: Boolean, isDisliked: Boolean, likeCount: Long, dislikeCount: Long)

    @Query("UPDATE posts SET isLiked = :isLiked, likeCount = :likeCount, isDisliked = 0 WHERE id = :postId")
    suspend fun updateLike(postId: Long, isLiked: Boolean, likeCount: Long)

    @Query("UPDATE posts SET isDisliked = :isDisliked, isLiked = 0 WHERE id = :postId")
    suspend fun updateDislike(postId: Long, isDisliked: Boolean)

    @Query("UPDATE posts SET isSaved = :isSaved WHERE id = :postId")
    suspend fun updateSaved(postId: Long, isSaved: Boolean)

    @Query("UPDATE posts SET isSubscribed = :isSubscribed WHERE channelName = :channelName")
    suspend fun updateSubscribeByChannel(channelName: String, isSubscribed: Boolean)

    @Query("UPDATE posts SET commentCount = commentCount + 1 WHERE id = :postId")
    suspend fun incrementCommentCount(postId: Long)

    @Query("UPDATE posts SET commentCount = CASE WHEN commentCount > 0 THEN commentCount - 1 ELSE 0 END WHERE id = :postId")
    suspend fun decrementCommentCount(postId: Long)

    @Query("SELECT COUNT(*) FROM posts")
    suspend fun getCount(): Int

    @Query("UPDATE posts SET isFeatured = :isFeatured WHERE id = :postId")
    suspend fun updateFeatured(postId: Long, isFeatured: Boolean)

    @Query("UPDATE posts SET isFlagged = :isFlagged WHERE id = :postId")
    suspend fun updateFlagged(postId: Long, isFlagged: Boolean)

    @Query("SELECT * FROM posts WHERE isFeatured = 1 ORDER BY id DESC")
    fun getFeaturedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE isFlagged = 1 ORDER BY id DESC")
    fun getFlaggedPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE status = 'PENDING' ORDER BY id DESC")
    fun getPendingVerificationPosts(): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE status = :status ORDER BY id DESC")
    fun getPostsByStatus(status: String): Flow<List<PostEntity>>

    @Query("SELECT * FROM posts WHERE status = 'APPROVED' ORDER BY id DESC")
    fun getApprovedPosts(): Flow<List<PostEntity>>

    @Query("UPDATE posts SET status = :status, isVerified = :isVerified, rejectionReason = :rejectionReason, approvedAt = :approvedAt, rejectedAt = :rejectedAt WHERE id = :postId")
    suspend fun updatePostStatusWithTimestamps(postId: Long, status: String, isVerified: Boolean, rejectionReason: String?, approvedAt: Long?, rejectedAt: Long?)

    @Query("UPDATE posts SET status = :status, isVerified = :isVerified, rejectionReason = :rejectionReason WHERE id = :postId")
    suspend fun updatePostStatus(postId: Long, status: String, isVerified: Boolean, rejectionReason: String?)

    @Query("UPDATE posts SET status = 'APPROVED', isVerified = 1, rejectionReason = NULL, approvedAt = :approvedAt WHERE id = :postId")
    suspend fun approvePostWithTime(postId: Long, approvedAt: Long = System.currentTimeMillis())

    @Query("UPDATE posts SET status = 'APPROVED', isVerified = 1, rejectionReason = NULL WHERE id = :postId")
    suspend fun approvePost(postId: Long)

    @Query("UPDATE posts SET status = 'REJECTED', isVerified = 0, rejectionReason = :reason, rejectedAt = :rejectedAt WHERE id = :postId")
    suspend fun rejectPostWithTime(postId: Long, reason: String, rejectedAt: Long = System.currentTimeMillis())

    @Query("UPDATE posts SET status = 'REJECTED', isVerified = 0, rejectionReason = :reason WHERE id = :postId")
    suspend fun rejectPost(postId: Long, reason: String)

    @Query("UPDATE posts SET isPremium = :isPremium WHERE id = :postId")
    suspend fun updatePostPremiumStatus(postId: Long, isPremium: Boolean)
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY id ASC")
    fun getCommentsForPost(postId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllComments(comments: List<CommentEntity>)

    @Query("UPDATE comments SET isLiked = :isLiked, likeCount = :likeCount WHERE id = :commentId")
    suspend fun updateCommentLike(commentId: Long, isLiked: Boolean, likeCount: Long)

    @Query("UPDATE comments SET isReported = 1 WHERE id = :commentId")
    suspend fun reportComment(commentId: Long)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteCommentById(commentId: Long)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT p.* FROM posts p INNER JOIN watch_history h ON p.id = h.postId WHERE h.userId = :userId ORDER BY h.watchedAt DESC")
    fun getWatchHistoryForUser(userId: String): Flow<List<PostEntity>>

    @Query("SELECT p.* FROM posts p INNER JOIN watch_history h ON p.id = h.postId ORDER BY h.watchedAt DESC")
    fun getWatchHistory(): Flow<List<PostEntity>>

    @Query("SELECT * FROM watch_history WHERE postId = :postId AND userId = :userId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getWatchProgressForPostAndUser(postId: Long, userId: String): WatchHistoryEntity?

    @Query("SELECT * FROM watch_history WHERE postId = :postId ORDER BY watchedAt DESC LIMIT 1")
    suspend fun getWatchProgressForPost(postId: Long): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordWatch(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE userId = :userId")
    suspend fun clearHistoryForUser(userId: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM users ORDER BY joinedDate DESC")
    fun getAllUsers(): Flow<List<com.example.data.model.UserAccountEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsersList(): List<com.example.data.model.UserAccountEntity>

    @Query("SELECT * FROM users WHERE uid = :uid")
    suspend fun getUserByUid(uid: String): com.example.data.model.UserAccountEntity?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): com.example.data.model.UserAccountEntity?

    @Query("SELECT * FROM users WHERE isBanned = 1")
    fun getBannedUsers(): Flow<List<com.example.data.model.UserAccountEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: com.example.data.model.UserAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUsers(users: List<com.example.data.model.UserAccountEntity>)

    @Update
    suspend fun updateUser(user: com.example.data.model.UserAccountEntity)

    @Delete
    suspend fun deleteUser(user: com.example.data.model.UserAccountEntity)

    @Query("UPDATE users SET isBanned = :isBanned, banReason = :banReason WHERE uid = :uid")
    suspend fun updateBanStatus(uid: String, isBanned: Boolean, banReason: String)

    @Query("UPDATE users SET role = :role WHERE uid = :uid")
    suspend fun updateUserRole(uid: String, role: String)

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUsersCount(): Int

    @Query("SELECT * FROM users WHERE LOWER(role) = LOWER(:role)")
    suspend fun getUsersByRoleDirect(role: String): List<com.example.data.model.UserAccountEntity>
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    fun getAllReports(): Flow<List<com.example.data.model.ReportEntity>>

    @Query("SELECT * FROM reports WHERE status = 'PENDING' ORDER BY timestamp DESC")
    fun getPendingReports(): Flow<List<com.example.data.model.ReportEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: com.example.data.model.ReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllReports(reports: List<com.example.data.model.ReportEntity>)

    @Update
    suspend fun updateReport(report: com.example.data.model.ReportEntity)

    @Delete
    suspend fun deleteReport(report: com.example.data.model.ReportEntity)

    @Query("UPDATE reports SET status = :status, actionTaken = :actionTaken WHERE id = :id")
    suspend fun updateReportStatus(id: Long, status: String, actionTaken: String)

    @Query("SELECT COUNT(*) FROM reports WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COUNT(*) FROM reports WHERE status = 'RESOLVED'")
    suspend fun getResolvedCount(): Int
}

@Dao
interface PushNotificationDao {
    @Query("SELECT * FROM push_notifications ORDER BY sentAt DESC")
    fun getAllNotifications(): Flow<List<com.example.data.model.PushNotificationLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: com.example.data.model.PushNotificationLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllNotifications(notifications: List<com.example.data.model.PushNotificationLogEntity>)

    @Query("DELETE FROM push_notifications")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM push_notifications")
    suspend fun getCount(): Int
}

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getSettings(): Flow<com.example.data.model.AppSystemSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getSettingsDirect(): com.example.data.model.AppSystemSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: com.example.data.model.AppSystemSettingsEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 50")
    fun getRecentLogs(): Flow<List<com.example.data.model.AdminAuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: com.example.data.model.AdminAuditLogEntity): Long
}

@Dao
interface CreatorPageDao {
    @Query("SELECT * FROM creator_pages ORDER BY id DESC")
    fun getAllPages(): Flow<List<com.example.data.model.CreatorPageEntity>>

    @Query("SELECT * FROM creator_pages WHERE creatorUid = :userId ORDER BY id DESC")
    fun getPagesForUser(userId: String): Flow<List<com.example.data.model.CreatorPageEntity>>

    @Query("SELECT * FROM creator_pages WHERE id = :id")
    suspend fun getPageById(id: Long): com.example.data.model.CreatorPageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPage(page: com.example.data.model.CreatorPageEntity): Long

    @Update
    suspend fun updatePage(page: com.example.data.model.CreatorPageEntity)

    @Delete
    suspend fun deletePage(page: com.example.data.model.CreatorPageEntity)

    @Query("UPDATE creator_pages SET totalWatchTimeSeconds = totalWatchTimeSeconds + :deltaSeconds, totalViews = totalViews + 1 WHERE id = :pageId")
    suspend fun addWatchTime(pageId: Long, deltaSeconds: Long)

    @Query("SELECT * FROM posts WHERE pageId = :pageId ORDER BY id DESC")
    fun getPostsByPage(pageId: Long): Flow<List<PostEntity>>

    @Query("UPDATE posts SET watchTimeSeconds = watchTimeSeconds + :deltaSeconds WHERE id = :postId")
    suspend fun addPostWatchTime(postId: Long, deltaSeconds: Long)

    @Query("SELECT COUNT(*) FROM creator_pages")
    suspend fun getCount(): Int
}

@Dao
interface ProSubscriptionDao {
    @Query("SELECT * FROM pro_subscriptions ORDER BY startedAt DESC")
    fun getAllSubscriptions(): Flow<List<com.example.data.model.ProSubscriptionEntity>>

    @Query("SELECT * FROM pro_subscriptions WHERE userId = :userId ORDER BY startedAt DESC")
    fun getSubscriptionsForUser(userId: String): Flow<List<com.example.data.model.ProSubscriptionEntity>>

    @Query("SELECT * FROM pro_subscriptions WHERE userId = :userId AND status = 'ACTIVE' AND expiresAt > :currentTime ORDER BY expiresAt DESC LIMIT 1")
    suspend fun getActiveSubscription(userId: String, currentTime: Long = System.currentTimeMillis()): com.example.data.model.ProSubscriptionEntity?

    @Query("SELECT * FROM pro_subscriptions WHERE userId = :userId AND status = 'ACTIVE' AND expiresAt > :currentTime ORDER BY expiresAt DESC LIMIT 1")
    fun observeActiveSubscription(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<com.example.data.model.ProSubscriptionEntity?>

    @Query("SELECT * FROM pro_subscriptions WHERE orderId = :orderId OR paymentId = :paymentId LIMIT 1")
    suspend fun getSubscriptionByPayment(orderId: String, paymentId: String): com.example.data.model.ProSubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: com.example.data.model.ProSubscriptionEntity): Long

    @Update
    suspend fun updateSubscription(subscription: com.example.data.model.ProSubscriptionEntity)

    @Query("UPDATE pro_subscriptions SET status = :status WHERE id = :id")
    suspend fun updateSubscriptionStatus(id: Long, status: String)

    @Query("SELECT COUNT(*) FROM pro_subscriptions")
    suspend fun getTotalSubscriptionsCount(): Int

    @Query("SELECT COUNT(DISTINCT userId) FROM pro_subscriptions WHERE status = 'ACTIVE' AND expiresAt > :currentTime")
    suspend fun getActiveProUsersCount(currentTime: Long = System.currentTimeMillis()): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM pro_subscriptions WHERE paymentStatus = 'SUCCESS'")
    suspend fun getTotalProRevenue(): Double
}

@Dao
interface ReferralDao {
    @Query("SELECT * FROM referrals ORDER BY joinedAt DESC")
    fun getAllReferrals(): Flow<List<com.example.data.model.ReferralEntity>>

    @Query("SELECT * FROM referrals WHERE referrerUid = :referrerUid ORDER BY joinedAt DESC")
    fun getReferralsByReferrer(referrerUid: String): Flow<List<com.example.data.model.ReferralEntity>>

    @Query("SELECT * FROM referrals WHERE refereeUid = :refereeUid LIMIT 1")
    suspend fun getReferralForReferee(refereeUid: String): com.example.data.model.ReferralEntity?

    @Query("SELECT * FROM referrals WHERE refereeUid = :refereeUid AND (hasPurchasedPro = 1 OR commissionStatus = 'AVAILABLE' OR commissionStatus = 'PENDING') LIMIT 1")
    suspend fun getPurchasedReferralForReferee(refereeUid: String): com.example.data.model.ReferralEntity?

    @Query("SELECT * FROM referrals WHERE paymentId = :paymentId LIMIT 1")
    suspend fun getReferralByPaymentId(paymentId: String): com.example.data.model.ReferralEntity?

    @Query("SELECT * FROM referrals WHERE referrerCode = :code ORDER BY joinedAt DESC")
    fun getReferralsByCode(code: String): Flow<List<com.example.data.model.ReferralEntity>>

    @Query("SELECT * FROM referrals WHERE id = :id LIMIT 1")
    suspend fun getReferralById(id: Long): com.example.data.model.ReferralEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReferral(referral: com.example.data.model.ReferralEntity): Long

    @Update
    suspend fun updateReferral(referral: com.example.data.model.ReferralEntity)

    @Query("UPDATE referrals SET commissionStatus = :status, rewardStatus = :status, hasPurchasedPro = :hasPurchased, proSubscriptionId = :subId WHERE id = :id")
    suspend fun updateReferralReward(id: Long, status: String, hasPurchased: Boolean, subId: Long?)

    @Query("UPDATE referrals SET isSuspicious = :isSuspicious, auditNote = :auditNote WHERE id = :id")
    suspend fun markSuspicious(id: Long, isSuspicious: Boolean, auditNote: String)

    @Query("SELECT COUNT(*) FROM referrals")
    suspend fun getTotalReferralsCount(): Int

    @Query("SELECT COUNT(*) FROM referrals WHERE hasPurchasedPro = 1")
    suspend fun getSuccessfulReferralsCount(): Int

    @Query("SELECT COALESCE(SUM(grossPayment), 0.0) FROM referrals WHERE hasPurchasedPro = 1 AND commissionStatus != 'REVERSED'")
    suspend fun getTotalReferralSales(): Double

    @Query("SELECT COALESCE(SUM(baseReferralCommission), 0.0) FROM referrals WHERE hasPurchasedPro = 1 AND commissionStatus != 'REVERSED'")
    suspend fun getTotalBaseCommission(): Double

    @Query("SELECT COALESCE(SUM(gatewayFee), 0.0) FROM referrals WHERE hasPurchasedPro = 1 AND commissionStatus != 'REVERSED'")
    suspend fun getTotalFeesDeducted(): Double

    @Query("SELECT COALESCE(SUM(finalReferralPayout), 0.0) FROM referrals WHERE commissionStatus = 'AVAILABLE' OR commissionStatus = 'WITHDRAWN'")
    suspend fun getTotalFinalReferralPayout(): Double

    @Query("SELECT COALESCE(SUM(ownerCommission), 0.0) FROM referrals WHERE hasPurchasedPro = 1 AND commissionStatus != 'REVERSED'")
    suspend fun getTotalOwnerCommissionFromReferrals(): Double

    @Query("SELECT COALESCE(SUM(finalReferralPayout), 0.0) FROM referrals WHERE commissionStatus = 'PENDING'")
    suspend fun getPendingCommission(): Double

    @Query("SELECT COALESCE(SUM(finalReferralPayout), 0.0) FROM referrals WHERE commissionStatus = 'AVAILABLE'")
    suspend fun getAvailableCommission(): Double

    @Query("SELECT COALESCE(SUM(finalReferralPayout), 0.0) FROM referrals WHERE commissionStatus = 'WITHDRAWN'")
    suspend fun getWithdrawnCommission(): Double

    @Query("SELECT COALESCE(SUM(finalReferralPayout), 0.0) FROM referrals WHERE commissionStatus = 'AVAILABLE' OR commissionStatus = 'CREDITED'")
    suspend fun getTotalRewardsCredited(): Double
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE userId = :userId")
    suspend fun getWallet(userId: String): com.example.data.model.WalletEntity?

    @Query("SELECT * FROM wallets WHERE userId = :userId")
    fun observeWallet(userId: String): Flow<com.example.data.model.WalletEntity?>

    @Query("SELECT * FROM wallets ORDER BY referralBalance DESC")
    fun getAllWallets(): Flow<List<com.example.data.model.WalletEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateWallet(wallet: com.example.data.model.WalletEntity)

    @Query("UPDATE wallets SET isFrozen = :isFrozen, freezeReason = :freezeReason WHERE userId = :userId")
    suspend fun updateWalletFreezeStatus(userId: String, isFrozen: Boolean, freezeReason: String)
}

@Dao
interface WalletTransactionDao {
    @Query("SELECT * FROM wallet_transactions WHERE userId = :userId ORDER BY timestamp DESC")
    fun getTransactionsForUser(userId: String): Flow<List<com.example.data.model.WalletTransactionEntity>>

    @Query("SELECT * FROM wallet_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<com.example.data.model.WalletTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: com.example.data.model.WalletTransactionEntity): Long
}

@Dao
interface WithdrawalRequestDao {
    @Query("SELECT * FROM withdrawal_requests ORDER BY requestedAt DESC")
    fun getAllWithdrawals(): Flow<List<com.example.data.model.WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE userId = :userId ORDER BY requestedAt DESC")
    fun getWithdrawalsForUser(userId: String): Flow<List<com.example.data.model.WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPendingWithdrawals(): Flow<List<com.example.data.model.WithdrawalRequestEntity>>

    @Query("SELECT * FROM withdrawal_requests WHERE id = :id")
    suspend fun getWithdrawalById(id: Long): com.example.data.model.WithdrawalRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWithdrawal(request: com.example.data.model.WithdrawalRequestEntity): Long

    @Update
    suspend fun updateWithdrawal(request: com.example.data.model.WithdrawalRequestEntity)

    @Query("UPDATE withdrawal_requests SET status = :status, processedAt = :processedAt, adminNotes = :notes, paymentReference = :ref WHERE id = :id")
    suspend fun updateWithdrawalStatus(id: Long, status: String, processedAt: Long?, notes: String, ref: String)

    @Query("UPDATE withdrawal_requests SET status = 'REJECTED', processedAt = :processedAt, rejectionReason = :reason, adminNotes = :notes WHERE id = :id")
    suspend fun rejectWithdrawal(id: Long, reason: String, notes: String, processedAt: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM withdrawal_requests WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM withdrawal_requests WHERE status = 'PENDING'")
    suspend fun getPendingAmount(): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM withdrawal_requests WHERE status = 'PAID'")
    suspend fun getPaidAmount(): Double
}

@Dao
interface OwnerChatDao {
    @Query("SELECT * FROM owner_chats ORDER BY lastMessageTimestamp DESC")
    fun getAllChats(): Flow<List<com.example.data.model.OwnerChatEntity>>

    @Query("SELECT * FROM owner_chats WHERE userId = :userId LIMIT 1")
    suspend fun getChatByUserId(userId: String): com.example.data.model.OwnerChatEntity?

    @Query("SELECT * FROM owner_chats WHERE userId = :userId LIMIT 1")
    fun observeChatByUserId(userId: String): Flow<com.example.data.model.OwnerChatEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateChat(chat: com.example.data.model.OwnerChatEntity)

    @Query("UPDATE owner_chats SET unreadCountForAdmin = 0 WHERE userId = :userId")
    suspend fun markAdminRead(userId: String)

    @Query("UPDATE owner_chats SET unreadCountForUser = 0 WHERE userId = :userId")
    suspend fun markUserRead(userId: String)

    @Query("UPDATE owner_chats SET isBlocked = :isBlocked WHERE userId = :userId")
    suspend fun updateBlockStatus(userId: String, isBlocked: Boolean)
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE chatUserId = :chatUserId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatUserId: String): Flow<List<com.example.data.model.ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: com.example.data.model.ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET isRead = 1 WHERE chatUserId = :chatUserId AND senderRole != :readerRole")
    suspend fun markMessagesAsRead(chatUserId: String, readerRole: String)
}

@Dao
interface MonetizationDao {
    @Query("SELECT * FROM monetization_applications ORDER BY appliedAt DESC")
    fun getAllApplications(): Flow<List<com.example.data.model.MonetizationApplicationEntity>>

    @Query("SELECT * FROM monetization_applications WHERE userId = :userId ORDER BY appliedAt DESC LIMIT 1")
    fun observeUserApplication(userId: String): Flow<com.example.data.model.MonetizationApplicationEntity?>

    @Query("SELECT * FROM monetization_applications WHERE userId = :userId ORDER BY appliedAt DESC LIMIT 1")
    suspend fun getUserApplication(userId: String): com.example.data.model.MonetizationApplicationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApplication(application: com.example.data.model.MonetizationApplicationEntity): Long

    @Update
    suspend fun updateApplication(application: com.example.data.model.MonetizationApplicationEntity)

    @Query("UPDATE monetization_applications SET status = :status, reviewedAt = :reviewedAt, rejectionReason = :rejectionReason, adminNotes = :adminNotes WHERE id = :id")
    suspend fun updateApplicationStatus(id: Long, status: String, reviewedAt: Long?, rejectionReason: String?, adminNotes: String)

    @Query("SELECT COUNT(*) FROM monetization_applications WHERE status = 'PENDING'")
    suspend fun getPendingApplicationsCount(): Int
}

@Dao
interface SavedAccountDao {
    @Query("SELECT * FROM saved_accounts ORDER BY lastActiveTimestamp DESC")
    fun getAllAccounts(): Flow<List<com.example.data.model.SavedAccountEntity>>

    @Query("SELECT * FROM saved_accounts WHERE uid = :uid LIMIT 1")
    suspend fun getAccountByUid(uid: String): com.example.data.model.SavedAccountEntity?

    @Query("SELECT * FROM saved_accounts WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveAccount(): com.example.data.model.SavedAccountEntity?

    @Query("SELECT * FROM saved_accounts WHERE isActive = 1 LIMIT 1")
    fun observeActiveAccount(): Flow<com.example.data.model.SavedAccountEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: com.example.data.model.SavedAccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<com.example.data.model.SavedAccountEntity>)

    @Update
    suspend fun updateAccount(account: com.example.data.model.SavedAccountEntity)

    @Delete
    suspend fun deleteAccount(account: com.example.data.model.SavedAccountEntity)

    @Query("DELETE FROM saved_accounts WHERE uid = :uid")
    suspend fun deleteAccountByUid(uid: String)

    @Query("UPDATE saved_accounts SET isActive = CASE WHEN uid = :uid THEN 1 ELSE 0 END, lastActiveTimestamp = CASE WHEN uid = :uid THEN :timestamp ELSE lastActiveTimestamp END")
    suspend fun setActiveAccount(uid: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE saved_accounts SET isActive = 0")
    suspend fun deactivateAllAccounts()

    @Query("SELECT COUNT(*) FROM saved_accounts")
    suspend fun getAccountsCount(): Int
}

@Dao
interface UserInteractionDao {
    // Likes
    @Query("SELECT postId FROM user_likes WHERE userId = :userId")
    fun getLikedPostIds(userId: String): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_likes WHERE userId = :userId AND postId = :postId)")
    suspend fun isPostLikedByUser(userId: String, postId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: com.example.data.model.UserLikeEntity)

    @Query("DELETE FROM user_likes WHERE userId = :userId AND postId = :postId")
    suspend fun deleteLike(userId: String, postId: Long)

    @Query("SELECT p.* FROM posts p INNER JOIN user_likes l ON p.id = l.postId WHERE l.userId = :userId ORDER BY l.timestamp DESC")
    fun getLikedPostsForUser(userId: String): Flow<List<PostEntity>>

    // Saves
    @Query("SELECT postId FROM user_saves WHERE userId = :userId")
    fun getSavedPostIds(userId: String): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_saves WHERE userId = :userId AND postId = :postId)")
    suspend fun isPostSavedByUser(userId: String, postId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: com.example.data.model.UserSavedEntity)

    @Query("DELETE FROM user_saves WHERE userId = :userId AND postId = :postId")
    suspend fun deleteSave(userId: String, postId: Long)

    @Query("SELECT p.* FROM posts p INNER JOIN user_saves s ON p.id = s.postId WHERE s.userId = :userId ORDER BY s.timestamp DESC")
    fun getSavedPostsForUser(userId: String): Flow<List<PostEntity>>

    // Subscriptions
    @Query("SELECT channelName FROM user_subscriptions WHERE userId = :userId")
    fun getSubscribedChannels(userId: String): Flow<List<String>>

    @Query("SELECT EXISTS(SELECT 1 FROM user_subscriptions WHERE userId = :userId AND channelName = :channelName)")
    suspend fun isSubscribedToChannel(userId: String, channelName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(sub: com.example.data.model.UserSubscriptionEntity)

    @Query("DELETE FROM user_subscriptions WHERE userId = :userId AND channelName = :channelName")
    suspend fun deleteSubscription(userId: String, channelName: String)

    @Query("SELECT COUNT(DISTINCT userId) FROM user_subscriptions WHERE channelName = :channelName")
    fun getSubscriberCountForChannel(channelName: String): Flow<Int>

    @Query("SELECT COUNT(DISTINCT userId) FROM user_subscriptions WHERE channelName = :channelName")
    suspend fun getSubscriberCountForChannelDirect(channelName: String): Int

    @Query("SELECT COUNT(DISTINCT userId) FROM user_subscriptions")
    suspend fun getTotalSubscriptionsCount(): Int

    @Query("DELETE FROM user_subscriptions WHERE userId = :userId")
    suspend fun deleteSubscriptionsForUser(userId: String)

    @Query("DELETE FROM user_likes WHERE userId = :userId")
    suspend fun deleteLikesForUser(userId: String)

    @Query("DELETE FROM user_saves WHERE userId = :userId")
    suspend fun deleteSavesForUser(userId: String)

    // User created posts
    @Query("SELECT * FROM posts WHERE creatorUid = :userId OR (isUserCreated = 1 AND creatorUid = :userId) ORDER BY id DESC")
    fun getPostsByCreator(userId: String): Flow<List<PostEntity>>
}



