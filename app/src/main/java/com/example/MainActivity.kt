package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.SatisfyRed
import com.example.ui.theme.SatisfyTheme
import com.example.ui.viewmodel.PlayerState
import com.example.ui.viewmodel.SatisfyViewModel
import com.example.ui.viewmodel.ScreenTab
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: SatisfyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by viewModel.isDarkMode.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val playerState by viewModel.playerState.collectAsStateWithLifecycle()
            val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
            val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

            val allPosts by viewModel.allPosts.collectAsStateWithLifecycle()
            val videoPosts by viewModel.videoPosts.collectAsStateWithLifecycle()
            val shortPosts by viewModel.shortPosts.collectAsStateWithLifecycle()
            val photoPosts by viewModel.photoPosts.collectAsStateWithLifecycle()
            val savedPosts by viewModel.savedPosts.collectAsStateWithLifecycle()
            val userCreatedPosts by viewModel.userCreatedPosts.collectAsStateWithLifecycle()
            val likedPosts by viewModel.likedPosts.collectAsStateWithLifecycle()
            val watchHistory by viewModel.watchHistory.collectAsStateWithLifecycle()
            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val creatorPages by viewModel.creatorPages.collectAsStateWithLifecycle()
            val selectedPage by viewModel.selectedPage.collectAsStateWithLifecycle()
            val selectedPagePosts by viewModel.selectedPagePosts.collectAsStateWithLifecycle()

            val activeComments by viewModel.activePostComments.collectAsStateWithLifecycle()
            val isUploading by viewModel.isUploading.collectAsStateWithLifecycle()
            val uploadProcessingState by viewModel.uploadProcessingState.collectAsStateWithLifecycle()
            val uploadMessage by viewModel.uploadSuccessMessage.collectAsStateWithLifecycle()
            val uploadType by viewModel.uploadType.collectAsStateWithLifecycle()

            // Admin state flows
            val isAdminAuthenticated by viewModel.isAdminAuthenticated.collectAsStateWithLifecycle()
            val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()
            val authError by viewModel.authError.collectAsStateWithLifecycle()
            val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()
            val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
            val allReports by viewModel.allReports.collectAsStateWithLifecycle()
            val pushNotifications by viewModel.pushNotifications.collectAsStateWithLifecycle()
            val appSettings by viewModel.appSettings.collectAsStateWithLifecycle()
            val auditLogs by viewModel.auditLogs.collectAsStateWithLifecycle()

            // Pro Membership, Wallet & Chat state flows
            val isUserPro by viewModel.isUserPro.collectAsStateWithLifecycle()
            val proSubscription by viewModel.proSubscription.collectAsStateWithLifecycle()
            val userWallet by viewModel.userWallet.collectAsStateWithLifecycle()
            val walletTransactions by viewModel.walletTransactions.collectAsStateWithLifecycle()
            val userWithdrawals by viewModel.userWithdrawals.collectAsStateWithLifecycle()
            val userReferrals by viewModel.userReferrals.collectAsStateWithLifecycle()
            val ownerChatInfo by viewModel.ownerChatInfo.collectAsStateWithLifecycle()
            val ownerChatMessages by viewModel.ownerChatMessages.collectAsStateWithLifecycle()

            // Admin Pro Management flows
            val allProSubscriptions by viewModel.allProSubscriptions.collectAsStateWithLifecycle()
            val allReferrals by viewModel.allReferrals.collectAsStateWithLifecycle()
            val allWallets by viewModel.allWallets.collectAsStateWithLifecycle()
            val allWithdrawals by viewModel.allWithdrawals.collectAsStateWithLifecycle()
            val allOwnerChats by viewModel.allOwnerChats.collectAsStateWithLifecycle()
            val adminActiveChatUserId by viewModel.adminActiveChatUserId.collectAsStateWithLifecycle()
            val adminActiveChatMessages by viewModel.adminActiveChatMessages.collectAsStateWithLifecycle()

            // Creator Shorts Analytics & Monetization flows
            val creatorAnalyticsSummary by viewModel.creatorAnalyticsSummary.collectAsStateWithLifecycle()
            val monetizationEligibility by viewModel.monetizationEligibility.collectAsStateWithLifecycle()
            val userMonetizationApplication by viewModel.userMonetizationApplication.collectAsStateWithLifecycle()
            val allMonetizationApplications by viewModel.allMonetizationApplications.collectAsStateWithLifecycle()

            var showCommentSheetForPost by remember { mutableStateOf<PostEntity?>(null) }
            var showAdminAuthDialog by remember { mutableStateOf(false) }
            var showCreatePageDialog by remember { mutableStateOf(false) }
            var showSettingsDialog by remember { mutableStateOf(false) }

            // Handle back navigation
            BackHandler(enabled = playerState.isExpanded || currentTab == ScreenTab.SEARCH || currentTab == ScreenTab.ADMIN || currentTab == ScreenTab.PAGE_DETAILS || currentTab == ScreenTab.PRO_MEMBERSHIP || currentTab == ScreenTab.WALLET || currentTab == ScreenTab.OWNER_CHAT || currentTab == ScreenTab.CREATOR_ANALYTICS || currentTab == ScreenTab.MONETIZATION || currentTab == ScreenTab.SATISFY_RULES || showCommentSheetForPost != null) {
                if (showCommentSheetForPost != null) {
                    showCommentSheetForPost = null
                } else if (playerState.isExpanded) {
                    viewModel.minimizePlayer()
                } else if (currentTab == ScreenTab.PAGE_DETAILS || currentTab == ScreenTab.ADMIN || currentTab == ScreenTab.PRO_MEMBERSHIP || currentTab == ScreenTab.WALLET || currentTab == ScreenTab.OWNER_CHAT || currentTab == ScreenTab.CREATOR_ANALYTICS || currentTab == ScreenTab.MONETIZATION || currentTab == ScreenTab.SATISFY_RULES) {
                    viewModel.currentTab.value = ScreenTab.PROFILE
                } else if (currentTab == ScreenTab.SEARCH) {
                    viewModel.currentTab.value = ScreenTab.HOME
                }
            }

            // Upload success snackbar auto dismiss
            LaunchedEffect(uploadMessage) {
                if (uploadMessage != null) {
                    delay(3500L)
                    viewModel.dismissSuccessMessage()
                }
            }

            SatisfyTheme(darkTheme = isDarkTheme) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = {
                            if (currentTab != ScreenTab.SHORTS && currentTab != ScreenTab.SEARCH && currentTab != ScreenTab.ADMIN && !playerState.isExpanded) {
                                SatisfyTopBar(
                                    avatarUrl = userProfile.avatarUrl,
                                    onSearchClick = { viewModel.currentTab.value = ScreenTab.SEARCH },
                                    onProfileClick = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                    isDarkTheme = isDarkTheme,
                                    onToggleTheme = { viewModel.isDarkMode.value = !isDarkTheme }
                                )
                            }
                        },
                        bottomBar = {
                            if (!playerState.isExpanded && currentTab != ScreenTab.ADMIN) {
                                Column {
                                    // Mini Player if active and minimized
                                    if (playerState.isMiniPlayerVisible && playerState.activePost != null) {
                                        MiniPlayer(
                                            playerState = playerState,
                                            onExpand = { viewModel.expandPlayer() },
                                            onTogglePlayPause = { viewModel.togglePlayPause() },
                                            onClose = { viewModel.closePlayer() }
                                        )
                                    }

                                    SatisfyBottomNavigation(
                                        currentTab = currentTab,
                                        onTabSelected = { tab ->
                                            viewModel.currentTab.value = tab
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (currentTab == ScreenTab.ADMIN) PaddingValues(0.dp) else innerPadding)
                        ) {
                            when (currentTab) {
                                ScreenTab.HOME -> {
                                    HomeScreen(
                                        posts = allPosts,
                                        shortPosts = shortPosts,
                                        selectedCategory = selectedCategory,
                                        categories = viewModel.categories,
                                        onSelectCategory = { viewModel.selectedCategory.value = it },
                                        onVideoClick = { video -> viewModel.openVideo(video, expanded = true) },
                                        onShortClick = { short ->
                                            viewModel.currentTab.value = ScreenTab.SHORTS
                                        },
                                        onToggleLike = { post -> viewModel.toggleLike(post) },
                                        onToggleSave = { post -> viewModel.toggleSave(post) },
                                        onDeletePost = { post -> viewModel.deletePost(post) }
                                    )
                                }
                                ScreenTab.SHORTS -> {
                                    ShortsScreen(
                                        shorts = shortPosts,
                                        onToggleLike = { short -> viewModel.toggleLike(short) },
                                        onToggleDislike = { short -> viewModel.toggleDislike(short) },
                                        onToggleSubscribe = { ch, isSub -> viewModel.toggleSubscribe(ch, isSub) },
                                        onOpenComments = { short ->
                                            viewModel.loadComments(short.id)
                                            showCommentSheetForPost = short
                                        },
                                        onUploadShortClick = {
                                            viewModel.uploadType.value = PostType.SHORT
                                            viewModel.currentTab.value = ScreenTab.CREATE
                                        }
                                    )
                                }
                                ScreenTab.CREATE -> {
                                    UploadScreen(
                                        categories = viewModel.categories,
                                        initialType = uploadType,
                                        uploadProcessingState = uploadProcessingState,
                                        onDismissProcessingModal = { viewModel.resetUploadState() },
                                        onPublish = { type, title, desc, cat, tags, thumb, media, duration ->
                                            viewModel.submitUpload(
                                                type = type,
                                                title = title,
                                                description = desc,
                                                category = cat,
                                                tags = tags,
                                                thumbnailUrl = thumb,
                                                mediaUrl = media,
                                                customDuration = duration
                                            )
                                        },
                                        isUploading = isUploading
                                    )
                                }
                                ScreenTab.PHOTOS -> {
                                    PhotoFeedScreen(
                                        photos = photoPosts,
                                        onToggleLike = { photo -> viewModel.toggleLike(photo) },
                                        onToggleSave = { photo -> viewModel.toggleSave(photo) },
                                        onToggleSubscribe = { ch, isSub -> viewModel.toggleSubscribe(ch, isSub) },
                                        onOpenComments = { photo ->
                                            viewModel.loadComments(photo.id)
                                            showCommentSheetForPost = photo
                                        },
                                        onUploadPhotoClick = {
                                            viewModel.uploadType.value = PostType.PHOTO
                                            viewModel.currentTab.value = ScreenTab.CREATE
                                        }
                                    )
                                }
                                ScreenTab.PROFILE -> {
                                    ProfileScreen(
                                        userProfile = userProfile,
                                        userUploadedPosts = userCreatedPosts,
                                        likedPosts = likedPosts,
                                        savedPosts = savedPosts,
                                        watchHistory = watchHistory,
                                        creatorPages = creatorPages,
                                        isAdmin = isAdminAuthenticated,
                                        isPro = isUserPro,
                                        proExpiresAt = proSubscription?.expiresAt ?: 0L,
                                        walletBalance = userWallet?.referralBalance ?: 0.0,
                                        referralCode = userProfile.referralCode,
                                        onNavigateToPro = { viewModel.currentTab.value = ScreenTab.PRO_MEMBERSHIP },
                                        onNavigateToWallet = { viewModel.currentTab.value = ScreenTab.WALLET },
                                        onNavigateToOwnerChat = { viewModel.currentTab.value = ScreenTab.OWNER_CHAT },
                                        onNavigateToAnalytics = { viewModel.currentTab.value = ScreenTab.CREATOR_ANALYTICS },
                                        onNavigateToMonetization = { viewModel.currentTab.value = ScreenTab.MONETIZATION },
                                        onNavigateToRules = { viewModel.currentTab.value = ScreenTab.SATISFY_RULES },
                                        onUpdateAvatarUri = { uri -> viewModel.updateProfileAvatarUri(uri) },
                                        onUpdateBannerUri = { uri -> viewModel.updateProfileBannerUri(uri) },
                                        onResetBanner = { viewModel.resetProfileBanner() },
                                        onResetAvatar = { viewModel.resetProfileAvatar() },
                                        onUpdateProfileInfo = { name, handle, bio, link ->
                                            viewModel.updateProfileInfo(name, handle, bio, link)
                                        },
                                        onOpenCreatePage = { showCreatePageDialog = true },
                                        onOpenPageDetails = { page -> viewModel.openPageDetails(page) },
                                        onOpenSettings = { showSettingsDialog = true },
                                        onSelectPost = { post ->
                                            if (post.type == PostType.SHORT) {
                                                viewModel.currentTab.value = ScreenTab.SHORTS
                                            } else {
                                                viewModel.openVideo(post, expanded = true)
                                            }
                                        },
                                        onDeletePost = { post -> viewModel.deletePost(post) },
                                        onClearHistory = { viewModel.clearWatchHistory() },
                                        onUploadClick = { viewModel.currentTab.value = ScreenTab.CREATE },
                                        onOpenAdminConsole = { viewModel.currentTab.value = ScreenTab.ADMIN },
                                        onRequestAdminLogin = { showAdminAuthDialog = true }
                                    )
                                }
                                ScreenTab.PRO_MEMBERSHIP -> {
                                    ProMembershipScreen(
                                        isPro = isUserPro,
                                        subscription = proSubscription,
                                        wallet = userWallet,
                                        userReferralCode = userProfile.referralCode,
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                        onPurchasePro = { referrerCode, paymentMethod, onResult ->
                                            viewModel.purchaseProSubscription(referrerCode, paymentMethod, onResult)
                                        },
                                        onOpenWallet = { viewModel.currentTab.value = ScreenTab.WALLET },
                                        onOpenOwnerChat = { viewModel.currentTab.value = ScreenTab.OWNER_CHAT }
                                    )
                                }
                                ScreenTab.WALLET -> {
                                    WalletScreen(
                                        wallet = userWallet,
                                        transactions = walletTransactions,
                                        withdrawals = userWithdrawals,
                                        referrals = userReferrals,
                                        userReferralCode = userProfile.referralCode,
                                        isPro = isUserPro,
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                        onRequestWithdrawal = { amount, method, details, name, onResult ->
                                            viewModel.submitWithdrawalRequest(amount, method, details, name, onResult)
                                        },
                                        onNavigateToPro = { viewModel.currentTab.value = ScreenTab.PRO_MEMBERSHIP }
                                    )
                                }
                                ScreenTab.OWNER_CHAT -> {
                                    OwnerChatScreen(
                                        isPro = isUserPro,
                                        chat = ownerChatInfo,
                                        messages = ownerChatMessages,
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                        onSendMessage = { text -> viewModel.sendUserChatMessage(text) },
                                        onNavigateToPro = { viewModel.currentTab.value = ScreenTab.PRO_MEMBERSHIP }
                                    )
                                }
                                ScreenTab.PAGE_DETAILS -> {
                                    val activePage = selectedPage ?: creatorPages.firstOrNull()
                                    if (activePage != null) {
                                        CreatorPageDetailScreen(
                                            page = activePage,
                                            allVideos = if (selectedPagePosts.isNotEmpty()) selectedPagePosts else videoPosts,
                                            onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                            onVideoClick = { video -> viewModel.openVideo(video, expanded = true) },
                                            onUpdateAvatarUri = { pageId, uri -> viewModel.updatePageAvatarUri(pageId, uri) },
                                            onUpdateBannerUri = { pageId, uri -> viewModel.updatePageBannerUri(pageId, uri) },
                                            onEditPageInfo = { pageId, name, cat, desc, link ->
                                                viewModel.updateCreatorPage(pageId, name, cat, desc, link)
                                            },
                                            onDeletePage = { page -> viewModel.deleteCreatorPage(page) },
                                            onUploadToPage = {
                                                viewModel.uploadType.value = PostType.VIDEO
                                                viewModel.currentTab.value = ScreenTab.CREATE
                                            }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("Page not found")
                                        }
                                    }
                                }
                                ScreenTab.SEARCH -> {
                                    SearchScreen(
                                        query = searchQuery,
                                        onQueryChange = { viewModel.searchQuery.value = it },
                                        allPosts = allPosts,
                                        onSelectPost = { post ->
                                            if (post.type == PostType.SHORT) {
                                                viewModel.currentTab.value = ScreenTab.SHORTS
                                            } else {
                                                viewModel.openVideo(post, expanded = true)
                                            }
                                        },
                                        onBack = { viewModel.currentTab.value = ScreenTab.HOME }
                                    )
                                }
                                ScreenTab.ADMIN -> {
                                    AdminDashboardScreen(
                                        currentAdmin = currentAdmin,
                                        allUsers = allUsers,
                                        allPosts = allPosts,
                                        allReports = allReports,
                                        pushNotifications = pushNotifications,
                                        appSettings = appSettings,
                                        auditLogs = auditLogs,
                                        categories = viewModel.categories,
                                        proSubscriptions = allProSubscriptions,
                                        referrals = allReferrals,
                                        wallets = allWallets,
                                        withdrawals = allWithdrawals,
                                        ownerChats = allOwnerChats,
                                        activeChatMessages = adminActiveChatMessages,
                                        activeChatUserId = adminActiveChatUserId,
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                        onLogout = { viewModel.logoutAdmin() },
                                        onBanUser = { uid, reason -> viewModel.banUser(uid, reason) },
                                        onUnbanUser = { uid -> viewModel.unbanUser(uid) },
                                        onUpdateUserRole = { uid, role -> viewModel.updateUserRole(uid, role) },
                                        onAddAdminUser = { name, email, role -> viewModel.addAdminUser(name, email, role) },
                                        onDeleteUser = { user -> viewModel.deleteUser(user) },
                                        onDeletePost = { post -> viewModel.adminDeletePost(post) },
                                        onEditPost = { post, title, desc, cat, tags, dur, thumb ->
                                            viewModel.adminEditPost(post, title, desc, cat, tags, dur, thumb)
                                        },
                                        onToggleFeatured = { post -> viewModel.adminToggleFeatured(post) },
                                        onToggleFlagged = { post -> viewModel.adminToggleFlagged(post) },
                                        onTogglePostPremium = { post -> viewModel.togglePostPremiumStatus(post) },
                                        onApproveVideo = { post, notes -> viewModel.adminApproveVideo(post, notes) },
                                        onRejectVideo = { post, reason -> viewModel.adminRejectVideo(post, reason) },
                                        onResolveReport = { report, action, delPost, banUser ->
                                            viewModel.resolveReport(report, action, delPost, banUser)
                                        },
                                        onDismissReport = { reportId -> viewModel.dismissReport(reportId) },
                                        onSendPushBroadcast = { title, body, topic, aud, url ->
                                            viewModel.sendPushBroadcast(title, body, topic, aud, url)
                                        },
                                        onSaveAppSettings = { settings -> viewModel.saveAppSettings(settings) },
                                        onSelectChatUser = { uid -> viewModel.selectAdminChatUser(uid) },
                                        onSendChatReply = { uid, msg -> viewModel.sendAdminChatMessage(uid, msg) },
                                        onToggleBlockChatUser = { uid, blocked -> viewModel.toggleBlockChatUser(uid, blocked) },
                                        onApproveWithdrawal = { id, ref, notes -> viewModel.adminApproveWithdrawal(id, ref, notes) },
                                        onRejectWithdrawal = { id, reason, notes -> viewModel.adminRejectWithdrawal(id, reason, notes) },
                                        onToggleFreezeWallet = { uid, frozen, reason -> viewModel.adminToggleFreezeWallet(uid, frozen, reason) },
                                        onToggleSuspiciousReferral = { id, susp, reason -> viewModel.adminToggleSuspiciousReferral(id, susp, reason) },
                                        onReverseReferralReward = { id, reason -> viewModel.adminReverseReferralReward(id, reason) },
                                        onCancelSubscription = { id -> viewModel.adminCancelProSubscription(id) },
                                        monetizationApplications = allMonetizationApplications,
                                        onApproveMonetization = { id, notes -> viewModel.adminApproveMonetization(id, notes) },
                                        onRejectMonetization = { id, reason, notes -> viewModel.adminRejectMonetization(id, reason, notes) }
                                    )
                                }
                                ScreenTab.CREATOR_ANALYTICS -> {
                                    CreatorAnalyticsScreen(
                                        summary = creatorAnalyticsSummary,
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE },
                                        onOpenShort = { short ->
                                            viewModel.currentTab.value = ScreenTab.SHORTS
                                        },
                                        onOpenVideo = { video ->
                                            viewModel.openVideo(video, expanded = true)
                                        }
                                    )
                                }
                                ScreenTab.MONETIZATION -> {
                                    MonetizationScreen(
                                        eligibility = monetizationEligibility,
                                        application = userMonetizationApplication,
                                        onSubmitApplication = { chName, chHandle, chAvatar ->
                                            viewModel.submitMonetizationApplication(chName, chHandle, chAvatar)
                                        },
                                        onOpenRules = { viewModel.currentTab.value = ScreenTab.SATISFY_RULES },
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE }
                                    )
                                }
                                ScreenTab.SATISFY_RULES -> {
                                    SatisfyRulesScreen(
                                        onBack = { viewModel.currentTab.value = ScreenTab.PROFILE }
                                    )
                                }
                            }
                        }
                    }

                    // Admin Auth Modal Dialog
                    if (showAdminAuthDialog) {
                        AdminAuthDialog(
                            isLoading = isAuthLoading,
                            errorMessage = authError,
                            onDismiss = { showAdminAuthDialog = false },
                            onLogin = { email, pass ->
                                viewModel.loginAdmin(email, pass) { success ->
                                    if (success) {
                                        showAdminAuthDialog = false
                                        viewModel.currentTab.value = ScreenTab.ADMIN
                                    }
                                }
                            }
                        )
                    }

                    // Full Screen Video Player Overlay
                    AnimatedVisibility(
                        visible = playerState.isExpanded && playerState.activePost != null,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            VideoDetailScreen(
                                playerState = playerState,
                                relatedVideos = videoPosts,
                                comments = activeComments,
                                onTogglePlayPause = { viewModel.togglePlayPause() },
                                onSeek = { pos -> viewModel.seekTo(pos) },
                                onSeekRelative = { delta -> viewModel.seekRelative(delta) },
                                onToggleMute = { viewModel.toggleMute() },
                                onSpeedChange = { speed -> viewModel.setPlaybackSpeed(speed) },
                                onQualityChange = { quality -> viewModel.setQuality(quality) },
                                onMinimize = { viewModel.minimizePlayer() },
                                onClose = { viewModel.closePlayer() },
                                onToggleControls = { viewModel.toggleControlsVisibility() },
                                onToggleLike = { p -> viewModel.toggleLike(p) },
                                onToggleDislike = { p -> viewModel.toggleDislike(p) },
                                onToggleSave = { p -> viewModel.toggleSave(p) },
                                onToggleSubscribe = { ch, isSub -> viewModel.toggleSubscribe(ch, isSub) },
                                onOpenComments = {
                                    val current = playerState.activePost
                                    if (current != null) {
                                        viewModel.loadComments(current.id)
                                        showCommentSheetForPost = current
                                    }
                                },
                                onSelectRelatedVideo = { newPost ->
                                    viewModel.openVideo(newPost, expanded = true)
                                },
                                onWatchTimeTick = { delta ->
                                    viewModel.recordPlaybackProgress(delta)
                                }
                            )
                        }
                    }

                    // Create Creator Page Dialog
                    if (showCreatePageDialog) {
                        CreatePageDialog(
                            onDismiss = { showCreatePageDialog = false },
                            onCreatePage = { name, category, description, handle, link, avatarUri, bannerUri ->
                                viewModel.createCreatorPage(
                                    name = name,
                                    category = category,
                                    description = description,
                                    handle = handle,
                                    link = link,
                                    avatarUri = avatarUri,
                                    bannerUri = bannerUri,
                                    onSuccess = { newPage ->
                                        showCreatePageDialog = false
                                        viewModel.openPageDetails(newPage)
                                    }
                                )
                            }
                        )
                    }

                    // Settings Dialog (Page Creation, Video Preferences, Dark Mode, Cache)
                    if (showSettingsDialog) {
                        SettingsDialog(
                            onDismiss = { showSettingsDialog = false },
                            pages = creatorPages,
                            onOpenCreatePage = { showCreatePageDialog = true },
                            onOpenPageDetails = { page -> viewModel.openPageDetails(page) },
                            isDarkTheme = isDarkTheme,
                            onToggleDarkTheme = { viewModel.isDarkMode.value = !isDarkTheme },
                            onClearHistory = { viewModel.clearWatchHistory() },
                            onOpenAdminConsole = {
                                if (isAdminAuthenticated) {
                                    viewModel.currentTab.value = ScreenTab.ADMIN
                                } else {
                                    showAdminAuthDialog = true
                                }
                            },
                            isAdmin = isAdminAuthenticated
                        )
                    }

                    // Comments Bottom Sheet Modal
                    if (showCommentSheetForPost != null) {
                        CommentBottomSheet(
                            comments = activeComments,
                            onAddComment = { text ->
                                showCommentSheetForPost?.let { p ->
                                    viewModel.addComment(p.id, text)
                                }
                            },
                            onToggleCommentLike = { comment ->
                                viewModel.toggleCommentLike(comment)
                            },
                            onDismiss = { showCommentSheetForPost = null }
                        )
                    }

                    // Upload Success Floating Toast
                    if (uploadMessage != null) {
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = Color(0xFF1E293B),
                            tonalElevation = 8.dp,
                            shadowElevation = 10.dp,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = uploadMessage ?: "",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
