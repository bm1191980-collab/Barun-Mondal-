package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PresencePrivacySetting
import com.example.data.model.PresenceStatus
import com.example.data.model.UserPresence
import com.example.data.model.UserProfile
import com.example.ui.theme.*

val SatisfyOnlineGreen = Color(0xFF10B981)
val SatisfyAwayAmber = Color(0xFFF59E0B)
val SatisfyOfflineGray = Color(0xFF94A3B8)
val SatisfyBusyRed = Color(0xFFEF4444)

/**
 * Animated Glowing Online Dot / Offline Dot for avatars
 */
@Composable
fun PresenceIndicator(
    isOnline: Boolean,
    status: PresenceStatus = if (isOnline) PresenceStatus.ONLINE else PresenceStatus.OFFLINE,
    size: Dp = 12.dp,
    showBorder: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.surface,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        PresenceStatus.ONLINE -> SatisfyOnlineGreen
        PresenceStatus.AWAY -> SatisfyAwayAmber
        PresenceStatus.BUSY -> SatisfyBusyRed
        PresenceStatus.OFFLINE -> SatisfyOfflineGray
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        // Outer glowing pulse ring when online
        if (status == PresenceStatus.ONLINE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = pulseAlpha))
            )
        }

        // Core Status Dot
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(statusColor)
                .then(
                    if (showBorder) Modifier.border(1.5.dp, borderColor, CircleShape)
                    else Modifier
                )
        )
    }
}

/**
 * Interactive Status Pill displayed on Profile headers, Creator details, and Chat
 */
