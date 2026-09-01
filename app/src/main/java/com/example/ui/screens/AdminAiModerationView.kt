package com.example.ui.screens

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostEntity
import com.example.data.model.PostType

/**
 * Admin AI Moderation View
 * Handles automated AI-moderated video queue, spam reach limitation, and flagged content reviews.
 */
@Composable
fun AdminAiModerationView(
    allPosts: List<PostEntity>,
    onApproveVideo: (PostEntity, String) -> Unit,
    onRejectVideo: (PostEntity, String) -> Unit,
    onResolveAiFlag: (Long, String) -> Unit,
    onSetSpamReachLimitation: (Long, Boolean) -> Unit,
    onDeletePost: (PostEntity) -> Unit
) {
    var selectedStatusFilter by remember { mutableStateOf("Flagged") }
    var searchQuery by remember { mutableStateOf("") }

    var videoToReject by remember { mutableStateOf<PostEntity?>(null) }
    var videoToPreview by remember { mutableStateOf<PostEntity?>(null) }
    var videoToClearFlag by remember { mutableStateOf<PostEntity?>(null) }

    val flaggedPosts = remember(allPosts) {
        allPosts.filter { it.isFlagged || it.status == "PENDING" }
    }
    val spamLimitedPosts = remember(allPosts) {
        allPosts.filter { it.isSpamLimited }
    }
    val autoApprovedPosts = remember(allPosts) {
        allPosts.filter { it.status == "APPROVED" && !it.isFlagged && !it.isSpamLimited }
    }
    val rejectedPosts = remember(allPosts) {
        allPosts.filter { it.status == "REJECTED" }
    }

    val filteredList = remember(allPosts, selectedStatusFilter, searchQuery) {
        val baseList = when (selectedStatusFilter) {
            "Flagged" -> flaggedPosts
            "Spam Limited" -> spamLimitedPosts
            "Live & Approved" -> autoApprovedPosts
            "Violations" -> rejectedPosts
            else -> allPosts
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.channelName.contains(searchQuery, ignoreCase = true) ||
                it.category.contains(searchQuery, ignoreCase = true) ||
                it.tags.contains(searchQuery, ignoreCase = true) ||
                (it.aiModerationReason ?: "").contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // AI Moderation Header Banner
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AI-Powered Automated Moderation Active",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Videos publish instantly. Admin review is only required for AI-flagged risks, spam, or user reports.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Queue Overview Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStatusFilter = "Flagged" }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AI Flagged", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        Icon(Icons.Filled.Flag, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${flaggedPosts.size}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.3f)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { selectedStatusFilter = "Spam Limited" }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Spam Throttled", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                        Icon(Icons.Filled.FilterListOff, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${spamLimitedPosts.size}",
                        fontSize = 18.sp,
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
                    .clickable { selectedStatusFilter = "Live & Approved" }
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Instant Live", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${autoApprovedPosts.size}",
                        fontSize = 18.sp,
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
            placeholder = { Text("Search by title, creator, AI flag reason...") },
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
        val statusTabs = listOf("Flagged", "Spam Limited", "Live & Approved", "Violations", "All")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(statusTabs) { tab ->
                val isSelected = selectedStatusFilter == tab
                val count = when (tab) {
                    "Flagged" -> flaggedPosts.size
                    "Spam Limited" -> spamLimitedPosts.size
                    "Live & Approved" -> autoApprovedPosts.size
                    "Violations" -> rejectedPosts.size
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
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = when (selectedStatusFilter) {
                            "Flagged" -> "No AI-flagged content for review 🎉"
                            "Spam Limited" -> "No spam-limited posts."
                            "Live & Approved" -> "No live videos found."
                            "Violations" -> "No violations found."
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
                    ModerationPostCard(
                        post = post,
                        onPreviewClick = { videoToPreview = post },
                        onClearFlagClick = { videoToClearFlag = post },
                        onToggleSpamLimit = {
                            onSetSpamReachLimitation(post.id, !post.isSpamLimited)
                        },
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
                    Text("AI Quality Score: ${post.aiQualityScore}/100 • Risk: ${post.aiModerationRiskScore}/100", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (!post.aiModerationReason.isNullOrBlank()) {
                        Text("AI Flag Reason: ${post.aiModerationReason}", fontSize = 12.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.Medium)
                    }
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
                        onResolveAiFlag(p.id, "Verified by Admin after preview")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Dismiss Flag & Approve")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToPreview = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Clear Flag / Restore Reach Dialog
    if (videoToClearFlag != null) {
        val post = videoToClearFlag!!
        var notes by remember { mutableStateOf("Content reviewed and cleared for standard distribution.") }

        AlertDialog(
            onDismissRequest = { videoToClearFlag = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear AI Flag & Restore Distribution")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "This will clear any moderation flags or spam limitations on '${post.title}' and restore full recommendation algorithm reach.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Resolution Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onResolveAiFlag(post.id, notes)
                        videoToClearFlag = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Clear Flag & Restore")
                }
            },
            dismissButton = {
                TextButton(onClick = { videoToClearFlag = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rejection / Violation Dialog
    if (videoToReject != null) {
        val post = videoToReject!!
        var rejectionReason by remember { mutableStateOf(post.aiModerationReason ?: "") }
        val commonReasons = listOf(
            "Harmful / Hate Speech / Community Guideline Violation",
            "Spam / Commercial Solicitation",
            "Duplicate / Stolen Video Content",
            "Misleading / Deceptive Metadata",
            "Low Video Standards / Abusive"
        )

        AlertDialog(
            onDismissRequest = { videoToReject = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Remove Violation")
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Specify violation reason for '${post.title}'. This will remove the post from public recommendation feeds.",
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Violation Categories:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        label = { Text("Violation Reason / Feedback") },
                        placeholder = { Text("Describe specific guideline issue...") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalReason = rejectionReason.ifBlank { "Violated Satisfy community safety guidelines." }
                        onRejectVideo(post, finalReason)
                        videoToReject = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Confirm Violation")
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
private fun ModerationPostCard(
    post: PostEntity,
    onPreviewClick: () -> Unit,
    onClearFlagClick: () -> Unit,
    onToggleSpamLimit: () -> Unit,
    onRejectClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isFlagged = post.isFlagged
    val isSpamLimited = post.isSpamLimited
    val isRejected = post.status == "REJECTED"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = when {
            isFlagged -> BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.6f))
            isSpamLimited -> BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
            isRejected -> BorderStroke(1.dp, Color(0xFF94A3B8).copy(alpha = 0.4f))
            else -> BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
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
                    // Status Badges & Type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        when {
                            isFlagged -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFEF4444).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.Flag, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("AI FLAGGED", color = Color(0xFFEF4444), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            isSpamLimited -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Filled.FilterListOff, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(10.dp))
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text("SPAM THROTTLED", color = Color(0xFFF59E0B), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            isRejected -> {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF94A3B8).copy(alpha = 0.15f)
                                ) {
                                    Text("VIOLATION REMOVED", color = Color(0xFF94A3B8), fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                            else -> {
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
                                        Text("LIVE & VERIFIED", color = Color(0xFF10B981), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Q: ${post.aiQualityScore}/100",
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
                        text = "Views: ${post.viewCount} • Retention: ${(post.avgRetentionRate * 100).toInt()}% • Shares: ${post.sharesCount}",
                        fontSize = 9.sp,
                        color = Color(0xFF3B82F6),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // If Flagged or Spam Limited, show reason box
            if (!post.aiModerationReason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isFlagged) Color(0xFFEF4444).copy(alpha = 0.1f) else Color(0xFFF59E0B).copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isFlagged) Icons.Filled.Warning else Icons.Filled.Info,
                            contentDescription = null,
                            tint = if (isFlagged) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "AI Reason: ${post.aiModerationReason}",
                            fontSize = 10.sp,
                            color = if (isFlagged) Color(0xFFEF4444) else Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium
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
                    if (isFlagged || isSpamLimited) {
                        Button(
                            onClick = onClearFlagClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Restore Reach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (!isRejected) {
                        OutlinedButton(
                            onClick = onToggleSpamLimit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Filled.FilterListOff, contentDescription = null, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Limit Spam", fontSize = 10.sp)
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
                            Text("Violation", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
