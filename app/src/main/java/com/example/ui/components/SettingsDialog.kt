package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.CreatorPageEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    pages: List<CreatorPageEntity>,
    onOpenCreatePage: () -> Unit,
    onOpenPageDetails: (CreatorPageEntity) -> Unit,
    isDarkTheme: Boolean,
    onToggleDarkTheme: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenAdminConsole: () -> Unit,
    isAdmin: Boolean = false,
    modifier: Modifier = Modifier
) {
    var autoPlayNext by remember { mutableStateOf(true) }
    var highQualityStream by remember { mutableStateOf(true) }
    var recordHistoryEnabled by remember { mutableStateOf(true) }
    var showClearHistoryConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Settings & Page Management",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Settings, Creator Pages & Studio",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                // Settings Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // SECTION 1: CREATOR PAGES & STUDIO (Highlighted user request)
                    item {
                        SettingsSectionHeader(title = "Creator Pages & Watch Time Studio")

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = SatisfyRed.copy(alpha = 0.08f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SatisfyRed.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(SatisfyRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AddBusiness,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Create Creator Page",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "Create a page to view exclusive Watch Time analytics",
                                                fontSize = 11.sp,
                                                color = SatisfyRed,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            onDismiss()
                                            onOpenCreatePage()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("settings_create_page_btn")
                                    ) {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("New Page", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Existing Pages List inside Settings
                    if (pages.isNotEmpty()) {
                        item {
                            Text(
                                text = "My Creator Pages (${pages.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        items(pages) { page ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onDismiss()
                                        onOpenPageDetails(page)
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                    ) {
                                        AsyncImage(
                                            model = page.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200" },
                                            contentDescription = page.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = page.name,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Filled.Verified,
                                                contentDescription = null,
                                                tint = SatisfyBlue,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Text(
                                            text = "${page.category} • ${page.handle}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = SatisfyRed.copy(alpha = 0.12f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.HourglassBottom,
                                                contentDescription = null,
                                                tint = SatisfyRed,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Watch Time",
                                                color = SatisfyRed,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 2: VIDEO PLAYBACK & WATCH PREFERENCES
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsSectionHeader(title = "Video Playback Settings")

                        SettingsSwitchItem(
                            icon = Icons.Filled.PlayCircle,
                            title = "Auto-Play Next Video",
                            subtitle = "Automatically plays the next video in feed",
                            checked = autoPlayNext,
                            onCheckedChange = { autoPlayNext = it }
                        )

                        SettingsSwitchItem(
                            icon = Icons.Filled.Hd,
                            title = "High Quality Streaming",
                            subtitle = "Loads videos in full HD 1080p quality",
                            checked = highQualityStream,
                            onCheckedChange = { highQualityStream = it }
                        )

                        SettingsSwitchItem(
                            icon = Icons.Filled.History,
                            title = "Save Watch History",
                            subtitle = "Keep a history of videos and shorts you watch",
                            checked = recordHistoryEnabled,
                            onCheckedChange = { recordHistoryEnabled = it }
                        )
                    }

                    // SECTION 3: THEME & DISPLAY
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsSectionHeader(title = "App Theme & Display")

                        SettingsSwitchItem(
                            icon = Icons.Filled.DarkMode,
                            title = "Dark Theme",
                            subtitle = "Enable dark mode for battery and eye comfort",
                            checked = isDarkTheme,
                            onCheckedChange = { onToggleDarkTheme() }
                        )
                    }

                    // SECTION 4: DATA & PRIVACY
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsSectionHeader(title = "Data & Privacy")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showClearHistoryConfirm = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Clear Watch History",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        text = "Delete all history of watched videos and shorts",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // SECTION 5: ADMIN CONSOLE
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        SettingsSectionHeader(title = "Admin & System")

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismiss()
                                    onOpenAdminConsole()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Security, contentDescription = null, tint = SatisfyGold)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Admin Dashboard & Console",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = if (isAdmin) "Admin panel active" else "Login to access moderation console",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(Icons.Filled.ChevronRight, contentDescription = null)
                            }
                        }
                    }

                    // Version footer
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Satisfy Video & Creator Studio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Version 2.5.0 • Watch Time Engine Enabled",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // Clear History Confirm Dialog
        if (showClearHistoryConfirm) {
            AlertDialog(
                onDismissRequest = { showClearHistoryConfirm = false },
                title = { Text("Clear Watch History?") },
                text = { Text("Are you sure you want to permanently clear your entire watch history?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showClearHistoryConfirm = false
                            onClearHistory()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Clear")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearHistoryConfirm = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(text = subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SatisfyRed,
                    checkedTrackColor = SatisfyRed.copy(alpha = 0.4f)
                )
            )
        }
    }
}
