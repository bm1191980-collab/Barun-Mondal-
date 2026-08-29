package com.example.ui.components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.SatisfyGold
import com.example.ui.theme.SatisfyRed
import com.example.ui.viewmodel.PlayerState
import kotlin.math.roundToInt

/**
 * Floating Picture-in-Picture (PiP) Mini Player.
 * - Draggable to any screen corner or position with bounds clamping.
 * - Displays active video thumbnail/stream with title & channel.
 * - Real-time progress bar.
 * - Play/Pause, Expand/Fullscreen, and Close controls.
 * - Allows full background interaction with other app features, swipe, and search.
 */
@Composable
fun FloatingMiniPlayer(
    playerState: PlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val playerWidthDp = 240.dp
    val playerHeightDp = 84.dp

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
    val infiniteTransition = rememberInfiniteTransition(label = "border_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .width(playerWidthDp)
            .height(playerHeightDp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = SatisfyRed.copy(alpha = 0.5f))
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1E1E28),
                        Color(0xFF121218)
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
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.5.dp,
                color = if (playerState.isPlaying) SatisfyRed.copy(alpha = glowAlpha) else Color.White.copy(alpha = 0.15f)
            )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Progress Bar
                LinearProgressIndicator(
                    progress = {
                        if (playerState.durationSeconds > 0f) {
                            (playerState.currentPositionSeconds / playerState.durationSeconds).coerceIn(0f, 1f)
                        } else 0f
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = SatisfyRed,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Video Thumbnail with Play badge & drag indicator
                    Box(
                        modifier = Modifier
                            .size(width = 68.dp, height = 52.dp)
                            .clip(RoundedCornerShape(10.dp))
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

                        // Gradient overlay on thumbnail
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                        )

                        // Mini Drag Icon hint at top-left
                        Icon(
                            imageVector = Icons.Filled.DragHandle,
                            contentDescription = "Drag to reposition",
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(2.dp)
                                .size(14.dp)
                        )

                        // HD / Live badge
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = if (playerState.isPlaying) SatisfyRed else Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(2.dp)
                        ) {
                            Text(
                                text = if (playerState.isPlaying) "PIP" else "PAUSE",
                                color = Color.White,
                                fontSize = 7.5.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Title & Channel info
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = post.title,
                            color = Color.White,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = post.channelName,
                            color = SatisfyGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Quick Actions (Play/Pause, Fullscreen/Expand, Close)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-2).dp)
                    ) {
                        // Play / Pause Button
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Filled.PauseCircleFilled else Icons.Filled.PlayCircleFilled,
                                contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                tint = if (playerState.isPlaying) SatisfyRed else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Expand / Return to Full Video Button
                        IconButton(
                            onClick = onExpand,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Fullscreen,
                                contentDescription = "Expand to Fullscreen",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Close Button
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close Mini Player",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
