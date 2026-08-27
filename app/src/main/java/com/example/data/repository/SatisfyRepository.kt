package com.example.data.repository

import com.example.data.local.CommentDao
import com.example.data.local.CreatorPageDao
import com.example.data.local.MonetizationDao
import com.example.data.local.PostDao
import com.example.data.local.WatchHistoryDao
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

class SatisfyRepository(
    private val postDao: PostDao,
    private val commentDao: CommentDao,
    private val historyDao: WatchHistoryDao,
    private val creatorPageDao: CreatorPageDao,
    private val monetizationDao: MonetizationDao
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
        if (postDao.getCount() > 0) {
            // Sanitize any existing posts that might have legacy unplayable string placeholders
            try {
                val existingPosts = postDao.getAllPostsList()
                existingPosts.forEachIndexed { index, post ->
                    val cleanUrl = com.example.data.service.SatisfyVideoEngine.getValidPlayableUrl(
                        post.mediaUrl,
                        post.type,
                        index
                    )
                    if (post.mediaUrl.isBlank() || post.mediaUrl != cleanUrl && (!post.mediaUrl.startsWith("http") && !post.mediaUrl.startsWith("file") && !post.mediaUrl.startsWith("content"))) {
                        postDao.updateMediaUrl(post.id, cleanUrl)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return
        }

        val seedPosts = listOf(
            // 1. Video: Satisfying Sand Simulation (Real public streaming MP4)
            PostEntity(
                type = PostType.VIDEO,
                title = "Most Satisfying Kinetic Sand & Fluid Art In 4K HDR 🌟",
                description = "Relax your mind with the ultimate satisfying kinetic sand slicing, rainbow drops, and ASMR crystal crush compilation. Headphones recommended for best experience!",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&q=80",
                channelId = "ch_satisfy_official",
                channelName = "Satisfy Studios",
                channelAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                subscriberCount = "2.4M subscribers",
                views = "840K views",
                viewCount = 840000L,
                likeCount = 42000L,
                dislikeCount = 120L,
                commentCount = 1840L,
                timeAgo = "2 hours ago",
                category = "Satisfying",
                duration = "09:56",
                durationSeconds = 596L,
                isVerified = true,
                tags = "#Satisfying #ASMR #Relaxing #KineticSand",
                status = "APPROVED"
            ),
            // 2. Video: Dhaka to Cox's Bazar Cinematic Drone Film (Real public streaming MP4)
            PostEntity(
                type = PostType.VIDEO,
                title = "Unexplored Bangladesh - 4K Cinematic Drone Travel Documentary 🇧🇩",
                description = "Exploring the beauty of Sajek Valley, Cox's Bazar longest sea beach, and Sylhet tea gardens in ultra cinematic 4K 60fps.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80",
                channelId = "ch_cinematic_bd",
                channelName = "Bengal Explorer",
                channelAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                subscriberCount = "980K subscribers",
                views = "520K views",
                viewCount = 520000L,
                likeCount = 38000L,
                dislikeCount = 95L,
                commentCount = 920L,
                timeAgo = "1 day ago",
                category = "Travel",
                duration = "00:15",
                durationSeconds = 15L,
                isVerified = true,
                tags = "#Bangladesh #Travel #Cinematic #Drone",
                status = "APPROVED"
            ),
            // 3. Video: Next Gen Cyberpunk AI Development (Real public streaming MP4)
            PostEntity(
                type = PostType.VIDEO,
                title = "I Built a Full Artificial Intelligence System in 24 Hours! 🤖⚡",
                description = "Building neural networks, real-time voice synthesis, and vision processing from scratch with Kotlin and Python.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&q=80",
                channelId = "ch_tech_guru",
                channelName = "CyberDev Lab",
                channelAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                subscriberCount = "1.5M subscribers",
                views = "310K views",
                viewCount = 310000L,
                likeCount = 28000L,
                dislikeCount = 80L,
                commentCount = 670L,
                timeAgo = "3 days ago",
                category = "Tech",
                duration = "00:15",
                durationSeconds = 15L,
                isVerified = true,
                tags = "#Programming #AI #Coding #Tech",
                status = "APPROVED"
            ),
            // 4. Video: Bengali Lofi Beats for Relaxing & Sleep (Real public streaming MP4)
            PostEntity(
                type = PostType.VIDEO,
                title = "Midnight Rain Lofi Chill Beats 🌧️ | Study & Relaxing Session",
                description = "Chill lo-fi hip hop beats crafted for deep focus, studying, coding, or sleeping peacefully during rain.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80",
                channelId = "ch_lofi_vibes",
                channelName = "Satisfy Chill Beats",
                channelAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                subscriberCount = "3.1M subscribers",
                views = "1.2M views",
                viewCount = 1200000L,
                likeCount = 95000L,
                dislikeCount = 210L,
                commentCount = 4300L,
                timeAgo = "4 days ago",
                category = "Music",
                duration = "10:53",
                durationSeconds = 653L,
                isVerified = true,
                tags = "#Lofi #ChillBeats #Rain #Music",
                status = "APPROVED"
            ),
            // 5. Video: Street Food Masterclass (Real public streaming MP4)
            PostEntity(
                type = PostType.VIDEO,
                title = "Old Dhaka Royal Kacchi Biryani Secret Recipe Revealed! 🍲",
                description = "Step by step authentic traditional mutton kacchi biryani cooked over firewood with aromatic saffron and potatoes.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyBlazes.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&q=80",
                channelId = "ch_food_express",
                channelName = "Gourmet Story BD",
                channelAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                subscriberCount = "720K subscribers",
                views = "450K views",
                viewCount = 450000L,
                likeCount = 31000L,
                dislikeCount = 65L,
                commentCount = 1200L,
                timeAgo = "1 week ago",
                category = "Cooking",
                duration = "00:15",
                durationSeconds = 15L,
                isVerified = true,
                tags = "#Cooking #Food #Biryani #Recipe",
                status = "APPROVED"
            ),

            // 6. SHORT 1: Speed Painting neon anime art (Real public streaming MP4)
            PostEntity(
                type = PostType.SHORT,
                title = "Speed drawing neon cyberpunk character in 60s! 🎨✨ #shorts",
                description = "Watch how the glowing neon lighting was rendered step by step! Like & subscribe for more daily art shorts.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=600&q=80",
                channelId = "ch_neon_artist",
                channelName = "Neon Canvas",
                channelAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                subscriberCount = "890K subscribers",
                views = "2.8M views",
                viewCount = 2800000L,
                likeCount = 340000L,
                dislikeCount = 1200L,
                commentCount = 5200L,
                timeAgo = "5 hours ago",
                category = "Art",
                duration = "00:15",
                durationSeconds = 15L,
                isVerified = true,
                tags = "#Shorts #Art #Drawing #Speedpaint",
                status = "APPROVED"
            ),
            // 7. SHORT 2: Satisfying Soap Bubble in sub-zero ice (Real public streaming MP4)
            PostEntity(
                type = PostType.SHORT,
                title = "Frozen bubble crystallization in slow-motion ASMR ❄️🧼 #satisfying",
                description = "Watch the ice crystals blossom across the soap bubble. Pure perfection!",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/WeAreGoingOnBullrun.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1483921020237-2ff51e8e4b22?w=600&q=80",
                channelId = "ch_satisfy_official",
                channelName = "Satisfy Studios",
                channelAvatar = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=150",
                subscriberCount = "2.4M subscribers",
                views = "5.1M views",
                viewCount = 5100000L,
                likeCount = 680000L,
                dislikeCount = 2300L,
                commentCount = 8900L,
                timeAgo = "1 day ago",
                category = "Satisfying",
                duration = "00:59",
                durationSeconds = 59L,
                isVerified = true,
                tags = "#Shorts #Satisfying #ASMR #Ice",
                status = "APPROVED"
            ),
            // 8. SHORT 3: Extreme FPV Drone mountain dive (Real public streaming MP4)
            PostEntity(
                type = PostType.SHORT,
                title = "FPV Drone dives 3000ft waterfall at 100mph! 🚀🌊",
                description = "Hold your breath! Extreme FPV mountain surfing through the clouds.",
                mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4",
                thumbnailUrl = "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?w=600&q=80",
                channelId = "ch_drone_racer",
                channelName = "Sky Flight FPV",
                channelAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                subscriberCount = "450K subscribers",
                views = "1.9M views",
                viewCount = 1900000L,
                likeCount = 210000L,
                dislikeCount = 940L,
                commentCount = 3100L,
                timeAgo = "2 days ago",
                category = "Gaming",
                duration = "12:14",
                durationSeconds = 734L,
                isVerified = true,
                tags = "#Shorts #FPV #Drone #Adventure",
                status = "APPROVED"
            ),

            // 9. PHOTO POST 1: Golden hour sunset photography
            PostEntity(
                type = PostType.PHOTO,
                title = "Golden Hour Reflections by the Bay 🌅📸",
                description = "Captured during this evening's golden sunset. The light reflections across the calm water looked surreal! Shot on 85mm f/1.4. Which tone do you like best?",
                mediaUrl = "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=900&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=900&q=80",
                channelId = "ch_photo_lens",
                channelName = "Aurora Photography",
                channelAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                subscriberCount = "320K followers",
                views = "85K views",
                viewCount = 85000L,
                likeCount = 8900L,
                dislikeCount = 42L,
                commentCount = 240L,
                timeAgo = "3 hours ago",
                category = "Photography",
                isVerified = true,
                tags = "#Photography #GoldenHour #Sunset #Community",
                status = "APPROVED"
            ),
            // 10. PHOTO POST 2: Cozy Workspace Setup
            PostEntity(
                type = PostType.PHOTO,
                title = "Minimalist Cyber-Workspace Night Setup 🖥️✨",
                description = "New studio setup update! Added the ambient backlights and mechanical custom keyboard. Let me know in comments what you think!",
                mediaUrl = "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=900&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1581291518857-4e27b48ff24e?w=900&q=80",
                channelId = "ch_tech_guru",
                channelName = "CyberDev Lab",
                channelAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                subscriberCount = "1.5M subscribers",
                views = "120K views",
                viewCount = 120000L,
                likeCount = 14500L,
                dislikeCount = 55L,
                commentCount = 510L,
                timeAgo = "1 day ago",
                category = "Tech",
                isVerified = true,
                tags = "#DeskSetup #Tech #Developer #Workspace",
                status = "APPROVED"
            ),
            // 11. PHOTO POST 3: Coffee Latte Art Perfection
            PostEntity(
                type = PostType.PHOTO,
                title = "Morning Swan Latte Art ☕🍃",
                description = "Freshly brewed single origin Ethiopian beans with delicate microfoam swan art to start the productive morning right.",
                mediaUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=900&q=80",
                thumbnailUrl = "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=900&q=80",
                channelId = "ch_barista_art",
                channelName = "The Coffee Ritual",
                channelAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                subscriberCount = "180K followers",
                views = "45K views",
                viewCount = 45000L,
                likeCount = 6200L,
                dislikeCount = 18L,
                commentCount = 130L,
                timeAgo = "2 days ago",
                category = "Lifestyle",
                isVerified = true,
                tags = "#Coffee #LatteArt #MorningVibes #Satisfy",
                status = "APPROVED"
            )
        )

        postDao.insertAll(seedPosts)

        // Seed initial creator pages if none exist
        if (creatorPageDao.getCount() == 0) {
            val defaultPage = CreatorPageEntity(
                name = "Satisfy Creator Studio",
                handle = "@satisfy_official",
                category = "Entertainment",
                description = "Official Creator Page for Satisfy Video Network. Sharing 4K ASMR, Cinematography, and tech workflows.",
                avatarUrl = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
                bannerUrl = "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800",
                websiteLink = "satisfy.app/@satisfy_official",
                followersCount = 24500,
                isVerified = true,
                totalWatchTimeSeconds = 485000L,
                totalViews = 54200L
            )
            creatorPageDao.insertPage(defaultPage)
        }

        // Seed initial comments
        val seedComments = listOf(
            CommentEntity(
                postId = 1,
                authorName = "Anik Chowdhury",
                authorAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                text = "The audio quality is insane! So satisfying to watch after a long day ❤️",
                timeAgo = "1 hour ago",
                likeCount = 145,
                isCreatorHearted = true
            ),
            CommentEntity(
                postId = 1,
                authorName = "Sadia Rahman",
                authorAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                text = "That rainbow sand slicing part was 10/10 perfection! Satisfy is my favorite app now.",
                timeAgo = "45 mins ago",
                likeCount = 88,
                isCreatorHearted = true
            ),
            CommentEntity(
                postId = 2,
                authorName = "Tanvir Hasan",
                authorAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                text = "Proud of Bangladesh! The drone shots over Sajek clouds are out of this world 🇧🇩🔥",
                timeAgo = "12 hours ago",
                likeCount = 312,
                isCreatorHearted = true
            ),
            CommentEntity(
                postId = 3,
                authorName = "Nusrat Jahan",
                authorAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                text = "Please do a full tutorial series on Android + Compose + Room architecture! Super clean!",
                timeAgo = "2 days ago",
                likeCount = 76,
                isCreatorHearted = false
            ),
            CommentEntity(
                postId = 9,
                authorName = "Fahim Ahmed",
                authorAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150",
                text = "Golden hour magic! What lens and aperture was this shot on? Beautiful tones!",
                timeAgo = "2 hours ago",
                likeCount = 42,
                isCreatorHearted = true
            )
        )
        commentDao.insertAllComments(seedComments)
    }
}