@Composable
fun PresenceStatusPill(
    isOnline: Boolean,
    statusText: String,
    customMoodOrActivity: String = "",
    status: PresenceStatus = if (isOnline) PresenceStatus.ONLINE else PresenceStatus.OFFLINE,
    isSelf: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        !isOnline -> SatisfyOfflineGray
        status == PresenceStatus.AWAY -> SatisfyAwayAmber
        status == PresenceStatus.BUSY -> SatisfyBusyRed
        else -> SatisfyOnlineGreen
    }

    val containerColor = when {
        isOnline -> statusColor.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isOnline) statusColor.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            )
            .testTag("presence_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PresenceIndicator(
                isOnline = isOnline,
                status = status,
                size = 8.dp,
                showBorder = false
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = statusText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isOnline) statusColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (customMoodOrActivity.isNotBlank()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "• $customMoodOrActivity",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSelf && onClick != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit Status & Privacy",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}

/**
 * Bottom Sheet / Dialog for Presence & Privacy Settings
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusAndPrivacyBottomSheet(
    userProfile: UserProfile,
    onDismiss: () -> Unit,
    onUpdateOnlineStatus: (Boolean) -> Unit,
    onUpdateShowLastSeen: (Boolean) -> Unit,
    onUpdatePrivacySetting: (PresencePrivacySetting) -> Unit,
    onUpdateCustomStatus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnline by remember { mutableStateOf(userProfile.showOnlineStatus) }
    var showLastSeen by remember { mutableStateOf(userProfile.showLastSeen) }
    var selectedPrivacy by remember { mutableStateOf(userProfile.presencePrivacy) }
    var customStatusText by remember { mutableStateOf(userProfile.customStatusMessage) }

    val presetMoods = remember {
        listOf(
            "🎬 Creating Videos",
            "🍿 Watching Shorts",
            "✨ Exploring Satisfy",
            "🎨 Designing Visuals",
            "🌙 Away / Busy",
            "⚡ VIP Pro Creator",
            "🎧 Listening to ASMR",
            "🚀 Open for Collab"
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier.testTag("status_and_privacy_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(SatisfyOnlineGreen, Color(0xFF059669)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WifiTethering,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Status & Privacy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Real-time presence & audience visibility",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Live Preview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (showOnline) SatisfyOnlineGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with live indicator
                    Box(modifier = Modifier.size(46.dp)) {
                        AsyncImage(
                            model = userProfile.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150" },
                            contentDescription = userProfile.name,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        PresenceIndicator(
                            isOnline = showOnline && selectedPrivacy != PresencePrivacySetting.NOBODY,
                            size = 13.dp,
                            showBorder = true,
                            borderColor = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.align(Alignment.BottomEnd)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userProfile.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            if (showOnline && selectedPrivacy != PresencePrivacySetting.NOBODY) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SatisfyOnlineGreen.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "Online",
                                        color = SatisfyOnlineGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = SatisfyOfflineGray.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = if (selectedPrivacy == PresencePrivacySetting.NOBODY) "Incognito 🔒" else "Offline",
                                        color = SatisfyOfflineGray,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = customStatusText.ifBlank { "Active on Satisfy ✨" },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Custom Status Input & Quick Moods
            Text(
                text = "Custom Status Message",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = customStatusText,
                onValueChange = {
                    customStatusText = it
                    onUpdateCustomStatus(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("custom_status_input"),
                placeholder = { Text("What are you doing on Satisfy?") },
                leadingIcon = {
                    Icon(Icons.Filled.EmojiEmotions, contentDescription = null, tint = SatisfyGold)
                },
                trailingIcon = {
                    if (customStatusText.isNotBlank()) {
                        IconButton(onClick = {
                            customStatusText = ""
                            onUpdateCustomStatus("")
                        }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Mood Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(presetMoods) { mood ->
                    val isSelected = customStatusText == mood
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                        ),
                        modifier = Modifier.clickable {
                            customStatusText = mood
                            onUpdateCustomStatus(mood)
                        }
                    ) {
                        Text(
                            text = mood,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Switches: Online Status & Last Seen
            Text(
                text = "Presence Switches",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            PresenceSwitchRow(
                icon = Icons.Filled.Circle,
                iconTint = SatisfyOnlineGreen,
                title = "Show Online Status",
                subtitle = "Let others know when you are actively using the app",
                checked = showOnline,
                onCheckedChange = {
                    showOnline = it
                    onUpdateOnlineStatus(it)
                },
                testTag = "toggle_online_status"
            )

            Spacer(modifier = Modifier.height(8.dp))

            PresenceSwitchRow(
                icon = Icons.Filled.AccessTime,
                iconTint = SatisfyBlue,
                title = "Show Last Seen Timestamp",
                subtitle = "Display when you were last active when offline",
                checked = showLastSeen,
                onCheckedChange = {
                    showLastSeen = it
                    onUpdateShowLastSeen(it)
                },
                testTag = "toggle_last_seen"
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Privacy Audience Selector (Everyone / Subscribers Only / Nobody)
            Text(
                text = "Who Can See My Activity?",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Control which viewers can see your real-time status and last seen",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrivacyOptionCard(
                    icon = Icons.Filled.Public,
                    title = "Everyone",
                    description = "All users and creators on Satisfy can view your status",
                    isSelected = selectedPrivacy == PresencePrivacySetting.EVERYONE,
                    onClick = {
                        selectedPrivacy = PresencePrivacySetting.EVERYONE
                        onUpdatePrivacySetting(PresencePrivacySetting.EVERYONE)
                    }
                )

                PrivacyOptionCard(
                    icon = Icons.Filled.Group,
                    title = "Subscribers Only",
                    description = "Only users subscribed to your channel can see when you are active",
                    isSelected = selectedPrivacy == PresencePrivacySetting.SUBSCRIBERS_ONLY,
                    onClick = {
                        selectedPrivacy = PresencePrivacySetting.SUBSCRIBERS_ONLY
                        onUpdatePrivacySetting(PresencePrivacySetting.SUBSCRIBERS_ONLY)
                    }
                )

                PrivacyOptionCard(
                    icon = Icons.Filled.Lock,
                    title = "Nobody (Incognito Mode)",
                    description = "Completely hide your online presence. You appear offline to everyone.",
                    isSelected = selectedPrivacy == PresencePrivacySetting.NOBODY,
                    onClick = {
                        selectedPrivacy = PresencePrivacySetting.NOBODY
                        onUpdatePrivacySetting(PresencePrivacySetting.NOBODY)
                    }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_presence_settings_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Done & Save Settings", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun PresenceSwitchRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}

@Composable
fun PrivacyOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
