package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*

enum class AdminTab {
    ANALYTICS,
    MONETIZATION,
    PRO_SYSTEM,
    REFERRALS,
    WITHDRAWALS,
    OWNER_CHATS,
    VERIFICATION,
    USERS,
    POSTS,
    REPORTS,
    NOTIFICATIONS,
    SETTINGS,
    AUDIT_LOGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    currentAdmin: AdminAuthUser?,
    allUsers: List<UserAccountEntity>,
    allPosts: List<PostEntity>,
    allReports: List<ReportEntity>,
    pushNotifications: List<PushNotificationLogEntity>,
    appSettings: AppSystemSettingsEntity?,
    auditLogs: List<AdminAuditLogEntity>,
    categories: List<String>,
    proSubscriptions: List<ProSubscriptionEntity> = emptyList(),
    referrals: List<ReferralEntity> = emptyList(),
    wallets: List<WalletEntity> = emptyList(),
    withdrawals: List<WithdrawalRequestEntity> = emptyList(),
    ownerChats: List<OwnerChatEntity> = emptyList(),
    monetizationApplications: List<MonetizationApplicationEntity> = emptyList(),
    activeChatMessages: List<ChatMessageEntity> = emptyList(),
    activeChatUserId: String? = null,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onBanUser: (String, String) -> Unit,
    onUnbanUser: (String) -> Unit,
    onUpdateUserRole: (String, String) -> Unit,
    onAddAdminUser: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteUser: (UserAccountEntity) -> Unit,
    onDeletePost: (PostEntity) -> Unit,
    onEditPost: (PostEntity, String, String, String, String, String, String) -> Unit,
    onToggleFeatured: (PostEntity) -> Unit,
    onToggleFlagged: (PostEntity) -> Unit,
    onTogglePostPremium: (PostEntity) -> Unit = {},
    onApproveVideo: (PostEntity, String) -> Unit = { _, _ -> },
    onRejectVideo: (PostEntity, String) -> Unit = { _, _ -> },
    onResolveReport: (ReportEntity, String, Boolean, Boolean) -> Unit,
    onDismissReport: (Long) -> Unit,
    onSendPushBroadcast: (String, String, String, String, String) -> Unit,
    onSaveAppSettings: (AppSystemSettingsEntity) -> Unit,
    onSelectChatUser: (String) -> Unit = {},
    onSendChatReply: (String, String) -> Unit = { _, _ -> },
    onToggleBlockChatUser: (String, Boolean) -> Unit = { _, _ -> },
    onApproveWithdrawal: (Long, String, String) -> Unit = { _, _, _ -> },
    onRejectWithdrawal: (Long, String, String) -> Unit = { _, _, _ -> },
    onToggleFreezeWallet: (String, Boolean, String) -> Unit = { _, _, _ -> },
    onToggleSuspiciousReferral: (Long, Boolean, String) -> Unit = { _, _, _ -> },
    onReverseReferralReward: (Long, String) -> Unit = { _, _ -> },
    onCancelSubscription: (Long) -> Unit = {},
    onApproveMonetization: (Long, String) -> Unit = { _, _ -> },
    onRejectMonetization: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    var selectedTab by remember { mutableStateOf(AdminTab.ANALYTICS) }
    val pendingReportCount = remember(allReports) { allReports.count { it.status == "PENDING" } }
    val pendingVideoCount = remember(allPosts) { allPosts.count { it.status == "PENDING" || (it.isUserCreated && !it.isVerified && it.status != "REJECTED" && it.status != "APPROVED") } }
    val pendingWithdrawalCount = remember(withdrawals) { withdrawals.count { it.status == "PENDING" } }
    val unreadChatCount = remember(ownerChats) { ownerChats.sumOf { it.unreadCountForAdmin } }
    val pendingMonetizationCount = remember(monetizationApplications) { monetizationApplications.count { it.status == "PENDING" } }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column {
                    // Top Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("admin_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to App",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFF9333EA)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Admin Console",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "SUPER ADMIN",
                                        color = Color(0xFF10B981),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = currentAdmin?.email ?: "admin@satisfy.app",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Logout Button
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.testTag("admin_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Logout,
                                contentDescription = "Sign Out Admin",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }

                    // Navigation Tabs
                    ScrollableTabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        edgePadding = 12.dp,
                        containerColor = Color.Transparent,
                        contentColor = SatisfyRed,
                        divider = {}
                    ) {
                        TabItem(
                            title = "Analytics",
                            icon = Icons.Filled.Analytics,
                            selected = selectedTab == AdminTab.ANALYTICS,
                            onClick = { selectedTab = AdminTab.ANALYTICS }
                        )
                        TabItem(
                            title = "Monetization",
                            icon = Icons.Filled.MonetizationOn,
                            badgeCount = pendingMonetizationCount,
                            isBadgeAlert = pendingMonetizationCount > 0,
                            selected = selectedTab == AdminTab.MONETIZATION,
                            onClick = { selectedTab = AdminTab.MONETIZATION }
                        )
                        TabItem(
                            title = "PRO System",
                            icon = Icons.Filled.WorkspacePremium,
                            badgeCount = proSubscriptions.size,
                            selected = selectedTab == AdminTab.PRO_SYSTEM,
                            onClick = { selectedTab = AdminTab.PRO_SYSTEM }
                        )
                        TabItem(
                            title = "Referrals",
                            icon = Icons.Filled.CardGiftcard,
                            badgeCount = referrals.size,
                            selected = selectedTab == AdminTab.REFERRALS,
                            onClick = { selectedTab = AdminTab.REFERRALS }
                        )
                        TabItem(
                            title = "Withdrawals",
                            icon = Icons.Filled.AccountBalance,
                            badgeCount = pendingWithdrawalCount,
                            isBadgeAlert = pendingWithdrawalCount > 0,
                            selected = selectedTab == AdminTab.WITHDRAWALS,
                            onClick = { selectedTab = AdminTab.WITHDRAWALS }
                        )
                        TabItem(
                            title = "VIP Chats",
                            icon = Icons.Filled.Forum,
                            badgeCount = unreadChatCount,
                            isBadgeAlert = unreadChatCount > 0,
                            selected = selectedTab == AdminTab.OWNER_CHATS,
                            onClick = { selectedTab = AdminTab.OWNER_CHATS }
                        )
                        TabItem(
                            title = "Verification",
                            icon = Icons.Filled.FactCheck,
                            badgeCount = pendingVideoCount,
                            isBadgeAlert = pendingVideoCount > 0,
                            selected = selectedTab == AdminTab.VERIFICATION,
                            onClick = { selectedTab = AdminTab.VERIFICATION }
                        )
                        TabItem(
                            title = "Users",
                            icon = Icons.Filled.People,
                            badgeCount = allUsers.size,
                            selected = selectedTab == AdminTab.USERS,
                            onClick = { selectedTab = AdminTab.USERS }
                        )
                        TabItem(
                            title = "Posts",
                            icon = Icons.Filled.VideoLibrary,
                            badgeCount = allPosts.size,
                            selected = selectedTab == AdminTab.POSTS,
                            onClick = { selectedTab = AdminTab.POSTS }
                        )
                        TabItem(
                            title = "Reports",
                            icon = Icons.Filled.Report,
                            badgeCount = pendingReportCount,
                            isBadgeAlert = pendingReportCount > 0,
                            selected = selectedTab == AdminTab.REPORTS,
                            onClick = { selectedTab = AdminTab.REPORTS }
                        )
                        TabItem(
                            title = "Push FCM",
                            icon = Icons.Filled.NotificationsActive,
                            selected = selectedTab == AdminTab.NOTIFICATIONS,
                            onClick = { selectedTab = AdminTab.NOTIFICATIONS }
                        )
                        TabItem(
                            title = "Settings",
                            icon = Icons.Filled.Settings,
                            selected = selectedTab == AdminTab.SETTINGS,
                            onClick = { selectedTab = AdminTab.SETTINGS }
                        )
                        TabItem(
                            title = "Audit Logs",
                            icon = Icons.Filled.History,
                            selected = selectedTab == AdminTab.AUDIT_LOGS,
                            onClick = { selectedTab = AdminTab.AUDIT_LOGS }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (selectedTab) {
                AdminTab.ANALYTICS -> AdminAnalyticsView(
                    allUsers = allUsers,
                    allPosts = allPosts,
                    allReports = allReports,
                    pushNotifications = pushNotifications,
                    auditLogs = auditLogs,
                    onNavigateToTab = { selectedTab = it }
                )
                AdminTab.MONETIZATION -> AdminMonetizationTabContent(
                    applications = monetizationApplications,
                    onApprove = onApproveMonetization,
                    onReject = onRejectMonetization
                )
                AdminTab.PRO_SYSTEM -> AdminProTabContent(
                    subscriptions = proSubscriptions,
                    onCancelSubscription = onCancelSubscription
                )
                AdminTab.REFERRALS -> AdminReferralsTabContent(
                    referrals = referrals,
                    onToggleSuspicious = onToggleSuspiciousReferral,
                    onReverseReward = onReverseReferralReward
                )
                AdminTab.WITHDRAWALS -> AdminWithdrawalsTabContent(
                    withdrawals = withdrawals,
                    onApproveWithdrawal = onApproveWithdrawal,
                    onRejectWithdrawal = onRejectWithdrawal,
                    onToggleFreezeWallet = onToggleFreezeWallet
                )
                AdminTab.OWNER_CHATS -> AdminOwnerChatsTabContent(
                    chats = ownerChats,
                    activeChatUserId = activeChatUserId,
                    messages = activeChatMessages,
                    onSelectChat = onSelectChatUser,
                    onSendReply = onSendChatReply,
                    onToggleBlockUser = onToggleBlockChatUser
                )
                AdminTab.VERIFICATION -> AdminVerificationView(
                    allPosts = allPosts,
                    onApproveVideo = onApproveVideo,
                    onRejectVideo = onRejectVideo,
                    onDeletePost = onDeletePost
                )
                AdminTab.USERS -> AdminUsersView(
                    users = allUsers,
                    onBanUser = onBanUser,
                    onUnbanUser = onUnbanUser,
                    onUpdateRole = onUpdateUserRole,
                    onAddAdminUser = onAddAdminUser,
                    onDeleteUser = onDeleteUser
                )
                AdminTab.POSTS -> AdminPostsView(
                    posts = allPosts,
                    categories = categories,
                    onDeletePost = onDeletePost,
                    onEditPost = onEditPost,
                    onToggleFeatured = onToggleFeatured,
                    onToggleFlagged = onToggleFlagged,
                    onTogglePremium = onTogglePostPremium
                )
                AdminTab.REPORTS -> AdminReportsView(
                    reports = allReports,
                    onResolveReport = onResolveReport,
                    onDismissReport = onDismissReport
                )
                AdminTab.NOTIFICATIONS -> AdminPushNotificationsView(
                    notifications = pushNotifications,
                    onSendPush = onSendPushBroadcast
                )
                AdminTab.SETTINGS -> AdminSettingsView(
                    settings = appSettings ?: AppSystemSettingsEntity(),
                    onSaveSettings = onSaveAppSettings
                )
                AdminTab.AUDIT_LOGS -> AdminAuditLogsView(logs = auditLogs)
            }
        }
    }
}

