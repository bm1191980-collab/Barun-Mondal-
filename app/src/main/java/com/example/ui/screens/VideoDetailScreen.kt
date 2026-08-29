package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return
    val context = LocalContext.current
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    val nextVideo = remember(post.id, relatedVideos) {
        relatedVideos.firstOrNull { it.id != post.id } ?: relatedVideos.firstOrNull()
    }

    // Intercept back button: if in fullscreen, exit fullscreen; otherwise minimize to Floating Mini Player
    BackHandler(enabled = true) {
        if (playerState.isFullscreen) {
            onToggleFullscreen()
        } else {
            onMinimize()
        }
    }

    if (playerState.isFullscreen) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
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
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Sticky Video Player at Top
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
                onWatchTimeTick = onWatchTimeTick
            )

            // Scrollable Video Details & Related Content
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

            // Interactive Actions Bar (Like, Dislike, Share, Save, Remix)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like / Dislike Split Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Like
                            Row(
                                modifier = Modifier
                                    .clickable { onToggleLike(post) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = "Like",
                                    tint = if (post.isLiked) SatisfyRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${post.likeCount}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (post.isLiked) SatisfyRed else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            VerticalDivider(
                                modifier = Modifier
                                    .height(18.dp)
                                    .padding(vertical = 2.dp),
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                            )

                            // Dislike
                            Box(
                                modifier = Modifier
                                    .clickable { onToggleDislike(post) }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (post.isDisliked) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                                    contentDescription = "Dislike",
                                    tint = if (post.isDisliked) SatisfyRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Share Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .height(38.dp)
                            .clickable {
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
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
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

                    // Save / Bookmark Button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (post.isSaved) SatisfyGold.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .height(38.dp)
                            .clickable { onToggleSave(post) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = if (post.isSaved) SatisfyGold else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (post.isSaved) "Saved" else "Save",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (post.isSaved) SatisfyGold else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Channel Subscribe Bar
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Channel Info (Click to open Public Creator Profile)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onOpenCreatorProfile(post.channelName, post.creatorUid, post.pageId)
                                }
                                .padding(end = 8.dp)
                                .testTag("video_detail_creator_profile_btn")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyDarkSurfaceVariant)
                            ) {
                                if (post.channelAvatar.isNotBlank()) {
                                    AsyncImage(
                                        model = post.channelAvatar,
                                        contentDescription = post.channelName,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = post.channelName.take(1),
                                            fontWeight = FontWeight.Bold,
                                            color = SatisfyRed
                                        )
                                    }
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
                                            tint = SatisfyRedLight,
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

                        // Subscribe Button
                        Button(
                            onClick = { onToggleSubscribe(post.channelName, post.isSubscribed) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (post.isSubscribed) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF2196F3),
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

            // Expandable Description Box
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Description",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (isDescriptionExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = "Toggle Description",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = post.description.ifBlank { "No description provided for this video." },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = if (isDescriptionExpanded) 20 else 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Comments Preview Card
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenComments() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
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
                                Text(
                                    text = "${post.commentCount}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.UnfoldMore,
                                contentDescription = "Open comments",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (comments.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val topComment = comments.first()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(SatisfyRed.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = topComment.authorName.take(1),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SatisfyRed
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectRelatedVideo(related) }
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    // Mini Thumbnail
                    Box(
                        modifier = Modifier
                            .size(width = 120.dp, height = 70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black)
                    ) {
                        if (related.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = related.thumbnailUrl,
                                contentDescription = related.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.Black.copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = related.duration,
                                fontSize = 10.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = related.title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = related.channelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${related.views} • ${related.timeAgo}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
}
