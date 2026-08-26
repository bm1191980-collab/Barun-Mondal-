package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProSubscriptionEntity
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
    onPurchasePro: (referralCode: String?, paymentMethod: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onOpenWallet: () -> Unit = {},
    onNavigateToWallet: () -> Unit = onOpenWallet,
    onOpenOwnerChat: () -> Unit = {},
    onNavigateToChat: () -> Unit = onOpenOwnerChat,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCheckoutDialog by remember { mutableStateOf(false) }
    var referralCodeInput by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("UPI (Google Pay / PhonePe / Paytm)") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val effectiveSub = activeSubscription ?: subscription
    val effectiveIsPro = isPro || (effectiveSub != null && effectiveSub.expiresAt > System.currentTimeMillis())
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("PRO Membership", fontWeight = FontWeight.Bold) },
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
                                    Color(0xFF4C1D95), // Deep purple
                                    Color(0xFF2E1065),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Crown Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
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
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Satisfy PRO Membership",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "₹5 / Month • Premium Access • Direct Owner Chat",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SatisfyGoldLight,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Status Card (Active / Inactive)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPro) Color(0xFF064E3B).copy(alpha = 0.8f) else Color(0xFF1E1B4B).copy(alpha = 0.85f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isPro) Color(0xFF10B981) else SatisfyGold.copy(alpha = 0.5f)
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
                                        imageVector = if (isPro) Icons.Filled.CheckCircle else Icons.Filled.Stars,
                                        contentDescription = null,
                                        tint = if (isPro) Color(0xFF34D399) else SatisfyGold,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = if (isPro) "PRO Active" else "PRO Status: Free User",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color.White
                                        )
                                        if (isPro && activeSubscription != null) {
                                            Text(
                                                text = "Expires: ${dateFormat.format(Date(activeSubscription.expiresAt))}",
                                                fontSize = 12.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        } else {
                                            Text(
                                                text = "Upgrade for just ₹5/mo to unlock all benefits",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }

                                if (isPro) {
                                    OutlinedButton(
                                        onClick = { showCheckoutDialog = true },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Renew", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        if (!isPro) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCheckoutDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .shadow(8.dp, RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SatisfyGold,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Filled.Bolt, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Get PRO for ₹5 / Month",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // ₹5 Payment Distribution Flow Card
            item {
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
                                tint = SatisfyRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "₹5 Payment & Referral Split Workflow",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Step 1: User pays ₹5
                            SplitNode(
                                title = "User Pays",
                                amount = "₹5",
                                subtitle = "Pro Monthly",
                                icon = Icons.Filled.CreditCard,
                                color = Color(0xFF6366F1)
                            )

                            Icon(
                                imageVector = Icons.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )

                            // Step 2: Referrer gets ₹4
                            SplitNode(
                                title = "Referrer Gets",
                                amount = "₹4",
                                subtitle = "Instant Wallet",
                                icon = Icons.Filled.CardGiftcard,
                                color = Color(0xFF10B981)
                            )

                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )

                            // Step 3: Owner gets ₹1
                            SplitNode(
                                title = "App Owner",
                                amount = "₹1",
                                subtitle = "Net Revenue",
                                icon = Icons.Filled.AccountBalance,
                                color = Color(0xFFF59E0B)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "ℹ️ Note: Payment gateway charges & taxes, if applicable, are handled according to the selected payment gateway provider.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Pro User Benefits Grid
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "PRO USER BENEFITS",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val benefits = listOf(
                        Triple(
                            Icons.Filled.Forum,
                            "Direct Chat with App Owner",
                            "1-to-1 VIP priority messaging with the app creator/admin team."
                        ),
                        Triple(
                            Icons.Filled.Verified,
                            "PRO Gold Badge",
                            "Prominent verified PRO badge displayed on your profile & videos."
                        ),
                        Triple(
                            Icons.Filled.LockOpen,
                            "Access to Premium Content",
                            "Unlock full HD & exclusive videos designated as PRO / Premium."
                        ),
                        Triple(
                            Icons.Filled.AccountBalanceWallet,
                            "Referral & Earnings Hub",
                            "Earn ₹4 per referred friend who upgrades to Pro. Withdraw anytime."
                        ),
                        Triple(
                            Icons.Filled.Hd,
                            "Ultra 4K & High Bitrate",
                            "Smooth crystal 60fps playback with maximum visual clarity."
                        ),
                        Triple(
                            Icons.Filled.Headphones,
                            "Priority Creator Support",
                            "Direct verification requests and fast-track upload approvals."
                        )
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        benefits.forEach { (icon, title, desc) ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = desc,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
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
                                text = if (isPro) "Chat 1-on-1 Now" else "Pro VIP Feature",
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
                                text = "Earn ₹4 / Invite",
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
                                            text = "ID: ${sub.paymentId} • ${dateFormat.format(Date(sub.startedAt))}",
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
                                text = "Pro Membership Checkout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "₹5.00 for 30 Days Access",
                                fontSize = 13.sp,
                                color = SatisfyGold,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SatisfyGold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "30 DAYS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SatisfyGold
                            )
                        }
                    }

                    HorizontalDivider()

                    // Referral Code Input
                    OutlinedTextField(
                        value = referralCodeInput,
                        onValueChange = { referralCodeInput = it.uppercase() },
                        label = { Text("Got a Referral Code? (Optional)") },
                        placeholder = { Text("e.g. APP12345") },
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
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = selectedPaymentMethod == method,
                                        onClick = { selectedPaymentMethod = method }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = method, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    // Split transparency note
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = SatisfyGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "₹5 Payment Breakdown: ₹4 to Referrer + ₹1 to App Owner (Taxes/gateway charges apply as per provider).",
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
                            Text("Verifying Payment on Server...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Pay ₹5.00 Securely & Activate PRO", fontSize = 15.sp, fontWeight = FontWeight.Bold)
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
        Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = amount, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(text = subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
