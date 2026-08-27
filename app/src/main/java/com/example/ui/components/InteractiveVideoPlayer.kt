package com.example.ui.components

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.model.PostEntity
import com.example.data.service.SatisfyVideoEngine
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
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
    val context = LocalContext.current

    // Internal player states
    var isBuffering by remember { mutableStateOf(true) }
    var playerError by remember { mutableStateOf<String?>(null) }
    var isPlaybackEnded by remember { mutableStateOf(false) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(if (post.durationSeconds > 0) post.durationSeconds * 1000L else 15000L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var isDraggingSlider by remember { mutableStateOf(false) }
    var sliderScrubPosition by remember { mutableFloatStateOf(0f) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var retryCount by remember { mutableIntStateOf(0) }

    // Double-tap visual indicators
    var doubleTapSeekLeft by remember { mutableStateOf(false) }
    var doubleTapSeekRight by remember { mutableStateOf(false) }

    // Auto-hide controls timer
    var controlsVisible by remember { mutableStateOf(true) }

    // ExoPlayer instance configured with custom DataSource & MediaSource
    val exoPlayer = remember(post.id, post.mediaUrl, retryCount) {
        SatisfyVideoEngine.createExoPlayer(context).apply {
            val mediaItem = SatisfyVideoEngine.createMediaItem(post.mediaUrl, post.type)
            setMediaItem(mediaItem)
            playWhenReady = playerState.isPlaying
            volume = if (playerState.isMuted) 0f else 1f
            playbackParameters = PlaybackParameters(playerState.playbackSpeed)
            prepare()
        }
    }

    // Attach ExoPlayer listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        playerError = null
                        isPlaybackEnded = false
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        playerError = null
                        isPlaybackEnded = false
                        if (exoPlayer.duration > 0) {
                            durationMs = exoPlayer.duration
                        }
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaybackEnded = true
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBuffering = false
                playerError = SatisfyVideoEngine.parsePlaybackException(error)
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Sync play/pause with playerState
    LaunchedEffect(playerState.isPlaying) {
        exoPlayer.playWhenReady = playerState.isPlaying
    }

    // Sync volume/mute with playerState
    LaunchedEffect(playerState.isMuted) {
        exoPlayer.volume = if (playerState.isMuted) 0f else 1f
    }

    // Sync speed with playerState
    LaunchedEffect(playerState.playbackSpeed) {
        exoPlayer.playbackParameters = PlaybackParameters(playerState.playbackSpeed)
    }

    // Real-time polling loop for high precision playback time, buffer track, and watch time recording
    LaunchedEffect(exoPlayer, isDraggingSlider) {
        var lastRecordedSecond = 0L
        while (isActive) {
            if (!isDraggingSlider) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                if (exoPlayer.duration > 0) {
                    durationMs = exoPlayer.duration
                }
            }

            if (exoPlayer.isPlaying) {
                val currentSec = currentPositionMs / 1000L
                if (currentSec != lastRecordedSecond) {
                    lastRecordedSecond = currentSec
                    onWatchTimeTick(1L)
                }
            }

            delay(250L)
        }
    }

    // Auto-hide controls after 4 seconds of inactivity when playing
    LaunchedEffect(controlsVisible, exoPlayer.isPlaying) {
        if (controlsVisible && exoPlayer.isPlaying) {
            delay(4000L)
            controlsVisible = false
        }
    }

    // Reset double-tap indicators after brief animation
    LaunchedEffect(doubleTapSeekLeft) {
        if (doubleTapSeekLeft) {
            delay(650L)
            doubleTapSeekLeft = false
        }
    }

    LaunchedEffect(doubleTapSeekRight) {
        if (doubleTapSeekRight) {
            delay(650L)
            doubleTapSeekRight = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        controlsVisible = !controlsVisible
                    },
                    onDoubleTap = { offset ->
                        if (offset.x < size.width / 2) {
                            // Rewind 10s
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            doubleTapSeekLeft = true
                            controlsVisible = true
                        } else {
                            // Forward 10s
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                            doubleTapSeekRight = true
                            controlsVisible = true
                        }
                    }
                )
            }
    ) {
        // Real Video Surface via AndroidView & PlayerView
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
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

        // Double-Tap Left Indicator
        AnimatedVisibility(
            visible = doubleTapSeekLeft,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Replay10,
                        contentDescription = "Rewind 10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "-10s",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Double-Tap Right Indicator
        AnimatedVisibility(
            visible = doubleTapSeekRight,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "+10s",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Filled.Forward10,
                        contentDescription = "Forward 10s",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Buffering Indicator
        if (isBuffering && playerError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = SatisfyRed,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Buffering stream...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Playback Error Overlay with Retry
        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Error",
                        tint = SatisfyRed,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Unable to play stream",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = playerError ?: "Network or codec issue",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            playerError = null
                            isBuffering = true
                            retryCount++
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Retry",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Playback", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Live HD / Quality Badge (Always visible on top right or with controls)
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.clickable { showQualityDialog = true }
            ) {
                Text(
                    text = playerState.quality,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SatisfyGold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (exoPlayer.isPlaying) SatisfyRed else Color.DarkGray
            ) {
                Text(
                    text = if (exoPlayer.isPlaying) "PLAYING" else "PAUSED",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Top Left Minimize Button
        IconButton(
            onClick = onMinimize,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
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

        // Full Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible && playerError == null,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Center Play/Pause & 10s Rewind/Forward Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Play / Pause / Replay
                    IconButton(
                        onClick = {
                            if (isPlaybackEnded) {
                                exoPlayer.seekTo(0L)
                                exoPlayer.play()
                                isPlaybackEnded = false
                            } else {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                                onTogglePlayPause()
                            }
                        },
                        modifier = Modifier
                            .size(62.dp)
                            .clip(CircleShape)
                            .background(SatisfyRed)
                    ) {
                        Icon(
                            imageVector = when {
                                isPlaybackEnded -> Icons.Filled.Replay
                                exoPlayer.isPlaying -> Icons.Filled.Pause
                                else -> Icons.Filled.PlayArrow
                            },
                            contentDescription = if (exoPlayer.isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(durationMs)
                            exoPlayer.seekTo(newPos)
                            currentPositionMs = newPos
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Scrubber Bar & Settings Row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.95f))
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Time and Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Current time / Total duration
                        val displayCurrentSec = if (isDraggingSlider) {
                            sliderScrubPosition.toLong()
                        } else {
                            (currentPositionMs / 1000L)
                        }
                        val displayDurationSec = (durationMs / 1000L).coerceAtLeast(1L)

                        Text(
                            text = "${formatSeconds(displayCurrentSec)} / ${formatSeconds(displayDurationSec)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Speed button
                            TextButton(
                                onClick = { showSpeedDialog = true },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${playerState.playbackSpeed}x",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Mute / Unmute toggle
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

                            // Quality Button
                            TextButton(
                                onClick = { showQualityDialog = true },
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
                    val maxSec = (durationMs / 1000f).coerceAtLeast(1f)
                    val sliderVal = if (isDraggingSlider) {
                        sliderScrubPosition
                    } else {
                        (currentPositionMs / 1000f).coerceIn(0f, maxSec)
                    }

                    Slider(
                        value = sliderVal,
                        onValueChange = { newVal ->
                            isDraggingSlider = true
                            sliderScrubPosition = newVal
                        },
                        onValueChangeFinished = {
                            isDraggingSlider = false
                            val seekTargetMs = (sliderScrubPosition * 1000f).toLong()
                            exoPlayer.seekTo(seekTargetMs)
                            currentPositionMs = seekTargetMs
                            onSeek(sliderScrubPosition)
                        },
                        valueRange = 0f..maxSec,
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

    // Playback Speed Dialog
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("Playback Speed", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSpeedChange(speed)
                                    showSpeedDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (speed == 1.0f) "1.0x (Normal)" else "${speed}x",
                                color = if (playerState.playbackSpeed == speed) SatisfyRed else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (playerState.playbackSpeed == speed) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playerState.playbackSpeed == speed) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = SatisfyRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Video Quality Dialog
    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Video Quality", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Auto (Recommended)", "1080p Full HD", "720p HD", "480p SD", "360p Data Saver").forEach { q ->
                        val shortQ = when {
                            q.startsWith("1080p") -> "1080p"
                            q.startsWith("720p") -> "720p"
                            q.startsWith("480p") -> "480p"
                            q.startsWith("360p") -> "360p"
                            else -> "Auto"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQualityChange(shortQ)
                                    showQualityDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = q,
                                color = if (playerState.quality == shortQ) SatisfyGold else MaterialTheme.colorScheme.onSurface,
                                fontWeight = if (playerState.quality == shortQ) FontWeight.Bold else FontWeight.Normal
                            )
                            if (playerState.quality == shortQ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) {
                    Text("Close")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        (playerState.currentPositionSeconds / playerState.durationSeconds).coerceIn(0f, 1f)
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
    val hrs = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
