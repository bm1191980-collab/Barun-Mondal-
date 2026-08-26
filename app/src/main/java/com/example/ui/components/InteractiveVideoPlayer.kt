package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerState
import kotlinx.coroutines.delay
import kotlin.math.sin

@Composable
fun InteractiveVideoPlayer(
    playerState: PlayerState,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSeekRelative: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    onQualityChange: (String) -> Unit,
    onMinimize: () -> Unit,
    onClose: () -> Unit,
    onToggleControls: () -> Unit,
    onWatchTimeTick: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return

    // Dynamic playback ticker
    LaunchedEffect(playerState.isPlaying, playerState.currentPositionSeconds) {
        if (playerState.isPlaying && playerState.currentPositionSeconds < playerState.durationSeconds) {
            delay(1000L)
            onSeek(playerState.currentPositionSeconds + (1f * playerState.playbackSpeed))
            onWatchTimeTick(1L)
        }
    }

    // Canvas animation phase for dynamic video simulation
    val infiniteTransition = rememberInfiniteTransition(label = "video_fx")
    val animPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "anim_phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleControls() },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            onSeekRelative(-10f)
                        } else {
                            onSeekRelative(10f)
                        }
                    }
                )
            }
    ) {
        // Video Scene / Visualizer Background
        if (post.thumbnailUrl.isNotBlank()) {
            AsyncImage(
                model = post.thumbnailUrl,
                contentDescription = post.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Realtime dynamic visual effect overlay while playing
        if (playerState.isPlaying) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            ) {
                val width = size.width
                val height = size.height
                val waveCount = 8

                for (i in 0 until waveCount) {
                    val progress = (animPhase + i * 45) % 360
                    val rad = Math.toRadians(progress.toDouble())
                    val cy = height * 0.7f + (sin(rad) * 20).toFloat()
                    val cx = (width / waveCount) * i + (width / (waveCount * 2))

                    drawCircle(
                        color = SatisfyRed.copy(alpha = 0.25f),
                        radius = 16f + (sin(rad) * 8).toFloat(),
                        center = Offset(cx, cy)
                    )
                }

                // Ambient gradient line
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            SatisfyNeonRed.copy(alpha = 0.7f),
                            SatisfyGold.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    ),
                    start = Offset(0f, height * 0.98f),
                    end = Offset(width, height * 0.98f),
                    strokeWidth = 3f
                )
            }
        }

        // Live HD / 4K Badge (Top Right)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = playerState.quality,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SatisfyGold
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (playerState.isPlaying) SatisfyRed else Color.Gray)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (playerState.isPlaying) "PLAYING" else "PAUSED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }

        // Top Left Minimize / Close Button
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Minimize Player",
                    tint = Color.White
                )
            }
        }

        // Controls Overlay
        AnimatedVisibility(
            visible = playerState.showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                // Center Controls: Rewind 10s, Play/Pause, Forward 10s
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = { onSeekRelative(-10f) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    IconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(SatisfyRed)
                    ) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSeekRelative(10f) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Bottom Scrubber Bar & Timers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Time and action row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatSeconds(playerState.currentPositionSeconds.toLong())} / ${formatSeconds(playerState.durationSeconds.toLong())}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Speed button
                            TextButton(
                                onClick = {
                                    val nextSpeed = when (playerState.playbackSpeed) {
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 1.0f
                                    }
                                    onSpeedChange(nextSpeed)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Mute toggle
                            IconButton(
                                onClick = onToggleMute,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (playerState.isMuted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    contentDescription = "Toggle Mute",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quality toggle
                            TextButton(
                                onClick = {
                                    val nextQuality = when (playerState.quality) {
                                        "1080p" -> "4K HDR"
                                        "4K HDR" -> "720p"
                                        else -> "1080p"
                                    }
                                    onQualityChange(nextQuality)
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = playerState.quality,
                                    color = SatisfyGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Scrubber Slider
                    Slider(
                        value = playerState.currentPositionSeconds,
                        onValueChange = { onSeek(it) },
                        valueRange = 0f..playerState.durationSeconds,
                        colors = SliderDefaults.colors(
                            thumbColor = SatisfyRed,
                            activeTrackColor = SatisfyRed,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    onExpand: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val post = playerState.activePost ?: return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        color = SatisfyDarkSurface,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            // Tiny progress indicator line
            LinearProgressIndicator(
                progress = {
                    if (playerState.durationSeconds > 0f) {
                        playerState.currentPositionSeconds / playerState.durationSeconds
                    } else 0f
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = SatisfyRed,
                trackColor = Color.Transparent
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini Thumbnail
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 36.dp)
                        .clip(RoundedCornerShape(6.dp))
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
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Channel Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = post.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                // Play / Pause
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Close Mini Player
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
