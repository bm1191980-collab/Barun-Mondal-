package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    allPosts: List<PostEntity>,
    recentSearches: List<RecentSearchEntity> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onRemoveRecentSearch: (String) -> Unit = {},
    onClearRecentSearches: () -> Unit = {},
    onSelectPost: (PostEntity) -> Unit,
    onBack: () -> Unit,
    onCreatorClick: (String, String, Long?) -> Unit = { _, _, _ -> },
    onToggleLike: (PostEntity) -> Unit = {},
    onToggleSave: (PostEntity) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }
    val coroutineScope = rememberCoroutineScope()

    var searchFilter by remember { mutableStateOf("All") }
    var trendingTab by remember { mutableStateOf("🔥 Hot Now") }
    var isGridView by remember { mutableStateOf(false) }
    var isVoiceListening by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    // Curated rich trending topics by category
    val trendingSearches = remember(trendingTab) {
        when (trendingTab) {
            "✨ 4K ASMR" -> listOf(
                TrendingSearchItem(query = "Deep Soap Carving ASMR", category = "ASMR", subtitle = "3.8M searches", views = "🔥 TOP 1", rank = 1, isHot = true),
                TrendingSearchItem(query = "Crushing Crunchy Blocks 4K", category = "ASMR", subtitle = "2.1M searches", views = "📈 RISING", rank = 2),
                TrendingSearchItem(query = "Kinetic Sand Slicing Audio", category = "ASMR", subtitle = "1.9M searches", views = "✨ 4K", rank = 3),
                TrendingSearchItem(query = "Satisfying Slime Stretching", category = "ASMR", subtitle = "1.4M searches", views = "⚡ VIRAL", rank = 4),
                TrendingSearchItem(query = "Soft Foam Squishing Relaxation", category = "ASMR", subtitle = "980K searches", views = "🎧 BINAURAL", rank = 5)
            )
            "🌿 Nature" -> listOf(
                TrendingSearchItem(query = "4K Ultra HD Forest Rain & Thunder", category = "Nature", subtitle = "2.6M searches", views = "🔥 TOP 1", rank = 1, isHot = true),
                TrendingSearchItem(query = "Crystal Clear Ocean Drone FPV", category = "Nature", subtitle = "1.8M searches", views = "✨ 4K UHD", rank = 2),
                TrendingSearchItem(query = "Patagonia Mountain Sunrise", category = "Nature", subtitle = "1.2M searches", views = "🌿 RELAX", rank = 3),
                TrendingSearchItem(query = "Bangladesh 4K River Calm", category = "Nature", subtitle = "940K searches", views = "🇧🇩 TRENDING", rank = 4),
                TrendingSearchItem(query = "Snow Fall in Silent Woods", category = "Nature", subtitle = "820K searches", views = "❄️ COZY", rank = 5)
            )
            "🎨 Art & Craft" -> listOf(
                TrendingSearchItem(query = "Pottery Wheel Clay Spinning", category = "Crafts", subtitle = "2.4M searches", views = "🔥 TOP 1", rank = 1, isHot = true),
                TrendingSearchItem(query = "Epoxy Resin Wood Ocean Table", category = "Crafts", subtitle = "1.7M searches", views = "✨ 4K", rank = 2),
                TrendingSearchItem(query = "Calligraphy Ink Flowing SlowMo", category = "Crafts", subtitle = "1.3M searches", views = "🎨 ART", rank = 3),
                TrendingSearchItem(query = "Glass Blowing Spiral Sculpting", category = "Crafts", subtitle = "990K searches", views = "🔥 HOT", rank = 4),
                TrendingSearchItem(query = "Leather Craft Hand Stitching", category = "Crafts", subtitle = "750K searches", views = "🧵 CRAFT", rank = 5)
            )
            "⚡ Viral" -> listOf(
                TrendingSearchItem(query = "1000 Ton Hydraulic Press vs Everything", category = "Viral", subtitle = "5.2M searches", views = "🔥 #1 VIRAL", rank = 1, isHot = true),
                TrendingSearchItem(query = "Laser Rust Cleaning 4K Ultra", category = "Viral", subtitle = "3.4M searches", views = "⚡ VIRAL", rank = 2),
                TrendingSearchItem(query = "Industrial Shredder High Speed", category = "Viral", subtitle = "2.8M searches", views = "💥 CRAZY", rank = 3),
                TrendingSearchItem(query = "Color Sorting Kinetic Marble Run", category = "Viral", subtitle = "1.6M searches", views = "🎯 60FPS", rank = 4),
                TrendingSearchItem(query = "Perfect Paint Mixing Palette ASMR", category = "Viral", subtitle = "1.1M searches", views = "🌈 ART", rank = 5)
            )
            else -> listOf(
                TrendingSearchItem(query = "Kinetic Sand Cutting & Crushing", category = "Satisfying", subtitle = "4.6M searches", views = "🔥 #1 TRENDING", rank = 1, isHot = true),
                TrendingSearchItem(query = "4K Hydraulic Press Experiments", category = "Physics", subtitle = "3.2M searches", views = "⚡ VIRAL", rank = 2),
                TrendingSearchItem(query = "Deep Soap Carving ASMR", category = "ASMR", subtitle = "2.7M searches", views = "📈 RISING", rank = 3),
                TrendingSearchItem(query = "Epoxy Resin River Table Pour", category = "Crafts", subtitle = "2.1M searches", views = "✨ 4K UHD", rank = 4),
                TrendingSearchItem(query = "Calm Ocean Waves 60 FPS Drone", category = "Nature", subtitle = "1.8M searches", views = "🌿 RELAX", rank = 5),
                TrendingSearchItem(query = "Satisfying Paint Mixing Colors", category = "Art", subtitle = "1.4M searches", views = "🎨 COLOR", rank = 6),
                TrendingSearchItem(query = "Laser Metal Rust Removal 4K", category = "Tech", subtitle = "1.1M searches", views = "⚡ FAST", rank = 7),
                TrendingSearchItem(query = "Pottery Centering Wheel ASMR", category = "Crafts", subtitle = "890K searches", views = "🎧 AUDIO", rank = 8)
            )
        }
    }

    // Extract unique creators from content
    val allCreators = remember(allPosts) {
        allPosts.groupBy { it.channelName.trim() }
            .filterKeys { it.isNotBlank() }
            .map { (name, creatorPosts) ->
                val first = creatorPosts.first()
                CreatorSearchResult(
                    channelName = name,
                    channelAvatar = first.channelAvatar,
                    creatorUid = first.creatorUid,
                    pageId = first.pageId,
                    subscriberCount = first.subscriberCount,
                    isVerified = creatorPosts.any { it.isVerified },
                    videoCount = creatorPosts.size,
                    topCategory = first.category.ifBlank { "Satisfying Creator" }
                )
            }
            .sortedByDescending { it.videoCount }
    }

    // Extract unique hashtags with post counts
    val allHashtags = remember(allPosts) {
        val tagCounts = mutableMapOf<String, Int>()
        allPosts.forEach { post ->
            val rawTags = post.tags.split(" ", ",", "#").filter { it.isNotBlank() }
            rawTags.forEach { raw ->
                val clean = "#" + raw.trim().trimStart('#')
                if (clean.length > 2) {
                    tagCounts[clean] = (tagCounts[clean] ?: 0) + 1
                }
            }
        }
        val defaultTags = listOf("#Satisfying", "#ASMR", "#4KVisuals", "#Nature", "#Relaxing", "#HydraulicPress", "#KineticSand", "#DroneFPV")
        defaultTags.forEach { tag ->
            if (!tagCounts.containsKey(tag)) {
                tagCounts[tag] = 2
            }
        }
        tagCounts.map { HashtagSearchResult(it.key, it.value) }.sortedByDescending { it.count }
    }

    val q = query.trim().lowercase()
    val qNoHash = q.trimStart('#')

    // Live matching suggestions (Titles, Tags, Creator Names)
    val liveSuggestions = remember(q, allPosts, allHashtags, allCreators) {
        if (q.isBlank()) emptyList() else {
            val list = mutableListOf<SearchSuggestion>()

            // Matching post titles
            allPosts.filter { it.title.lowercase().contains(q) }
                .distinctBy { it.title }
                .take(3)
                .forEach { post ->
                    list.add(
                        SearchSuggestion(
                            text = post.title,
                            type = SuggestionType.VIDEO,
                            subtitle = "${post.views} • ${post.category}"
                        )
                    )
                }

            // Matching creator channels
            allCreators.filter { it.channelName.lowercase().contains(q) }
                .take(2)
                .forEach { creator ->
                    list.add(
                        SearchSuggestion(
                            text = creator.channelName,
                            type = SuggestionType.CREATOR,
                            subtitle = "${creator.subscriberCount} subscribers"
                        )
                    )
                }

            // Matching hashtags
            allHashtags.filter { it.hashtag.lowercase().contains(qNoHash) }
                .take(2)
                .forEach { hash ->
                    list.add(
                        SearchSuggestion(
                            text = hash.hashtag,
                            type = SuggestionType.HASHTAG,
                            subtitle = "${hash.count} videos"
                        )
                    )
                }

            list.take(5)
        }
    }

    val matchingCreators = remember(q, allCreators) {
        if (q.isBlank()) emptyList() else {
            allCreators.filter { creator ->
                creator.channelName.lowercase().contains(q) ||
                creator.topCategory.lowercase().contains(q) ||
                creator.channelName.lowercase().replace(" ", "").contains(q.replace("@", ""))
            }
        }
    }

    val matchingHashtags = remember(q, allHashtags) {
        if (q.isBlank()) allHashtags.take(8) else {
            allHashtags.filter { it.hashtag.lowercase().contains(qNoHash) }
        }
    }

    val searchResults = remember(q, searchFilter, allPosts) {
        val base = if (q.isBlank()) emptyList() else {
            allPosts.filter { post ->
                post.title.lowercase().contains(q) ||
                post.description.lowercase().contains(q) ||
                post.channelName.lowercase().contains(q) ||
                post.tags.lowercase().contains(q) ||
                post.tags.lowercase().contains(qNoHash) ||
                post.category.lowercase().contains(q)
            }
        }

        when (searchFilter) {
            "Videos" -> base.filter { it.type == PostType.VIDEO }
            "Shorts ⚡" -> base.filter { it.type == PostType.SHORT }
            "Photos 📸" -> base.filter { it.type == PostType.PHOTO }
            "4K Ultra" -> base.filter { it.title.contains("4K", ignoreCase = true) || it.tags.contains("4K", ignoreCase = true) }
            else -> base
        }
    }

    // Confirmation dialog for clearing recent search history
    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear search history?", fontWeight = FontWeight.Bold) },
            text = { Text("This will remove all recent searches from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearRecentSearches()
                        showClearHistoryDialog = false
                    }
                ) {
                    Text("Clear All", color = SatisfyRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Modern Search Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Polished Search Input Pill
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(25.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                        border = BorderStroke(
                            1.dp,
                            if (query.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = if (query.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            TextField(
                                value = query,
                                onValueChange = onQueryChange,
                                placeholder = {
                                    Text(
                                        "Search 4K ASMR, #tags, @creators...",
                                        fontSize = 13.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("search_text_input"),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    disabledIndicatorColor = Color.Transparent
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(
                                    onSearch = {
                                        if (query.isNotBlank()) {
                                            onRecordSearch(query)
                                        }
                                        focusManager.clearFocus()
                                    }
                                )
                            )

                            if (query.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        onQueryChange("")
                                        focusManager.clearFocus()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Voice Search Mic Button
                            IconButton(
                                onClick = {
                                    isVoiceListening = !isVoiceListening
                                    if (isVoiceListening) {
                                        coroutineScope.launch {
                                            delay(1400)
                                            val voicePicks = listOf("Kinetic Sand ASMR", "Hydraulic Press 4K", "Soap Carving 4K", "4K Rain Relaxation", "Deep Ocean 4K")
                                            val picked = voicePicks.random()
                                            onQueryChange(picked)
                                            onRecordSearch(picked)
                                            isVoiceListening = false
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isVoiceListening) SatisfyRed.copy(alpha = 0.2f)
                                        else Color.Transparent
                                    )
                            ) {
                                Icon(
                                    imageVector = if (isVoiceListening) Icons.Filled.Mic else Icons.Outlined.Mic,
                                    contentDescription = "Voice Search",
                                    tint = if (isVoiceListening) SatisfyRed else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Voice listening animated banner
                AnimatedVisibility(
                    visible = isVoiceListening,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = SatisfyRed.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, SatisfyRed.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = SatisfyRed
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Listening for satisfying queries... (Speak now)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SatisfyRed
                            )
                        }
                    }
                }
            }
        }

        // Filter Chips Row
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf("All", "Videos", "Shorts ⚡", "4K Ultra", "Creators 👑", "#Hashtags", "Photos 📸")
            items(filters) { filter ->
                val isSelected = searchFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { searchFilter = filter },
                    label = {
                        Text(
                            filter,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Live Auto-Complete Suggestions Dropdown (while typing)
        if (query.isNotBlank() && liveSuggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Suggestions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    liveSuggestions.forEach { suggestion ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onQueryChange(suggestion.text)
                                    onRecordSearch(suggestion.text)
                                    focusManager.clearFocus()
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (suggestion.type) {
                                    SuggestionType.CREATOR -> Icons.Filled.Person
                                    SuggestionType.HASHTAG -> Icons.Filled.Tag
                                    SuggestionType.VIDEO -> Icons.Filled.PlayCircle
                                    SuggestionType.QUERY -> Icons.Filled.Search
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = suggestion.text,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (suggestion.subtitle.isNotBlank()) {
                                    Text(
                                        text = suggestion.subtitle,
                                        fontSize = 10.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onQueryChange(suggestion.text) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.NorthWest,
                                    contentDescription = "Insert query",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Main Content Area: Default State (Recent + Trending + Categories) vs Results State
        if (query.isBlank()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 90.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Recent Searches (from Room DB)
                if (recentSearches.isNotEmpty()) {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.History,
                                        contentDescription = "Recent Searches",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Recent Searches",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                TextButton(
                                    onClick = { showClearHistoryDialog = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "Clear All",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SatisfyRed
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Recent search list items
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                recentSearches.take(6).forEach { recent ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                onQueryChange(recent.query)
                                                onRecordSearch(recent.query)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.History,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = recent.query,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            IconButton(
                                                onClick = { onRemoveRecentSearch(recent.query) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Close,
                                                    contentDescription = "Remove search",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 2: Trending Searches with Category Tabs & Ranking
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalFireDepartment,
                                contentDescription = "Trending",
                                tint = SatisfyRed,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trending on Satisfy 🔥",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = SatisfyRed.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "LIVE RANKING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = SatisfyRed,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Category Tabs for Trending Searches
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val tabs = listOf("🔥 Hot Now", "✨ 4K ASMR", "🌿 Nature", "🎨 Art & Craft", "⚡ Viral")
                            items(tabs) { tab ->
                                val isSelected = trendingTab == tab
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { trendingTab = tab },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    border = BorderStroke(
                                        0.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.secondary else Color.Transparent
                                    )
                                ) {
                                    Text(
                                        text = tab,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Ranked Trending Items List
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                trendingSearches.forEachIndexed { index, item ->
                                    TrendingSearchRow(
                                        item = item,
                                        onClick = {
                                            onQueryChange(item.query)
                                            onRecordSearch(item.query)
                                        }
                                    )
                                    if (index < trendingSearches.size - 1) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 14.dp),
                                            thickness = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 3: Popular Hashtag Cloud
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = "Hashtags",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Popular Tags",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(allHashtags.take(10)) { hash ->
                                Surface(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onQueryChange(hash.hashtag)
                                            onRecordSearch(hash.hashtag)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = hash.hashtag,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${hash.count} videos",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Section 4: Explore Popular Categories Grid
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Explore Satisfying Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val categories = listOf(
                            Pair("Kinetic Sand", "🏖️"),
                            Pair("Hydraulic Press", "⚙️"),
                            Pair("Soap Carving", "🧼"),
                            Pair("Epoxy Resin", "🌊"),
                            Pair("4K Drone FPV", "🛸"),
                            Pair("Pottery ASMR", "🏺")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.take(3).forEach { cat ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onQueryChange(cat.first)
                                            onRecordSearch(cat.first)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = cat.second, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = cat.first,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.drop(3).take(3).forEach { cat ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onQueryChange(cat.first)
                                            onRecordSearch(cat.first)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(text = cat.second, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = cat.first,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Search Results Mode
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // Header with results count & Layout switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${searchResults.size} results for \"$query\"",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (searchFilter != "Creators 👑" && searchFilter != "#Hashtags") {
                        IconButton(
                            onClick = { isGridView = !isGridView },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                                contentDescription = "Toggle View",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                if (isGridView && searchFilter != "Creators 👑" && searchFilter != "#Hashtags") {
                    // Grid Results Layout
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(searchResults, key = { it.id }) { post ->
                            SearchVideoGridCard(
                                post = post,
                                onClick = {
                                    onRecordSearch(query)
                                    onSelectPost(post)
                                }
                            )
                        }
                    }
                } else {
                    // Standard List Results Layout
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Creators Section
                        if ((searchFilter == "All" || searchFilter == "Creators 👑") && matchingCreators.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Creators & Channels",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            items(matchingCreators, key = { it.channelName }) { creator ->
                                CreatorSearchResultCard(
                                    creator = creator,
                                    onVisitProfile = {
                                        onRecordSearch(creator.channelName)
                                        onCreatorClick(creator.channelName, creator.creatorUid, creator.pageId)
                                    }
                                )
                            }
                        }

                        // Hashtags Section
                        if (searchFilter == "#Hashtags") {
                            items(matchingHashtags, key = { it.hashtag }) { hash ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            onQueryChange(hash.hashtag)
                                            onRecordSearch(hash.hashtag)
                                            searchFilter = "All"
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                shape = CircleShape,
                                                color = SatisfyRed.copy(alpha = 0.15f),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(Icons.Filled.Tag, contentDescription = null, tint = SatisfyRed, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(hash.hashtag, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                Text("Trending in Satisfying Video", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        Text("${hash.count} videos", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Main Video / Post Items
                        if (searchFilter != "Creators 👑" && searchFilter != "#Hashtags") {
                            if (searchResults.isEmpty() && matchingCreators.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                modifier = Modifier.size(68.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.SearchOff,
                                                        contentDescription = "No Results",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(36.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "No results found for '$query'",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Try searching for Sand, #ASMR, Hydraulic Press, or @creator",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(searchResults, key = { it.id }) { post ->
                                    SearchVideoListCard(
                                        post = post,
                                        onClick = {
                                            onRecordSearch(query)
                                            onSelectPost(post)
                                        },
                                        onToggleLike = { onToggleLike(post) },
                                        onToggleSave = { onToggleSave(post) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendingSearchRow(
    item: TrendingSearchItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Rank Badge
        Surface(
            shape = CircleShape,
            color = when (item.rank) {
                1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                else -> MaterialTheme.colorScheme.surfaceVariant
            },
            border = BorderStroke(
                1.dp,
                when (item.rank) {
                    1 -> Color(0xFFFFD700)
                    2 -> Color(0xFFC0C0C0)
                    3 -> Color(0xFFCD7F32)
                    else -> Color.Transparent
                }
            ),
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "${item.rank}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = when (item.rank) {
                        1 -> Color(0xFFFFB300)
                        2 -> Color(0xFF90A4AE)
                        3 -> Color(0xFFB0703C)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.query,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${item.category} • ${item.subtitle}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (item.isHot) SatisfyRed.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        ) {
            Text(
                text = item.views,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (item.isHot) SatisfyRed else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        Icon(
            imageVector = Icons.Filled.NorthWest,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
fun SearchVideoListCard(
    post: PostEntity,
    onClick: () -> Unit,
    onToggleLike: () -> Unit = {},
    onToggleSave: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Video Thumbnail
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 72.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // 4K Badge (top left)
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = "4K UHD",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F),
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }

                // Duration / Short Badge (bottom right)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = Color.Black.copy(alpha = 0.85f)
                ) {
                    Text(
                        text = if (post.type == PostType.SHORT) "⚡ SHORT" else post.duration,
                        fontSize = 9.sp,
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
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = post.channelName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (post.isVerified) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${post.views} • ${post.category}",
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SearchVideoGridCard(
    post: PostEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = post.thumbnailUrl,
                    contentDescription = post.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                    shape = RoundedCornerShape(3.dp),
                    color = Color.Black.copy(alpha = 0.8f)
                ) {
                    Text(
                        text = if (post.type == PostType.SHORT) "⚡ SHORT" else post.duration,
                        fontSize = 8.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                    )
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = post.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${post.channelName} • ${post.views}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
