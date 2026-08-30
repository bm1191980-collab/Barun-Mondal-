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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PostEntity
import com.example.data.model.PostType
import com.example.data.model.CreatorSearchResult
import com.example.data.model.HashtagSearchResult
import com.example.data.service.SatisfyVideoEngine
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    posts: List<PostEntity>,
    shortPosts: List<PostEntity>,
    selectedCategory: String,
    categories: List<String>,
    onSelectCategory: (String) -> Unit,
    onVideoClick: (PostEntity) -> Unit,
    onShortClick: (PostEntity) -> Unit,
    onToggleLike: (PostEntity) -> Unit,
    onToggleSave: (PostEntity) -> Unit,
    onDeletePost: (PostEntity) -> Unit,
    onCreatorClick: (String, String, Long?) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchFilterType by remember { mutableStateOf("All") } // All, Videos, Shorts, #Hashtags, Creators
    val focusManager = LocalFocusManager.current

    // Extract unique creators from posts
    val allCreators = remember(posts, shortPosts) {
        val combined = posts + shortPosts
        combined.groupBy { it.channelName.trim() }
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
                    topCategory = first.category.ifBlank { "Creator" }
                )
            }
            .sortedByDescending { it.videoCount }
    }

    // Extract all unique hashtags from posts
    val allHashtags = remember(posts, shortPosts) {
        val combined = posts + shortPosts
        val tagCounts = mutableMapOf<String, Int>()
        combined.forEach { post ->
            val rawTags = post.tags.split(" ", ",", "#").filter { it.isNotBlank() }
            rawTags.forEach { raw ->
                val clean = "#" + raw.trim().trimStart('#')
                if (clean.length > 2) {
                    tagCounts[clean] = (tagCounts[clean] ?: 0) + 1
                }
            }
        }
        // Add popular defaults if sparse
        val defaultPopular = listOf("#Satisfying", "#ASMR", "#4KVisuals", "#Nature", "#Relaxing", "#Cinematics", "#Bangladesh4K", "#Coding", "#Tech")
        defaultPopular.forEach { tag ->
            if (!tagCounts.containsKey(tag)) {
                tagCounts[tag] = 1
            }
        }
        tagCounts.map { HashtagSearchResult(it.key, it.value) }.sortedByDescending { it.count }
    }

    val isSearchActive = searchQuery.trim().isNotBlank()
    val cleanQuery = searchQuery.trim().lowercase()

    // Matching Creators
    val matchingCreators = remember(cleanQuery, allCreators) {
        if (cleanQuery.isBlank()) emptyList() else {
            allCreators.filter { creator ->
                creator.channelName.lowercase().contains(cleanQuery) ||
                creator.topCategory.lowercase().contains(cleanQuery) ||
                creator.channelName.lowercase().replace(" ", "").contains(cleanQuery.replace("@", ""))
            }
        }
    }

    // Matching Hashtags
    val matchingHashtags = remember(cleanQuery, allHashtags) {
        if (cleanQuery.isBlank()) allHashtags.take(8) else {
            val qTag = cleanQuery.trimStart('#')
            allHashtags.filter { it.hashtag.lowercase().contains(qTag) }
        }
    }

    // Filtered Video & Short Posts for Search or Feed
    val matchingPosts = remember(cleanQuery, posts, shortPosts, searchFilterType) {
        if (cleanQuery.isBlank()) emptyList() else {
            val combined = posts + shortPosts
            val qNoHash = cleanQuery.trimStart('#')
            combined.filter { post ->
                post.title.lowercase().contains(cleanQuery) ||
                post.description.lowercase().contains(cleanQuery) ||
                post.channelName.lowercase().contains(cleanQuery) ||
                post.tags.lowercase().contains(cleanQuery) ||
                post.tags.lowercase().contains(qNoHash) ||
                post.category.lowercase().contains(cleanQuery)
            }
        }
    }

    val filteredFeedPosts = remember(posts, selectedCategory) {
        if (selectedCategory == "All") {
            posts.filter { it.type == PostType.VIDEO }
        } else {
            posts.filter {
                it.type == PostType.VIDEO &&
                (it.category.equals(selectedCategory, ignoreCase = true) || it.tags.contains(selectedCategory, ignoreCase = true))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // TOP FEED SEARCH BAR
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
            border = BorderStroke(
                1.dp,
                if (isSearchActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                TextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search content, #hashtags, creators...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("feed_top_search_input"),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                )

                if (isSearchActive) {
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            focusManager.clearFocus()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tag,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Explore",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // POPULAR HASHTAGS ROW
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val displayTags = if (isSearchActive && matchingHashtags.isNotEmpty()) matchingHashtags.map { it.hashtag }
                             else allHashtags.take(10).map { it.hashtag }

            items(displayTags) { tag ->
                val isTagSelected = searchQuery.equals(tag, ignoreCase = true) || searchQuery.equals(tag.trimStart('#'), ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (isTagSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable {
                            if (isTagSelected) {
                                searchQuery = ""
                            } else {
                                searchQuery = tag
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tag,
                            contentDescription = null,
                            tint = if (isTagSelected) Color.White else SatisfyRed,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tag.trimStart('#'),
                            fontSize = 12.sp,
                            fontWeight = if (isTagSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isTagSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // IF SEARCH IS ACTIVE: SHOW SEARCH RESULTS & FILTER TABS
        if (isSearchActive) {
            // Search Mode Filter Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf("All", "Videos", "Shorts", "Creators", "#Hashtags")
                items(filters) { filter ->
                    val isSelected = searchFilterType == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { searchFilterType = filter },
                        label = {
                            val countLabel = when (filter) {
                                "All" -> matchingPosts.size + matchingCreators.size
                                "Videos" -> matchingPosts.count { it.type == PostType.VIDEO }
                                "Shorts" -> matchingPosts.count { it.type == PostType.SHORT }
                                "Creators" -> matchingCreators.size
                                "#Hashtags" -> matchingHashtags.size
                                else -> 0
                            }
                            Text(
                                text = if (countLabel > 0) "$filter ($countLabel)" else filter,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Search Content List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section: Creator Profiles Matching Query (if All or Creators selected)
                if ((searchFilterType == "All" || searchFilterType == "Creators") && matchingCreators.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "CREATORS & CHANNELS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${matchingCreators.size}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    items(matchingCreators, key = { it.channelName + it.creatorUid }) { creator ->
                        CreatorSearchResultCard(
                            creator = creator,
                            onVisitProfile = {
                                onCreatorClick(creator.channelName, creator.creatorUid, creator.pageId)
                            }
                        )
                    }
                }

                // Section: Matching Shorts (if Shorts or All selected)
                val matchingShorts = matchingPosts.filter { it.type == PostType.SHORT }
                if ((searchFilterType == "All" || searchFilterType == "Shorts") && matchingShorts.isNotEmpty()) {
                    item {
                        ShortsShelf(
                            shorts = matchingShorts,
                            onShortClick = onShortClick
                        )
                    }
                }

                // Section: Matching Videos (if Videos or All selected)
                val matchingVideos = matchingPosts.filter { it.type == PostType.VIDEO }
                if ((searchFilterType == "All" || searchFilterType == "Videos") && matchingVideos.isNotEmpty()) {
                    if (searchFilterType == "All" && matchingCreators.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Videocam,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "VIDEOS & CONTENT",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }

                    items(matchingVideos, key = { it.id }) { post ->
                        VideoCard(
                            post = post,
                            onClick = { onVideoClick(post) },
                            onToggleLike = { onToggleLike(post) },
                            onToggleSave = { onToggleSave(post) },
                            onDeletePost = { onDeletePost(post) },
                            onCreatorClick = { onCreatorClick(post.channelName, post.creatorUid, post.pageId) }
                        )
                    }
                }

                // Section: Matching Hashtags detailed list (if #Hashtags selected)
                if (searchFilterType == "#Hashtags") {
                    items(matchingHashtags, key = { it.hashtag }) { hash ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    searchQuery = hash.hashtag
                                    searchFilterType = "All"
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Tag,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = hash.hashtag,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${hash.count} related post${if (hash.count != 1) "s" else ""}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        searchQuery = hash.hashtag
                                        searchFilterType = "All"
                                    },
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("View Feed", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Empty Search State
                val hasAnyResult = matchingPosts.isNotEmpty() || matchingCreators.isNotEmpty() || (searchFilterType == "#Hashtags" && matchingHashtags.isNotEmpty())
                if (!hasAnyResult) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.SearchOff,
                                    contentDescription = "No Results",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(60.dp)
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "No matches found for '$searchQuery'",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Try searching for creators like @satisfy_creator, #Satisfying, #Nature, or 4K",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedButton(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Clear Search")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // NORMAL FEED VIEW (Category Filter Chips + Smooth Infinite Scrolling Feed)
            val listState = rememberLazyListState()
            val coroutineScope = rememberCoroutineScope()
            val context = LocalContext.current

            var displayedLimit by remember(selectedCategory) { mutableIntStateOf(6) }
            var isLoadingMore by remember { mutableStateOf(false) }
            var isCategorySwitching by remember(selectedCategory) { mutableStateOf(false) }

            // Category switch smooth transition trigger
            LaunchedEffect(selectedCategory) {
                isCategorySwitching = true
                displayedLimit = 6
                listState.scrollToItem(0)
                delay(220)
                isCategorySwitching = false
            }

            // Infinite cyclic video stream generator from base database posts
            val visiblePagedVideos = remember(filteredFeedPosts, displayedLimit) {
                if (filteredFeedPosts.isEmpty()) emptyList()
                else {
                    val result = mutableListOf<PostEntity>()
                    var i = 0
                    while (result.size < displayedLimit) {
                        val basePost = filteredFeedPosts[i % filteredFeedPosts.size]
                        val cycle = i / filteredFeedPosts.size
                        if (cycle == 0) {
                            result.add(basePost)
                        } else {
                            result.add(basePost.copy(id = basePost.id + (cycle * 100_000L)))
                        }
                        i++
                    }
                    result
                }
            }

            // Continuous background preloading of video streams and thumbnail assets
            LaunchedEffect(visiblePagedVideos) {
                if (visiblePagedVideos.isNotEmpty()) {
                    SatisfyVideoEngine.preloadFeedItems(context, visiblePagedVideos.take(4), count = 4)
                }
            }

            val currentScrollIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
            LaunchedEffect(currentScrollIndex) {
                if (visiblePagedVideos.isNotEmpty()) {
                    val nextUpcoming = visiblePagedVideos.drop(currentScrollIndex).take(4)
                    if (nextUpcoming.isNotEmpty()) {
                        SatisfyVideoEngine.preloadFeedItems(context, nextUpcoming, count = 4)
                    }
                }
            }

            // Lazy loading detection when nearing bottom
            val shouldLoadMore by remember {
                derivedStateOf {
                    val layoutInfo = listState.layoutInfo
                    val total = layoutInfo.totalItemsCount
                    val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    total > 0 && lastVisible >= total - 2
                }
            }

            LaunchedEffect(shouldLoadMore) {
                if (shouldLoadMore && !isLoadingMore && filteredFeedPosts.isNotEmpty()) {
                    isLoadingMore = true
                    delay(350) // Smooth asynchronous batch load simulation
                    displayedLimit += 4
                    isLoadingMore = false
                }
            }

            val showScrollToTop by remember {
                derivedStateOf { listState.firstVisibleItemIndex > 3 }
            }

            // Modern Category Filter Chips Row with animated gradient pills
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    val isSelected = category == selectedCategory
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(30.dp))
                            .clickable { onSelectCategory(category) }
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    } else {
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.surfaceVariant,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (isCategorySwitching) {
                    // Shimmer Skeleton Feed during category transition
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        repeat(2) {
                            VideoCardSkeleton()
                        }
                    }
                } else {
                    // Main Infinite Scrolling Feed List
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 860.dp)
                            .align(Alignment.TopCenter),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // First 2 videos
                        val topVideos = visiblePagedVideos.take(2)
                        items(topVideos, key = { it.id }) { post ->
                            VideoCard(
                                post = post,
                                onClick = { onVideoClick(post) },
                                onToggleLike = { onToggleLike(post) },
                                onToggleSave = { onToggleSave(post) },
                                onDeletePost = { onDeletePost(post) },
                                onCreatorClick = { onCreatorClick(post.channelName, post.creatorUid, post.pageId) }
                            )
                        }

                        // Satisfy Shorts Horizontal Shelf with modern blue & purple accent styling
                        if (shortPosts.isNotEmpty()) {
                            item(key = "shorts_shelf_item") {
                                ShortsShelf(
                                    shorts = shortPosts,
                                    onShortClick = onShortClick
                                )
                            }
                        }

                        // Remaining lazy loaded paged videos
                        val remainingVideos = visiblePagedVideos.drop(2)
                        items(remainingVideos, key = { it.id }) { post ->
                            VideoCard(
                                post = post,
                                onClick = { onVideoClick(post) },
                                onToggleLike = { onToggleLike(post) },
                                onToggleSave = { onToggleSave(post) },
                                onDeletePost = { onDeletePost(post) },
                                onCreatorClick = { onCreatorClick(post.channelName, post.creatorUid, post.pageId) }
                            )
                        }

                        // Infinite Scrolling Lazy Loading Indicator / Preload Progress Footer
                        if (visiblePagedVideos.isNotEmpty() && isLoadingMore) {
                            item(key = "infinite_scroll_footer") {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.5.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = "Streaming more 4K satisfying videos (Preloaded)...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredFeedPosts.isEmpty()) {
                            item(key = "empty_feed_state") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                                            modifier = Modifier.size(72.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Outlined.VideoLibrary,
                                                    contentDescription = "Empty",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No videos in '$selectedCategory'",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Try selecting 'All' or upload your own creation!",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Smooth Animated Scroll To Top Button
                androidx.compose.animation.AnimatedVisibility(
                    visible = showScrollToTop,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 20.dp, end = 16.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(0)
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ArrowUpward,
                            contentDescription = "Scroll to top",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CreatorSearchResultCard(
    creator: CreatorSearchResult,
    onVisitProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onVisitProfile() }
            .testTag("creator_result_${creator.channelName}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Creator Avatar with Gradient Ring
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .border(
                        2.dp,
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                        ),
                        CircleShape
                    )
            ) {
                AsyncImage(
                    model = creator.channelAvatar.ifBlank { "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=200" },
                    contentDescription = creator.channelName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = creator.channelName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (creator.isVerified) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Verified Creator",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${creator.subscriberCount} • ${creator.videoCount} video${if (creator.videoCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "Category: ${creator.topCategory}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onVisitProfile,
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "View Profile",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun VideoCard(
    post: PostEntity,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onDeletePost: () -> Unit,
    onCreatorClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // High-res Thumbnail Container with Duration Badge & 16:9 Aspect Ratio
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Color(0xFF0F1523))
            ) {
                if (post.thumbnailUrl.isNotBlank()) {
                    AsyncImage(
                        model = post.thumbnailUrl,
                        contentDescription = post.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                // Smooth gradient shade at bottom of thumbnail
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                // Top Right Badges: Cached Indicator
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Bolt,
                            contentDescription = "Preloaded Fast Cache",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "PRELOADED",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Frosted Duration Badge (Bottom Right)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.75f)
                ) {
                    Text(
                        text = post.duration,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // User Created Badge with Blue-Purple Gradient (Top Left)
                if (post.isUserCreated) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                    )
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "YOUR UPLOAD",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }

            // Details Row: Avatar + Title + Channel Info + 3-dot Menu
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Channel Avatar with modern gradient border ring
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            CircleShape
                        )
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onCreatorClick() },
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

                Spacer(modifier = Modifier.width(12.dp))

                // Title & Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onCreatorClick() }
                    ) {
                        Text(
                            text = post.channelName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (post.isVerified) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "Verified Channel",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }

                        Text(
                            text = " • ${post.views} • ${post.timeAgo}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // More Options Dropdown Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = "More Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text(if (post.isSaved) "Remove from Saved" else "Save to Watch Later") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (post.isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    tint = if (post.isSaved) SatisfyGold else MaterialTheme.colorScheme.primary
                                )
                            },
                            onClick = {
                                onToggleSave()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (post.isLiked) "Liked" else "Like Video") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (post.isLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                    contentDescription = null,
                                    tint = if (post.isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            },
                            onClick = {
                                onToggleLike()
                                showMenu = false
                            }
                        )
                        if (post.isUserCreated) {
                            DropdownMenuItem(
                                text = { Text("Delete Upload", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    onDeletePost()
                                    showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortsShelf(
    shorts: List<PostEntity>,
    onShortClick: (PostEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            // Shelf Header with Brand Accent
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.example.ui.components.SatisfyAnimatedLogo(
                    size = 26.dp,
                    isAnimated = true,
                    withBackgroundBadge = true,
                    badgeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Satisfy Shorts",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "HOT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Horizontal Row of Shorts Cards
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shorts, key = { it.id }) { short ->
                    ShortCard(short = short, onClick = { onShortClick(short) })
                }
            }
        }
    }
}

@Composable
fun ShortCard(
    short: PostEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(150.dp)
            .height(245.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (short.thumbnailUrl.isNotBlank()) {
                AsyncImage(
                    model = short.thumbnailUrl,
                    contentDescription = short.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.25f),
                                Color.Black.copy(alpha = 0.88f)
                            )
                        )
                    )
            )

            // Top Shorts badge with electric blue & purple gradient
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            )
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "⚡ SHORT",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // Bottom Title & Views
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
            ) {
                Text(
                    text = short.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${short.views} views",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun VideoCardSkeleton(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer_skeleton")
    val translateAnim by transition.animateFloat(
        initialValue = -300f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Shimmer 16:9 Thumbnail Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(shimmerBrush)
            )

            // Shimmer Avatar + Title rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(shimmerBrush)
                    )
                }
            }
        }
    }
}
