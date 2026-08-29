package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class RuleCategory(val title: String, val icon: ImageVector, val tagColor: Color) {
    ALL("All Guidelines", Icons.Default.Rule, Color(0xFF6366F1)),
    GENERAL("General Rules", Icons.Default.Shield, Color(0xFF3B82F6)),
    VIDEO("Video Upload", Icons.Default.Videocam, Color(0xFF8B5CF6)),
    SHORTS("Shorts (Max 90s)", Icons.Default.PlayArrow, Color(0xFFEC4899)),
    CREATOR("Creator Conduct", Icons.Default.Person, Color(0xFF10B981)),
    MONETIZATION("Monetization", Icons.Default.MonetizationOn, Color(0xFFF59E0B)),
    USER("Viewer & Users", Icons.Default.People, Color(0xFF06B6D4))
}

data class RuleItem(
    val id: String,
    val category: RuleCategory,
    val title: String,
    val shortSummary: String,
    val detailedDescription: String,
    val allowedPoints: List<String>,
    val prohibitedPoints: List<String>,
    val penaltyNotice: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatisfyRulesScreen(
    onBack: () -> Unit,
    onNavigateToMonetization: (() -> Unit)? = null,
    onNavigateToUpload: (() -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(RuleCategory.ALL) }
    var expandedRuleId by remember { mutableStateOf<String?>("shorts_duration_limit") }

    val allRules = remember {
        listOf(
            RuleItem(
                id = "shorts_duration_limit",
                category = RuleCategory.SHORTS,
                title = "Shorts Maximum Duration: 90 Seconds",
                shortSummary = "Shorts videos must strictly have a maximum duration of 90 seconds (1 minute 30 seconds).",
                detailedDescription = "Satisfy Shorts is built for concise, vertical, high-engagement content. Any video exceeding 90 seconds cannot be published as a Short and must be submitted through standard long-form Video upload.",
                allowedPoints = listOf(
                    "Vertical 9:16 aspect ratio videos up to 90 seconds (01:30)",
                    "Captivating loops, micro-tutorials, cinematics, and satisfying highlights",
                    "Original sound clips, licensed backing tracks, and voiceovers"
                ),
                prohibitedPoints = listOf(
                    "Uploading videos longer than 90 seconds to the Shorts feed",
                    "Distorted aspect ratios with massive blank filler bars",
                    "Repetitive static image slideshows disguised as video Shorts"
                ),
                penaltyNotice = "Videos exceeding 90 seconds are automatically routed to standard Video upload."
            ),
            RuleItem(
                id = "general_respect_safety",
                category = RuleCategory.GENERAL,
                title = "Community Safety & Respect",
                shortSummary = "Zero tolerance for harassment, hate speech, bullying, scams, or malicious behavior.",
                detailedDescription = "Satisfy is a welcoming creative community. We strictly enforce policies against harassment, targeted hostility, hate speech based on protected characteristics, and deceptive scams.",
                allowedPoints = listOf(
                    "Respectful constructive critique and community discussions",
                    "Creative expression and positive entertainment",
                    "Reporting violating content via the in-app reporting tool"
                ),
                prohibitedPoints = listOf(
                    "Hate speech, discrimination, racial slurs, or harassment",
                    "Doxxing, sharing private personal information without consent",
                    "Scams, pyramid schemes, or deceptive phishing links"
                ),
                penaltyNotice = "Violation results in immediate content removal and permanent account ban."
            ),
            RuleItem(
                id = "video_originality_copyright",
                category = RuleCategory.VIDEO,
                title = "Originality & Copyright Compliance",
                shortSummary = "Creators must own all rights or hold appropriate commercial licenses for uploaded media.",
                detailedDescription = "You must be the original creator or have explicit authorized permission to publish any audio, video, or imagery in your content. Unauthorized re-uploads of other creators' work will be taken down.",
                allowedPoints = listOf(
                    "Original recordings, licensed royalty-free soundtracks, and credited collaborations",
                    "Transformative commentary, educational reviews, and parodies within fair use",
                    "High-resolution 1080p and 4K media renders"
                ),
                prohibitedPoints = listOf(
                    "Ripping and re-uploading content from other creators without authorization",
                    "Using copyrighted music without valid licenses",
                    "Uploading NSFW, sexually explicit, graphic violence, or dangerous content"
                ),
                penaltyNotice = "3 copyright strikes will lead to channel termination and monetization revocation."
            ),
            RuleItem(
                id = "creator_traffic_authenticity",
                category = RuleCategory.CREATOR,
                title = "Traffic Integrity & Anti-Farming",
                shortSummary = "Strict prohibition against bot traffic, view bots, click farms, and artificial watch time.",
                detailedDescription = "Satisfy evaluates creator metrics strictly on authentic human engagement. Using automated bots, scripts, view-exchange groups, or click farms to inflate views or watch time is strictly prohibited.",
                allowedPoints = listOf(
                    "Organic discovery through tags, search, and quality recommendations",
                    "Sharing your content across genuine social media channels and communities",
                    "Building genuine subscriber connections through regular uploads"
                ),
                prohibitedPoints = listOf(
                    "Purchasing bot views, fake subscribers, or simulated watch hours",
                    "Running looping scripts or background tabs to inflate watch time",
                    "Sub4Sub schemes or coordinated engagement manipulation"
                ),
                penaltyNotice = "Artificial metrics will be wiped, monetization denied, and accounts blacklisted."
            ),
            RuleItem(
                id = "monetization_eligibility_payouts",
                category = RuleCategory.MONETIZATION,
                title = "Monetization Partner Eligibility",
                shortSummary = "Creators must achieve 500 Subscribers AND (4,000 Normal Video Hours OR 10,000 Shorts Hours).",
                detailedDescription = "To qualify for the Satisfy Creator Monetization Partner Program, your channel must meet verified organic milestones and pass our manual content compliance review.",
                allowedPoints = listOf(
                    "Meeting Requirement Pathway 1: 500 Subscribers + 4,000 Normal Video Watch Hours",
                    "Meeting Requirement Pathway 2: 500 Subscribers + 10,000 Shorts Watch Hours",
                    "Maintaining a channel in good standing with zero active copyright strikes"
                ),
                prohibitedPoints = listOf(
                    "Submitting monetization applications with recycled or low-effort automated content",
                    "Manipulating watch hours or using bots to artificially reach eligibility thresholds",
                    "Attempting multi-account fraud or tax identity falsification"
                ),
                penaltyNotice = "Non-compliant applications are rejected with detailed reasons; fraudulent channels are banned."
            ),
            RuleItem(
                id = "viewer_conduct_comments",
                category = RuleCategory.USER,
                title = "Viewer Conduct & Comment Etiquette",
                shortSummary = "Guidelines for comments, reviews, reporting, and constructive interaction.",
                detailedDescription = "Viewers are expected to keep discussions constructive and respectful. Spamming links, mass-disliking campaigns, and frivolous report abuses are monitored by our moderation engine.",
                allowedPoints = listOf(
                    "Engaging with creators through comments, likes, and shares",
                    "Submitting genuine, factual reports when content violates community rules",
                    "Supporting creators with tips and Pro referrals"
                ),
                prohibitedPoints = listOf(
                    "Spamming repetitive promotional messages or external links in comments",
                    "Coordinated brigading or targeted mass-dislike harassment",
                    "Filing false or fraudulent reports against legitimate creators"
                ),
                penaltyNotice = "Spam accounts and report abusers will have commenting privileges revoked."
            )
        )
    }

    val filteredRules = remember(searchQuery, selectedCategory) {
        allRules.filter { rule ->
            val matchesCategory = (selectedCategory == RuleCategory.ALL || rule.category == selectedCategory)
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                rule.title.contains(searchQuery, ignoreCase = true) ||
                        rule.shortSummary.contains(searchQuery, ignoreCase = true) ||
                        rule.detailedDescription.contains(searchQuery, ignoreCase = true)
            }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        com.example.ui.components.SatisfyAnimatedLogo(
                            size = 28.dp,
                            isAnimated = true,
                            withBackgroundBadge = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Satisfy Platform Rules",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "Community Standards & Creator Policies",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("rules_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            // Header Hero Banner
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF4F46E5).copy(alpha = 0.85f),
                                        Color(0xFF7C3AED).copy(alpha = 0.95f),
                                        Color(0xFF0F172A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Gavel,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Satisfy Guidelines & Integrity",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 17.sp
                                    )
                                    Text(
                                        text = "Official standards for creators, videos, and viewers",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Every creator and viewer on Satisfy is expected to uphold content quality, respect intellectual property, and maintain community safety. Review the strict guidelines below.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (onNavigateToMonetization != null) {
                                    OutlinedButton(
                                        onClick = onNavigateToMonetization,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = Color.White
                                        ),
                                        border = ButtonDefaults.outlinedButtonBorder.copy(
                                            brush = Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFFBBF24)))
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MonetizationOn,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Monetization", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }

                                if (onNavigateToUpload != null) {
                                    Button(
                                        onClick = onNavigateToUpload,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color.White,
                                            contentColor = Color(0xFF4F46E5)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Upload,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Upload", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search rules, policies, or restrictions...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rules_search_field")
                )
            }

            // Category Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(RuleCategory.entries) { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.title, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = cat.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Results count
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${filteredRules.size} ${if (filteredRules.size == 1) "Policy Guideline" else "Policy Guidelines"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tap any card to view full policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            // Rule Cards
            items(filteredRules, key = { it.id }) { rule ->
                val isExpanded = expandedRuleId == rule.id
                RuleCard(
                    rule = rule,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedRuleId = if (isExpanded) null else rule.id
                    }
                )
            }
        }
    }
}

@Composable
fun RuleCard(
    rule: RuleItem,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onToggleExpand() }
            .testTag("rule_card_${rule.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(rule.category.tagColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = rule.category.icon,
                        contentDescription = null,
                        tint = rule.category.tagColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = rule.category.title,
                        fontSize = 11.sp,
                        color = rule.category.tagColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = rule.shortSummary,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                lineHeight = 18.sp
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    Text(
                        text = rule.detailedDescription,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        lineHeight = 19.sp
                    )

                    // Allowed Points
                    if (rule.allowedPoints.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Permitted & Encouraged",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF10B981)
                                )
                            }
                            rule.allowedPoints.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("•", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = point,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Prohibited Points
                    if (rule.prohibitedPoints.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444).copy(alpha = 0.1f))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Strictly Prohibited",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }
                            rule.prohibitedPoints.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("•", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                    Text(
                                        text = point,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }

                    // Penalty Notice
                    if (rule.penaltyNotice != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF59E0B).copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Enforcement: ${rule.penaltyNotice}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }
    }
}
