package com.example.ui.screens

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadScreen(
    categories: List<String>,
    onPublish: (type: PostType, title: String, description: String, category: String, tags: String, thumbUrl: String, mediaUrl: String, duration: String) -> Unit,
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    initialType: PostType = PostType.VIDEO
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(initialType) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Satisfying") }
    var tags by remember { mutableStateOf("#Satisfy #Trending") }
    var selectedDuration by remember { mutableStateOf("05:30") }

    // Device Gallery states
    var selectedGalleryUri by remember { mutableStateOf<Uri?>(null) }
    var selectedGalleryFileName by remember { mutableStateOf("") }
    var selectedGalleryFileSize by remember { mutableStateOf("") }
    var selectedCustomThumbnailUri by remember { mutableStateOf<Uri?>(null) }

    var selectedPresetImage by remember {
        mutableStateOf("https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&q=80")
    }
    var customImageUrl by remember { mutableStateOf("") }

    // Helper to query file metadata
    fun extractMediaInfo(uri: Uri, isVideo: Boolean) {
        selectedGalleryUri = uri
        try {
            // Get file name and size
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        val fileName = cursor.getString(nameIndex)
                        selectedGalleryFileName = fileName
                        if (title.isBlank()) {
                            val cleanName = fileName.substringBeforeLast(".")
                                .replace("_", " ")
                                .replace("-", " ")
                            title = cleanName.replaceFirstChar { it.uppercase() }
                        }
                    }
                    if (sizeIndex != -1) {
                        val sizeBytes = cursor.getLong(sizeIndex)
                        val mb = sizeBytes / (1024.0 * 1024.0)
                        selectedGalleryFileSize = if (mb >= 1.0) {
                            String.format("%.1f MB", mb)
                        } else {
                            val kb = sizeBytes / 1024.0
                            String.format("%.0f KB", kb)
                        }
                    }
                }
            }

            // Extract video duration if video
            if (isVideo) {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, uri)
                    val durationMsStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    val durationMs = durationMsStr?.toLongOrNull() ?: 0L
                    if (durationMs > 0) {
                        val totalSeconds = durationMs / 1000
                        val minutes = totalSeconds / 60
                        val seconds = totalSeconds % 60
                        selectedDuration = String.format("%02d:%02d", minutes, seconds)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    try {
                        retriever.release()
                    } catch (ignored: Exception) {}
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            selectedGalleryFileName = uri.lastPathSegment ?: "Gallery File"
        }
    }

    // Gallery Video Pickers
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            extractMediaInfo(uri, isVideo = true)
        }
    }

    val videoFallbackLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            extractMediaInfo(uri, isVideo = true)
        }
    }

    // Gallery Photo Picker
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            extractMediaInfo(uri, isVideo = false)
        }
    }

    val customThumbnailPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedCustomThumbnailUri = uri
        }
    }

    // Preset aesthetic photos/thumbnails for quick selection
    val presetImages = listOf(
        "https://images.unsplash.com/photo-1579783900882-c0d3dad7b119?w=800&q=80", // Kinetic sand
        "https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&q=80", // Tech Cyber
        "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&q=80", // Beach travel
        "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=800&q=80", // Music
        "https://images.unsplash.com/photo-1563379091339-03b21ab4a4f8?w=800&q=80", // Cooking
        "https://images.unsplash.com/photo-1542751371-adc38448a05e?w=800&q=80", // Gaming
        "https://images.unsplash.com/photo-1495616811223-4d98c6e9c869?w=800&q=80", // Golden Sunset
        "https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd?w=800&q=80"  // Coffee
    )

    val activeThumbnail = when {
        selectedCustomThumbnailUri != null -> selectedCustomThumbnailUri.toString()
        selectedGalleryUri != null && selectedType == PostType.PHOTO -> selectedGalleryUri.toString()
        customImageUrl.isNotBlank() -> customImageUrl
        else -> selectedPresetImage
    }

    val activeMediaUrl = selectedGalleryUri?.toString() ?: activeThumbnail

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Upload Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudUpload,
                        contentDescription = "Upload",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Satisfy Creator Studio",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Upload Video, Create Short, or Share Photo",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Post Type Selector (Segmented Row)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PostType.values().forEach { type ->
                        val isSelected = selectedType == type
                        val label = when (type) {
                            PostType.VIDEO -> "🎬 Video"
                            PostType.SHORT -> "⚡ Short"
                            PostType.PHOTO -> "📸 Photo"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(28.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .clickable { selectedType = type }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // --- DEVICE GALLERY PICKER SECTION ---
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (selectedGalleryUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedType == PostType.PHOTO) Icons.Filled.PhotoLibrary else Icons.Filled.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedType == PostType.PHOTO) "Device Photo Gallery" else "Device Video Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "গ্যালারি থেকে সরাসরি আপলোড করুন",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedGalleryUri == null) {
                            // Big Gallery Selection Action Button
                            Button(
                                onClick = {
                                    if (selectedType == PostType.PHOTO) {
                                        try {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        } catch (e: Exception) {
                                            // Fallback
                                            videoFallbackLauncher.launch("image/*")
                                        }
                                    } else {
                                        try {
                                            videoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                            )
                                        } catch (e: Exception) {
                                            // Fallback
                                            videoFallbackLauncher.launch("video/*")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(24.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (selectedType == PostType.PHOTO) "Choose Photo from Gallery" else "Choose Video from Gallery",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            // Selected Media Information Card
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surface,
                                tonalElevation = 3.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Thumbnail / Video icon badge
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (selectedType == PostType.PHOTO) {
                                            AsyncImage(
                                                model = selectedGalleryUri,
                                                contentDescription = "Selected Photo",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Filled.PlayCircle,
                                                contentDescription = "Video",
                                                tint = Color.White,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SatisfyGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                                            ) {
                                                Text(
                                                    text = "✓ GALLERY FILE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = SatisfyGreen
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = selectedGalleryFileName.ifBlank { "Selected Media" },
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (selectedGalleryFileSize.isNotBlank()) {
                                                Text(
                                                    text = selectedGalleryFileSize,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (selectedType != PostType.PHOTO && selectedDuration.isNotBlank()) {
                                                Text(
                                                    text = "• Length: $selectedDuration",
                                                    fontSize = 11.sp,
                                                    color = SatisfyGold
                                                )
                                            }
                                        }
                                    }

                                    // Action to change/remove
                                    Column(horizontalAlignment = Alignment.End) {
                                        IconButton(
                                            onClick = {
                                                selectedGalleryUri = null
                                                selectedGalleryFileName = ""
                                                selectedGalleryFileSize = ""
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "Remove",
                                                tint = SatisfyRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                if (selectedType == PostType.PHOTO) {
                                                    photoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                    )
                                                } else {
                                                    videoPickerLauncher.launch(
                                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                                    )
                                                }
                                            },
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text(
                                                text = "Change",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Custom Thumbnail Picker Option for Videos
                        if (selectedType != PostType.PHOTO) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        customThumbnailPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedCustomThumbnailUri != null) "Custom Thumbnail: Attached ✓" else "Select Custom Thumbnail from Gallery (ঐচ্ছিক)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedCustomThumbnailUri != null) SatisfyGreen else MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Preset Thumbnails & Web URLs (Alternative Selection)
            item {
                Column {
                    Text(
                        text = if (selectedType == PostType.PHOTO) "Or Pick Aesthetic Preset / Web Photo" else "Or Pick Preset Thumbnail / Web Link",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Image Picker Horizontal Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(presetImages) { imgUrl ->
                            val isSelected = activeThumbnail == imgUrl && selectedCustomThumbnailUri == null
                            Box(
                                modifier = Modifier
                                    .size(width = 90.dp, height = 65.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) SatisfyRed else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        selectedCustomThumbnailUri = null
                                        customImageUrl = ""
                                        selectedPresetImage = imgUrl
                                    }
                            ) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(SatisfyRed),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Custom URL Input Option
                    OutlinedTextField(
                        value = customImageUrl,
                        onValueChange = {
                            customImageUrl = it
                            selectedCustomThumbnailUri = null
                        },
                        label = { Text("Or Paste Custom Image/Thumbnail URL") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Link, contentDescription = null, tint = SatisfyRed)
                        },
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Title Input
            item {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title *") },
                        placeholder = {
                            Text(
                                when (selectedType) {
                                    PostType.VIDEO -> "e.g. 4K Relaxing Forest ASMR & Waterfall"
                                    PostType.SHORT -> "e.g. Satisfying neon light animation #shorts"
                                    PostType.PHOTO -> "e.g. Stunning golden sunset over the skyline"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedType == PostType.VIDEO) Icons.Filled.Title else Icons.Filled.Edit,
                                contentDescription = null,
                                tint = SatisfyRed
                            )
                        }
                    )

                    // Quick Title Suggestions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val suggestion = when (selectedType) {
                            PostType.VIDEO -> "Kinetic Sand ASMR 4K ✨"
                            PostType.SHORT -> "Amazing 3D Illusion ⚡"
                            PostType.PHOTO -> "Golden Hour Magic 🌅"
                        }
                        SuggestionChip(
                            onClick = { title = suggestion },
                            label = { Text("Suggest: $suggestion", fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            // Description Input
            item {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description & Notes") },
                    placeholder = { Text("Tell your viewers what makes this video/post satisfying...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Category Selection
            item {
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories.filter { it != "All" }) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal) },
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
                }
            }

            // Tags Input
            item {
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Hashtags") },
                    placeholder = { Text("#Satisfying #Trending #Bangla #4K") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Tag, contentDescription = null, tint = SatisfyBlue)
                    }
                )
            }

            // Video Duration (if Video)
            if (selectedType == PostType.VIDEO) {
                item {
                    OutlinedTextField(
                        value = selectedDuration,
                        onValueChange = { selectedDuration = it },
                        label = { Text("Duration (MM:SS)") },
                        placeholder = { Text("05:30") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(imageVector = Icons.Filled.Timer, contentDescription = null, tint = SatisfyGold)
                        }
                    )
                }
            }

            // Live Card Preview
            item {
                Column {
                    Text(
                        text = "Live Preview",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .background(Color.Black)
                            ) {
                                AsyncImage(
                                    model = activeThumbnail,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color.Black.copy(alpha = 0.8f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (selectedType == PostType.SHORT) "0:45" else selectedDuration,
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = title.ifBlank { "Untitled ${selectedType.name.lowercase().replaceFirstChar { it.uppercase() }}" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "You • $selectedCategory • Just now",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Publish Button
            item {
                Button(
                    onClick = {
                        onPublish(
                            selectedType,
                            title,
                            description,
                            selectedCategory,
                            tags,
                            activeThumbnail,
                            activeMediaUrl,
                            selectedDuration
                        )
                    },
                    enabled = title.isNotBlank() && !isUploading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(32.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Publishing to Satisfy...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(
                            imageVector = Icons.Filled.RocketLaunch,
                            contentDescription = "Publish",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (selectedType) {
                                PostType.VIDEO -> "Publish Video"
                                PostType.SHORT -> "Publish Short"
                                PostType.PHOTO -> "Share Photo Post"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
