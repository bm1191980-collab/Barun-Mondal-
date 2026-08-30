package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerState
import kotlin.math.roundToInt

/**
 * Floating Picture-in-Picture (PiP) Mini Player.
 * - Live continuous video & audio playback after minimizing.
 * - Resumes full screen at the exact timestamp when expanded.
 * - Play/Pause, Full Screen (Expand), and Close controls.
 * - Draggable anywhere across the screen.
 */
@OptIn(UnstableApi::class)
@Composable
fun FloatingMiniPlayer(
    playerState: PlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    exoPlayer: ExoPlayer? = null,
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val playerWidthDp = 270.dp
    val playerHeightDp = 92.dp

    val playerWidthPx = with(density) { playerWidthDp.toPx() }
    val playerHeightPx = with(density) { playerHeightDp.toPx() }

    // Initial position: Bottom-Right corner above navigation bar
    var offsetX by remember {
        mutableFloatStateOf((screenWidthPx - playerWidthPx - with(density) { 16.dp.toPx() }).coerceAtLeast(0f))
    }
    var offsetY by remember {
        mutableFloatStateOf((screenHeightPx - playerHeightPx - with(density) { 96.dp.toPx() }).coerceAtLeast(0f))
    }

    // Breathing border glow animation while playing
    val infiniteTransition = rememberInfiniteTransition(label = "miniPlayerGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    fun formatMiniTimestamp(seconds: Float): String {
        val total = seconds.toInt().coerceAtLeast(0)
        val m = total / 60
        val s = total % 60
        return String.format(java.util.Locale.US, "%02d:%02d", m, s)
    }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(playerWidthDp)
            .height(playerHeightDp)
            .shadow(
                16.dp,
                RoundedCornerShape(18.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            )
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF131D31),
                        Color(0xFF0C121E)
                    )
                )
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val maxX = (screenWidthPx - playerWidthPx).coerceAtLeast(0f)
                    val maxY = (screenHeightPx - playerHeightPx - with(density) { 40.dp.toPx() }).coerceAtLeast(0f)
                    val minY = with(density) { 48.dp.toPx() }

                    offsetX = (offsetX + dragAmount.x).coerceIn(0f, maxX)
                    offsetY = (offsetY + dragAmount.y).coerceIn(minY, maxY)
                }
            }
            .clickable { onExpand() }
    ) {
        // Glowing outline
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(18.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (playerState.isPlaying) {
                    MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha)
                } else {
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                }
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Progress Bar with gradient
                LinearProgressIndicator(
                    progress = {
                        if (playerState.durationSeconds > 0f) {
                            (playerState.currentPositionSeconds / playerState.durationSeconds).coerceIn(0f, 1f)
                        } else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video View / Live Player Surface with drag indicator
                    Box(
                        modifier = Modifier
                            .size(width = 82.dp, height = 60.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black)
                    ) {
                        if (exoPlayer != null) {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                                        layoutParams = FrameLayout.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                    }
                                },
                                update = { playerView ->
                                    playerView.player = exoPlayer
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (post.thumbnailUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.thumbnailUrl,
                                contentDescription = post.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Mini Drag Handle hint at top-left
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "Drag to reposition",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .size(14.dp)
                        )

                        // PIP / LIVE badge
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (playerState.isPlaying) SatisfyRed else Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                        ) {
                            Text(
                                text = if (playerState.isPlaying) "LIVE" else "PAUSE",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title, Channel & Real-Time Timestamp
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 2.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = post.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = post.channelName,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${formatMiniTimestamp(playerState.currentPositionSeconds)} / ${formatMiniTimestamp(playerState.durationSeconds)}",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }

                    // Quick Actions: Play/Pause, Full Screen, Close
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-4).dp)
                    ) {
                        // 1. Play / Pause Control
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("mini_player_play_pause")
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                contentDescription = if (playerState.isPlaying) "Pause video" else "Play video",
                                tint = if (playerState.isPlaying) MaterialTheme.colorScheme.primary else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        // 2. Full Screen / Expand Control
                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("mini_player_fullscreen")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Full Screen / Expand video",
                                tint = Color.White.copy(alpha = 0.95f),
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // 3. Close Control
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("mini_player_close")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close video player",
                                tint = Color.White.copy(alpha = 0.75f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
