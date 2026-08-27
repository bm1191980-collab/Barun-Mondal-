package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SatisfyDatabase
import com.example.data.model.*
import com.example.data.repository.AdminRepository
import com.example.data.repository.ProRepository
import com.example.data.repository.SatisfyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

enum class ScreenTab {
    HOME,
    SHORTS,
    CREATE,
    PHOTOS,
    PROFILE,
    SEARCH,
    ADMIN,
    PAGE_DETAILS,
    PRO_MEMBERSHIP,
    WALLET,
    OWNER_CHAT,
    CREATOR_ANALYTICS,
    MONETIZATION,
    SATISFY_RULES
}

data class PlayerState(
    val activePost: PostEntity? = null,
    val isPlaying: Boolean = true,
    val currentPositionSeconds: Float = 0f,
    val durationSeconds: Float = 300f,
    val isMuted: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val quality: String = "1080p",
    val isExpanded: Boolean = false,
    val isMiniPlayerVisible: Boolean = false,
    val showControls: Boolean = true
)

data class UploadProcessingState(
    val isUploading: Boolean = false,
    val progress: Float = 0f,
    val progressPercentage: Int = 0,
    val stage: String = "",
    val statusMessage: String = "",
    val isProcessing: Boolean = false,
    val isCompleted: Boolean = false,
    val status: VideoStatus = VideoStatus.UPLOADING,
    val uploadedPost: PostEntity? = null,
    val errorMessage: String? = null
)

class SatisfyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SatisfyRepository
    val adminRepository: AdminRepository
    val proRepository: ProRepository

    val currentTab = MutableStateFlow(ScreenTab.HOME)
    val selectedCategory = MutableStateFlow("All")
    val searchQuery = MutableStateFlow("")

    val playerState = MutableStateFlow(PlayerState())
    val isDarkMode = MutableStateFlow(true)

    // Creator Pages state
    val creatorPages: StateFlow<List<CreatorPageEntity>>
    val selectedPage = MutableStateFlow<CreatorPageEntity?>(null)
    private val _selectedPagePosts = MutableStateFlow<List<PostEntity>>(emptyList())
    val selectedPagePosts: StateFlow<List<PostEntity>> = _selectedPagePosts.asStateFlow()

    // Admin state flows
    val isAdminAuthenticated: StateFlow<Boolean>
    val currentAdmin: StateFlow<AdminAuthUser?>
    val authError: StateFlow<String?>
    val isAuthLoading: StateFlow<Boolean>
    val allUsers: StateFlow<List<UserAccountEntity>>
    val allReports: StateFlow<List<ReportEntity>>
    val pendingReports: StateFlow<List<ReportEntity>>
    val pushNotifications: StateFlow<List<PushNotificationLogEntity>>
    val appSettings: StateFlow<AppSystemSettingsEntity?>
    val auditLogs: StateFlow<List<AdminAuditLogEntity>>

    // PRO & WALLET & CHAT STATE FLOWS
    val proSubscription: StateFlow<ProSubscriptionEntity?>
    val isUserPro: StateFlow<Boolean>
    val userWallet: StateFlow<WalletEntity?>
    val walletTransactions: StateFlow<List<WalletTransactionEntity>>
    val userWithdrawals: StateFlow<List<WithdrawalRequestEntity>>
    val userReferrals: StateFlow<List<ReferralEntity>>
    val ownerChatInfo: StateFlow<OwnerChatEntity?>
    val ownerChatMessages: StateFlow<List<ChatMessageEntity>>

    // ADMIN PRO STATE FLOWS
    val allProSubscriptions: StateFlow<List<ProSubscriptionEntity>>
    val allReferrals: StateFlow<List<ReferralEntity>>
    val allWallets: StateFlow<List<WalletEntity>>
    val allWithdrawals: StateFlow<List<WithdrawalRequestEntity>>
    val allOwnerChats: StateFlow<List<OwnerChatEntity>>
    val adminActiveChatUserId = MutableStateFlow<String?>(null)
    val adminActiveChatMessages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())

    // Upload draft state
    val uploadType = MutableStateFlow(PostType.VIDEO)
    val uploadTitle = MutableStateFlow("")
    val uploadDescription = MutableStateFlow("")
    val uploadCategory = MutableStateFlow("Satisfying")
    val uploadTags = MutableStateFlow("#Satisfy #Trending")
    val uploadThumbnailUrl = MutableStateFlow("")
    val uploadMediaUrl = MutableStateFlow("")
    val isUploading = MutableStateFlow(false)
    val uploadSuccessMessage = MutableStateFlow<String?>(null)
    val uploadProcessingState = MutableStateFlow(UploadProcessingState())

    // Monetization & Analytics state flows
    val allMonetizationApplications: StateFlow<List<MonetizationApplicationEntity>>
    val userMonetizationApplication: StateFlow<MonetizationApplicationEntity?>
    val creatorAnalyticsSummary: StateFlow<CreatorAnalyticsSummary>
    val monetizationEligibility: StateFlow<MonetizationEligibility>

    // Comments for active video
    private val _activePostComments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val activePostComments: StateFlow<List<CommentEntity>> = _activePostComments.asStateFlow()

    // User Profile state flow
    val userProfile = MutableStateFlow(UserProfile())

    // Database flows
    val allPosts: StateFlow<List<PostEntity>>
    val pendingVerificationPosts: StateFlow<List<PostEntity>>
    val approvedPosts: StateFlow<List<PostEntity>>
    val videoPosts: StateFlow<List<PostEntity>>
    val shortPosts: StateFlow<List<PostEntity>>
    val photoPosts: StateFlow<List<PostEntity>>
    val savedPosts: StateFlow<List<PostEntity>>
    val userCreatedPosts: StateFlow<List<PostEntity>>
    val likedPosts: StateFlow<List<PostEntity>>
    val watchHistory: StateFlow<List<PostEntity>>

    val categories = listOf(
        "All", "Satisfying", "Travel", "Tech", "Music", "Cooking", "Art", "Gaming", "Photography", "Lifestyle"
    )

    init {
        val database = SatisfyDatabase.getDatabase(application)
        repository = SatisfyRepository(
            postDao = database.postDao(),
            commentDao = database.commentDao(),
            historyDao = database.watchHistoryDao(),
            creatorPageDao = database.creatorPageDao(),
            monetizationDao = database.monetizationDao()
        )
        adminRepository = AdminRepository(
            context = application.applicationContext,
            userAccountDao = database.userAccountDao(),
            postDao = database.postDao(),
            reportDao = database.reportDao(),
            pushNotificationDao = database.pushNotificationDao(),
            appSettingsDao = database.appSettingsDao(),
            auditLogDao = database.auditLogDao()
        )

        isAdminAuthenticated = adminRepository.isAdminAuthenticated
        currentAdmin = adminRepository.currentAdmin
        authError = adminRepository.authError
        isAuthLoading = adminRepository.isAuthLoading

        allUsers = adminRepository.allUsers.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        allReports = adminRepository.allReports.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        pendingReports = adminRepository.pendingReports.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        pushNotifications = adminRepository.pushNotifications.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        appSettings = adminRepository.appSettings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            AppSystemSettingsEntity()
        )
        auditLogs = adminRepository.auditLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allPosts = repository.allPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        pendingVerificationPosts = repository.pendingVerificationPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        approvedPosts = repository.approvedPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        videoPosts = repository.videoPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        shortPosts = repository.shortPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        photoPosts = repository.photoPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        savedPosts = repository.savedPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        userCreatedPosts = repository.userCreatedPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        likedPosts = repository.likedPosts.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        watchHistory = repository.watchHistory.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        creatorPages = repository.creatorPages.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        // Reactively update selected page posts
        viewModelScope.launch {
            selectedPage.collectLatest { page ->
                if (page != null) {
                    repository.getPostsByPage(page.id).collectLatest { posts ->
                        _selectedPagePosts.value = posts
                    }
                } else {
                    _selectedPagePosts.value = emptyList()
                }
            }
        }

        // Load saved user profile
        userProfile.value = loadUserProfileFromPrefs()

        // Pro Repository initialization
        proRepository = ProRepository(
            context = application.applicationContext,
            proSubscriptionDao = database.proSubscriptionDao(),
            referralDao = database.referralDao(),
            walletDao = database.walletDao(),
            walletTransactionDao = database.walletTransactionDao(),
            withdrawalRequestDao = database.withdrawalRequestDao(),
            ownerChatDao = database.ownerChatDao(),
            chatMessageDao = database.chatMessageDao(),
            userAccountDao = database.userAccountDao(),
            postDao = database.postDao(),
            auditLogDao = database.auditLogDao()
        )

        proSubscription = proRepository.getUserSubscription(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        isUserPro = proRepository.isUserPro(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            false
        )
        userWallet = proRepository.getUserWallet(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        walletTransactions = proRepository.getUserTransactions(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        userWithdrawals = proRepository.getUserWithdrawals(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        userReferrals = proRepository.getUserReferrals(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        ownerChatInfo = proRepository.getOwnerChatForUser(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
        ownerChatMessages = proRepository.getChatMessages(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allProSubscriptions = proRepository.allSubscriptions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        allReferrals = proRepository.allReferrals.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        allWallets = proRepository.allWallets.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        allWithdrawals = proRepository.allWithdrawals.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        allOwnerChats = proRepository.allOwnerChats.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allMonetizationApplications = repository.monetizationApplications.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        userMonetizationApplication = repository.observeUserMonetizationApplication(userProfile.value.uid).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        creatorAnalyticsSummary = combine(
            allPosts,
            userCreatedPosts,
            userProfile
        ) { all, created, profile ->
            val userPosts = if (created.isNotEmpty()) {
                created
            } else {
                all.filter { it.channelName.equals(profile.name, ignoreCase = true) || it.isUserCreated }
            }
            val shorts = userPosts.filter { it.type == PostType.SHORT }
            val videos = userPosts.filter { it.type == PostType.VIDEO }

            val shortsViews = shorts.sumOf { it.viewCount }
            val shortsWatchSecs = shorts.sumOf { it.watchTimeSeconds }
            val videoViews = videos.sumOf { it.viewCount }
            val videoWatchSecs = videos.sumOf { it.watchTimeSeconds }

            // Extract numeric subscriber count if possible, fallback to 1250L
            val rawSubText = profile.subscriberCount.replace("subscribers", "").replace("subscriber", "").trim()
            val subCount = when {
                rawSubText.endsWith("K", ignoreCase = true) -> {
                    val num = rawSubText.dropLast(1).toDoubleOrNull() ?: 1.2
                    (num * 1000).toLong()
                }
                rawSubText.endsWith("M", ignoreCase = true) -> {
                    val num = rawSubText.dropLast(1).toDoubleOrNull() ?: 1.0
                    (num * 1000000).toLong()
                }
                else -> rawSubText.toLongOrNull() ?: 1250L
            }

            CreatorAnalyticsSummary(
                totalShortsUploaded = shorts.size,
                totalShortsViews = shortsViews,
                totalShortsWatchTimeSeconds = shortsWatchSecs,
                totalVideosUploaded = videos.size,
                totalVideoViews = videoViews,
                totalVideoWatchTimeSeconds = videoWatchSecs,
                totalSubscribers = subCount,
                individualShorts = shorts,
                individualVideos = videos
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            CreatorAnalyticsSummary()
        )

        monetizationEligibility = creatorAnalyticsSummary.map { summary ->
            val subs = summary.totalSubscribers
            val normalHours = summary.totalVideoWatchTimeSeconds / 3600.0
            val shortsHours = summary.totalShortsWatchTimeSeconds / 3600.0

            val subsMet = subs >= 500L
            val normalMet = normalHours >= 4000.0
            val shortsMet = shortsHours >= 10000.0

            val pathwayAMet = subsMet && normalMet
            val pathwayBMet = subsMet && shortsMet
            val isEligible = pathwayAMet || pathwayBMet

            MonetizationEligibility(
                currentSubscribers = subs,
                requiredSubscribers = 500L,
                isSubscriberRequirementMet = subsMet,

                currentNormalWatchHours = normalHours,
                requiredNormalWatchHours = 4000.0,
                isNormalWatchRequirementMet = normalMet,

                currentShortsWatchHours = shortsHours,
                requiredShortsWatchHours = 10000.0,
                isShortsWatchRequirementMet = shortsMet,

                isPathwayAMet = pathwayAMet,
                isPathwayBMet = pathwayBMet,
                isEligible = isEligible,

                remainingSubscribers = maxOf(0L, 500L - subs),
                remainingNormalWatchHours = maxOf(0.0, 4000.0 - normalHours),
                remainingShortsWatchHours = maxOf(0.0, 10000.0 - shortsHours)
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MonetizationEligibility()
        )

        // Listen to admin active chat user changes
        viewModelScope.launch {
            adminActiveChatUserId.collectLatest { uid: String? ->
                if (uid != null) {
                    proRepository.getChatMessages(uid).collectLatest { msgs ->
                        adminActiveChatMessages.value = msgs
                    }
                } else {
                    adminActiveChatMessages.value = emptyList()
                }
            }
        }

        // Seed initial data
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            adminRepository.seedAdminInitialData()
            proRepository.seedInitialProData(userProfile.value.uid, userProfile.value.referralCode)
        }
    }

    // Video Player Actions
    fun openVideo(post: PostEntity, expanded: Boolean = true) {
        val totalSecs = parseDurationToSeconds(post.duration)
        playerState.value = PlayerState(
            activePost = post,
            isPlaying = true,
            currentPositionSeconds = 0f,
            durationSeconds = totalSecs.toFloat(),
            isExpanded = expanded,
            isMiniPlayerVisible = true,
            showControls = true
        )
        viewModelScope.launch {
            repository.incrementViewCount(post.id)
            repository.recordWatchHistory(post.id)
            loadComments(post.id)
            // Refresh active post with updated view count
            val updated = repository.getPostById(post.id)
            if (updated != null && playerState.value.activePost?.id == post.id) {
                playerState.value = playerState.value.copy(activePost = updated)
            }
        }
    }

    fun minimizePlayer() {
        playerState.value = playerState.value.copy(isExpanded = false)
    }

    fun expandPlayer() {
        playerState.value = playerState.value.copy(isExpanded = true)
    }

    fun closePlayer() {
        playerState.value = PlayerState(activePost = null, isPlaying = false, isMiniPlayerVisible = false, isExpanded = false)
    }

    fun togglePlayPause() {
        playerState.value = playerState.value.copy(isPlaying = !playerState.value.isPlaying)
    }

    fun seekTo(seconds: Float) {
        playerState.value = playerState.value.copy(
            currentPositionSeconds = seconds.coerceIn(0f, playerState.value.durationSeconds)
        )
    }

    fun seekRelative(deltaSeconds: Float) {
        val newPos = (playerState.value.currentPositionSeconds + deltaSeconds).coerceIn(0f, playerState.value.durationSeconds)
        playerState.value = playerState.value.copy(currentPositionSeconds = newPos)
    }

    fun toggleMute() {
        playerState.value = playerState.value.copy(isMuted = !playerState.value.isMuted)
    }

    fun setPlaybackSpeed(speed: Float) {
        playerState.value = playerState.value.copy(playbackSpeed = speed)
    }

    fun setQuality(quality: String) {
        playerState.value = playerState.value.copy(quality = quality)
    }

    fun toggleControlsVisibility() {
        playerState.value = playerState.value.copy(showControls = !playerState.value.showControls)
    }

    fun loadComments(postId: Long) {
        viewModelScope.launch {
            repository.getCommentsForPost(postId).collect { comments ->
                _activePostComments.value = comments
            }
        }
    }

    // Likes, Saves, Subscribes
    fun toggleLike(post: PostEntity) {
        viewModelScope.launch {
            repository.toggleLike(post)
            // Update active post reference if matched
            if (playerState.value.activePost?.id == post.id) {
                val updated = repository.getPostById(post.id)
                if (updated != null) {
                    playerState.value = playerState.value.copy(activePost = updated)
                }
            }
        }
    }

    fun toggleDislike(post: PostEntity) {
        viewModelScope.launch {
            repository.toggleDislike(post)
            if (playerState.value.activePost?.id == post.id) {
                val updated = repository.getPostById(post.id)
                if (updated != null) {
                    playerState.value = playerState.value.copy(activePost = updated)
                }
            }
        }
    }

    fun toggleSave(post: PostEntity) {
        viewModelScope.launch {
            repository.toggleSave(post)
            if (playerState.value.activePost?.id == post.id) {
                val updated = repository.getPostById(post.id)
                if (updated != null) {
                    playerState.value = playerState.value.copy(activePost = updated)
                }
            }
        }
    }

    fun toggleSubscribe(channelName: String, currentSubscribed: Boolean) {
        viewModelScope.launch {
            repository.toggleSubscribe(channelName, currentSubscribed)
            val active = playerState.value.activePost
            if (active != null && active.channelName == channelName) {
                val updated = repository.getPostById(active.id)
                if (updated != null) {
                    playerState.value = playerState.value.copy(activePost = updated)
                }
            }
        }
    }

    fun addComment(postId: Long, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            repository.addComment(postId, text)
            loadComments(postId)
            val updated = repository.getPostById(postId)
            if (updated != null && playerState.value.activePost?.id == postId) {
                playerState.value = playerState.value.copy(activePost = updated)
            }
        }
    }

    fun toggleCommentLike(comment: CommentEntity) {
        viewModelScope.launch {
            repository.toggleCommentLike(comment)
        }
    }

    fun deletePost(post: PostEntity) {
        viewModelScope.launch {
            repository.deletePost(post)
            if (playerState.value.activePost?.id == post.id) {
                closePlayer()
            }
        }
    }

    fun clearWatchHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    // Uploading New Video/Photo/Short with real-time staged progress
    fun submitUpload(
        type: PostType,
        title: String,
        description: String,
        category: String,
        tags: String,
        thumbnailUrl: String,
        mediaUrl: String,
        customDuration: String = "08:30"
    ) {
        if (title.isBlank()) return

        viewModelScope.launch {
            isUploading.value = true
            uploadProcessingState.value = UploadProcessingState(
                isUploading = true,
                progress = 0.1f,
                progressPercentage = 10,
                stage = "Initiating upload...",
                statusMessage = "Uploading... 10%",
                status = VideoStatus.UPLOADING
            )

            kotlinx.coroutines.delay(400)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                progress = 0.25f,
                progressPercentage = 25,
                stage = "Uploading... 25%",
                statusMessage = "Uploading... 25%"
            )

            kotlinx.coroutines.delay(500)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                progress = 0.50f,
                progressPercentage = 50,
                stage = "Uploading... 50%",
                statusMessage = "Uploading... 50%"
            )

            kotlinx.coroutines.delay(500)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                progress = 0.75f,
                progressPercentage = 75,
                stage = "Uploading... 75%",
                statusMessage = "Uploading... 75%"
            )

            kotlinx.coroutines.delay(400)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                progress = 1.0f,
                progressPercentage = 100,
                stage = "Upload Complete",
                statusMessage = "Upload Complete"
            )

            kotlinx.coroutines.delay(400)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                isProcessing = true,
                stage = "Processing video streams & formats...",
                statusMessage = "Processing 4K Stream & Audio Codecs...",
                status = VideoStatus.PROCESSING
            )

            kotlinx.coroutines.delay(600)
            uploadProcessingState.value = uploadProcessingState.value.copy(
                stage = "Verifying copyright & quality compliance...",
                statusMessage = "Verifying content quality & copyright...",
                status = VideoStatus.PROCESSING
            )

            val finalThumbnail = if (thumbnailUrl.isNotBlank()) thumbnailUrl else when (type) {
                PostType.VIDEO -> "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=800&q=80"
                PostType.SHORT -> "https://images.unsplash.com/photo-1518770660439-4636190af475?w=600&q=80"
                PostType.PHOTO -> "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80"
            }

            val currentProf = userProfile.value
            val newPost = PostEntity(
                type = type,
                title = title.trim(),
                description = description.trim(),
                category = category,
                tags = if (tags.isNotBlank()) tags.trim() else "#Satisfy #New",
                thumbnailUrl = finalThumbnail,
                mediaUrl = if (mediaUrl.isNotBlank()) mediaUrl else "custom_uploaded_content",
                channelId = "user_me",
                channelName = currentProf.name.ifBlank { "Satisfy Creator" },
                channelAvatar = currentProf.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                subscriberCount = currentProf.subscriberCount,
                views = "0 views",
                viewCount = 0L,
                likeCount = 0L,
                dislikeCount = 0L,
                commentCount = 0L,
                timeAgo = "Just now",
                duration = if (type == PostType.SHORT) "0:45" else customDuration,
                isVerified = false,
                isUserCreated = true,
                status = VideoStatus.PENDING.name,
                creatorUid = currentAdmin.value?.uid ?: "user_me"
            )

            val createdId = repository.createPost(newPost)
            val insertedPost = newPost.copy(id = createdId)

            kotlinx.coroutines.delay(400)
            uploadProcessingState.value = UploadProcessingState(
                isUploading = false,
                isProcessing = false,
                isCompleted = true,
                progress = 1.0f,
                progressPercentage = 100,
                stage = "Submitted for Admin Verification 🚀",
                statusMessage = "Submitted for Admin Verification",
                status = VideoStatus.PENDING,
                uploadedPost = insertedPost
            )
            isUploading.value = false

            uploadSuccessMessage.value = when (type) {
                PostType.VIDEO -> "Video submitted! Admin will verify before publishing. ⏳"
                PostType.SHORT -> "Short submitted for verification! ⚡"
                PostType.PHOTO -> "Post submitted for verification! 📸"
            }

            // Reset upload form
            uploadTitle.value = ""
            uploadDescription.value = ""
            uploadThumbnailUrl.value = ""
            uploadMediaUrl.value = ""
        }
    }

    fun resetUploadProcessingState() {
        uploadProcessingState.value = UploadProcessingState()
        isUploading.value = false
    }

    fun resetUploadState() {
        resetUploadProcessingState()
    }

    fun cancelUpload() {
        uploadProcessingState.value = UploadProcessingState()
        isUploading.value = false
    }

    fun dismissSuccessMessage() {
        uploadSuccessMessage.value = null
    }

    // --- ADMIN VERIFICATION ACTIONS ---
    fun adminApproveVideo(post: PostEntity, notes: String = "") {
        viewModelScope.launch {
            val adminEmail = currentAdmin.value?.email ?: AdminRepository.PRIMARY_SUPERADMIN_EMAIL
            adminRepository.approveVideo(post.id, adminEmail, notes)
        }
    }

    fun adminRejectVideo(post: PostEntity, reason: String) {
        viewModelScope.launch {
            val adminEmail = currentAdmin.value?.email ?: AdminRepository.PRIMARY_SUPERADMIN_EMAIL
            adminRepository.rejectVideo(post.id, adminEmail, reason)
        }
    }

    // --- ADMIN ACTIONS & USER MANAGEMENT ---
    fun loginAdmin(email: String, pass: String, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = adminRepository.signInAdmin(email, pass)
            onComplete(success)
        }
    }

    fun logoutAdmin() {
        adminRepository.signOutAdmin()
        if (currentTab.value == ScreenTab.ADMIN) {
            currentTab.value = ScreenTab.HOME
        }
    }

    fun banUser(uid: String, reason: String) {
        viewModelScope.launch {
            adminRepository.banUser(uid, reason)
        }
    }

    fun unbanUser(uid: String) {
        viewModelScope.launch {
            adminRepository.unbanUser(uid)
        }
    }

    fun updateUserRole(uid: String, role: String) {
        viewModelScope.launch {
            adminRepository.updateUserRole(uid, role)
        }
    }

    fun addAdminUser(name: String, email: String, role: String, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            val result = adminRepository.addAdminUser(name, email, role)
            result.onSuccess { msg ->
                onResult(true, msg)
            }.onFailure { err ->
                onResult(false, err.message ?: "Failed to add administrator")
            }
        }
    }

    fun deleteUser(user: UserAccountEntity) {
        viewModelScope.launch {
            adminRepository.deleteUser(user)
        }
    }

    fun adminDeletePost(post: PostEntity) {
        viewModelScope.launch {
            adminRepository.deletePost(post)
            if (playerState.value.activePost?.id == post.id) {
                closePlayer()
            }
        }
    }

    fun adminEditPost(
        post: PostEntity,
        newTitle: String,
        newDesc: String,
        newCategory: String,
        newTags: String,
        newDuration: String,
        newThumbUrl: String
    ) {
        viewModelScope.launch {
            val updated = post.copy(
                title = newTitle.trim(),
                description = newDesc.trim(),
                category = newCategory,
                tags = newTags.trim(),
                duration = if (newDuration.isNotBlank()) newDuration else post.duration,
                thumbnailUrl = if (newThumbUrl.isNotBlank()) newThumbUrl else post.thumbnailUrl
            )
            adminRepository.updatePost(updated)
            if (playerState.value.activePost?.id == post.id) {
                playerState.value = playerState.value.copy(activePost = updated)
            }
        }
    }

    fun adminToggleFeatured(post: PostEntity) {
        viewModelScope.launch {
            adminRepository.toggleFeatured(post.id, post.isFeatured)
        }
    }

    fun adminToggleFlagged(post: PostEntity) {
        viewModelScope.launch {
            adminRepository.toggleFlagged(post.id, post.isFlagged)
        }
    }

    fun resolveReport(report: ReportEntity, actionTaken: String, deleteTarget: Boolean = false, banTargetUser: Boolean = false) {
        viewModelScope.launch {
            if (deleteTarget && report.targetType == "POST") {
                val post = repository.getPostById(report.targetId)
                if (post != null) {
                    adminRepository.deletePost(post)
                }
            }
            if (banTargetUser) {
                val allUsrs = allUsers.value
                val matched = allUsrs.find { it.name == report.reportedUser || it.email == report.reportedUser }
                if (matched != null) {
                    adminRepository.banUser(matched.uid, "Banned due to report #${report.id} (${report.reason})")
                }
            }
            adminRepository.resolveReport(report.id, actionTaken)
        }
    }

    fun dismissReport(reportId: Long) {
        viewModelScope.launch {
            adminRepository.dismissReport(reportId)
        }
    }

    fun deleteReport(report: ReportEntity) {
        viewModelScope.launch {
            adminRepository.deleteReport(report)
        }
    }

    fun reportContent(
        targetId: Long,
        targetType: String,
        targetTitle: String,
        reportedUser: String,
        reason: String,
        details: String
    ) {
        viewModelScope.launch {
            adminRepository.createReport(
                targetId = targetId,
                targetType = targetType,
                targetTitle = targetTitle,
                reportedUser = reportedUser,
                reason = reason,
                details = details
            )
        }
    }

    fun sendPushBroadcast(
        title: String,
        body: String,
        topic: String,
        audienceLabel: String,
        actionUrl: String = ""
    ) {
        viewModelScope.launch {
            adminRepository.sendPushNotification(
                title = title,
                body = body,
                targetTopic = topic,
                targetAudienceLabel = audienceLabel,
                actionUrl = actionUrl
            )
        }
    }

    fun saveAppSettings(settings: AppSystemSettingsEntity) {
        viewModelScope.launch {
            adminRepository.saveAppSettings(settings)
        }
    }

    // --- USER PROFILE CUSTOMIZATION (GALLERY AVATAR & BANNER) ---

    fun saveImageFromGalleryUri(uri: Uri, isAvatar: Boolean): String {
        return try {
            val fileName = if (isAvatar) "user_custom_avatar.jpg" else "user_custom_banner.jpg"
            val file = File(getApplication<Application>().filesDir, fileName)
            getApplication<Application>().contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            uri.toString()
        }
    }

    fun updateProfileAvatarUri(uri: Uri) {
        val path = saveImageFromGalleryUri(uri, isAvatar = true)
        val current = userProfile.value
        val updated = current.copy(avatarUrl = path)
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun updateProfileBannerUri(uri: Uri) {
        val path = saveImageFromGalleryUri(uri, isAvatar = false)
        val current = userProfile.value
        val updated = current.copy(bannerUrl = path)
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun resetProfileBanner() {
        val current = userProfile.value
        val updated = current.copy(bannerUrl = "")
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun resetProfileAvatar() {
        val current = userProfile.value
        val updated = current.copy(avatarUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200")
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    fun updateProfileInfo(name: String, handle: String, bio: String, link: String = "") {
        val current = userProfile.value
        val formattedHandle = if (handle.trim().startsWith("@")) handle.trim() else "@${handle.trim().ifBlank { "satisfy_creator" }}"
        val updated = current.copy(
            name = name.trim().ifBlank { current.name },
            handle = formattedHandle,
            bio = bio.trim().ifBlank { current.bio },
            link = link.trim().ifBlank { current.link }
        )
        userProfile.value = updated
        saveUserProfileToPrefs(updated)
    }

    private fun saveUserProfileToPrefs(profile: UserProfile) {
        val prefs = getApplication<Application>().getSharedPreferences("satisfy_user_profile", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("user_name", profile.name)
            .putString("user_handle", profile.handle)
            .putString("user_bio", profile.bio)
            .putString("user_avatar", profile.avatarUrl)
            .putString("user_banner", profile.bannerUrl)
            .putString("user_link", profile.link)
            .apply()
    }

    private fun loadUserProfileFromPrefs(): UserProfile {
        val prefs = getApplication<Application>().getSharedPreferences("satisfy_user_profile", Context.MODE_PRIVATE)
        return UserProfile(
            name = prefs.getString("user_name", "Satisfy Creator") ?: "Satisfy Creator",
            handle = prefs.getString("user_handle", "@satisfy_creator") ?: "@satisfy_creator",
            bio = prefs.getString("user_bio", "Welcome to my Satisfy channel! Sharing satisfying video creations, 4K nature cinematography, and community photography. ✨") ?: "",
            avatarUrl = prefs.getString("user_avatar", "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200") ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200",
            bannerUrl = prefs.getString("user_banner", "") ?: "",
            link = prefs.getString("user_link", "satisfy.app/@satisfy_creator") ?: "satisfy.app/@satisfy_creator"
        )
    }

    // --- CREATOR PAGES & WATCH TIME ACTIONS ---

    fun openPageDetails(page: CreatorPageEntity) {
        selectedPage.value = page
        currentTab.value = ScreenTab.PAGE_DETAILS
    }

    fun createCreatorPage(
        name: String,
        category: String,
        description: String,
        handle: String,
        link: String = "",
        avatarUri: Uri? = null,
        bannerUri: Uri? = null,
        onSuccess: (CreatorPageEntity) -> Unit = {}
    ) {
        viewModelScope.launch {
            val avatarPath = avatarUri?.let { saveImageFromGalleryUri(it, isAvatar = true) }
                ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200"
            val bannerPath = bannerUri?.let { saveImageFromGalleryUri(it, isAvatar = false) }
                ?: "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800"

            val cleanHandle = if (handle.startsWith("@")) handle else "@$handle"

            val newPage = CreatorPageEntity(
                name = name.trim().ifBlank { "Creator Page" },
                handle = cleanHandle,
                category = category,
                description = description.trim(),
                avatarUrl = avatarPath,
                bannerUrl = bannerPath,
                websiteLink = link.trim(),
                followersCount = 1200L,
                isVerified = false,
                totalWatchTimeSeconds = 18600L, // initial seed watch time ~5.2 hours
                totalViews = 1420L
            )
            val id = repository.createPage(newPage)
            val created = newPage.copy(id = id)
            selectedPage.value = created
            onSuccess(created)
        }
    }

    fun updateCreatorPage(
        pageId: Long,
        name: String,
        category: String,
        description: String,
        link: String
    ) {
        viewModelScope.launch {
            val existing = repository.getPageById(pageId) ?: return@launch
            val updated = existing.copy(
                name = name.trim().ifBlank { existing.name },
                category = category,
                description = description.trim(),
                websiteLink = link.trim()
            )
            repository.updatePage(updated)
            if (selectedPage.value?.id == pageId) {
                selectedPage.value = updated
            }
        }
    }

    fun updatePageAvatarUri(pageId: Long, uri: Uri) {
        viewModelScope.launch {
            val existing = repository.getPageById(pageId) ?: return@launch
            val path = saveImageFromGalleryUri(uri, isAvatar = true)
            val updated = existing.copy(avatarUrl = path)
            repository.updatePage(updated)
            if (selectedPage.value?.id == pageId) {
                selectedPage.value = updated
            }
        }
    }

    fun updatePageBannerUri(pageId: Long, uri: Uri) {
        viewModelScope.launch {
            val existing = repository.getPageById(pageId) ?: return@launch
            val path = saveImageFromGalleryUri(uri, isAvatar = false)
            val updated = existing.copy(bannerUrl = path)
            repository.updatePage(updated)
            if (selectedPage.value?.id == pageId) {
                selectedPage.value = updated
            }
        }
    }

    fun deleteCreatorPage(page: CreatorPageEntity) {
        viewModelScope.launch {
            repository.deletePage(page)
            if (selectedPage.value?.id == page.id) {
                selectedPage.value = null
                currentTab.value = ScreenTab.PROFILE
            }
        }
    }

    fun recordPlaybackProgress(deltaSeconds: Long = 1L) {
        val active = playerState.value.activePost ?: return
        viewModelScope.launch {
            repository.recordVideoWatchTime(
                postId = active.id,
                deltaSeconds = deltaSeconds,
                pageId = active.pageId
            )
            val currentPage = selectedPage.value
            if (currentPage != null && (active.pageId == currentPage.id || currentPage.id == 1L)) {
                val updatedPage = repository.getPageById(currentPage.id)
                if (updatedPage != null) {
                    selectedPage.value = updatedPage
                }
            }
        }
    }

    fun formatWatchTime(totalSeconds: Long): String {
        val hours = totalSeconds / 3600.0
        return if (hours >= 1.0) {
            String.format(java.util.Locale.US, "%.1f Hours", hours)
        } else {
            val mins = totalSeconds / 60
            "$mins Minutes"
        }
    }

    fun formatWatchTimeBangla(totalSeconds: Long): String {
        return formatWatchTime(totalSeconds)
    }

    private fun parseDurationToSeconds(duration: String): Long {
        return try {
            val parts = duration.split(":").map { it.trim().toLong() }
            if (parts.size == 2) {
                parts[0] * 60 + parts[1]
            } else if (parts.size == 3) {
                parts[0] * 3600 + parts[1] * 60 + parts[2]
            } else {
                180L
            }
        } catch (e: Exception) {
            180L
        }
    }

    // ==========================================
    // PRO MEMBERSHIP & REFERRAL REWARD METHODS
    // ==========================================

    fun purchaseProSubscription(
        referrerCode: String?,
        paymentMethod: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            proRepository.purchaseProMembership(
                userId = userProfile.value.uid,
                userName = userProfile.value.name,
                userEmail = userProfile.value.email,
                referralCodeApplied = referrerCode,
                paymentMethod = paymentMethod
            ) { result ->
                onResult(result.isSuccess, if (result.isSuccess) "Pro Membership activated successfully! Welcome to Pro." else (result.errorMessage ?: "Payment failed. Please try again."))
            }
        }
    }

    fun submitWithdrawalRequest(
        amount: Double,
        payoutMethod: String,
        payoutDetails: String,
        accountHolderName: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            proRepository.requestWithdrawal(
                userId = userProfile.value.uid,
                userName = userProfile.value.name,
                userEmail = userProfile.value.email,
                amount = amount,
                paymentMethod = payoutMethod,
                paymentDetails = payoutDetails,
                accountHolderName = accountHolderName,
                onResult = onResult
            )
        }
    }

    fun sendUserChatMessage(message: String) {
        viewModelScope.launch {
            proRepository.sendUserChatMessage(
                userId = userProfile.value.uid,
                userName = userProfile.value.name,
                userAvatar = userProfile.value.avatarUrl,
                userEmail = userProfile.value.email,
                message = message
            )
        }
    }

    fun sendAdminChatMessage(userId: String, message: String) {
        viewModelScope.launch {
            proRepository.sendAdminReplyMessage(
                targetUserId = userId,
                adminName = "Satisfy Owner / Support",
                message = message
            )
        }
    }

    fun selectAdminChatUser(userId: String) {
        adminActiveChatUserId.value = userId
        viewModelScope.launch {
            proRepository.markChatAsRead(userId, readerRole = "ADMIN")
        }
    }

    fun toggleBlockChatUser(userId: String, isBlocked: Boolean) {
        viewModelScope.launch {
            proRepository.setChatBlocked(userId, isBlocked)
        }
    }

    fun adminApproveWithdrawal(withdrawalId: Long, paymentRef: String, adminNotes: String) {
        viewModelScope.launch {
            proRepository.approveWithdrawal(withdrawalId, paymentRef, adminNotes)
        }
    }

    fun adminRejectWithdrawal(withdrawalId: Long, reason: String, adminNotes: String) {
        viewModelScope.launch {
            proRepository.rejectWithdrawal(withdrawalId, reason, adminNotes)
        }
    }

    fun adminToggleFreezeWallet(userId: String, isFrozen: Boolean, reason: String) {
        viewModelScope.launch {
            proRepository.setWalletFrozen(userId, isFrozen, reason)
        }
    }

    fun adminToggleSuspiciousReferral(referralId: Long, isSuspicious: Boolean, reason: String) {
        viewModelScope.launch {
            proRepository.setReferralSuspicious(referralId, isSuspicious, reason)
        }
    }

    fun adminReverseReferralReward(referralId: Long, reason: String) {
        viewModelScope.launch {
            proRepository.reverseReferralReward(referralId, reason)
        }
    }

    fun adminCancelProSubscription(subscriptionId: Long) {
        viewModelScope.launch {
            proRepository.cancelSubscription(subscriptionId)
        }
    }

    fun togglePostPremiumStatus(post: PostEntity) {
        viewModelScope.launch {
            proRepository.togglePostPremiumStatus(post.id, !post.isPremium)
        }
    }

    fun submitMonetizationApplication(onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                val summary = creatorAnalyticsSummary.value
                val profile = userProfile.value
                val normalHours = summary.totalVideoWatchTimeSeconds / 3600.0
                val shortsHours = summary.totalShortsWatchTimeSeconds / 3600.0

                val app = MonetizationApplicationEntity(
                    userId = profile.uid,
                    channelName = profile.name,
                    channelHandle = profile.handle,
                    channelAvatar = profile.avatarUrl,
                    subscriberCount = summary.totalSubscribers,
                    normalVideoWatchHours = normalHours,
                    shortsWatchHours = shortsHours,
                    totalShortsCount = summary.totalShortsUploaded,
                    totalVideosCount = summary.totalVideosUploaded,
                    status = "PENDING"
                )
                repository.submitMonetizationApplication(app)
                adminRepository.recordAuditLog("MONETIZATION_APPLY", "Submitted monetization application for ${profile.name}", profile.uid)
                onResult(true, "Monetization application submitted successfully! Review pending.")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to submit application")
            }
        }
    }

    fun adminApproveMonetization(applicationId: Long, notes: String = "", onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.updateMonetizationStatus(
                    id = applicationId,
                    status = "APPROVED",
                    reviewedAt = System.currentTimeMillis(),
                    rejectionReason = null,
                    adminNotes = notes
                )
                adminRepository.recordAuditLog("MONETIZATION_APPROVE", "Approved monetization application #$applicationId", "admin")
                onResult(true, "Monetization application approved!")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to approve application")
            }
        }
    }

    fun adminRejectMonetization(applicationId: Long, reason: String, notes: String = "", onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch {
            try {
                repository.updateMonetizationStatus(
                    id = applicationId,
                    status = "REJECTED",
                    reviewedAt = System.currentTimeMillis(),
                    rejectionReason = reason,
                    adminNotes = notes
                )
                adminRepository.recordAuditLog("MONETIZATION_REJECT", "Rejected monetization application #$applicationId. Reason: $reason", "admin")
                onResult(true, "Monetization application rejected.")
            } catch (e: Exception) {
                onResult(false, e.message ?: "Failed to reject application")
            }
        }
    }
}
