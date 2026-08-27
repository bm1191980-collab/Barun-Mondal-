package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import coil.compose.AsyncImage
import com.example.data.model.*

enum class AnalyticsContentFilter {
    ALL,
    SHORTS_ONLY,
    VIDEOS_ONLY
}

enum class AnalyticsSortOption(val title: String) {
    MOST_VIEWS("Most Views"),
    MOST_WATCH_TIME("Most Watch Time"),
    NEWEST("Newest First")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorAnalyticsScreen(
    analyticsSummary: CreatorAnalyticsSummary,
    userProfile: UserProfile,
    onBack: () -> Unit,
    onNavigateToMonetization: () -> Unit,
    onNavigateToRules: () -> Unit,
    onSelectPost: (PostEntity) -> Unit
) {
    var contentFilter by remember { mutableStateOf(AnalyticsContentFilter.ALL) }
    var sortOption by remember { mutableStateOf(AnalyticsSortOption.MOST_VIEWS) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredShorts = remember(analyticsSummary.individualShorts, searchQuery, sortOption) {
        val list = analyticsSummary.individualShorts.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
        }
        when (sortOption) {
            AnalyticsSortOption.MOST_VIEWS -> list.sortedByDescending { it.viewCount }
            AnalyticsSortOption.MOST_WATCH_TIME -> list.sortedByDescending { it.watchTimeSeconds }
            AnalyticsSortOption.NEWEST -> list.sortedByDescending { it.id }
        }
    }

    val filteredVideos = remember(analyticsSummary.individualVideos, searchQuery, sortOption) {
        val list = analyticsSummary.individualVideos.filter {
            searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
        }
        when (sortOption) {
            AnalyticsSortOption.MOST_VIEWS -> list.sortedByDescending { it.viewCount }
            AnalyticsSortOption.MOST_WATCH_TIME -> list.sortedByDescending { it.watchTimeSeconds }
            AnalyticsSortOption.NEWEST -> list.sortedByDescending { it.id }
        }
    }

    val displayPosts = remember(contentFilter, filteredShorts, filteredVideos) {
        when (contentFilter) {
            AnalyticsContentFilter.ALL -> (filteredShorts + filteredVideos).sortedByDescending {
                when (sortOption) {
                    AnalyticsSortOption.MOST_VIEWS -> it.viewCount
                    AnalyticsSortOption.MOST_WATCH_TIME -> it.watchTimeSeconds
                    AnalyticsSortOption.NEWEST -> it.id
                }
            }
            AnalyticsContentFilter.SHORTS_ONLY -> filteredShorts
            AnalyticsContentFilter.VIDEOS_ONLY -> filteredVideos
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Creator Shorts Analytics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = userProfile.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("analytics_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToMonetization,
                        modifier = Modifier.testTag("analytics_to_monetization_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = "Monetization",
                            tint = Color(0xFFF59E0B)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToRules,
                        modifier = Modifier.testTag("analytics_to_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.HelpOutline,
                            contentDescription = "Rules"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Header Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFFEC4899).copy(alpha = 0.85f),
                                        Color(0xFF8B5CF6).copy(alpha = 0.95f),
                                        Color(0xFF1E1B4B)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = userProfile.avatarUrl.ifEmpty { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = userProfile.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "${userProfile.handle} • Real-time Channel Database",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Top stats bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = formatCount(analyticsSummary.totalSubscribers),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Subscribers",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = formatCount(analyticsSummary.totalShortsViews + analyticsSummary.totalVideoViews),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Total Views",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                                VerticalDivider(
                                    modifier = Modifier.height(30.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val totalSecs = analyticsSummary.totalShortsWatchTimeSeconds + analyticsSummary.totalVideoWatchTimeSeconds
                                    Text(
                                        text = formatWatchHoursDouble(totalSecs / 3600.0),
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 18.sp
                                    )
                                    Text(
                                        text = "Total Watch Time",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 1: Shorts Performance Metrics (Requested)
            item {
                Text(
                    text = "Shorts Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Shorts Uploaded",
                        value = "${analyticsSummary.totalShortsUploaded}",
                        subtitle = "Active in feed",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFFEC4899),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Shorts Views",
                        value = formatCount(analyticsSummary.totalShortsViews),
                        subtitle = "${analyticsSummary.totalShortsViews} total plays",
                        icon = Icons.Default.Visibility,
                        accentColor = Color(0xFF8B5CF6),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Shorts Watch Time",
                        value = formatWatchTime(analyticsSummary.totalShortsWatchTimeSeconds),
                        subtitle = "${formatWatchHoursDouble(analyticsSummary.totalShortsWatchTimeSeconds / 3600.0)} total",
                        icon = Icons.Default.Timer,
                        accentColor = Color(0xFFF43F5E),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Avg Short Duration",
                        value = if (analyticsSummary.totalShortsUploaded > 0) {
                            val avgSecs = analyticsSummary.totalShortsWatchTimeSeconds / maxOf(1, analyticsSummary.totalShortsViews)
                            "${avgSecs}s / view"
                        } else "0s",
                        subtitle = "Audience retention",
                        icon = Icons.Default.Speed,
                        accentColor = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 2: Normal Video Performance Metrics (Requested)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Long-Form Video Performance",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Normal Video Views",
                        value = formatCount(analyticsSummary.totalVideoViews),
                        subtitle = "${analyticsSummary.totalVideoViews} total plays",
                        icon = Icons.Default.Videocam,
                        accentColor = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Normal Video Watch Time",
                        value = formatWatchTime(analyticsSummary.totalVideoWatchTimeSeconds),
                        subtitle = "${formatWatchHoursDouble(analyticsSummary.totalVideoWatchTimeSeconds / 3600.0)} total",
                        icon = Icons.Default.AccessTime,
                        accentColor = Color(0xFF06B6D4),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Section 3: Individual Shorts & Videos Breakdown (Requested)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Individual Content Breakdown",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${displayPosts.size} items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Filter Chips & Search Bar
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = contentFilter == AnalyticsContentFilter.ALL,
                            onClick = { contentFilter = AnalyticsContentFilter.ALL },
                            label = { Text("All (${analyticsSummary.individualShorts.size + analyticsSummary.individualVideos.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = contentFilter == AnalyticsContentFilter.SHORTS_ONLY,
                            onClick = { contentFilter = AnalyticsContentFilter.SHORTS_ONLY },
                            label = { Text("Shorts Only (${analyticsSummary.individualShorts.size})", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFFEC4899)
                                )
                            }
                        )
                        FilterChip(
                            selected = contentFilter == AnalyticsContentFilter.VIDEOS_ONLY,
                            onClick = { contentFilter = AnalyticsContentFilter.VIDEOS_ONLY },
                            label = { Text("Videos Only (${analyticsSummary.individualVideos.size})", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = Color(0xFF3B82F6)
                                )
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search your content...", fontSize = 13.sp) },
                            leadingIcon = {
                                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                        )

                        var sortMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { sortMenuExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sort,
                                    contentDescription = "Sort",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(sortOption.title, fontSize = 12.sp)
                            }

                            DropdownMenu(
                                expanded = sortMenuExpanded,
                                onDismissRequest = { sortMenuExpanded = false }
                            ) {
                                AnalyticsSortOption.entries.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt.title) },
                                        onClick = {
                                            sortOption = opt
                                            sortMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Content List
            if (displayPosts.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "No content matches your filter",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Upload new Shorts or Videos to track real-time analytics and watch time.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            } else {
                items(displayPosts, key = { it.id }) { post ->
                    IndividualContentAnalyticsCard(
                        post = post,
                        onClick = { onSelectPost(post) }
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
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
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun IndividualContentAnalyticsCard(
    post: PostEntity,
    onClick: () -> Unit
) {
    val isShort = post.type == PostType.SHORT
    val typeBadgeColor = if (isShort) Color(0xFFEC4899) else Color(0xFF3B82F6)

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .testTag("analytics_item_${post.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail with type badge & duration
            Box(
                modifier = Modifier
                    .width(if (isShort) 64.dp else 96.dp)
                    .height(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            ) {
                AsyncImage(
                    model = post.thumbnailUrl.ifEmpty { post.mediaUrl },
                    contentDescription = post.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Type badge
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(typeBadgeColor)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isShort) "SHORT" else "VIDEO",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = post.duration.ifEmpty { "00:00" },
                        fontSize = 9.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Info & Metrics
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = post.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${post.timeAgo} • ${post.category}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Views
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${post.viewCount} views",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Watch time
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFFF43F5E),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = formatWatchTime(post.watchTimeSeconds),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Likes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${post.likeCount}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
