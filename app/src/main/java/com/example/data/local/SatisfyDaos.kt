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

    @Query("SELECT * FROM posts WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR tags LIKE '%' || :query || '%' OR channelName LIKE '%' || :query || '%' ORDER BY id DESC")
    fun searchPosts(query: String): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<PostEntity>)

    @Update
    suspend fun updatePost(post: PostEntity)

    @Delete
    suspend fun deletePost(post: PostEntity)

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
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY id DESC")
    fun getCommentsForPost(postId: Long): Flow<List<CommentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllComments(comments: List<CommentEntity>)

    @Query("UPDATE comments SET isLiked = :isLiked, likeCount = :likeCount WHERE id = :commentId")
    suspend fun updateCommentLike(commentId: Long, isLiked: Boolean, likeCount: Long)

    @Delete
    suspend fun deleteComment(comment: CommentEntity)
}

@Dao
interface WatchHistoryDao {
    @Query("SELECT p.* FROM posts p INNER JOIN watch_history h ON p.id = h.postId ORDER BY h.watchedAt DESC")
    fun getWatchHistory(): Flow<List<PostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordWatch(history: WatchHistoryEntity)

    @Query("DELETE FROM watch_history")
    suspend fun clearHistory()
}

@Dao
interface UserAccountDao {
    @Query("SELECT * FROM users ORDER BY joinedDate DESC")
    fun getAllUsers(): Flow<List<com.example.data.model.UserAccountEntity>>

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

