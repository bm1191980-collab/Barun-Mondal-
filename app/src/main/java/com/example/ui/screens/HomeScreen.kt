package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.ui.theme.*
import com.example.ui.viewmodel.ScreenTab

@Composable
fun HomeScreen(
    posts: List<PostEntity>,
    shortPosts: List<PostEntity>,
    selectedCategory: String,
    categories: List<String>,
    onSelectCategory: (String) -> Unit,
    onVideoClick: (PostEntity) -> Unit,
    onShortClick: (PostEntity) -> Unit,
    onToggleLike: (PostEntity) -> Unit,
    onToggleSave: (PostEntity) -> Unit,
    onDeletePost: (PostEntity) -> Unit,
    onCreatorClick: (String, String, Long?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val filteredPosts = remember(posts, selectedCategory) {
        if (selectedCategory == "All") {
            posts.filter { it.type == PostType.VIDEO }
        } else {
            posts.filter { it.type == PostType.VIDEO && (it.category.equals(selectedCategory, ignoreCase = true) || it.tags.contains(selectedCategory, ignoreCase = true)) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Category Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectCategory(category) },
                    label = {
                        Text(
                            text = category,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            fontSize = 13.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp
                    ),
                    shape = RoundedCornerShape(32.dp)
                )
            }
        }

        // Main Feed List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // First 2 videos
            items(filteredPosts.take(2), key = { it.id }) { post ->
                VideoCard(
                    post = post,
                    onClick = { onVideoClick(post) },
                    onToggleLike = { onToggleLike(post) },
                    onToggleSave = { onToggleSave(post) },
                    onDeletePost = { onDeletePost(post) },
                    onCreatorClick = { onCreatorClick(post.channelName, post.creatorUid, post.pageId) }
                )
            }

            // Satisfy Shorts Horizontal Shelf
            if (shortPosts.isNotEmpty()) {
                item {
                    ShortsShelf(
                        shorts = shortPosts,
                        onShortClick = onShortClick
                    )
                }
            }

            // Remaining videos
            items(filteredPosts.drop(2), key = { it.id }) { post ->
                VideoCard(
                    post = post,
                    onClick = { onVideoClick(post) },
                    onToggleLike = { onToggleLike(post) },
                    onToggleSave = { onToggleSave(post) },
                    onDeletePost = { onDeletePost(post) },
                    onCreatorClick = { onCreatorClick(post.channelName, post.creatorUid, post.pageId) }
                )
            }

            if (filteredPosts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.VideoLibrary,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No videos in '$selectedCategory'",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Try selecting 'All' or upload your own video!",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoCard(
    post: PostEntity,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onDeletePost: () -> Unit,
    onCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        // High-res Thumbnail Container with Duration Badge
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color.Black)
        ) {
            if (post.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient shade at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                        )
                    )
            )

            // Duration Badge (Bottom Right)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.8f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = post.duration,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // User Created Badge (Top Left)
            if (post.isUserCreated) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SatisfyRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "YOUR UPLOAD",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        }

        // Details Row: Avatar + Title + Metadata + 3-dot Menu
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Channel Avatar (Clickable to open Creator Profile)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(SatisfyDarkSurfaceVariant)
                    .clickable { onCreatorClick() }
            ) {
                if (post.channelAvatar.isNotBlank()) {
                    AsyncImage(
                        model = post.channelAvatar,
                        contentDescription = post.channelName,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = post.channelName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = SatisfyRed
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onCreatorClick() }
                ) {
                    Text(
                        text = post.channelName,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (post.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified Channel",
                            tint = SatisfyRedLight,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    Text(
                        text = " • ${post.views} • ${post.timeAgo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // More Options Dropdown Menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (post.isSaved) "Remove from Saved" else "Save to Watch Later") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                tint = if (post.isSaved) SatisfyGold else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onToggleSave()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(if (post.isLiked) "Liked" else "Like Video") },
                        leadingIcon = {
                            Icon(
                                imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = null,
                                tint = if (post.isLiked) SatisfyRed else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onToggleLike()
                            showMenu = false
                        }
                    )
                    if (post.isUserCreated) {
                        DropdownMenuItem(
                            text = { Text("Delete Upload", color = SatisfyRed) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = SatisfyRed
                                )
                            },
                            onClick = {
                                onDeletePost()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsShelf(
    shorts: List<PostEntity>,
    onShortClick: (PostEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Shelf Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.example.ui.components.SatisfyAnimatedLogo(
                size = 24.dp,
                isAnimated = true,
                withBackgroundBadge = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Satisfy Shorts",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Horizontal Row of Shorts Cards
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(shorts, key = { it.id }) { short ->
                ShortCard(short = short, onClick = { onShortClick(short) })
            }
        }
    }
}

@Composable
fun ShortCard(
    short: PostEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(150.dp)
            .height(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(SatisfyDarkSurfaceVariant)
            .clickable { onClick() }
    ) {
        if (short.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = short.thumbnailUrl,
                contentDescription = short.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // Top Shorts badge
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SatisfyRed)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text("⚡ SHORT", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
        }

        // Bottom Title & Views
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
        ) {
            Text(
                text = short.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = short.views,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
