package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.NotificationEntity
import com.example.data.model.NotificationPreferences
import com.example.data.model.NotificationType
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class NotificationFilterCategory(val label: String, val icon: ImageVector) {
    ALL("All", Icons.Default.AllInclusive),
    UNREAD("Unread", Icons.Default.MarkChatUnread),
    VIDEOS("Videos & Subs", Icons.Default.VideoLibrary),
    COMMUNITY("Comments & Likes", Icons.Default.ChatBubble),
    MONETIZATION("Monetization & Pro", Icons.Default.MonetizationOn),
    SYSTEM("Announcements", Icons.Default.Campaign)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    notifications: List<NotificationEntity>,
    unreadCount: Int,
    isFirebaseConnected: Boolean,
    preferences: NotificationPreferences,
    onBack: () -> Unit,
    onNotificationClick: (NotificationEntity) -> Unit,
    onMarkAsRead: (Long, String) -> Unit,
    onMarkAllAsRead: () -> Unit,
    onTogglePin: (Long) -> Unit,
    onDeleteNotification: (Long, String) -> Unit,
    onClearAll: () -> Unit,
    onUpdatePreferences: (NotificationPreferences) -> Unit,
    onSimulateNotification: (NotificationType, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf(NotificationFilterCategory.ALL) }
    var showSimulatorDialog by remember { mutableStateOf(false) }
    var showPreferencesDialog by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Filter notifications based on selected category
    val filteredNotifications = remember(notifications, selectedCategory) {
        when (selectedCategory) {
            NotificationFilterCategory.ALL -> notifications
            NotificationFilterCategory.UNREAD -> notifications.filter { !it.isRead }
            NotificationFilterCategory.VIDEOS -> notifications.filter {
                it.type == NotificationType.VIDEO_UPLOAD || it.type == NotificationType.SUBSCRIBER
            }
            NotificationFilterCategory.COMMUNITY -> notifications.filter {
                it.type == NotificationType.COMMENT || it.type == NotificationType.COMMENT_REPLY || it.type == NotificationType.LIKE
            }
            NotificationFilterCategory.MONETIZATION -> notifications.filter {
                it.type == NotificationType.MONETIZATION_UPDATE || it.type == NotificationType.PRO_MEMBERSHIP || it.type == NotificationType.WALLET_PAYOUT
            }
            NotificationFilterCategory.SYSTEM -> notifications.filter {
                it.type == NotificationType.ADMIN_BROADCAST || it.type == NotificationType.SYSTEM_ALERT
            }
        }
    }

    // Separate pinned vs standard notifications
    val pinnedList = remember(filteredNotifications) { filteredNotifications.filter { it.isPinned } }
    val regularList = remember(filteredNotifications) { filteredNotifications.filter { !it.isPinned } }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("notification_back_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Notifications",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (unreadCount > 0) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.testTag("unread_count_badge")
                                        ) {
                                            Text(
                                                text = "$unreadCount new",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Live Firebase Sync Indicator
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    val pulseAnim = rememberInfiniteTransition(label = "pulse")
                                    val alpha by pulseAnim.animateFloat(
                                        initialValue = 0.4f,
                                        targetValue = 1.0f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1000, easing = LinearEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "alpha"
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isFirebaseConnected) Color(0xFF10B981).copy(alpha = alpha)
                                                else Color(0xFFF59E0B)
                                            )
                                    )

                                    Spacer(modifier = Modifier.width(6.dp))

                                    Text(
                                        text = if (isFirebaseConnected) "Firebase Real-Time Connected" else "Offline Cache Mode",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isFirebaseConnected) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                }
                            }
                        }

                        // Action Buttons
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Test Simulator Button
                            IconButton(
                                onClick = { showSimulatorDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyCyan.copy(alpha = 0.15f))
                                    .testTag("notification_simulator_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = "Simulate Real-Time Notification",
                                    tint = SatisfyCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Mark All Read Button
                            if (unreadCount > 0) {
                                IconButton(
                                    onClick = onMarkAllAsRead,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                        .testTag("mark_all_read_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DoneAll,
                                        contentDescription = "Mark all as read",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }

                            // Preferences / Settings Button
                            IconButton(
                                onClick = { showPreferencesDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                    .testTag("notification_preferences_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Settings,
                                    contentDescription = "Notification Settings",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Filter Category Carousel
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(NotificationFilterCategory.values()) { category ->
                            val isSelected = selectedCategory == category
                            val categoryCount = when (category) {
                                NotificationFilterCategory.ALL -> notifications.size
                                NotificationFilterCategory.UNREAD -> unreadCount
                                NotificationFilterCategory.VIDEOS -> notifications.count { it.type == NotificationType.VIDEO_UPLOAD || it.type == NotificationType.SUBSCRIBER }
                                NotificationFilterCategory.COMMUNITY -> notifications.count { it.type == NotificationType.COMMENT || it.type == NotificationType.COMMENT_REPLY || it.type == NotificationType.LIKE }
                                NotificationFilterCategory.MONETIZATION -> notifications.count { it.type == NotificationType.MONETIZATION_UPDATE || it.type == NotificationType.PRO_MEMBERSHIP || it.type == NotificationType.WALLET_PAYOUT }
                                NotificationFilterCategory.SYSTEM -> notifications.count { it.type == NotificationType.ADMIN_BROADCAST || it.type == NotificationType.SYSTEM_ALERT }
                            }

                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.Transparent
                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .clickable { selectedCategory = category }
                                    .testTag("category_pill_${category.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = category.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = category.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (categoryCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(18.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = "$categoryCount",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
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
            if (filteredNotifications.isEmpty()) {
                // Empty State
                EmptyNotificationState(
                    category = selectedCategory,
                    onTriggerTest = { showSimulatorDialog = true },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Pinned Section Header (if any)
                    if (pinnedList.isNotEmpty()) {
                        item(key = "header_pinned") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = null,
                                    tint = SatisfyGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "PINNED NOTIFICATIONS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SatisfyGold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }

                        items(pinnedList, key = { "pinned_${it.id}" }) { item ->
                            NotificationCardItem(
                                notification = item,
                                onClick = {
                                    onMarkAsRead(item.id, item.firestoreId)
                                    onNotificationClick(item)
                                },
                                onTogglePin = { onTogglePin(item.id) },
                                onDelete = { onDeleteNotification(item.id, item.firestoreId) },
                                onMarkAsRead = { onMarkAsRead(item.id, item.firestoreId) }
                            )
                        }

                        item(key = "divider_pinned") {
                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                thickness = 1.dp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }

                    // Regular Notifications List
                    items(regularList, key = { "notif_${it.id}" }) { item ->
                        NotificationCardItem(
                            notification = item,
                            onClick = {
                                onMarkAsRead(item.id, item.firestoreId)
                                onNotificationClick(item)
                            },
                            onTogglePin = { onTogglePin(item.id) },
                            onDelete = { onDeleteNotification(item.id, item.firestoreId) },
                            onMarkAsRead = { onMarkAsRead(item.id, item.firestoreId) }
                        )
                    }

                    // Clear all button at bottom
                    if (notifications.isNotEmpty()) {
                        item(key = "footer_clear_all") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                OutlinedButton(
                                    onClick = { showClearConfirmDialog = true },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteSweep,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clear All Notifications", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Live Notification Simulator Dialog
    if (showSimulatorDialog) {
        NotificationSimulatorDialog(
            onDismiss = { showSimulatorDialog = false },
            onTrigger = { type, title, body ->
                onSimulateNotification(type, title, body)
                showSimulatorDialog = false
            }
        )
    }

    // Notification Preferences Dialog
    if (showPreferencesDialog) {
        NotificationPreferencesDialog(
            preferences = preferences,
            onDismiss = { showPreferencesDialog = false },
            onSave = { updated ->
                onUpdatePreferences(updated)
                showPreferencesDialog = false
            }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Clear All Notifications?") },
            text = { Text("Are you sure you want to remove all notification history? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun NotificationCardItem(
    notification: NotificationEntity,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onDelete: () -> Unit,
    onMarkAsRead: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = !notification.isRead

    val borderColor = when (notification.type) {
        NotificationType.VIDEO_UPLOAD -> SatisfyCyan
        NotificationType.MONETIZATION_UPDATE -> SatisfyGold
        NotificationType.PRO_MEMBERSHIP -> SatisfyPurple
        NotificationType.WALLET_PAYOUT -> Color(0xFF10B981)
        NotificationType.LIKE -> Color(0xFFEF4444)
        NotificationType.COMMENT, NotificationType.COMMENT_REPLY -> SatisfyElectricBlue
        NotificationType.ADMIN_BROADCAST -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("notification_card_${notification.id}"),
        shape = RoundedCornerShape(16.dp),
        color = if (isUnread) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isUnread) borderColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        ),
        tonalElevation = if (isUnread) 3.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Sender Avatar with Type Badge
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (notification.senderAvatar.isNotBlank()) {
                        AsyncImage(
                            model = notification.senderAvatar,
                            contentDescription = null,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(borderColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = notification.type.badgeEmoji,
                                fontSize = 20.sp
                            )
                        }
                    }

                    // Small Type Emoji Pill
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = notification.type.badgeEmoji,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Content Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = notification.senderName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = formatTimeAgo(notification.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (notification.isPinned) {
                                Icon(
                                    imageVector = Icons.Filled.PushPin,
                                    contentDescription = "Pinned",
                                    tint = SatisfyGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            if (isUnread) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(borderColor)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = notification.title,
                        fontSize = 13.sp,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = notification.body,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )

                    // Optional Action Bar at Bottom of card
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = borderColor.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, borderColor.copy(alpha = 0.25f))
                        ) {
                            Text(
                                text = notification.type.displayName.uppercase(),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = borderColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = onTogglePin,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (notification.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                                    contentDescription = "Pin notification",
                                    tint = if (notification.isPinned) SatisfyGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            if (isUnread) {
                                IconButton(
                                    onClick = onMarkAsRead,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Done,
                                        contentDescription = "Mark read",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                // Optional Thumbnail preview
                if (notification.targetThumbnailUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    AsyncImage(
                        model = notification.targetThumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyNotificationState(
    category: NotificationFilterCategory,
    onTriggerTest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (category == NotificationFilterCategory.ALL) "No Notifications Yet" else "No ${category.label} Found",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Real-time notifications about subscribed channels, video comments, monetization approvals, and admin announcements will appear here instantly.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onTriggerTest,
            colors = ButtonDefaults.buttonColors(containerColor = SatisfyCyan),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Bolt,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Simulate Real-Time Notification",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun NotificationSimulatorDialog(
    onDismiss: () -> Unit,
    onTrigger: (NotificationType, String, String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = null,
                            tint = SatisfyCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Real-Time Simulator",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "Select a scenario to trigger an instant Firebase real-time sync event & in-app floating banner.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                )

                val scenarios = listOf(
                    Triple(
                        NotificationType.VIDEO_UPLOAD,
                        "ASMR Soap Carving 4K Uploaded",
                        "Zen ASMR uploaded: 'Hypnotic Rainbow Soap Carving with Pure Spatial Sound'."
                    ),
                    Triple(
                        NotificationType.COMMENT,
                        "Viral Comment on Your Post",
                        "Liam Vance commented: 'This 60FPS hydraulic press slow-motion is pure ASMR bliss! ❤️'"
                    ),
                    Triple(
                        NotificationType.MONETIZATION_UPDATE,
                        "Monetization Application Approved! 💰",
                        "Congratulations! Your Satisfy Partner Application has been approved by the Admin team."
                    ),
                    Triple(
                        NotificationType.PRO_MEMBERSHIP,
                        "Satisfy Pro Activated 💎",
                        "Your Satisfy Pro Ultra plan is now active! Enjoy 4K HDR playback and zero ads."
                    ),
                    Triple(
                        NotificationType.WALLET_PAYOUT,
                        "Referral Payout Received: +$5.00 💵",
                        "Two new creators used your referral link. $5.00 has been credited to your Wallet."
                    ),
                    Triple(
                        NotificationType.ADMIN_BROADCAST,
                        "Satisfy Platform Update v2.0 ⚡",
                        "SuperAdmin broadcast: Live streaming and multi-track audio now enabled for all creators!"
                    )
                )

                scenarios.forEach { (type, title, body) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onTrigger(type, title, body) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = type.badgeEmoji,
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = body,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = SatisfyCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationPreferencesDialog(
    preferences: NotificationPreferences,
    onDismiss: () -> Unit,
    onSave: (NotificationPreferences) -> Unit
) {
    var pushEnabled by remember { mutableStateOf(preferences.pushEnabled) }
    var inAppBanner by remember { mutableStateOf(preferences.inAppBannerEnabled) }
    var soundVibrate by remember { mutableStateOf(preferences.soundVibrateEnabled) }
    var videoAlerts by remember { mutableStateOf(preferences.videoUploadAlerts) }
    var commentAlerts by remember { mutableStateOf(preferences.commentMentionAlerts) }
    var monetizationAlerts by remember { mutableStateOf(preferences.monetizationAlerts) }
    var adminAlerts by remember { mutableStateOf(preferences.adminBroadcastAlerts) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notification Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                PreferenceToggleItem(
                    title = "System Push Notifications",
                    subtitle = "Receive push alerts when app is in background",
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )

                PreferenceToggleItem(
                    title = "In-App Live Banners",
                    subtitle = "Show top floating heads-up banner on new events",
                    checked = inAppBanner,
                    onCheckedChange = { inAppBanner = it }
                )

                PreferenceToggleItem(
                    title = "Sound & Vibration",
                    subtitle = "Play chime and vibrate on incoming alerts",
                    checked = soundVibrate,
                    onCheckedChange = { soundVibrate = it }
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                PreferenceToggleItem(
                    title = "New Video Uploads",
                    subtitle = "Alerts when creators you follow upload new 4K videos",
                    checked = videoAlerts,
                    onCheckedChange = { videoAlerts = it }
                )

                PreferenceToggleItem(
                    title = "Comments & Mentions",
                    subtitle = "Alerts when people reply to your posts or comments",
                    checked = commentAlerts,
                    onCheckedChange = { commentAlerts = it }
                )

                PreferenceToggleItem(
                    title = "Monetization & Wallet Alerts",
                    subtitle = "Financial milestone updates & referral credits",
                    checked = monetizationAlerts,
                    onCheckedChange = { monetizationAlerts = it }
                )

                PreferenceToggleItem(
                    title = "SuperAdmin Announcements",
                    subtitle = "Community broadcasts & system feature updates",
                    checked = adminAlerts,
                    onCheckedChange = { adminAlerts = it }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSave(
                            preferences.copy(
                                pushEnabled = pushEnabled,
                                inAppBannerEnabled = inAppBanner,
                                soundVibrateEnabled = soundVibrate,
                                videoUploadAlerts = videoAlerts,
                                commentMentionAlerts = commentAlerts,
                                monetizationAlerts = monetizationAlerts,
                                adminBroadcastAlerts = adminAlerts
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Preferences", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PreferenceToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(36.dp)
        )
    }
}

fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
