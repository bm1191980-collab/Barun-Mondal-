package com.example.ui.screens

import android.content.Intent
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.ExoPlayer
import coil.compose.AsyncImage
import com.example.data.model.CommentEntity
import com.example.data.model.PostEntity
import com.example.ui.components.InteractiveVideoPlayer
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerState

@Composable
fun VideoDetailScreen(
    playerState: PlayerState,
    relatedVideos: List<PostEntity>,
    comments: List<CommentEntity>,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekRelative: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onToggleControls: () -> Unit,
    onToggleFullscreen: () -> Unit = {},
    onOpenCreatorProfile: (String, String, Long?) -> Unit = { _, _, _ -> },
    onToggleLike: (PostEntity) -> Unit,
    onToggleDislike: (PostEntity) -> Unit,
    onToggleSave: (PostEntity) -> Unit,
    onToggleSubscribe: (String, Boolean) -> Unit,
    onOpenComments: () -> Unit,
    onSelectRelatedVideo: (PostEntity) -> Unit,
    onWatchTimeTick: (Long) -> Unit = {},
    exoPlayer: ExoPlayer? = null,
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isEffectivelyFullscreen = playerState.isFullscreen || isLandscape
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val nextVideo = remember(post.id, relatedVideos) {
        relatedVideos.firstOrNull { it.id != post.id } ?: relatedVideos.firstOrNull()
    }

    // Intercept back button: if in fullscreen, exit fullscreen; otherwise close player directly inside screen (no PiP)
    BackHandler(enabled = true) {
        if (playerState.isFullscreen) {
            onToggleFullscreen()
        } else {
            onClose()
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(if (isEffectivelyFullscreen) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        val isTabletWideScreen = maxWidth >= 720.dp && !isEffectivelyFullscreen

        if (isEffectivelyFullscreen) {
            // Fullscreen & Landscape Video Player - Expands to 100% full screen
            InteractiveVideoPlayer(
                playerState = playerState.copy(isFullscreen = true),
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                onSeekRelative = onSeekRelative,
                onToggleMute = onToggleMute,
                onSpeedChange = onSpeedChange,
                onQualityChange = onQualityChange,
                onMinimize = onClose,
                onClose = if (playerState.isFullscreen) onToggleFullscreen else onClose,
                onToggleControls = onToggleControls,
                onToggleFullscreen = onToggleFullscreen,
                nextVideo = nextVideo,
                onPlayNextVideo = onSelectRelatedVideo,
                onWatchTimeTick = onWatchTimeTick,
                exoPlayer = exoPlayer,
                modifier = Modifier.fillMaxSize()
            )
        } else if (isTabletWideScreen) {
            // Adaptive 2-Pane Layout for Tablets / Foldables / Landscape
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Pane: Player + Primary Video Metadata
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InteractiveVideoPlayer(
                        playerState = playerState,
                        onTogglePlayPause = onTogglePlayPause,
                        onSeek = onSeek,
                        onSeekRelative = onSeekRelative,
                        onToggleMute = onToggleMute,
                        onSpeedChange = onSpeedChange,
                        onQualityChange = onQualityChange,
                        onMinimize = onMinimize,
                        onClose = onClose,
                        onToggleControls = onToggleControls,
                        onToggleFullscreen = onToggleFullscreen,
                        nextVideo = nextVideo,
                        onPlayNextVideo = onSelectRelatedVideo,
                        onWatchTimeTick = onWatchTimeTick,
                        exoPlayer = exoPlayer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                    )

                    // Video Title & Stats
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${post.views} • ${post.timeAgo} • ${post.tags}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Interactive Actions Bar
                    VideoActionsBar(
                        post = post,
                        onToggleLike = { onToggleLike(post) },
                        onToggleDislike = { onToggleDislike(post) },
                        onToggleSave = { onToggleSave(post) },
                        onShare = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Watch '${post.title}' on Satisfy Video: https://satisfy.social/watch?v=${post.id}"
                                )
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Share Satisfy Video")
                            context.startActivity(shareIntent)
                        }
                    )

                    // Channel Subscribe Bar
                    ChannelSubscribeBar(
                        post = post,
                        onOpenCreatorProfile = { onOpenCreatorProfile(post.channelName, post.creatorUid, post.pageId) },
                        onToggleSubscribe = { onToggleSubscribe(post.channelName, post.isSubscribed) }
                    )

                    // Description Box
                    VideoDescriptionBox(
                        description = post.description,
                        isExpanded = isDescriptionExpanded,
                        onToggleExpand = { isDescriptionExpanded = !isDescriptionExpanded }
                    )

                    // Comments Preview Box
                    CommentsPreviewBox(
                        commentCount = post.commentCount,
                        topComment = comments.firstOrNull(),
                        onOpenComments = onOpenComments
                    )
                }

                // Right Pane: Related Videos Up Next
                LazyColumn(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "Up Next on Satisfy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(relatedVideos.filter { it.id != post.id }, key = { it.id }) { related ->
                        RelatedVideoCard(
                            post = related,
                            onClick = { onSelectRelatedVideo(related) }
                        )
                    }
                }
            }
        } else {
            // Standard Portrait / Phone Layout
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                InteractiveVideoPlayer(
                    playerState = playerState,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeek = onSeek,
                    onSeekRelative = onSeekRelative,
                    onToggleMute = onToggleMute,
                    onSpeedChange = onSpeedChange,
                    onQualityChange = onQualityChange,
                    onMinimize = onMinimize,
                    onClose = onClose,
                    onToggleControls = onToggleControls,
                    onToggleFullscreen = onToggleFullscreen,
                    nextVideo = nextVideo,
                    onPlayNextVideo = onSelectRelatedVideo,
                    onWatchTimeTick = onWatchTimeTick,
                    exoPlayer = exoPlayer,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Video Title & Stats
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = post.title,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${post.views} • ${post.timeAgo} • ${post.tags}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Interactive Actions Bar (Like, Dislike, Share, Save)
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            VideoActionsBar(
                                post = post,
                                onToggleLike = { onToggleLike(post) },
                                onToggleDislike = { onToggleDislike(post) },
                                onToggleSave = { onToggleSave(post) },
                                onShare = {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "Watch '${post.title}' on Satisfy Video: https://satisfy.social/watch?v=${post.id}"
                                        )
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share Satisfy Video")
                                    context.startActivity(shareIntent)
                                }
                            )
                        }
                    }

                    // Channel Subscribe Bar
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            ChannelSubscribeBar(
                                post = post,
                                onOpenCreatorProfile = { onOpenCreatorProfile(post.channelName, post.creatorUid, post.pageId) },
                                onToggleSubscribe = { onToggleSubscribe(post.channelName, post.isSubscribed) }
                            )
                        }
                    }

                    // Expandable Description Box
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            VideoDescriptionBox(
                                description = post.description,
                                isExpanded = isDescriptionExpanded,
                                onToggleExpand = { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                        }
                    }

                    // Quick Comments Preview Card
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CommentsPreviewBox(
                                commentCount = post.commentCount,
                                topComment = comments.firstOrNull(),
                                onOpenComments = onOpenComments
                            )
                        }
                    }

                    // Up Next Header
                    item {
                        Text(
                            text = "Up Next on Satisfy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }

                    // Related Videos List
                    items(relatedVideos.filter { it.id != post.id }, key = { it.id }) { related ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            RelatedVideoCard(
                                post = related,
                                onClick = { onSelectRelatedVideo(related) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoActionsBar(
    post: PostEntity,
    onToggleLike: () -> Unit,
    onToggleDislike: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Like / Dislike Split Pill
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier.height(40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Like
                Row(
                    modifier = Modifier
                        .clickable { onToggleLike() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                        contentDescription = "Like",
                        tint = if (post.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${post.likeCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (post.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                VerticalDivider(
                    modifier = Modifier
                        .height(18.dp)
                        .padding(vertical = 2.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )

                // Dislike
                Box(
                    modifier = Modifier
                        .clickable { onToggleDislike() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (post.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                        contentDescription = "Dislike",
                        tint = if (post.isDisliked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Share Button
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .height(40.dp)
                .clickable { onShare() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Save Button
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (post.isSaved) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(
                1.dp,
                if (post.isSaved) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier
                .height(40.dp)
                .clickable { onToggleSave() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "Save",
                    tint = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (post.isSaved) "Saved" else "Save",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (post.isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun ChannelSubscribeBar(
    post: PostEntity,
    onOpenCreatorProfile: () -> Unit,
    onToggleSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onOpenCreatorProfile() }
                    .padding(end = 8.dp)
                    .testTag("video_detail_creator_profile_btn")
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            ),
                            CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (post.channelAvatar.isNotBlank()) {
                        AsyncImage(
                            model = post.channelAvatar,
                            contentDescription = post.channelName,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(1.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = post.channelName.take(1),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = post.channelName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (post.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                    Text(
                        text = post.subscriberCount,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onToggleSubscribe,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (post.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (post.isSubscribed) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = if (post.isSubscribed) 0.dp else 2.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (post.isSubscribed) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "Subscribed Bell",
                        modifier = Modifier.size(14.dp),
                        tint = SatisfyGold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subscribed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Subscribe", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun VideoDescriptionBox(
    description: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Description",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = "Toggle Description",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = description.ifBlank { "No description provided for this video." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (isExpanded) 20 else 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CommentsPreviewBox(
    commentCount: Long,
    topComment: CommentEntity?,
    onOpenComments: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onOpenComments() },
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Comments",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "$commentCount",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Filled.UnfoldMore,
                    contentDescription = "Open comments",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }

            if (topComment != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = topComment.authorName.take(1),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${topComment.authorName}: ${topComment.text}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RelatedVideoCard(
    post: PostEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            // Mini Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 72.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = post.duration,
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = post.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = post.channelName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${post.views} • ${post.timeAgo}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
