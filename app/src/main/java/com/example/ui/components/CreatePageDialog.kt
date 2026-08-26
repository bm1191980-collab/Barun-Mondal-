package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePageDialog(
    onDismiss: () -> Unit,
    onCreatePage: (
        name: String,
        category: String,
        description: String,
        handle: String,
        link: String,
        avatarUri: Uri?,
        bannerUri: Uri?
    ) -> Unit
) {
    var pageName by remember { mutableStateOf("") }
    var pageHandle by remember { mutableStateOf("") }
    var pageCategory by remember { mutableStateOf("Entertainment") }
    var pageDescription by remember { mutableStateOf("") }
    var pageLink by remember { mutableStateOf("") }

    var selectedAvatarUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBannerUri by remember { mutableStateOf<Uri?>(null) }

    val categories = listOf(
        "Entertainment", "Technology", "Gaming", "Vlog", "Cooking", "Art", "Travel", "Music", "Education", "Lifestyle"
    )

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedAvatarUri = uri
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedBannerUri = uri
        }
    }

    // Auto generate handle
    LaunchedEffect(pageName) {
        if (pageHandle.isBlank() || pageHandle.startsWith("@")) {
            val sanitized = pageName.trim().lowercase().replace("\\s+".toRegex(), "_")
            if (sanitized.isNotBlank()) {
                pageHandle = "@$sanitized"
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(SatisfyRed),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddBusiness,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "নতুন পেজ তৈরি করুন",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Create Creator Page & Unlock Watch Time",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                }

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Banner & Avatar Live Preview Picker
                    Text(
                        text = "পেজের ছবি ও ব্যানার (Gallery Upload)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(SatisfyRedDark, Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            )
                            .clickable {
                                bannerPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                    ) {
                        if (selectedBannerUri != null) {
                            AsyncImage(
                                model = selectedBannerUri,
                                contentDescription = "Selected Banner",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("ব্যানার বাছুন", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Avatar Picker inside Banner
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 12.dp, bottom = 8.dp)
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(2.dp)
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                        ) {
                            if (selectedAvatarUri != null) {
                                AsyncImage(
                                    model = selectedAvatarUri,
                                    contentDescription = "Selected Avatar",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = SatisfyRed)
                                }
                            }
                        }
                    }

                    // Page Name Input
                    OutlinedTextField(
                        value = pageName,
                        onValueChange = { pageName = it },
                        label = { Text("পেজের নাম * (Page Name)") },
                        placeholder = { Text("যেমন: Satisfy Gaming Hub") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Badge, contentDescription = null, tint = SatisfyRed)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("page_name_input")
                    )

                    // Page Handle Input
                    OutlinedTextField(
                        value = pageHandle,
                        onValueChange = { pageHandle = it },
                        label = { Text("ইউজারনেম / হ্যান্ডেল (@Handle)") },
                        placeholder = { Text("@page_handle") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.AlternateEmail, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Category Selector
                    Column {
                        Text(
                            text = "ক্যাটাগরি নির্বাচন করুন (Category)",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = pageCategory == cat,
                                    onClick = { pageCategory = cat },
                                    label = { Text(cat, fontSize = 12.sp) },
                                    leadingIcon = if (pageCategory == cat) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Description / Bio
                    OutlinedTextField(
                        value = pageDescription,
                        onValueChange = { pageDescription = it },
                        label = { Text("পেজের বিবরণ / Bio") },
                        placeholder = { Text("এই পেজে আপনি কী ধরণের ভিডিও ও কন্টেন্ট প্রকাশ করবেন?") },
                        minLines = 3,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Website / Social Link
                    OutlinedTextField(
                        value = pageLink,
                        onValueChange = { pageLink = it },
                        label = { Text("ওয়েবসাইট অথবা সোশ্যাল লিংক (ঐচ্ছিক)") },
                        placeholder = { Text("satisfy.app/@your_page") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.Link, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Information Note
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = SatisfyBlue.copy(alpha = 0.10f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SatisfyBlue.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = SatisfyBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "পেজ তৈরি সম্পন্ন হলে পেজের স্টুডিওতে সরাসরি 'ভিডিও ওয়াচ টাইম' এবং 'মনিটাইজেশন অ্যানালিটিক্স' দেখতে পারবেন।",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Footer Buttons
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("বাতিল")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onCreatePage(
                                    pageName.trim(),
                                    pageCategory,
                                    pageDescription.trim(),
                                    pageHandle.trim(),
                                    pageLink.trim(),
                                    selectedAvatarUri,
                                    selectedBannerUri
                                )
                            },
                            enabled = pageName.isNotBlank(),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SatisfyRed),
                            modifier = Modifier.testTag("submit_create_page_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("পেজ তৈরি করুন", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
