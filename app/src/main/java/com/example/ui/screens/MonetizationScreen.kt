package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonetizationScreen(
    eligibility: MonetizationEligibility,
    application: MonetizationApplicationEntity?,
    userProfile: UserProfile,
    onBack: () -> Unit,
    onNavigateToAnalytics: () -> Unit,
    onNavigateToRules: () -> Unit,
    onApplyForMonetization: (onResult: (Boolean, String) -> Unit) -> Unit
) {
    var showApplyDialog by remember { mutableStateOf(false) }
    var applySnackbarMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val statusText = when {
        application?.status == "APPROVED" -> "Partner Verified & Monetized"
        application?.status == "PENDING" -> "Application Under Review"
        application?.status == "REJECTED" -> "Application Rejected"
        eligibility.isEligible -> "Eligible to Apply"
        else -> "Not Yet Eligible"
    }

    val statusBadgeColor = when {
        application?.status == "APPROVED" -> Color(0xFF10B981)
        application?.status == "PENDING" -> Color(0xFFF59E0B)
        application?.status == "REJECTED" -> Color(0xFFEF4444)
        eligibility.isEligible -> Color(0xFF10B981)
        else -> Color(0xFF94A3B8)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Creator Monetization",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Partner Program & Revenue Sharing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("monetization_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToAnalytics,
                        modifier = Modifier.testTag("monetization_to_analytics_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = "Analytics",
                            tint = Color(0xFFEC4899)
                        )
                    }
                    IconButton(
                        onClick = onNavigateToRules,
                        modifier = Modifier.testTag("monetization_to_rules_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                            contentDescription = "Rules"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            if (applySnackbarMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { applySnackbarMessage = null }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(applySnackbarMessage!!)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 36.dp)
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
                                        Color(0xFFF59E0B).copy(alpha = 0.85f),
                                        Color(0xFFD97706).copy(alpha = 0.95f),
                                        Color(0xFF1E1B4B)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    com.example.ui.components.SatisfyAnimatedLogo(
                                        size = 38.dp,
                                        isAnimated = true,
                                        withBackgroundBadge = true,
                                        badgeColor = Color.White.copy(alpha = 0.9f)
                                    )
                                    Column {
                                        Text(
                                            text = "Satisfy Partner Program",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 17.sp
                                        )
                                        Text(
                                            text = userProfile.name,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                // Status pill
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(statusBadgeColor)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Earn revenue from views, ad placements, and premium subscriber interactions on your original Satisfy Shorts and Videos.",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            // Application Status Card if applied
            if (application != null) {
                item {
                    val isPending = application.status == "PENDING"
                    val isApproved = application.status == "APPROVED"
                    val isRejected = application.status == "REJECTED"

                    val bgTint = when {
                        isApproved -> Color(0xFF10B981).copy(alpha = 0.1f)
                        isPending -> Color(0xFFF59E0B).copy(alpha = 0.1f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.1f)
                    }
                    val borderTint = when {
                        isApproved -> Color(0xFF10B981).copy(alpha = 0.4f)
                        isPending -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                        else -> Color(0xFFEF4444).copy(alpha = 0.4f)
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgTint),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, borderTint, RoundedCornerShape(16.dp))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isApproved -> Icons.Default.CheckCircle
                                        isPending -> Icons.Default.HourglassEmpty
                                        else -> Icons.Default.Error
                                    },
                                    contentDescription = null,
                                    tint = statusBadgeColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Application Status: ${application.status}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()) }
                            val dateStr = dateFormat.format(Date(application.appliedAt))
                            Text(
                                text = "Submitted on $dateStr",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )

                            if (isApproved) {
                                Text(
                                    text = "Congratulations! Your channel has been verified and approved for monetization. Ads and revenue share are actively generating payouts to your creator wallet.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF10B981),
                                    lineHeight = 18.sp
                                )
                            } else if (isPending) {
                                Text(
                                    text = "Our review team is evaluating your channel content, originality, and organic traffic metrics. You will be notified once complete.",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                    lineHeight = 18.sp
                                )
                            } else if (isRejected) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = "Rejection Reason:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFFEF4444)
                                    )
                                    Text(
                                        text = application.rejectionReason ?: "Content originality or platform compliance issues were detected during review.",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        lineHeight = 18.sp
                                    )
                                    Text(
                                        text = "You can address the issues and re-apply once your content meets all platform policies.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Eligibility Pathways Overview
            item {
                Text(
                    text = "Eligibility Requirements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Pathway A & B Indicator Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PathwayStatusCard(
                        title = "Pathway 1: Long Video",
                        requirementText = "500 Subs + 4K Video Hrs",
                        isMet = eligibility.isPathwayAMet,
                        modifier = Modifier.weight(1f)
                    )
                    PathwayStatusCard(
                        title = "Pathway 2: Shorts",
                        requirementText = "500 Subs + 10K Shorts Hrs",
                        isMet = eligibility.isPathwayBMet,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Requirement 1: Subscribers Progress Bar (Requested)
            item {
                RequirementProgressCard(
                    title = "Subscribers Requirement",
                    currentValue = eligibility.currentSubscribers.toDouble(),
                    requiredValue = eligibility.requiredSubscribers.toDouble(),
                    currentDisplay = "${eligibility.currentSubscribers}",
                    requiredDisplay = "500",
                    unit = "subscribers",
                    isMet = eligibility.isSubscriberRequirementMet,
                    icon = Icons.Default.People,
                    accentColor = Color(0xFF8B5CF6),
                    remainingText = if (eligibility.isSubscriberRequirementMet) "Requirement Achieved!" else "${eligibility.remainingSubscribers} more subscribers needed"
                )
            }

            // Requirement 2: Normal Video Watch Hours Progress Bar (Requested)
            item {
                RequirementProgressCard(
                    title = "Normal Video Watch Time",
                    currentValue = eligibility.currentNormalWatchHours,
                    requiredValue = eligibility.requiredNormalWatchHours,
                    currentDisplay = String.format(Locale.US, "%.1f", eligibility.currentNormalWatchHours),
                    requiredDisplay = "4,000",
                    unit = "hours",
                    isMet = eligibility.isNormalWatchRequirementMet,
                    icon = Icons.Default.Videocam,
                    accentColor = Color(0xFF3B82F6),
                    remainingText = if (eligibility.isNormalWatchRequirementMet) "Requirement Achieved!" else "${String.format(Locale.US, "%.1f", eligibility.remainingNormalWatchHours)} normal video hours needed"
                )
            }

            // Requirement 3: Shorts Watch Hours Progress Bar (Requested)
            item {
                RequirementProgressCard(
                    title = "Shorts Watch Time",
                    currentValue = eligibility.currentShortsWatchHours,
                    requiredValue = eligibility.requiredShortsWatchHours,
                    currentDisplay = String.format(Locale.US, "%.1f", eligibility.currentShortsWatchHours),
                    requiredDisplay = "10,000",
                    unit = "hours",
                    isMet = eligibility.isShortsWatchRequirementMet,
                    icon = Icons.Default.PlayArrow,
                    accentColor = Color(0xFFEC4899),
                    remainingText = if (eligibility.isShortsWatchRequirementMet) "Requirement Achieved!" else "${String.format(Locale.US, "%.1f", eligibility.remainingShortsWatchHours)} Shorts watch hours needed"
                )
            }

            // Explanation of Watch Time (Requested)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "How Watch Time is Counted",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Text(
                            text = "• Normal Video Watch Time: Real cumulative seconds viewers spend watching your regular long-form videos, converted into hours (3,600s = 1 hour).\n" +
                                    "• Shorts Watch Time: Real cumulative seconds spent watching your vertical Shorts in the feed, converted into hours.\n" +
                                    "• Quality Verification: Only genuine organic watch time from approved videos is counted toward eligibility thresholds.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Apply Button Action Area (Requested: active ONLY when eligible)
            item {
                val canApply = (eligibility.isEligible && (application == null || application.status == "REJECTED"))
                val isAlreadyApproved = application?.status == "APPROVED"
                val isAlreadyPending = application?.status == "PENDING"

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (canApply) {
                                showApplyDialog = true
                            }
                        },
                        enabled = canApply,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF59E0B),
                            contentColor = Color.Black,
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("apply_monetization_button")
                    ) {
                        Icon(
                            imageVector = when {
                                isAlreadyApproved -> Icons.Default.CheckCircle
                                isAlreadyPending -> Icons.Default.HourglassEmpty
                                canApply -> Icons.Default.MonetizationOn
                                else -> Icons.Default.Lock
                            },
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isAlreadyApproved -> "Monetization Active"
                                isAlreadyPending -> "Application Under Review"
                                canApply -> "Apply for Creator Monetization"
                                else -> "Monetization Locked (Requirements Not Met)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    if (!eligibility.isEligible) {
                        Text(
                            text = "To unlock the apply button, complete either Pathway 1 (500 Subs + 4K Normal Video Hours) or Pathway 2 (500 Subs + 10K Shorts Hours).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    // Apply Confirmation Dialog
    if (showApplyDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showApplyDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Submit Monetization Application", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "You have met the required milestones for Satisfy Partner Program!",
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Channel: ${userProfile.name} (${userProfile.handle})",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "By submitting, you agree to Satisfy Creator Terms, copyright originality standards, and community guidelines.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSubmitting = true
                        onApplyForMonetization { success, message ->
                            isSubmitting = false
                            showApplyDialog = false
                            applySnackbarMessage = message
                        }
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B), contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_apply_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    } else {
                        Text("Submit Application", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showApplyDialog = false },
                    enabled = !isSubmitting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PathwayStatusCard(
    title: String,
    requirementText: String,
    isMet: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isMet) Color(0xFF10B981).copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.border(
            1.dp,
            if (isMet) Color(0xFF10B981).copy(alpha = 0.4f) else Color.Transparent,
            RoundedCornerShape(14.dp)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = requirementText,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Text(
                text = if (isMet) "✓ Pathway Qualified" else "In Progress",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun RequirementProgressCard(
    title: String,
    currentValue: Double,
    requiredValue: Double,
    currentDisplay: String,
    requiredDisplay: String,
    unit: String,
    isMet: Boolean,
    icon: ImageVector,
    accentColor: Color,
    remainingText: String
) {
    val progress = (currentValue / requiredValue).coerceIn(0.0, 1.0).toFloat()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "$currentDisplay / $requiredDisplay $unit",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                if (isMet) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Done",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = if (isMet) Color(0xFF10B981) else accentColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = remainingText,
                    fontSize = 11.sp,
                    color = if (isMet) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = if (isMet) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}
