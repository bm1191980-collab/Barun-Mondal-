package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProSubscriptionEntity
import com.example.data.model.SatisfyProPlan
import com.example.data.model.UserProfile
import com.example.data.service.PaymentGatewayService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProMembershipScreen(
    userProfile: UserProfile = UserProfile(),
    isPro: Boolean = false,
    subscription: ProSubscriptionEntity? = null,
    activeSubscription: ProSubscriptionEntity? = subscription,
    wallet: com.example.data.model.WalletEntity? = null,
    userReferralCode: String = userProfile.referralCode,
    allSubscriptions: List<ProSubscriptionEntity> = emptyList(),
    onBack: () -> Unit = {},
    onPurchasePro: (plan: SatisfyProPlan, referralCode: String?, paymentMethod: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onOpenWallet: () -> Unit = {},
    onNavigateToWallet: () -> Unit = onOpenWallet,
    onOpenOwnerChat: () -> Unit = {},
    onNavigateToChat: () -> Unit = onOpenOwnerChat,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val effectiveSub = activeSubscription ?: subscription
    val effectiveIsPro = isPro || (effectiveSub != null && effectiveSub.expiresAt > System.currentTimeMillis())
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    // Active plan detection
    val currentActivePlan = remember(effectiveSub, effectiveIsPro) {
        if (effectiveIsPro && effectiveSub != null) {
            SatisfyProPlan.fromPlanId(effectiveSub.planId)
        } else {
            null
        }
    }

    // Currently selected plan in UI for viewing/purchasing
    var selectedPlan by remember(currentActivePlan) {
        mutableStateOf(currentActivePlan ?: SatisfyProPlan.PRO)
    }

    var showCheckoutDialog by remember { mutableStateOf(false) }
    var referralCodeInput by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("UPI (Google Pay / PhonePe / Paytm)") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("PRO Subscriptions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4C1D95),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = SatisfyGold)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Hero Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF4C1D95),
                                    Color(0xFF2E1065),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Crown Badge
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(SatisfyGold, Color(0xFFD97706))
                                    )
                                )
                                .shadow(8.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = "Pro Crown",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Satisfy PRO Plans",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Choose from 3 monthly plans: Pro, Premium Pro, or Super Premium Pro",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SatisfyGoldLight,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Active Plan Status Banner
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (effectiveIsPro) Color(0xFF064E3B).copy(alpha = 0.85f) else Color(0xFF1E1B4B).copy(alpha = 0.85f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (effectiveIsPro) Color(0xFF10B981) else SatisfyGold.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (effectiveIsPro) Icons.Filled.CheckCircle else Icons.Filled.Stars,
                                        contentDescription = null,
                                        tint = if (effectiveIsPro) Color(0xFF34D399) else SatisfyGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (effectiveIsPro && currentActivePlan != null) "${currentActivePlan.planName} Active" else "Subscription: Free Account",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        if (effectiveIsPro && effectiveSub != null) {
                                            Text(
                                                text = "ID: ${effectiveSub.planId} • Expires: ${dateFormat.format(Date(effectiveSub.expiresAt))}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        } else {
                                            Text(
                                                text = "Select a plan below to upgrade your experience",
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (effectiveIsPro && currentActivePlan != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SatisfyGold,
                                        contentColor = Color.Black
                                    ) {
                                        Text(
                                            text = "₹${currentActivePlan.priceInr.toInt()}/mo",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3 Subscription Plans Tier Selection
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "AVAILABLE MONTHLY PLANS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Plan Cards
                    SatisfyProPlan.entries.forEach { plan ->
                        val isCurrentActive = effectiveIsPro && currentActivePlan == plan
                        val isSelected = selectedPlan == plan
                        val isUpgrade = currentActivePlan != null && plan.tierLevel > currentActivePlan.tierLevel
                        val isDowngrade = currentActivePlan != null && plan.tierLevel < currentActivePlan.tierLevel

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlan = plan },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    when (plan) {
                                        SatisfyProPlan.SUPER_PREMIUM_PRO -> Color(0xFF3B1564)
                                        SatisfyProPlan.PREMIUM_PRO -> Color(0xFF1E293B)
                                        SatisfyProPlan.PRO -> Color(0xFF172554)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                }
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) {
                                    when (plan) {
                                        SatisfyProPlan.SUPER_PREMIUM_PRO -> Color(0xFFEC4899)
                                        SatisfyProPlan.PREMIUM_PRO -> SatisfyGold
                                        SatisfyProPlan.PRO -> Color(0xFF3B82F6)
                                    }
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top Row: Plan Name, ID & Price
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedPlan = plan }
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = plan.planName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 16.sp,
                                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                                )
                                                if (isCurrentActive) {
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = Color(0xFF10B981)
                                                    ) {
                                                        Text(
                                                            text = "ACTIVE",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = "ID: ${plan.planId}",
                                                fontSize = 10.sp,
                                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${plan.priceInr.toInt()}",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = when (plan) {
                                                SatisfyProPlan.SUPER_PREMIUM_PRO -> Color(0xFFF472B6)
                                                SatisfyProPlan.PREMIUM_PRO -> SatisfyGold
                                                SatisfyProPlan.PRO -> Color(0xFF60A5FA)
                                            }
                                        )
                                        Text(
                                            text = plan.billingPeriod,
                                            fontSize = 11.sp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Text(
                                    text = plan.tagLine,
                                    fontSize = 12.sp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                HorizontalDivider(
                                    color = if (isSelected) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 0.5.dp
                                )

                                // Features List
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    plan.features.forEach { feat ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = when (plan) {
                                                    SatisfyProPlan.SUPER_PREMIUM_PRO -> Color(0xFFF472B6)
                                                    SatisfyProPlan.PREMIUM_PRO -> SatisfyGold
                                                    SatisfyProPlan.PRO -> Color(0xFF34D399)
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = feat,
                                                fontSize = 12.sp,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                // Action Button
                                Spacer(modifier = Modifier.height(4.dp))
                                Button(
                                    onClick = {
                                        selectedPlan = plan
                                        showCheckoutDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when {
                                            isCurrentActive -> Color(0xFF059669)
                                            plan == SatisfyProPlan.SUPER_PREMIUM_PRO -> Color(0xFFDB2777)
                                            plan == SatisfyProPlan.PREMIUM_PRO -> SatisfyGold
                                            else -> Color(0xFF2563EB)
                                        },
                                        contentColor = if (plan == SatisfyProPlan.PREMIUM_PRO && !isCurrentActive) Color.Black else Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isCurrentActive -> Icons.Filled.Autorenew
                                            isUpgrade -> Icons.Filled.TrendingUp
                                            isDowngrade -> Icons.Filled.TrendingDown
                                            else -> Icons.Filled.Bolt
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when {
                                            isCurrentActive -> "Renew ${plan.planName} (₹${plan.priceInr.toInt()}/mo)"
                                            isUpgrade -> "Upgrade to ${plan.planName} (₹${plan.priceInr.toInt()}/mo)"
                                            isDowngrade -> "Switch to ${plan.planName} (₹${plan.priceInr.toInt()}/mo)"
                                            else -> "Activate ${plan.planName} (₹${plan.priceInr.toInt()}/mo)"
                                        },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dynamic 50/50 Revenue Split Workflow Card for selected plan
            item {
                val settlement = remember(selectedPlan) {
                    PaymentGatewayService.calculateCommissionSplit(
                        plan = selectedPlan,
                        customGatewayFee = selectedPlan.gatewayFeeInr,
                        hasReferral = true
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = SatisfyGold,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "${selectedPlan.planName} 50/50 Split Workflow",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // User pays
                            SplitNode(
                                title = "User Pays",
                                amount = "₹${selectedPlan.priceInr.toInt()}",
                                subtitle = "Monthly Gross",
                                icon = Icons.Filled.CreditCard,
                                color = Color(0xFF6366F1)
                            )

                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )

                            // Referrer gets 50% net
                            SplitNode(
                                title = "Referrer (50%)",
                                amount = "₹${String.format(Locale.US, "%.2f", settlement.finalReferralPayout)}",
                                subtitle = "Net Wallet",
                                icon = Icons.Filled.CardGiftcard,
                                color = Color(0xFF10B981)
                            )

                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )

                            // Owner gets 50% net
                            SplitNode(
                                title = "Owner (50%)",
                                amount = "₹${String.format(Locale.US, "%.2f", settlement.ownerCommission)}",
                                subtitle = "Net Revenue",
                                icon = Icons.Filled.AccountBalance,
                                color = Color(0xFFF59E0B)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "ℹ️ ₹${selectedPlan.priceInr.toInt()} Gross - ₹${String.format(Locale.US, "%.2f", settlement.gatewayFee)} Gateway Fee = ₹${String.format(Locale.US, "%.2f", settlement.netSettledFromGateway)} Net Split equally (50% Referrer | 50% Owner).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Quick Navigation Shortcuts (Owner Chat & Referral Wallet)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToChat() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF312E81).copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Filled.Forum,
                                contentDescription = null,
                                tint = Color(0xFFA5B4FC),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Owner Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (effectiveIsPro) "Chat 1-on-1 VIP" else "PRO Feature",
                                fontSize = 11.sp,
                                color = Color(0xFFC7D2FE)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onNavigateToWallet() },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B).copy(alpha = 0.8f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Icon(
                                imageVector = Icons.Filled.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color(0xFF6EE7B7),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "My Wallet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color.White
                            )
                            Text(
                                text = "50% Referral Share",
                                fontSize = 11.sp,
                                color = Color(0xFFA7F3D0)
                            )
                        }
                    }
                }
            }

            // Subscription History Section
            if (allSubscriptions.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            text = "SUBSCRIPTION HISTORY",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        allSubscriptions.take(5).forEach { sub ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = sub.planName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Plan: ${sub.planId} • ${dateFormat.format(Date(sub.startedAt))}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "₹${sub.amount.toInt()}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = SatisfyGold
                                        )
                                        Text(
                                            text = sub.status,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (sub.status == "ACTIVE") Color(0xFF10B981) else Color(0xFFEF4444)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkout & Payment Bottom Sheet Dialog
        if (showCheckoutDialog) {
            ModalBottomSheet(
                onDismissRequest = { if (!isProcessingPayment) showCheckoutDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Activate ${selectedPlan.planName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "₹${selectedPlan.priceInr.toInt()}.00 / month • 30 Days Access",
                                fontSize = 13.sp,
                                color = SatisfyGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = SatisfyGold.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = selectedPlan.planId,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatisfyGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Referral Code Input
                    OutlinedTextField(
                        value = referralCodeInput,
                        onValueChange = { referralCodeInput = it.uppercase() },
                        label = { Text("Referral Code (Optional)") },
                        placeholder = { Text("e.g. SATISFY100") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Filled.CardGiftcard, contentDescription = null, tint = SatisfyGold)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Payment Method Selector
                    Text("Select Payment Gateway", fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    val paymentMethods = listOf(
                        "UPI (Google Pay / PhonePe / Paytm)",
                        "Razorpay (Debit / Credit Cards)",
                        "Net Banking / Wallets"
                    )

                    paymentMethods.forEach { method ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedPaymentMethod == method) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (selectedPaymentMethod == method) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPaymentMethod == method,
                                    onClick = { selectedPaymentMethod = method }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = method, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    // Dynamic Split transparency note
                    val modalSettlement = PaymentGatewayService.calculateCommissionSplit(
                        plan = selectedPlan,
                        customGatewayFee = selectedPlan.gatewayFeeInr,
                        hasReferral = referralCodeInput.isNotBlank()
                    )

                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "₹${selectedPlan.priceInr.toInt()} Payment: ₹${String.format(Locale.US, "%.2f", modalSettlement.finalReferralPayout)} Referrer Payout (50% net) + ₹${String.format(Locale.US, "%.2f", modalSettlement.ownerCommission)} Owner (50% net).",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Pay Button
                    Button(
                        onClick = {
                            isProcessingPayment = true
                            onPurchasePro(
                                selectedPlan,
                                referralCodeInput.ifBlank { null },
                                selectedPaymentMethod
                            ) { success, msg ->
                                isProcessingPayment = false
                                showCheckoutDialog = false
                                snackbarMessage = msg
                            }
                        },
                        enabled = !isProcessingPayment,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SatisfyGold,
                            contentColor = Color.Black
                        )
                    ) {
                        if (isProcessingPayment) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verifying Payment with Gateway...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay ₹${selectedPlan.priceInr.toInt()}.00 & Activate ${selectedPlan.planName}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SplitNode(
    title: String,
    amount: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(90.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(text = amount, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}
