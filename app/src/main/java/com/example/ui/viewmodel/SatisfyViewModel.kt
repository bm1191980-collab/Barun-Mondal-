package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SatisfyDatabase
import com.example.data.model.*
import com.example.data.repository.AdminRepository
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
    PAGE_DETAILS
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

class SatisfyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: SatisfyRepository
    val adminRepository: AdminRepository

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

    // Comments for active video
    private val _activePostComments = MutableStateFlow<List<CommentEntity>>(emptyList())
    val activePostComments: StateFlow<List<CommentEntity>> = _activePostComments.asStateFlow()

    // User Profile state flow
    val userProfile = MutableStateFlow(UserProfile())

    // Database flows
    val allPosts: StateFlow<List<PostEntity>>
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
            creatorPageDao = database.creatorPageDao()
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

        // Seed initial data
        viewModelScope.launch {
            repository.checkAndSeedInitialData()
            adminRepository.seedAdminInitialData()
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
            repository.recordWatchHistory(post.id)
            loadComments(post.id)
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

    // Uploading New Video/Photo/Short
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
                views = "1 view",
                viewCount = 1L,
                likeCount = 0L,
                dislikeCount = 0L,
                commentCount = 0L,
                timeAgo = "Just now",
                duration = if (type == PostType.SHORT) "0:45" else customDuration,
                isVerified = false,
                isUserCreated = true
            )

            val createdId = repository.createPost(newPost)
            isUploading.value = false
            uploadSuccessMessage.value = when (type) {
                PostType.VIDEO -> "Video uploaded successfully to Satisfy! 🎉"
                PostType.SHORT -> "Satisfy Short published successfully! ⚡"
                PostType.PHOTO -> "Photo shared to Community Feed! 📸"
            }

            // Reset upload form
            uploadTitle.value = ""
            uploadDescription.value = ""
            uploadThumbnailUrl.value = ""
            uploadMediaUrl.value = ""

            // Switch to appropriate tab
            currentTab.value = when (type) {
                PostType.VIDEO -> ScreenTab.HOME
                PostType.SHORT -> ScreenTab.SHORTS
                PostType.PHOTO -> ScreenTab.PHOTOS
            }
        }
    }

    fun dismissSuccessMessage() {
        uploadSuccessMessage.value = null
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
        val hours = totalSeconds / 3600.0
        return if (hours >= 1.0) {
            String.format(java.util.Locale.US, "%.1f ঘণ্টা", hours)
        } else {
            val mins = totalSeconds / 60
            "$mins মিনিট"
        }
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
}
