package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.CreatorPageEntity
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorPageDetailScreen(
    page: CreatorPageEntity,
    allVideos: List<PostEntity>,
    onBack: () -> Unit,
    onVideoClick: (PostEntity) -> Unit,
    onUpdateAvatarUri: (Long, Uri) -> Unit,
    onUpdateBannerUri: (Long, Uri) -> Unit,
    onEditPageInfo: (Long, String, String, String, String) -> Unit,
    onDeletePage: (CreatorPageEntity) -> Unit,
    onUploadToPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Watch Time & Analytics", "Videos", "About Page")

    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isFollowing by remember { mutableStateOf(true) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Page Avatar Picker
    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateAvatarUri(page.id, uri)
            snackbarMessage = "পেজের প্রোফাইল ছবি আপডেট হয়েছে!"
        }
    }

    // Page Banner Picker
    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUpdateBannerUri(page.id, uri)
            snackbarMessage = "পেজের কভার ব্যানার আপডেট হয়েছে!"
        }
    }

    // Calculate watch time metrics
    val totalSeconds = remember(page.totalWatchTimeSeconds, allVideos) {
        if (page.totalWatchTimeSeconds > 0) {
            page.totalWatchTimeSeconds
        } else {
            // Aggregate from videos or calculate realistic baseline
            val videoSecs = allVideos.sumOf { it.watchTimeSeconds }
            if (videoSecs > 0) videoSecs else 485000L
        }
    }

    val totalHours = totalSeconds / 3600.0
    val totalMinutes = totalSeconds / 60
    val totalPageViews = if (page.totalViews > 0) page.totalViews else allVideos.sumOf { it.viewCount } + 54200L

    // Progress toward 4,000 Watch Hours monetization target
    val monetizationTarget = 4000.0
    val monetizationProgress = (totalHours / monetizationTarget).coerceIn(0.0, 1.0).toFloat()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Top App Bar with back and actions
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("page_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Text(
                        text = page.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                    )

                    Row {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit Page",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete Page",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            // Cover Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(SatisfyRedDark, Color(0xFF4A0E4E), Color(0xFF0F172A))
                            )
                        )
                ) {
                    if (page.bannerUrl.isNotBlank()) {
                        AsyncImage(
                            model = page.bannerUrl,
                            contentDescription = "Page Banner",
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable {
                                    bannerPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                                    )
                                )
                        )
                    }

                    // Change banner button
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clickable {
                                bannerPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddPhotoAlternate,
                                contentDescription = "Change Cover",
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ব্যানার পরিবর্তন", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }

            // Page Header (Avatar, Name, Badges, Follow button)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-36).dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Page Avatar
                        Box(
                            modifier = Modifier
                                .size(86.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(3.dp)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            AsyncImage(
                                model = page.avatarUrl.ifBlank { "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200" },
                                contentDescription = page.name,
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            // Camera icon overlay
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyRed)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CameraAlt,
                                    contentDescription = "Change Avatar",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        // Actions: Follow & Upload
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { isFollowing = !isFollowing },
                                shape = RoundedCornerShape(32.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent
                                )
                            ) {
                                Icon(
                                    imageVector = if (isFollowing) Icons.Filled.Check else Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isFollowing) "Following" else "Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = onUploadToPage,
                                shape = RoundedCornerShape(32.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SatisfyRed,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudUpload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ভিডিও আপলোড", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Title & Verified Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = page.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = "Verified Page",
                            tint = SatisfyBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SatisfyRed.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Page • ${page.category}",
                                color = SatisfyRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = "${page.handle} • ${page.followersCount + (if (isFollowing) 1 else 0)} followers • Creator Page",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (page.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    if (page.websiteLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = "Website",
                                tint = SatisfyBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = page.websiteLink,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = SatisfyBlue
                            )
                        }
                    }
                }
            }

            // Navigation Tabs (Watch Time & Analytics, Videos, About)
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (index == 0) {
                                        Icon(
                                            imageVector = Icons.Filled.Timeline,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = if (selectedTab == 0) SatisfyRed else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = title,
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Tab 0: Watch Time & Analytics Dashboard
            if (selectedTab == 0) {
                // Main Watch Time Stats Cards
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // High-Impact Watch Time Hero Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(SatisfyRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.HourglassBottom,
                                                contentDescription = "Watch Time",
                                                tint = SatisfyRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = "মোট ওয়াচ টাইম (Total Watch Time)",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "পেজের সকল ভিডিওর মোট দেখা সময়কাল",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.ArrowUpward,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "+24.5%",
                                                color = Color(0xFF10B981),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1f Hours", totalHours),
                                            fontSize = 32.sp,
                                            fontWeight = FontWeight.Black,
                                            color = SatisfyRed
                                        )
                                        Text(
                                            text = "≈ %,d মিনিট ওয়াচ টাইম".format(totalMinutes),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "গড় রিটেনশন",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "4m 28s (68%)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = SatisfyGold
                                        )
                                    }
                                }
                            }
                        }

                        // Watch Time Visual Trend Chart (Custom Canvas)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ওয়াচ টাইম অ্যাক্টিভিটি গ্রাফ (গত ২৮ দিন)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "ঘণ্টা/দিন",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Sparkline / Area Chart
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    val points = listOf(
                                        0.2f, 0.35f, 0.28f, 0.45f, 0.60f, 0.55f, 0.72f, 0.68f, 0.85f, 0.92f, 0.88f, 1.0f
                                    )
                                    val w = size.width
                                    val h = size.height
                                    val stepX = w / (points.size - 1)

                                    val path = Path()
                                    val fillPath = Path()

                                    points.forEachIndexed { i, frac ->
                                        val x = i * stepX
                                        val y = h - (frac * (h - 20.dp.toPx())) - 10.dp.toPx()
                                        if (i == 0) {
                                            path.moveTo(x, y)
                                            fillPath.moveTo(x, h)
                                            fillPath.lineTo(x, y)
                                        } else {
                                            path.lineTo(x, y)
                                            fillPath.lineTo(x, y)
                                        }
                                    }
                                    fillPath.lineTo(w, h)
                                    fillPath.close()

                                    // Draw area gradient
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            listOf(SatisfyRed.copy(alpha = 0.35f), SatisfyRed.copy(alpha = 0.02f))
                                        )
                                    )

                                    // Draw line
                                    drawPath(
                                        path = path,
                                        color = SatisfyRed,
                                        style = Stroke(width = 3.dp.toPx())
                                    )

                                    // Draw dots on peaks
                                    points.forEachIndexed { i, frac ->
                                        val x = i * stepX
                                        val y = h - (frac * (h - 20.dp.toPx())) - 10.dp.toPx()
                                        if (i == points.lastIndex || i == points.size / 2) {
                                            drawCircle(color = SatisfyRed, radius = 5.dp.toPx(), center = Offset(x, y))
                                            drawCircle(color = Color.White, radius = 2.5.dp.toPx(), center = Offset(x, y))
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Day 1", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Day 14", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Today", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SatisfyRed)
                                }
                            }
                        }

                        // Monetization Target Card (4,000 Watch Hours Requirement)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.MonetizationOn,
                                            contentDescription = null,
                                            tint = SatisfyGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "মনিটাইজেশন ওয়াচ টাইম লক্ষ্যমাত্রা",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = "${(monetizationProgress * 100).toInt()}%",
                                        fontWeight = FontWeight.Bold,
                                        color = SatisfyGold,
                                        fontSize = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                LinearProgressIndicator(
                                    progress = { monetizationProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = SatisfyGold,
                                    trackColor = MaterialTheme.colorScheme.surface
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f / 4,000 Hours", totalHours),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "লক্ষ্য: ৪,০০০ ঘণ্টা",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Per-Video Watch Time Header
                        Text(
                            text = "প্রতিটি ভিডিওর ওয়াচ টাইম বিশ্লেষণ (Video Breakdown)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Individual Video Watch Time Cards
                items(allVideos) { video ->
                    val videoWatchHours = if (video.watchTimeSeconds > 0) {
                        video.watchTimeSeconds / 3600.0
                    } else {
                        (video.viewCount * 0.0028).coerceAtLeast(1.2)
                    }
                    val retentionPercent = remember(video.id) {
                        65 + (video.id % 25).toInt()
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clickable { onVideoClick(video) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Video Thumbnail
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 65.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = video.thumbnailUrl,
                                    contentDescription = video.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color.Black.copy(alpha = 0.8f),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = video.duration,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.HourglassTop,
                                        contentDescription = null,
                                        tint = SatisfyRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format(java.util.Locale.US, "%.1f Hours Watch Time", videoWatchHours),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SatisfyRed
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                Text(
                                    text = "${video.views} • $retentionPercent% Avg Duration",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Tab 1: Video List Grid
            if (selectedTab == 1) {
                if (allVideos.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VideoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "এই পেজে এখনও কোনো ভিডিও নেই",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = onUploadToPage) {
                                Text("প্রথম ভিডিও আপলোড করুন")
                            }
                        }
                    }
                } else {
                    items(allVideos) { post ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { onVideoClick(post) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 120.dp, height = 70.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    AsyncImage(
                                        model = post.thumbnailUrl,
                                        contentDescription = post.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = post.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${post.views} • ${post.timeAgo}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tab 2: About Page Info
            if (selectedTab == 2) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("পেজের বিবরণ ও তথ্য", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Divider()
                                Row {
                                    Text("ক্যাটাগরি: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(page.category, fontSize = 13.sp)
                                }
                                Row {
                                    Text("ইউজারনেম: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(page.handle, fontSize = 13.sp)
                                }
                                Row {
                                    Text("লিংক: ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(page.websiteLink.ifBlank { "N/A" }, fontSize = 13.sp, color = SatisfyBlue)
                                }
                                Text("বিবরণ:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(page.description.ifBlank { "কোনো বিবরণ নেই।" }, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // Edit Page Dialog
        if (showEditDialog) {
            EditPageDialog(
                page = page,
                onDismiss = { showEditDialog = false },
                onSave = { name, cat, desc, link ->
                    onEditPageInfo(page.id, name, cat, desc, link)
                    showEditDialog = false
                    snackbarMessage = "পেজের তথ্য সংরক্ষিত হয়েছে!"
                }
            )
        }

        // Delete Page Confirm Dialog
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("পেজ মুছে ফেলবেন?") },
                text = { Text("আপনি কি নিশ্চিত যে '${page.name}' পেজটি মুছে ফেলতে চান?") },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirm = false
                            onDeletePage(page)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("মুছুন")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("বাতিল")
                    }
                }
            )
        }
    }
}

@Composable
fun EditPageDialog(
    page: CreatorPageEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, category: String, description: String, link: String) -> Unit
) {
    var name by remember { mutableStateOf(page.name) }
    var category by remember { mutableStateOf(page.category) }
    var description by remember { mutableStateOf(page.description) }
    var link by remember { mutableStateOf(page.websiteLink) }

    val categories = listOf("Entertainment", "Technology", "Gaming", "Vlog", "Cooking", "Art", "Travel", "Music", "Education", "Lifestyle")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("পেজের তথ্য এডিট করুন", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("পেজের নাম") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category Dropdown
                Text("ক্যাটাগরি", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 11.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("পেজের বিবরণ (Bio)") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("ওয়েবসাইট / সোশ্যাল লিংক") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, category, description, link) },
                enabled = name.isNotBlank()
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}