@Composable
private fun TabItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    badgeCount: Int? = null,
    isBadgeAlert: Boolean = false,
    onClick: () -> Unit
) {
    Tab(
        selected = selected,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) SatisfyRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) SatisfyRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (badgeCount != null && badgeCount > 0) {
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isBadgeAlert) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = badgeCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isBadgeAlert) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 1. ANALYTICS VIEW
// ----------------------------------------------------
@Composable
fun AdminAnalyticsView(
    allUsers: List<UserAccountEntity>,
    allPosts: List<PostEntity>,
    allReports: List<ReportEntity>,
    pushNotifications: List<PushNotificationLogEntity>,
    auditLogs: List<AdminAuditLogEntity>,
    onNavigateToTab: (AdminTab) -> Unit
) {
    val totalVideos = remember(allPosts) { allPosts.count { it.type == PostType.VIDEO } }
    val totalShorts = remember(allPosts) { allPosts.count { it.type == PostType.SHORT } }
    val totalPhotos = remember(allPosts) { allPosts.count { it.type == PostType.PHOTO } }
    val totalViews = remember(allPosts) { allPosts.sumOf { it.viewCount } }
    val totalLikes = remember(allPosts) { allPosts.sumOf { it.likeCount } }
    val pendingReports = remember(allReports) { allReports.count { it.status == "PENDING" } }
    val bannedUsers = remember(allUsers) { allUsers.count { it.isBanned } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Live Status Banner
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Satisfy Production Cluster",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Firebase Auth + Firestore Sync Online • Realtime FCM Active",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "99.99% HEALTH",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // KPI Metric Cards Grid
        item {
            Text(
                text = "Key Metrics",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Users",
                    value = "${allUsers.size}",
                    subtext = "$bannedUsers Banned",
                    icon = Icons.Filled.People,
                    accentColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(AdminTab.USERS) }
                )
                MetricCard(
                    title = "Total Posts",
                    value = "${allPosts.size}",
                    subtext = "$totalVideos V • $totalShorts S • $totalPhotos P",
                    icon = Icons.Filled.VideoLibrary,
                    accentColor = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(AdminTab.POSTS) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Total Views",
                    value = formatViews(totalViews),
                    subtext = "${formatViews(totalLikes)} Likes",
                    icon = Icons.Filled.Visibility,
                    accentColor = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Pending Reports",
                    value = "$pendingReports",
                    subtext = if (pendingReports > 0) "Needs Attention" else "Clean Queue",
                    icon = Icons.Filled.Report,
                    accentColor = if (pendingReports > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateToTab(AdminTab.REPORTS) }
                )
            }
        }

        // Content Distribution Visualizer
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Content Breakdown",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val totalContent = maxOf(1, allPosts.size)
                    val videoPct = (totalVideos * 100) / totalContent
                    val shortPct = (totalShorts * 100) / totalContent
                    val photoPct = (totalPhotos * 100) / totalContent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                    ) {
                        Box(modifier = Modifier.weight(maxOf(1f, totalVideos.toFloat())).fillMaxHeight().background(Color(0xFFE11D48)))
                        Box(modifier = Modifier.weight(maxOf(1f, totalShorts.toFloat())).fillMaxHeight().background(Color(0xFF3B82F6)))
                        Box(modifier = Modifier.weight(maxOf(1f, totalPhotos.toFloat())).fillMaxHeight().background(Color(0xFF10B981)))
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LegendItem(color = Color(0xFFE11D48), label = "Videos ($videoPct%)", count = totalVideos)
                        LegendItem(color = Color(0xFF3B82F6), label = "Shorts ($shortPct%)", count = totalShorts)
                        LegendItem(color = Color(0xFF10B981), label = "Photos ($photoPct%)", count = totalPhotos)
                    }
                }
            }
        }

        // Weekly Views Activity Bar Chart
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Weekly Activity Trend",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "+24.8% this week",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    val days = listOf("Mon" to 0.45f, "Tue" to 0.60f, "Wed" to 0.52f, "Thu" to 0.85f, "Fri" to 0.72f, "Sat" to 0.95f, "Sun" to 1.0f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEach { (day, fraction) ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(20.dp)
                                        .fillMaxHeight(fraction)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFFE11D48), Color(0xFF9333EA))
                                            )
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Admin Actions
        item {
            Text(
                text = "Quick Actions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onNavigateToTab(AdminTab.NOTIFICATIONS) },
                    colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Push FCM", fontSize = 12.sp)
                }
                OutlinedButton(
                    onClick = { onNavigateToTab(AdminTab.REPORTS) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Moderate", fontSize = 12.sp)
                }
            }
        }

        // Recent Audit Activity stream
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Recent Admin Audit Actions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    if (auditLogs.isEmpty()) {
                        Text(
                            text = "No recent actions recorded.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        auditLogs.take(4).forEach { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3B82F6))
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${log.action}: ${log.details}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = log.adminEmail,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label ($count)",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ----------------------------------------------------
// 2. USER MANAGEMENT VIEW
// ----------------------------------------------------
@Composable
fun AdminUsersView(
    users: List<UserAccountEntity>,
    onBanUser: (String, String) -> Unit,
    onUnbanUser: (String) -> Unit,
    onUpdateRole: (String, String) -> Unit,
    onAddAdminUser: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteUser: (UserAccountEntity) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("All") }

    var userToBan by remember { mutableStateOf<UserAccountEntity?>(null) }
    var banReason by remember { mutableStateOf("Violation of Community Guidelines") }

    var userToEditRole by remember { mutableStateOf<UserAccountEntity?>(null) }
    var userToDelete by remember { mutableStateOf<UserAccountEntity?>(null) }
    var showAddAdminDialog by remember { mutableStateOf(false) }

    val filteredUsers = remember(users, searchQuery, selectedRoleFilter) {
        users.filter { user ->
            val matchesSearch = user.name.contains(searchQuery, ignoreCase = true) ||
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.uid.contains(searchQuery, ignoreCase = true)
            val matchesRole = when (selectedRoleFilter) {
                "All" -> true
                "Super Admin" -> user.role == "superadmin"
                "Admin" -> user.role == "admin" || user.role == "superadmin"
                "Moderator" -> user.role == "moderator"
                "Creator" -> user.role == "creator"
                "Banned" -> user.isBanned
                else -> true
            }
            matchesSearch && matchesRole
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search & Filter
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search users by name, email, or UID...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_user_search_field"),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Role Filter Chips
        val roleFilters = listOf("All", "Admin", "Moderator", "Creator", "Banned")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(roleFilters) { role ->
                FilterChip(
                    selected = selectedRoleFilter == role,
                    onClick = { selectedRoleFilter = role },
                    label = { Text(role, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Users (${filteredUsers.size})",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = { showAddAdminDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.testTag("add_admin_button")
            ) {
                Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Add Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredUsers, key = { it.uid }) { user ->
                UserCard(
                    user = user,
                    onBanClick = { userToBan = user },
                    onUnbanClick = { onUnbanUser(user.uid) },
                    onRoleClick = { userToEditRole = user },
                    onDeleteClick = { userToDelete = user }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Add Admin Dialog (Only accessible to existing admins)
    if (showAddAdminDialog) {
        var newAdminName by remember { mutableStateOf("") }
        var newAdminEmail by remember { mutableStateOf("") }
        var newAdminRole by remember { mutableStateOf("admin") }
        var addError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showAddAdminDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Grant Admin Privilege", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Only existing Admins can grant administrative access to new accounts.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newAdminName,
                        onValueChange = { newAdminName = it },
                        label = { Text("Full Name") },
                        placeholder = { Text("e.g. Tanzim Ahmed") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAdminEmail,
                        onValueChange = { newAdminEmail = it },
                        label = { Text("Admin Email Address") },
                        placeholder = { Text("e.g. member@satisfy.app") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Assign Role:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = newAdminRole == "admin",
                            onClick = { newAdminRole = "admin" },
                            label = { Text("Administrator") }
                        )
                        FilterChip(
                            selected = newAdminRole == "moderator",
                            onClick = { newAdminRole = "moderator" },
                            label = { Text("Moderator") }
                        )
                    }

                    if (addError != null) {
                        Text(addError!!, color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAdminEmail.isBlank() || !newAdminEmail.contains("@")) {
                            addError = "Please enter a valid email address."
                        } else {
                            onAddAdminUser(newAdminName.ifBlank { "Admin User" }, newAdminEmail.trim(), newAdminRole)
                            showAddAdminDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Grant Role")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAdminDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Ban User Dialog
    if (userToBan != null) {
        AlertDialog(
            onDismissRequest = { userToBan = null },
            title = { Text("Ban User: ${userToBan?.name}") },
            text = {
                Column {
                    Text("Specify reason for banning this user:", fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    val reasons = listOf(
                        "Violation of Community Guidelines",
                        "Spamming commercial links & bots",
                        "Inappropriate / Graphic media content",
                        "Harassment or abusive comments",
                        "Copyright infringement"
                    )
                    reasons.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { banReason = r }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = banReason == r,
                                onClick = { banReason = r }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(r, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToBan?.let { onBanUser(it.uid, banReason) }
                        userToBan = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Ban")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToBan = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Role Dialog
    if (userToEditRole != null) {
        var selectedRole by remember { mutableStateOf(userToEditRole!!.role) }
        AlertDialog(
            onDismissRequest = { userToEditRole = null },
            title = { Text("Change Role for ${userToEditRole?.name}") },
            text = {
                Column {
                    val roles = listOf("user", "creator", "moderator", "admin")
                    roles.forEach { r ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedRole = r }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRole == r,
                                onClick = { selectedRole = r }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(r.uppercase(), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToEditRole?.let { onUpdateRole(it.uid, selectedRole) }
                        userToEditRole = null
                    }
                ) {
                    Text("Save Role")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEditRole = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete User Dialog
    if (userToDelete != null) {
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text("Delete Account: ${userToDelete?.name}?") },
            text = {
                Text("This action is permanent and will remove all user records and database mappings from Firestore.", fontSize = 13.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToDelete?.let { onDeleteUser(it) }
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Permanently")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserCard(
    user: UserAccountEntity,
    onBanClick: () -> Unit,
    onUnbanClick: () -> Unit,
    onRoleClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isRootSuperAdmin = user.email.equals("bm1191980@gmail.com", ignoreCase = true) || user.role.equals("superadmin", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (user.isBanned) BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = if (user.avatarUrl.isNotBlank()) user.avatarUrl else "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = user.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        RoleBadge(role = user.role)
                        if (user.isBanned) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "BANNED",
                                    color = Color(0xFFEF4444),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = user.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Joined: ${user.joinedDate} • Posts: ${user.postsCount}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }

            if (user.isBanned && user.banReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Ban Reason: ${user.banReason}",
                        fontSize = 11.sp,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRootSuperAdmin) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFE11D48).copy(alpha = 0.12f),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = Color(0xFFE11D48), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Primary Super Admin (Protected)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE11D48))
                        }
                    }
                } else {
                    TextButton(onClick = onRoleClick) {
                        Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Role", fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    if (user.isBanned) {
                        Button(
                            onClick = onUnbanClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Unban", fontSize = 11.sp)
                        }
                    } else {
                        OutlinedButton(
                            onClick = onBanClick,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Ban", fontSize = 11.sp, color = Color(0xFFEF4444))
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDeleteClick) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete User", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun RoleBadge(role: String) {
    val (color, label) = when (role.lowercase()) {
        "superadmin" -> Color(0xFFE11D48) to "SUPER ADMIN"
        "admin" -> Color(0xFFE11D48) to "ADMIN"
        "moderator" -> Color(0xFF8B5CF6) to "MOD"
        "creator" -> Color(0xFF3B82F6) to "CREATOR"
        else -> Color(0xFF64748B) to "USER"
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
        )
    }
}

// ----------------------------------------------------
// 3. POSTS MANAGEMENT VIEW
// ----------------------------------------------------
@Composable
fun AdminPostsView(
    posts: List<PostEntity>,
    categories: List<String>,
    onDeletePost: (PostEntity) -> Unit,
    onEditPost: (PostEntity, String, String, String, String, String, String) -> Unit,
    onToggleFeatured: (PostEntity) -> Unit,
    onToggleFlagged: (PostEntity) -> Unit,
    onTogglePremium: (PostEntity) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("All") }

    var postToEdit by remember { mutableStateOf<PostEntity?>(null) }
    var postToDelete by remember { mutableStateOf<PostEntity?>(null) }

    val filteredPosts = remember(posts, searchQuery, selectedTypeFilter) {
        posts.filter { p ->
            val matchesQuery = p.title.contains(searchQuery, ignoreCase = true) ||
                    p.channelName.contains(searchQuery, ignoreCase = true) ||
                    p.tags.contains(searchQuery, ignoreCase = true)
            val matchesType = when (selectedTypeFilter) {
                "All" -> true
                "Videos" -> p.type == PostType.VIDEO
                "Shorts" -> p.type == PostType.SHORT
                "Photos" -> p.type == PostType.PHOTO
                "Featured" -> p.isFeatured
                "Flagged" -> p.isFlagged
                "PRO Only" -> p.isPremium
                else -> true
            }
            matchesQuery && matchesType
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search posts by title, creator, tags...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        val filters = listOf("All", "Videos", "Shorts", "Photos", "Featured", "Flagged", "PRO Only")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filters) { filter ->
                FilterChip(
                    selected = selectedTypeFilter == filter,
                    onClick = { selectedTypeFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Total Posts (${filteredPosts.size})",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredPosts, key = { it.id }) { post ->
                AdminPostCard(
                    post = post,
                    onEditClick = { postToEdit = post },
                    onDeleteClick = { postToDelete = post },
                    onToggleFeatured = { onToggleFeatured(post) },
                    onToggleFlagged = { onToggleFlagged(post) },
                    onTogglePremium = { onTogglePremium(post) }
                )
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // Edit Post Dialog
    if (postToEdit != null) {
        var editTitle by remember { mutableStateOf(postToEdit!!.title) }
        var editDesc by remember { mutableStateOf(postToEdit!!.description) }
        var editCategory by remember { mutableStateOf(postToEdit!!.category) }
        var editTags by remember { mutableStateOf(postToEdit!!.tags) }
        var editDuration by remember { mutableStateOf(postToEdit!!.duration) }
        var editThumb by remember { mutableStateOf(postToEdit!!.thumbnailUrl) }

        AlertDialog(
            onDismissRequest = { postToEdit = null },
            title = { Text("Edit Post Details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editTags,
                        onValueChange = { editTags = it },
                        label = { Text("Tags") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        postToEdit?.let { p ->
                            onEditPost(p, editTitle, editDesc, editCategory, editTags, editDuration, editThumb)
                        }
                        postToEdit = null
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Post Dialog
    if (postToDelete != null) {
        AlertDialog(
            onDismissRequest = { postToDelete = null },
            title = { Text("Delete Post: ${postToDelete?.title}?") },
            text = { Text("Are you sure you want to permanently delete this content?", fontSize = 13.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        postToDelete?.let { onDeletePost(it) }
                        postToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { postToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AdminPostCard(
    post: PostEntity,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleFeatured: () -> Unit,
    onToggleFlagged: () -> Unit,
    onTogglePremium: () -> Unit = {}
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (post.isFeatured) BorderStroke(1.5.dp, Color(0xFFF59E0B)) else if (post.isPremium) BorderStroke(1.5.dp, SatisfyGold) else null
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail preview
                Box(
                    modifier = Modifier
                        .size(width = 90.dp, height = 60.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = if (post.thumbnailUrl.isNotBlank()) post.thumbnailUrl else "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=400",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = post.duration,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (post.isPremium) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = SatisfyGold.copy(alpha = 0.25f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("PRO ONLY", color = SatisfyGold, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        if (post.isFeatured) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFF59E0B).copy(alpha = 0.2f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("FEATURED", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = post.type.name,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${post.channelName} • ${post.views} • ${post.category}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Toggle Premium / Free button
                IconButton(onClick = onTogglePremium) {
                    Icon(
                        imageVector = if (post.isPremium) Icons.Filled.WorkspacePremium else Icons.Outlined.WorkspacePremium,
                        contentDescription = "Toggle Pro Status",
                        tint = if (post.isPremium) SatisfyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Feature toggle button
                IconButton(onClick = onToggleFeatured) {
                    Icon(
                        imageVector = if (post.isFeatured) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Feature Post",
                        tint = if (post.isFeatured) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Edit button
                IconButton(onClick = onEditClick) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit Post",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Delete button
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Post",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. REPORT MANAGEMENT VIEW
// ----------------------------------------------------
@Composable
fun AdminReportsView(
    reports: List<ReportEntity>,
    onResolveReport: (ReportEntity, String, Boolean, Boolean) -> Unit,
    onDismissReport: (Long) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf("PENDING") }
    var reportToAction by remember { mutableStateOf<ReportEntity?>(null) }

    val filteredReports = remember(reports, selectedStatusFilter) {
        if (selectedStatusFilter == "ALL") reports
        else reports.filter { it.status == selectedStatusFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedStatusFilter == "PENDING",
                onClick = { selectedStatusFilter = "PENDING" },
                label = { Text("Pending (${reports.count { it.status == "PENDING" }})") }
            )
            FilterChip(
                selected = selectedStatusFilter == "RESOLVED",
                onClick = { selectedStatusFilter = "RESOLVED" },
                label = { Text("Resolved (${reports.count { it.status == "RESOLVED" }})") }
            )
            FilterChip(
                selected = selectedStatusFilter == "ALL",
                onClick = { selectedStatusFilter = "ALL" },
                label = { Text("All (${reports.size})") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredReports.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Report Queue Clean!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("No reports matching current filter.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredReports, key = { it.id }) { report ->
                    ReportCard(
                        report = report,
                        onActionClick = { reportToAction = report },
                        onDismissClick = { onDismissReport(report.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Action sheet dialog for report
    if (reportToAction != null) {
        var actionOption by remember { mutableStateOf("DELETE_POST") }
        AlertDialog(
            onDismissRequest = { reportToAction = null },
            title = { Text("Moderate: ${reportToAction?.reason}") },
            text = {
                Column {
                    Text("Target: ${reportToAction?.targetTitle}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Reported User: ${reportToAction?.reportedUser}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select Moderation Action:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))

                    val actions = listOf(
                        "DELETE_POST" to "Delete Target Post & Resolve Report",
                        "BAN_USER" to "Ban Offending User & Delete Content",
                        "RESOLVE_ONLY" to "Mark Resolved (No Deletion)"
                    )
                    actions.forEach { (key, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { actionOption = key }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = actionOption == key, onClick = { actionOption = key })
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(label, fontSize = 12.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        reportToAction?.let { r ->
                            when (actionOption) {
                                "DELETE_POST" -> onResolveReport(r, "Content removed by admin", true, false)
                                "BAN_USER" -> onResolveReport(r, "User banned & content removed", true, true)
                                "RESOLVE_ONLY" -> onResolveReport(r, "Reviewed & approved", false, false)
                            }
                        }
                        reportToAction = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionOption == "RESOLVE_ONLY") Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                ) {
                    Text("Apply Action")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToAction = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ReportCard(
    report: ReportEntity,
    onActionClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = if (report.status == "PENDING") BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)) else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = report.reason.uppercase(),
                            color = Color(0xFFEF4444),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (report.status == "PENDING") Color(0xFFF59E0B).copy(alpha = 0.15f) else Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = report.status,
                            color = if (report.status == "PENDING") Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = report.timeAgo,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Target: ${report.targetTitle}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Reported User: ${report.reportedUser} • By: ${report.reporterName}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (report.details.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Notes: ${report.details}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (report.actionTaken.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Action Taken: ${report.actionTaken}",
                    fontSize = 10.sp,
                    color = Color(0xFF10B981),
                    fontWeight = FontWeight.Bold
                )
            }

            if (report.status == "PENDING") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissClick) {
                        Text("Dismiss", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Take Action", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 5. PUSH NOTIFICATIONS BROADCAST (FCM)
// ----------------------------------------------------
@Composable
fun AdminPushNotificationsView(
    notifications: List<PushNotificationLogEntity>,
    onSendPush: (String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf("all_users") }
    var selectedAudienceLabel by remember { mutableStateOf("All Users") }
    var actionUrl by remember { mutableStateOf("") }
    var showSuccessBanner by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Push Composer Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Campaign, contentDescription = null, tint = SatisfyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FCM Push Notification Broadcast",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    // Audience Selector
                    Text("Target Audience Topic:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedTopic == "all_users",
                            onClick = { selectedTopic = "all_users"; selectedAudienceLabel = "All Users" },
                            label = { Text("All Users", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTopic == "creators",
                            onClick = { selectedTopic = "creators"; selectedAudienceLabel = "Creators" },
                            label = { Text("Creators", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedTopic == "system_alert",
                            onClick = { selectedTopic = "system_alert"; selectedAudienceLabel = "System Alert" },
                            label = { Text("System Alert", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Notification Title") },
                        placeholder = { Text("e.g., 🔥 New Satisfying Video Trending!") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("push_title_field"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Notification Message Body") },
                        placeholder = { Text("Enter the push message delivered to all devices...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("push_body_field"),
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = actionUrl,
                        onValueChange = { actionUrl = it },
                        label = { Text("Deep Link / Action Route (Optional)") },
                        placeholder = { Text("satisfy://feed/featured") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Live Notification Preview
                    if (title.isNotBlank() || body.isNotBlank()) {
                        Text("Live Lockscreen Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SatisfyRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (title.isNotBlank()) title else "Notification Title",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (body.isNotBlank()) body else "Notification message preview...",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Dispatch Button
                    Button(
                        onClick = {
                            if (title.isNotBlank() && body.isNotBlank()) {
                                onSendPush(title, body, selectedTopic, selectedAudienceLabel, actionUrl)
                                showSuccessBanner = true
                                title = ""
                                body = ""
                                actionUrl = ""
                            }
                        },
                        enabled = title.isNotBlank() && body.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("send_push_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Push Broadcast (FCM)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Broadcast History List
        item {
            Text(
                text = "Broadcast History (${notifications.size})",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(notifications, key = { it.id }) { push ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = push.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF3B82F6).copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = push.targetAudienceLabel,
                                    color = Color(0xFF3B82F6),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = push.body,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Delivered to ~${push.deliveredCount} devices • ${push.sentTimeFormatted}",
                            fontSize = 10.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

// ----------------------------------------------------
// 6. APP & SYSTEM SETTINGS VIEW
// ----------------------------------------------------
@Composable
fun AdminSettingsView(
    settings: AppSystemSettingsEntity,
    onSaveSettings: (AppSystemSettingsEntity) -> Unit
) {
    var isMaintenance by remember(settings) { mutableStateOf(settings.isMaintenanceMode) }
    var allowReg by remember(settings) { mutableStateOf(settings.allowNewRegistrations) }
    var autoMod by remember(settings) { mutableStateOf(settings.autoModerationEnabled) }
    var announcement by remember(settings) { mutableStateOf(settings.announcementBanner) }
    var announcementEnabled by remember(settings) { mutableStateOf(settings.announcementEnabled) }
    var maxUploadMb by remember(settings) { mutableStateOf(settings.maxUploadSizeMb.toString()) }
    var guidelinesUrl by remember(settings) { mutableStateOf(settings.communityGuidelinesUrl) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "System Operational Switches",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        item {
            SettingToggleCard(
                title = "Maintenance Mode",
                description = "Temporarily pause user logins and media uploads during scheduled server updates.",
                checked = isMaintenance,
                onCheckedChange = { isMaintenance = it }
            )
        }

        item {
            SettingToggleCard(
                title = "Allow New User Registrations",
                description = "Enable or disable new user account creation on the Satisfy network.",
                checked = allowReg,
                onCheckedChange = { allowReg = it }
            )
        }

        item {
            SettingToggleCard(
                title = "AI Content Auto-Moderation",
                description = "Automatically analyze uploaded images and titles for policy violations.",
                checked = autoMod,
                onCheckedChange = { autoMod = it }
            )
        }

        item {
            // Global Announcement Banner
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Announcement Banner",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Switch(
                            checked = announcementEnabled,
                            onCheckedChange = { announcementEnabled = it }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = announcement,
                        onValueChange = { announcement = it },
                        label = { Text("Banner Message") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2
                    )
                }
            }
        }

        item {
            // Limits & Configs
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Configurations",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = maxUploadMb,
                        onValueChange = { maxUploadMb = it },
                        label = { Text("Max Video Upload Size (MB)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = guidelinesUrl,
                        onValueChange = { guidelinesUrl = it },
                        label = { Text("Community Guidelines URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        }

        item {
            Button(
                onClick = {
                    val updated = settings.copy(
                        isMaintenanceMode = isMaintenance,
                        allowNewRegistrations = allowReg,
                        autoModerationEnabled = autoMod,
                        announcementBanner = announcement,
                        announcementEnabled = announcementEnabled,
                        maxUploadSizeMb = maxUploadMb.toIntOrNull() ?: 500,
                        communityGuidelinesUrl = guidelinesUrl
                    )
                    onSaveSettings(updated)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_settings_button")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save System Settings", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

// ----------------------------------------------------
// 7. AUDIT LOGS VIEW
// ----------------------------------------------------
@Composable
fun AdminAuditLogsView(logs: List<AdminAuditLogEntity>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Audit Logs & Moderation Trail (${logs.size})",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(10.dp))

        if (logs.isEmpty()) {
            Text("No audit log events available.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = log.action,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = log.timeFormatted,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = log.details,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "By: ${log.adminEmail}",
                                    fontSize = 9.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

private fun formatViews(views: Long): String {
    return when {
        views >= 1000000 -> String.format("%.1fM", views / 1000000.0)
        views >= 1000 -> String.format("%.1fK", views / 1000.0)
        else -> views.toString()
    }
}

// ----------------------------------------------------
// 8. VIDEO & CREATOR VERIFICATION VIEW
// ----------------------------------------------------
@Composable
fun AdminVerificationView(
    allPosts: List<PostEntity>,
    onApproveVideo: (PostEntity, String) -> Unit,
    onRejectVideo: (PostEntity, String) -> Unit,
    onDeletePost: (PostEntity) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf("Pending") }
    var searchQuery by remember { mutableStateOf("") }

    var videoToReject by remember { mutableStateOf<PostEntity?>(null) }
    var videoToPreview by remember { mutableStateOf<PostEntity?>(null) }
    var videoToApprove by remember { mutableStateOf<PostEntity?>(null) }

    val pendingPosts = remember(allPosts) {
        allPosts.filter {
            it.status == "PENDING" || (it.isUserCreated && !it.isVerified && it.status != "REJECTED" && it.status != "APPROVED")
        }
    }
    val approvedPosts = remember(allPosts) {
        allPosts.filter { it.status == "APPROVED" || (it.isVerified && it.status != "REJECTED") }
    }
    val rejectedPosts = remember(allPosts) {
        allPosts.filter { it.status == "REJECTED" }
    }

    val filteredList = remember(allPosts, selectedStatusFilter, searchQuery) {
        val baseList = when (selectedStatusFilter) {
            "Pending" -> pendingPosts
            "Approved" -> approvedPosts
            "Rejected" -> rejectedPosts
            else -> allPosts
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.channelName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Queue Overview Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStatusFilter = "Pending" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pending", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${pendingPosts.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStatusFilter = "Approved" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Approved", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${approvedPosts.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStatusFilter = "Rejected" }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Rejected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${rejectedPosts.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search submissions by title, creator, category...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Close, contentDescription = "Clear")
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips
        val statusTabs = listOf("Pending", "Approved", "Rejected", "All")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(statusTabs) { tab ->
                val isSelected = selectedStatusFilter == tab
                val count = when (tab) {
                    "Pending" -> pendingPosts.size
                    "Approved" -> approvedPosts.size
                    "Rejected" -> rejectedPosts.size
                    else -> allPosts.size
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedStatusFilter = tab },
                    label = { Text("$tab ($count)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.FactCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (selectedStatusFilter) {
                            "Pending" -> "No pending videos for review 🎉"
                            "Approved" -> "No approved videos found."
                            "Rejected" -> "No rejected submissions."
                            else -> "No videos matching filter."
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredList, key = { it.id }) { post ->
                    VerificationPostCard(
                        post = post,
                        onPreviewClick = { videoToPreview = post },
                        onApproveClick = { videoToApprove = post },
                        onRejectClick = { videoToReject = post },
                        onDeleteClick = { onDeletePost(post) }
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    // Video Preview Dialog
    if (videoToPreview != null) {
        val post = videoToPreview!!
        AlertDialog(
            onDismissRequest = { videoToPreview = null },
            title = {
                Text("Review Content: ${post.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    ) {
                        AsyncImage(
                            model = if (post.thumbnailUrl.isNotBlank()) post.thumbnailUrl else "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=800",
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Creator: ${post.channelName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Category: ${post.category} • Duration: ${post.duration}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Description: ${post.description.ifBlank { "No description provided." }}", fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tags: ${post.tags}", fontSize = 11.sp, color = Color(0xFF3B82F6))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = videoToPreview!!
                        videoToPreview = null
                        onApproveVideo(p, "Verified by Super Admin after preview")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Approve & Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToPreview = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Approve Dialog
    if (videoToApprove != null) {
        val post = videoToApprove!!
        var approveNotes by remember { mutableStateOf("Meets all community guidelines and video quality standards.") }

        AlertDialog(
            onDismissRequest = { videoToApprove = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Approve & Publish Video?")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This will immediately publish '${post.title}' to the Satisfy public feed for all users.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = approveNotes,
                        onValueChange = { approveNotes = it },
                        label = { Text("Audit / Verification Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApproveVideo(post, approveNotes)
                        videoToApprove = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Approve Video")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToApprove = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection Dialog
    if (videoToReject != null) {
        val post = videoToReject!!
        var rejectionReason by remember { mutableStateOf("") }
        val commonReasons = listOf(
            "Violates Community Guidelines (Inappropriate / NSFW)",
            "Copyright / Duplicate Content Detected",
            "Low Video / Audio Quality Standards",
            "Misleading Title / Clickbait Tags",
            "Spam or Commercial Solicitation"
        )

        AlertDialog(
            onDismissRequest = { videoToReject = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Reject Submission")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Specify the rejection reason for '${post.title}'. The creator will be notified with this reason.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Common Reasons:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    commonReasons.forEach { reason ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (rejectionReason == reason) Color(0xFFEF4444).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (rejectionReason == reason) BorderStroke(1.dp, Color(0xFFEF4444)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable { rejectionReason = reason }
                        ) {
                            Text(
                                text = reason,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                color = if (rejectionReason == reason) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        label = { Text("Custom Rejection Feedback") },
                        placeholder = { Text("Describe specific guideline issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = rejectionReason.ifBlank { "Did not meet Satisfy content quality standards." }
                        onRejectVideo(post, finalReason)
                        videoToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Rejection")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToReject = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun VerificationPostCard(
    post: PostEntity,
    onPreviewClick: () -> Unit,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isPending = post.status == "PENDING" || (post.isUserCreated && !post.isVerified && post.status != "REJECTED" && post.status != "APPROVED")
    val isApproved = post.status == "APPROVED" || (post.isVerified && post.status != "REJECTED")
    val isRejected = post.status == "REJECTED"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = when {
            isPending -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            isApproved -> BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
            isRejected -> BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f))
            else -> null
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Thumbnail preview
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPreviewClick() }
                ) {
                    AsyncImage(
                        model = if (post.thumbnailUrl.isNotBlank()) post.thumbnailUrl else "https://images.unsplash.com/photo-1536240478700-b869070f9279?w=400",
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(3.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = post.duration,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                        )
                    }
                    // Play icon overlay
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Preview",
                            tint = Color.White,
                            modifier = Modifier
                                .padding(4.dp)
                                .size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // Status Badge & Type
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when {
                            isPending -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.HourglassTop, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("PENDING REVIEW", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            isApproved -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("APPROVED & LIVE", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            isRejected -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("REJECTED", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = post.type.name,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = post.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "By ${post.channelName} • ${post.category}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tags: ${post.tags}",
                        fontSize = 9.sp,
                        color = Color(0xFF3B82F6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // If Rejected, show rejection reason box
            if (isRejected && !post.rejectionReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFEF4444).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Reason: ${post.rejectionReason}",
                            fontSize = 10.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))

            // Action row for Admin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPreviewClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Preview", fontSize = 11.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (!isApproved) {
                        Button(
                            onClick = onApproveClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Approve", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (!isRejected) {
                        OutlinedButton(
                            onClick = onRejectClick,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminMonetizationTabContent(
    applications: List<MonetizationApplicationEntity>,
    onApprove: (Long, String) -> Unit,
    onReject: (Long, String, String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedAppForApproval by remember { mutableStateOf<MonetizationApplicationEntity?>(null) }
    var selectedAppForRejection by remember { mutableStateOf<MonetizationApplicationEntity?>(null) }
    var adminNotesInput by remember { mutableStateOf("") }
    var rejectionReasonInput by remember { mutableStateOf("Did not meet content originality or traffic integrity standards.") }

    val pendingCount = remember(applications) { applications.count { it.status == "PENDING" } }
    val approvedCount = remember(applications) { applications.count { it.status == "APPROVED" } }
    val rejectedCount = remember(applications) { applications.count { it.status == "REJECTED" } }

    val filteredList = remember(applications, selectedFilter, searchQuery) {
        applications.filter { app ->
            val matchesFilter = when (selectedFilter) {
                "PENDING" -> app.status == "PENDING"
                "APPROVED" -> app.status == "APPROVED"
                "REJECTED" -> app.status == "REJECTED"
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                app.channelName.contains(searchQuery, ignoreCase = true) ||
                        app.channelHandle.contains(searchQuery, ignoreCase = true) ||
                        app.userId.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Summary Header Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCard(
                    title = "Pending Review",
                    value = "$pendingCount",
                    subtext = if (pendingCount > 0) "Needs Review" else "Clean Queue",
                    icon = Icons.Default.HourglassEmpty,
                    accentColor = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Approved Partners",
                    value = "$approvedCount",
                    subtext = "Monetized",
                    icon = Icons.Default.CheckCircle,
                    accentColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Rejected",
                    value = "$rejectedCount",
                    subtext = "Needs Reapply",
                    icon = Icons.Default.Cancel,
                    accentColor = Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search Bar & Filter Chips
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search creator by name, handle, or ID...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("ALL" to "All (${applications.size})", "PENDING" to "Pending ($pendingCount)", "APPROVED" to "Approved ($approvedCount)", "REJECTED" to "Rejected ($rejectedCount)").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        // Applications list
        if (filteredList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No monetization applications found",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "When eligible creators submit their channel for monetization, they will appear here for review.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredList, key = { it.id }) { app ->
                MonetizationAdminCard(
                    app = app,
                    onApproveClick = {
                        adminNotesInput = ""
                        selectedAppForApproval = app
                    },
                    onRejectClick = {
                        rejectionReasonInput = "Did not meet content originality or traffic integrity standards."
                        adminNotesInput = ""
                        selectedAppForRejection = app
                    }
                )
            }
        }
    }

    // Approval Dialog
    if (selectedAppForApproval != null) {
        val app = selectedAppForApproval!!
        AlertDialog(
            onDismissRequest = { selectedAppForApproval = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Approve Monetization Partner", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Are you sure you want to approve ${app.channelName} (${app.channelHandle}) for the Satisfy Partner Program?",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = adminNotesInput,
                        onValueChange = { adminNotesInput = it },
                        label = { Text("Admin Notes (Optional)") },
                        placeholder = { Text("e.g. Verified organic traffic and high quality 4K renders") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onApprove(app.id, adminNotesInput)
                        selectedAppForApproval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Approve Channel", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForApproval = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection Dialog
    if (selectedAppForRejection != null) {
        val app = selectedAppForRejection!!
        AlertDialog(
            onDismissRequest = { selectedAppForRejection = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = { Text("Reject Monetization Application", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Specify the exact reason for rejecting ${app.channelName}. The creator will be shown this feedback:",
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = rejectionReasonInput,
                        onValueChange = { rejectionReasonInput = it },
                        label = { Text("Rejection Reason *") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = adminNotesInput,
                        onValueChange = { adminNotesInput = it },
                        label = { Text("Internal Admin Notes (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onReject(app.id, rejectionReasonInput, adminNotesInput)
                        selectedAppForRejection = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Reject Application", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedAppForRejection = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MonetizationAdminCard(
    app: MonetizationApplicationEntity,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit
) {
    val isApproved = app.status == "APPROVED"
    val isPending = app.status == "PENDING"
    val isRejected = app.status == "REJECTED"

    val badgeColor = when {
        isApproved -> Color(0xFF10B981)
        isPending -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Channel Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = app.channelAvatar.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                        contentDescription = app.channelName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = app.channelName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            if (isApproved) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
                            }
                        }
                        Text(
                            text = "${app.channelHandle} • User: ${app.userId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .border(1.dp, badgeColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = app.status,
                        color = badgeColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Metrics Grid
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${app.subscriberCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (app.subscriberCount >= 500) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Text("Subscribers (>=500)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f hrs", app.normalVideoWatchHours),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (app.normalVideoWatchHours >= 4000.0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                    Text("Long Video (4K hrs)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f hrs", app.shortsWatchHours),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (app.shortsWatchHours >= 10000.0) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
                    )
                    Text("Shorts (10K hrs)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            // Notes / Rejection Reason display
            if (isRejected && app.rejectionReason != null) {
                Text(
                    text = "Rejection Reason: ${app.rejectionReason}",
                    fontSize = 11.sp,
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.Medium
                )
            }
            if (app.adminNotes != null && app.adminNotes.isNotBlank()) {
                Text(
                    text = "Admin Note: ${app.adminNotes}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isApproved) {
                    Button(
                        onClick = onApproveClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve Partner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (!isRejected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onRejectClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reject", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

