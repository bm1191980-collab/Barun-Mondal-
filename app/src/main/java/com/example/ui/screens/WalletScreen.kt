package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.ReferralEntity
import com.example.data.model.UserProfile
import com.example.data.model.WalletEntity
import com.example.data.model.WalletTransactionEntity
import com.example.data.model.WithdrawalRequestEntity
import com.example.data.service.PaymentGatewayService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    userProfile: UserProfile = UserProfile(),
    wallet: WalletEntity? = null,
    transactions: List<WalletTransactionEntity> = emptyList(),
    referrals: List<ReferralEntity> = emptyList(),
    withdrawals: List<WithdrawalRequestEntity> = emptyList(),
    userReferralCode: String = userProfile.referralCode,
    isPro: Boolean = false,
    onBack: () -> Unit = {},
    onRequestWithdrawal: (amount: Double, method: String, details: String, holderName: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onNavigateToPro: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showWithdrawDialog by remember { mutableStateOf(false) }
    var withdrawAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("UPI") }
    var paymentDetails by remember { mutableStateOf("") }
    var accountHolderName by remember { mutableStateOf(userProfile.name) }
    var isSubmittingWithdrawal by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val effectiveReferralCode = userReferralCode.ifBlank { userProfile.referralCode.ifBlank { "SATISFY100" } }
    val referralCode = effectiveReferralCode
    val referralLink = "https://satisfy.app/ref/$referralCode"
    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    val availableBalance = wallet?.referralBalance ?: 0.0
    val totalEarned = wallet?.totalEarned ?: 0.0
    val totalWithdrawn = wallet?.totalWithdrawn ?: 0.0
    val pendingAmount = wallet?.pendingWithdrawalAmount ?: 0.0
    val successfulReferralsCount = wallet?.successfulReferralsCount ?: referrals.count { it.hasPurchasedPro }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Referral Earnings & Wallet", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF064E3B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = { TextButton(onClick = { snackbarMessage = null }) { Text("OK", color = SatisfyGold) } }
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
            // Header Balance Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF064E3B),
                                    Color(0xFF022C22),
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
                        Text(
                            text = "REFERRAL WALLET",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = Color(0xFFA7F3D0)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "₹${"%.2f".format(availableBalance)}",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        )

                        Text(
                            text = "Available Balance for Withdrawal",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Withdraw Action Button
                        Button(
                            onClick = {
                                if (availableBalance < PaymentGatewayService.MINIMUM_WITHDRAWAL_INR) {
                                    snackbarMessage = "Minimum withdrawal balance is ₹${PaymentGatewayService.MINIMUM_WITHDRAWAL_INR.toInt()}."
                                } else {
                                    showWithdrawDialog = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .shadow(6.dp, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.AccountBalance, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Withdraw Earnings", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }

            // Stats Metrics Row
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(
                        title = "Total Earned",
                        value = "₹${totalEarned.toInt()}",
                        icon = Icons.Filled.TrendingUp,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Pro Referrals",
                        value = "$successfulReferralsCount",
                        icon = Icons.Filled.People,
                        tint = SatisfyGold,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Withdrawn",
                        value = "₹${totalWithdrawn.toInt()}",
                        icon = Icons.Filled.CheckCircle,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.weight(1f)
                    )
                    if (pendingAmount > 0) {
                        MetricCard(
                            title = "Pending",
                            value = "₹${pendingAmount.toInt()}",
                            icon = Icons.Filled.HourglassEmpty,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Referral Link & Code Share Card
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
                                imageVector = Icons.Filled.CardGiftcard,
                                contentDescription = null,
                                tint = SatisfyGold,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "Invite Friends & Earn ₹4 Each",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "When a friend registers using your referral code and purchases PRO for ₹5, you instantly receive ₹4 in your Referral Wallet!",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Code Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.background)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Your Unique Referral Code", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = referralCode,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Referral Code", referralCode)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "Referral Code Copied: $referralCode", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Code", modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Join Satisfy App! Use my referral code $referralCode to get Pro access for just ₹5/month and unlock VIP creator features! Download: $referralLink"
                                            )
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Referral Link"))
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Share, contentDescription = "Share", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Transaction Ledger Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = "TRANSACTION LEDGER",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (transactions.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "No transactions yet. Share your referral code to start earning ₹4 per upgrade!",
                                modifier = Modifier.padding(20.dp),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            items(transactions) { txn ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (txn.type) {
                                            "REFERRAL_REWARD" -> Color(0xFF10B981).copy(alpha = 0.2f)
                                            "WITHDRAWAL_COMPLETED" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                            "WITHDRAWAL_REQUEST" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            else -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when (txn.type) {
                                        "REFERRAL_REWARD" -> Icons.Filled.ArrowDownward
                                        "WITHDRAWAL_COMPLETED" -> Icons.Filled.ArrowUpward
                                        "WITHDRAWAL_REQUEST" -> Icons.Filled.HourglassEmpty
                                        else -> Icons.Filled.Refresh
                                    },
                                    contentDescription = null,
                                    tint = when (txn.type) {
                                        "REFERRAL_REWARD" -> Color(0xFF10B981)
                                        "WITHDRAWAL_COMPLETED" -> Color(0xFF3B82F6)
                                        "WITHDRAWAL_REQUEST" -> Color(0xFFF59E0B)
                                        else -> Color(0xFFEF4444)
                                    },
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = txn.description,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${dateFormat.format(Date(txn.timestamp))} • ${txn.transactionId}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (txn.amount >= 0) "+₹${txn.amount.toInt()}" else "-₹${(-txn.amount).toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                color = if (txn.amount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Text(
                                text = "Bal: ₹${txn.balanceAfter.toInt()}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Withdrawal Request Bottom Sheet
        if (showWithdrawDialog) {
            ModalBottomSheet(
                onDismissRequest = { if (!isSubmittingWithdrawal) showWithdrawDialog = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Withdraw Referral Earnings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        text = "Available Balance: ₹${availableBalance.toInt()} (Min ₹${PaymentGatewayService.MINIMUM_WITHDRAWAL_INR.toInt()})",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider()

                    // Amount Input
                    OutlinedTextField(
                        value = withdrawAmount,
                        onValueChange = { withdrawAmount = it.filter { c -> c.isDigit() } },
                        label = { Text("Withdrawal Amount (₹)") },
                        placeholder = { Text("e.g. 40") },
                        singleLine = true,
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Method Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = paymentMethod == "UPI",
                            onClick = { paymentMethod = "UPI" },
                            label = { Text("UPI (GPay / PhonePe)") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = paymentMethod == "Bank Transfer",
                            onClick = { paymentMethod = "Bank Transfer" },
                            label = { Text("Bank Transfer / IMPS") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Payment details input
                    OutlinedTextField(
                        value = paymentDetails,
                        onValueChange = { paymentDetails = it },
                        label = { Text(if (paymentMethod == "UPI") "UPI ID (e.g. user@oksbi)" else "Account No & IFSC Code") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = accountHolderName,
                        onValueChange = { accountHolderName = it },
                        label = { Text("Beneficiary Account Holder Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val amountVal = withdrawAmount.toDoubleOrNull() ?: 0.0
                            if (amountVal < PaymentGatewayService.MINIMUM_WITHDRAWAL_INR) {
                                Toast.makeText(context, "Minimum withdrawal is ₹${PaymentGatewayService.MINIMUM_WITHDRAWAL_INR.toInt()}", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (amountVal > availableBalance) {
                                Toast.makeText(context, "Insufficient balance", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (paymentDetails.isBlank() || accountHolderName.isBlank()) {
                                Toast.makeText(context, "Please enter all withdrawal details", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            isSubmittingWithdrawal = true
                            onRequestWithdrawal(
                                amountVal,
                                paymentMethod,
                                paymentDetails,
                                accountHolderName
                            ) { success, msg ->
                                isSubmittingWithdrawal = false
                                showWithdrawDialog = false
                                snackbarMessage = msg
                            }
                        },
                        enabled = !isSubmittingWithdrawal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        if (isSubmittingWithdrawal) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                        } else {
                            Text("Confirm Withdrawal Request", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}
